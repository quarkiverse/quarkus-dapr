package io.quarkiverse.dapr.invoke;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprHttp;
import io.dapr.client.domain.HttpExtension;
import io.dapr.utils.TypeRef;
import io.smallrye.mutiny.Uni;
import reactor.core.publisher.Mono;

/**
 * {@link InvocationHandler} backing the interfaces annotated with {@link RegisterDaprInvokeClient}. Each interface
 * method is translated into a Dapr service invocation call.
 */
public class DaprInvokeClientHandler implements InvocationHandler {

    private static final Pattern PATH_TEMPLATE = Pattern.compile("\\{\\s*([^}:\\s]+)\\s*(?::[^}]*)?}");

    private final Class<?> clientInterface;
    private final String appId;
    private final DaprClient daprClient;
    private final Map<Method, MethodMetadata> methods = new HashMap<>();
    private final Map<Method, MethodHandle> defaultMethods = new ConcurrentHashMap<>();

    DaprInvokeClientHandler(Class<?> clientInterface, String appId, DaprClient daprClient) {
        this.clientInterface = clientInterface;
        this.appId = appId;
        this.daprClient = daprClient;
        String basePath = pathOf(clientInterface.getAnnotation(Path.class));
        for (Method method : clientInterface.getMethods()) {
            if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            methods.put(method, new MethodMetadata(method, basePath));
        }
    }

    /**
     * Creates a proxy instance of the given client interface.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> clientInterface, String appId, DaprClient daprClient) {
        if (!clientInterface.isInterface()) {
            throw new IllegalArgumentException(
                    "@RegisterDaprInvokeClient can only be used on interfaces: " + clientInterface.getName());
        }
        return (T) Proxy.newProxyInstance(clientInterface.getClassLoader(), new Class<?>[] { clientInterface },
                new DaprInvokeClientHandler(clientInterface, appId, daprClient));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return clientInterface.getName() + "[appId=" + appId + "]";
                default:
                    throw new UnsupportedOperationException(method.toString());
            }
        }
        if (method.isDefault()) {
            return defaultMethodHandle(method).bindTo(proxy).invokeWithArguments(args == null ? new Object[0] : args);
        }
        MethodMetadata metadata = methods.get(method);
        if (metadata == null) {
            throw new UnsupportedOperationException(method.toString());
        }
        return metadata.invoke(args == null ? new Object[0] : args);
    }

    private MethodHandle defaultMethodHandle(Method method) throws IllegalAccessException {
        MethodHandle handle = defaultMethods.get(method);
        if (handle == null) {
            Class<?> declaringClass = method.getDeclaringClass();
            handle = MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup())
                    .unreflectSpecial(method, declaringClass);
            defaultMethods.put(method, handle);
        }
        return handle;
    }

    private static String pathOf(Path path) {
        return path == null ? "" : path.value();
    }

    private static String joinPaths(String basePath, String methodPath) {
        String path = (basePath + "/" + methodPath).replaceAll("/{2,}", "/");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static String encodePathSegment(Object value) {
        return URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private enum ResultKind {
        BLOCKING,
        MONO,
        UNI,
        COMPLETION_STAGE
    }

    private enum ParameterKind {
        PATH,
        QUERY,
        HEADER,
        BODY
    }

    private final class MethodMetadata {

        private final Method method;
        private final String pathTemplate;
        private final DaprHttp.HttpMethods httpMethod;
        private final ParameterKind[] parameterKinds;
        private final String[] parameterNames;
        private final ResultKind resultKind;
        private final TypeRef<?> resultType;

        MethodMetadata(Method method, String basePath) {
            this.method = method;
            this.pathTemplate = joinPaths(basePath, pathOf(method.getAnnotation(Path.class)));
            this.httpMethod = httpMethodOf(method);

            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            this.parameterKinds = new ParameterKind[parameterAnnotations.length];
            this.parameterNames = new String[parameterAnnotations.length];
            boolean hasBody = false;
            for (int i = 0; i < parameterAnnotations.length; i++) {
                parameterKinds[i] = ParameterKind.BODY;
                for (Annotation annotation : parameterAnnotations[i]) {
                    if (annotation instanceof PathParam) {
                        parameterKinds[i] = ParameterKind.PATH;
                        parameterNames[i] = ((PathParam) annotation).value();
                    } else if (annotation instanceof QueryParam) {
                        parameterKinds[i] = ParameterKind.QUERY;
                        parameterNames[i] = ((QueryParam) annotation).value();
                    } else if (annotation instanceof HeaderParam) {
                        parameterKinds[i] = ParameterKind.HEADER;
                        parameterNames[i] = ((HeaderParam) annotation).value();
                    }
                }
                if (parameterKinds[i] == ParameterKind.BODY) {
                    if (hasBody) {
                        throw new IllegalStateException("Method " + method + " of Dapr invoke client "
                                + clientInterface.getName()
                                + " declares more than one body parameter; annotate the other parameters with"
                                + " @PathParam, @QueryParam or @HeaderParam");
                    }
                    hasBody = true;
                }
            }

            Class<?> returnType = method.getReturnType();
            Type genericReturnType = method.getGenericReturnType();
            if (Mono.class.equals(returnType)) {
                this.resultKind = ResultKind.MONO;
                this.resultType = TypeRef.get(firstTypeArgument(genericReturnType));
            } else if (Uni.class.equals(returnType)) {
                this.resultKind = ResultKind.UNI;
                this.resultType = TypeRef.get(firstTypeArgument(genericReturnType));
            } else if (CompletionStage.class.equals(returnType) || CompletableFuture.class.equals(returnType)) {
                this.resultKind = ResultKind.COMPLETION_STAGE;
                this.resultType = TypeRef.get(firstTypeArgument(genericReturnType));
            } else {
                this.resultKind = ResultKind.BLOCKING;
                this.resultType = void.class.equals(returnType) ? TypeRef.VOID : TypeRef.get(genericReturnType);
            }
        }

        Object invoke(Object[] args) {
            String path = pathTemplate;
            Map<String, List<String>> queryParams = new LinkedHashMap<>();
            Map<String, String> headers = new LinkedHashMap<>();
            Map<String, Object> pathParams = new HashMap<>();
            Object body = null;
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                switch (parameterKinds[i]) {
                    case PATH:
                        pathParams.put(parameterNames[i], arg);
                        break;
                    case QUERY:
                        addQueryParam(queryParams, parameterNames[i], arg);
                        break;
                    case HEADER:
                        if (arg != null) {
                            headers.put(parameterNames[i], String.valueOf(arg));
                        }
                        break;
                    default:
                        body = arg;
                }
            }
            if (!pathParams.isEmpty()) {
                path = resolvePath(path, pathParams);
            }

            HttpExtension httpExtension = new HttpExtension(httpMethod, queryParams, headers);
            Mono<?> result = daprClient.invokeMethod(appId, path, body, httpExtension, Collections.emptyMap(),
                    resultType);
            switch (resultKind) {
                case MONO:
                    return result;
                case UNI:
                    return Uni.createFrom().completionStage(result::toFuture);
                case COMPLETION_STAGE:
                    return result.toFuture();
                default:
                    return result.block();
            }
        }

        private String resolvePath(String template, Map<String, Object> pathParams) {
            Matcher matcher = PATH_TEMPLATE.matcher(template);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String name = matcher.group(1);
                Object value = pathParams.get(name);
                if (value == null) {
                    throw new IllegalArgumentException("No value provided for path parameter '" + name + "' of method "
                            + method);
                }
                matcher.appendReplacement(sb, Matcher.quoteReplacement(encodePathSegment(value)));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }

        private void addQueryParam(Map<String, List<String>> queryParams, String name, Object value) {
            if (value == null) {
                return;
            }
            List<String> values = queryParams.computeIfAbsent(name, k -> new ArrayList<>());
            if (value instanceof Collection) {
                for (Object v : (Collection<?>) value) {
                    values.add(String.valueOf(v));
                }
            } else if (value instanceof Object[]) {
                for (Object v : (Object[]) value) {
                    values.add(String.valueOf(v));
                }
            } else {
                values.add(String.valueOf(value));
            }
        }

        private DaprHttp.HttpMethods httpMethodOf(Method method) {
            for (Annotation annotation : method.getAnnotations()) {
                HttpMethod httpMethod = annotation.annotationType().getAnnotation(HttpMethod.class);
                if (httpMethod != null) {
                    try {
                        return DaprHttp.HttpMethods.valueOf(httpMethod.value());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Unsupported HTTP method " + httpMethod.value() + " on method "
                                + method + " of Dapr invoke client " + clientInterface.getName(), e);
                    }
                }
            }
            return DaprHttp.HttpMethods.POST;
        }

        private Type firstTypeArgument(Type type) {
            if (type instanceof ParameterizedType) {
                return ((ParameterizedType) type).getActualTypeArguments()[0];
            }
            throw new IllegalStateException("Return type of method " + method + " of Dapr invoke client "
                    + clientInterface.getName() + " must declare its type argument");
        }
    }
}
