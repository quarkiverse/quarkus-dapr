package io.quarkiverse.dapr.workflows.rest;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "http://localhost:9999")
public interface WorkflowRestClient {

    @POST
    @Path("/users")
    Response create(CreateUserRequest createUserRequest);
}
