package io.quarkiverse.dapr.workflows.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import io.quarkus.logging.Log;

@ApplicationScoped
public class GetUserInfoWorkflowActivity implements WorkflowActivity {

    @Inject
    @RestClient
    UsersRestClient users;

    @Override
    public Object run(WorkflowActivityContext workflowActivityContext) {

        String userId = workflowActivityContext.getInput(String.class);

        Response response = users.getByID(userId);

        User user = response.readEntity(User.class);

        Log.info("User instance:" + user);

        return user;
    }
}
