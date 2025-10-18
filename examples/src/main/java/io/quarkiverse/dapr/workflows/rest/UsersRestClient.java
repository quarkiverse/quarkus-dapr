package io.quarkiverse.dapr.workflows.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/users")
@RegisterRestClient(configKey = "users-api")
public interface UsersRestClient {

    @GET
    @Path("/{userId}")
    Response getByID(@PathParam("userId") String id);
}
