package io.quarkiverse.dapr.invoke;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import io.dapr.config.Properties;

/**
 * Registered on every {@link RegisterDaprInvokeClient} REST Client to authenticate against the Dapr sidecar when an
 * API token is configured ({@code DAPR_API_TOKEN}), as the Dapr SDK clients do.
 */
public class DaprInvokeClientRequestFilter implements ClientRequestFilter {

    private static final String API_TOKEN_HEADER = "dapr-api-token";

    @Override
    public void filter(ClientRequestContext requestContext) {
        String apiToken = Properties.API_TOKEN.get();
        if (apiToken != null && !apiToken.isBlank() && !requestContext.getHeaders().containsKey(API_TOKEN_HEADER)) {
            requestContext.getHeaders().putSingle(API_TOKEN_HEADER, apiToken);
        }
    }
}
