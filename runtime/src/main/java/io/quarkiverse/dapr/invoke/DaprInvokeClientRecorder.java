package io.quarkiverse.dapr.invoke;

import java.util.function.Function;

import io.dapr.client.DaprClient;
import io.quarkiverse.dapr.config.ConfigUtils;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * DaprInvokeClientRecorder
 */
@Recorder
public class DaprInvokeClientRecorder {

    public Function<SyntheticCreationalContext<Object>, Object> createClient(String interfaceName, String appId) {
        return new Function<>() {
            @Override
            public Object apply(SyntheticCreationalContext<Object> context) {
                Class<?> clientInterface;
                try {
                    clientInterface = Class.forName(interfaceName, false, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Unable to load Dapr invoke client " + interfaceName, e);
                }
                String resolvedAppId = ConfigUtils.getConfigValueIfNecessary(appId, true);
                return DaprInvokeClientHandler.create(clientInterface, resolvedAppId,
                        context.getInjectedReference(DaprClient.class));
            }
        };
    }
}
