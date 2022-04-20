package io.quarkiverse.dapr.core;

import java.util.List;
import java.util.Map;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.*;
import io.dapr.utils.TypeRef;

/**
 * SyncDaprClient
 *
 * @author naah69
 * @date 22022-04-01 17:42:02
 */
public class SyncDaprClient implements AutoCloseable {
    private final DaprClient daprClient;

    public SyncDaprClient() {
        daprClient = new DaprClientBuilder().build();
    }

    public SyncDaprClient(DaprClient daprClient) {
        this.daprClient = daprClient;
    }

    /**
     * Waits for the sidecar, giving up after timeout.
     *
     * @param timeoutInMilliseconds Timeout in milliseconds to wait for sidecar.
     * @return a Mono plan of type void.
     */
    public void waitForSidecar(int timeoutInMilliseconds) {
        daprClient.waitForSidecar(timeoutInMilliseconds).block();
    }

    /**
     * Publish an event.
     *
     * @param pubsubName the pubsub name we will publish the event to
     * @param topicName the topicName where the event will be published.
     * @param data the event's data to be published, use byte[] for skipping serialization.
     * @return a Mono plan of type void.
     */
    public void publishEvent(String pubsubName, String topicName, Object data) {
        daprClient.publishEvent(pubsubName, topicName, data).block();
    }

    /**
     * Publish an event.
     *
     * @param pubsubName the pubsub name we will publish the event to
     * @param topicName the topicName where the event will be published.
     * @param data the event's data to be published, use byte[] for skipping serialization.
     * @param metadata The metadata for the published event.
     * @return a Mono plan of type void.
     */
    public void publishEvent(String pubsubName, String topicName, Object data, Map<String, String> metadata) {
        daprClient.publishEvent(pubsubName, topicName, data, metadata).block();
    }

    /**
     * Publish an event.
     *
     * @param request the request for the publish event.
     * @return a Mono plan of a Dapr's void response.
     */
    void publishEvent(PublishEventRequest request) {
        daprClient.publishEvent(request).block();
    }

    /**
     * Invoke a service method, using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param data The data to be sent to invoke the service, use byte[] to skip serialization.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link io.dapr.client.domain.HttpExtension#NONE} otherwise.
     * @param metadata Metadata (in GRPC) or headers (in HTTP) to be sent in data.
     * @param type The Type needed as return for the call.
     * @param <T> The Type of the return, use byte[] to skip serialization.
     * @return A Mono Plan of type T.
     */
    public <T> T invokeMethod(String appId, String methodName, Object data, HttpExtension httpExtension,
            Map<String, String> metadata, TypeRef<T> type) {
        return daprClient.invokeMethod(appId, methodName, data, httpExtension, metadata, type).block();
    }

    /**
     * Invoke a service method, using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link HttpExtension#NONE} otherwise.
     * @param metadata Metadata (in GRPC) or headers (in HTTP) to be sent in request.
     * @param clazz The type needed as return for the call.
     * @param <T> The Type of the return, use byte[] to skip serialization.
     * @return A Mono Plan of type T.
     */
    public <T> T invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
            Map<String, String> metadata, Class<T> clazz) {
        return daprClient.invokeMethod(appId, methodName, request, httpExtension, metadata, clazz).block();
    }

    /**
     * Invoke a service method, using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link HttpExtension#NONE} otherwise.
     * @param type The Type needed as return for the call.
     * @param <T> The Type of the return, use byte[] to skip serialization.
     * @return A Mono Plan of type T.
     */
    public <T> T invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
            TypeRef<T> type) {
        return daprClient.invokeMethod(appId, methodName, request, httpExtension, type).block();
    }

    /**
     * Invoke a service method, using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link HttpExtension#NONE} otherwise.
     * @param clazz The type needed as return for the call.
     * @param <T> The Type of the return, use byte[] to skip serialization.
     * @return A Mono Plan of type T.
     */
    public <T> T invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
            Class<T> clazz) {
        return daprClient.invokeMethod(appId, methodName, request, httpExtension, clazz).block();
    }

    /**
     * Invoke a service method, using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link HttpExtension#NONE} otherwise.
     * @param metadata Metadata (in GRPC) or headers (in HTTP) to be sent in request.
     * @param type The Type needed as return for the call.
     * @param <T> The Type of the return, use byte[] to skip serialization.
     * @return A Mono Plan of type T.
     */
    public <T> T invokeMethod(String appId, String methodName, HttpExtension httpExtension, Map<String, String> metadata,
            TypeRef<T> type) {
        return daprClient.invokeMethod(appId, methodName, httpExtension, metadata, type).block();
    }

    /**
     * Invoke a service method, using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link HttpExtension#NONE} otherwise.
     * @param metadata Metadata (in GRPC) or headers (in HTTP) to be sent in request.
     * @param clazz The type needed as return for the call.
     * @param <T> The Type of the return, use byte[] to skip serialization.
     * @return A Mono Plan of type T.
     */
    public <T> T invokeMethod(String appId, String methodName, HttpExtension httpExtension, Map<String, String> metadata,
            Class<T> clazz) {
        return daprClient.invokeMethod(appId, methodName, httpExtension, metadata, clazz).block();
    }

    /**
     * Invoke a service method, using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link HttpExtension#NONE} otherwise.
     * @param metadata Metadata (in GRPC) or headers (in HTTP) to be sent in request.
     * @return A Mono Plan of type void.
     */
    public void invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension,
            Map<String, String> metadata) {
        daprClient.invokeMethod(appId, methodName, request, httpExtension, metadata).block();
    }

    /**
     * Invoke a service method, using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link HttpExtension#NONE} otherwise.
     * @return A Mono Plan of type void.
     */
    public void invokeMethod(String appId, String methodName, Object request, HttpExtension httpExtension) {
        daprClient.invokeMethod(appId, methodName, request, httpExtension).block();
    }

    /**
     * Invoke a service method, using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link HttpExtension#NONE} otherwise.
     * @param metadata Metadata (in GRPC) or headers (in HTTP) to be sent in request.
     * @return A Mono Plan of type void.
     */
    public void invokeMethod(String appId, String methodName, HttpExtension httpExtension, Map<String, String> metadata) {
        daprClient.invokeMethod(appId, methodName, httpExtension, metadata).block();
    }

    /**
     * Invoke a service method, without using serialization.
     *
     * @param appId The Application ID where the service is.
     * @param methodName The actual Method to be call in the application.
     * @param request The request to be sent to invoke the service, use byte[] to skip serialization.
     * @param httpExtension Additional fields that are needed if the receiving app is listening on
     *        HTTP, {@link HttpExtension#NONE} otherwise.
     * @param metadata Metadata (in GRPC) or headers (in HTTP) to be sent in request.
     * @return A Mono Plan of type byte[].
     */
    public byte[] invokeMethod(String appId, String methodName, byte[] request, HttpExtension httpExtension,
            Map<String, String> metadata) {
        return daprClient.invokeMethod(appId, methodName, request, httpExtension, metadata).block();
    }

    /**
     * Invoke a service method.
     *
     * @param invokeMethodRequest Request object.
     * @param type The Type needed as return for the call.
     * @param <T> The Type of the return, use byte[] to skip serialization.
     * @return A Mono Plan of type T.
     */
    public <T> T invokeMethod(InvokeMethodRequest invokeMethodRequest, TypeRef<T> type) {
        return daprClient.invokeMethod(invokeMethodRequest, type).block();
    }

    /**
     * Invokes a Binding operation.
     *
     * @param bindingName The bindingName of the biding to call.
     * @param operation The operation to be performed by the binding request processor.
     * @param data The data to be processed, use byte[] to skip serialization.
     * @return an empty Mono.
     */
    public void invokeBinding(String bindingName, String operation, Object data) {
        daprClient.invokeBinding(bindingName, operation, data).block();
    }

    /**
     * Invokes a Binding operation, skipping serialization.
     *
     * @param bindingName The name of the biding to call.
     * @param operation The operation to be performed by the binding request processor.
     * @param data The data to be processed, skipping serialization.
     * @param metadata The metadata map.
     * @return a Mono plan of type byte[].
     */
    public byte[] invokeBinding(String bindingName, String operation, byte[] data, Map<String, String> metadata) {
        return daprClient.invokeBinding(bindingName, operation, data, metadata).block();
    }

    /**
     * Invokes a Binding operation.
     *
     * @param bindingName The name of the biding to call.
     * @param operation The operation to be performed by the binding request processor.
     * @param data The data to be processed, use byte[] to skip serialization.
     * @param type The type being returned.
     * @param <T> The type of the return
     * @return a Mono plan of type T.
     */
    public <T> T invokeBinding(String bindingName, String operation, Object data, TypeRef<T> type) {
        return daprClient.invokeBinding(bindingName, operation, data, type).block();
    }

    /**
     * Invokes a Binding operation.
     *
     * @param bindingName The name of the biding to call.
     * @param operation The operation to be performed by the binding request processor.
     * @param data The data to be processed, use byte[] to skip serialization.
     * @param clazz The type being returned.
     * @param <T> The type of the return
     * @return a Mono plan of type T.
     */
    public <T> T invokeBinding(String bindingName, String operation, Object data, Class<T> clazz) {
        return daprClient.invokeBinding(bindingName, operation, data, clazz).block();
    }

    /**
     * Invokes a Binding operation.
     *
     * @param bindingName The name of the biding to call.
     * @param operation The operation to be performed by the binding request processor.
     * @param data The data to be processed, use byte[] to skip serialization.
     * @param metadata The metadata map.
     * @param type The type being returned.
     * @param <T> The type of the return
     * @return a Mono plan of type T.
     */
    public <T> T invokeBinding(String bindingName, String operation, Object data, Map<String, String> metadata,
            TypeRef<T> type) {
        return daprClient.invokeBinding(bindingName, operation, data, metadata, type).block();
    }

    /**
     * Invokes a Binding operation.
     *
     * @param bindingName The name of the biding to call.
     * @param operation The operation to be performed by the binding request processor.
     * @param data The data to be processed, use byte[] to skip serialization.
     * @param metadata The metadata map.
     * @param clazz The type being returned.
     * @param <T> The type of the return
     * @return a Mono plan of type T.
     */
    public <T> T invokeBinding(String bindingName, String operation, Object data, Map<String, String> metadata,
            Class<T> clazz) {
        return daprClient.invokeBinding(bindingName, operation, data, metadata, clazz).block();
    }

    /**
     * Invokes a Binding operation.
     *
     * @param request The binding invocation request.
     * @param type The type being returned.
     * @param <T> The type of the return
     * @return a Mono plan of type T.
     */
    public <T> T invokeBinding(InvokeBindingRequest request, TypeRef<T> type) {
        return daprClient.invokeBinding(request, type).block();
    }

    /**
     * Retrieve a State based on their key.
     *
     * @param storeName The name of the state store.
     * @param state State to be re-retrieved.
     * @param type The type of State needed as return.
     * @param <T> The type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> State<T> getState(String storeName, State<T> state, TypeRef<T> type) {
        return daprClient.getState(storeName, state, type).block();
    }

    /**
     * Retrieve a State based on their key.
     *
     * @param storeName The name of the state store.
     * @param state State to be re-retrieved.
     * @param clazz The type of State needed as return.
     * @param <T> The type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> State<T> getState(String storeName, State<T> state, Class<T> clazz) {
        return daprClient.getState(storeName, state, clazz).block();
    }

    /**
     * Retrieve a State based on their key.
     *
     * @param storeName The name of the state store.
     * @param key The key of the State to be retrieved.
     * @param type The type of State needed as return.
     * @param <T> The type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> State<T> getState(String storeName, String key, TypeRef<T> type) {
        return daprClient.getState(storeName, key, type).block();
    }

    /**
     * Retrieve a State based on their key.
     *
     * @param storeName The name of the state store.
     * @param key The key of the State to be retrieved.
     * @param clazz The type of State needed as return.
     * @param <T> The type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> State<T> getState(String storeName, String key, Class<T> clazz) {
        return daprClient.getState(storeName, key, clazz).block();
    }

    /**
     * Retrieve a State based on their key.
     *
     * @param storeName The name of the state store.
     * @param key The key of the State to be retrieved.
     * @param options Optional settings for retrieve operation.
     * @param type The Type of State needed as return.
     * @param <T> The Type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> State<T> getState(String storeName, String key, StateOptions options, TypeRef<T> type) {
        return daprClient.getState(storeName, key, options, type).block();
    }

    /**
     * Retrieve a State based on their key.
     *
     * @param storeName The name of the state store.
     * @param key The key of the State to be retrieved.
     * @param options Optional settings for retrieve operation.
     * @param clazz The Type of State needed as return.
     * @param <T> The Type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> State<T> getState(String storeName, String key, StateOptions options, Class<T> clazz) {
        return daprClient.getState(storeName, key, options, clazz).block();
    }

    /**
     * Retrieve a State based on their key.
     *
     * @param request The request to get state.
     * @param type The Type of State needed as return.
     * @param <T> The Type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> State<T> getState(GetStateRequest request, TypeRef<T> type) {
        return daprClient.getState(request, type).block();
    }

    /**
     * Retrieve bulk States based on their keys.
     *
     * @param storeName The name of the state store.
     * @param keys The keys of the State to be retrieved.
     * @param type The type of State needed as return.
     * @param <T> The type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> List<State<T>> getBulkState(String storeName, List<String> keys, TypeRef<T> type) {
        return daprClient.getBulkState(storeName, keys, type).block();
    }

    /**
     * Retrieve bulk States based on their keys.
     *
     * @param storeName The name of the state store.
     * @param keys The keys of the State to be retrieved.
     * @param clazz The type of State needed as return.
     * @param <T> The type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> List<State<T>> getBulkState(String storeName, List<String> keys, Class<T> clazz) {
        return daprClient.getBulkState(storeName, keys, clazz).block();
    }

    /**
     * Retrieve bulk States based on their keys.
     *
     * @param request The request to get state.
     * @param type The Type of State needed as return.
     * @param <T> The Type of the return.
     * @return A Mono Plan for the requested State.
     */
    public <T> List<State<T>> getBulkState(GetBulkStateRequest request, TypeRef<T> type) {
        return daprClient.getBulkState(request, type).block();
    }

    /**
     * Execute a transaction.
     *
     * @param storeName The name of the state store.
     * @param operations The operations to be performed.
     * @return a Mono plan of type void
     */
    public void executeStateTransaction(String storeName,
            List<TransactionalStateOperation<?>> operations) {
        daprClient.executeStateTransaction(storeName, operations).block();
    }

    /**
     * Execute a transaction.
     *
     * @param request Request to execute transaction.
     * @return a Mono plan of type Response void
     */
    public void executeStateTransaction(ExecuteStateTransactionRequest request) {
        daprClient.executeStateTransaction(request).block();
    }

    /**
     * Save/Update a list of states.
     *
     * @param storeName The name of the state store.
     * @param states The States to be saved.
     * @return a Mono plan of type void.
     */
    public void saveBulkState(String storeName, List<State<?>> states) {
        daprClient.saveBulkState(storeName, states).block();
    }

    /**
     * Save/Update a list of states.
     *
     * @param request Request to save states.
     * @return a Mono plan of type void.
     */
    public void saveBulkState(SaveStateRequest request) {
        daprClient.saveBulkState(request).block();
    }

    /**
     * Save/Update a state.
     *
     * @param storeName The name of the state store.
     * @param key The key of the state.
     * @param value The value of the state.
     * @return a Mono plan of type void.
     */
    public void saveState(String storeName, String key, Object value) {
        daprClient.saveState(storeName, key, value).block();
    }

    /**
     * Save/Update a state.
     *
     * @param storeName The name of the state store.
     * @param key The key of the state.
     * @param etag The etag to be used.
     * @param value The value of the state.
     * @param options The Options to use for each state.
     * @return a Mono plan of type void.
     */
    public void saveState(String storeName, String key, String etag, Object value, StateOptions options) {
        daprClient.saveState(storeName, key, etag, value, options).block();
    }

    /**
     * Delete a state.
     *
     * @param storeName The name of the state store.
     * @param key The key of the State to be removed.
     * @return a Mono plan of type void.
     */
    public void deleteState(String storeName, String key) {
        daprClient.deleteState(storeName, key).block();
    }

    /**
     * Delete a state.
     *
     * @param storeName The name of the state store.
     * @param key The key of the State to be removed.
     * @param etag Optional etag for conditional delete.
     * @param options Optional settings for state operation.
     * @return a Mono plan of type void.
     */
    public void deleteState(String storeName, String key, String etag, StateOptions options) {
        daprClient.deleteState(storeName, key, etag, options).block();
    }

    /**
     * Delete a state.
     *
     * @param request Request to delete a state.
     * @return a Mono plan of type void.
     */
    public void deleteState(DeleteStateRequest request) {
        daprClient.deleteState(request).block();
    }

    /**
     * Fetches a secret from the configured vault.
     *
     * @param storeName Name of vault component in Dapr.
     * @param secretName Secret to be fetched.
     * @param metadata Optional metadata.
     * @return Key-value pairs for the secret.
     */
    public Map<String, String> getSecret(String storeName, String secretName, Map<String, String> metadata) {
        return daprClient.getSecret(storeName, secretName, metadata).block();
    }

    /**
     * Fetches a secret from the configured vault.
     *
     * @param storeName Name of vault component in Dapr.
     * @param secretName Secret to be fetched.
     * @return Key-value pairs for the secret.
     */
    public Map<String, String> getSecret(String storeName, String secretName) {
        return daprClient.getSecret(storeName, secretName).block();
    }

    /**
     * Fetches a secret from the configured vault.
     *
     * @param request Request to fetch secret.
     * @return Key-value pairs for the secret.
     */
    public Map<String, String> getSecret(GetSecretRequest request) {
        return daprClient.getSecret(request).block();
    }

    /**
     * Fetches all secrets from the configured vault.
     *
     * @param storeName Name of vault component in Dapr.
     * @return Key-value pairs for all the secrets in the state store.
     */
    public Map<String, Map<String, String>> getBulkSecret(String storeName) {
        return daprClient.getBulkSecret(storeName).block();
    }

    /**
     * Fetches all secrets from the configured vault.
     *
     * @param storeName Name of vault component in Dapr.
     * @param metadata Optional metadata.
     * @return Key-value pairs for all the secrets in the state store.
     */
    public Map<String, Map<String, String>> getBulkSecret(String storeName, Map<String, String> metadata) {
        return daprClient.getBulkSecret(storeName, metadata).block();
    }

    /**
     * Fetches all secrets from the configured vault.
     *
     * @param request Request to fetch secret.
     * @return Key-value pairs for the secret.
     */
    public Map<String, Map<String, String>> getBulkSecret(GetBulkSecretRequest request) {
        return daprClient.getBulkSecret(request).block();
    }

    /**
     * Gracefully shutdown the dapr runtime.
     *
     * @return a Mono plan of type void.
     */
    public void shutdown() {
        daprClient.shutdown().block();
    }

    @Override
    public void close() throws Exception {
        daprClient.close();
    }
}
