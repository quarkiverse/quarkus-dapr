package io.quarkiverse.dapr.workflows.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkus.logging.Log;

@ApplicationScoped
public class DoRequestWorkflowActivity implements WorkflowActivity {

    @RestClient
    WorkflowRestClient restClient;

    @Override
    public Object run(WorkflowActivityContext workflowActivityContext) {

        CreateUserRequest input = workflowActivityContext.getInput(CreateUserRequest.class);

        try {
            Response response = restClient.create(input);
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                return "ERROR";
            }
            return "CREATED";
        } catch (Exception e) {
            Log.error(e);
            return "REST_CLIENT_ERROR";
        }
    }
}
