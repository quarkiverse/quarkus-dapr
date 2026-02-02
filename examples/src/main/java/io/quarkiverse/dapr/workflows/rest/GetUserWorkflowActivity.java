package io.quarkiverse.dapr.workflows.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;

@ApplicationScoped
public class GetUserWorkflowActivity implements WorkflowActivity {

    @Inject
    @RestClient
    UsersRestClient usersRestClient;

    @Override
    public Object run(WorkflowActivityContext ctx) {

        String userId = ctx.getInput(String.class);

        Response response = usersRestClient.getByID(userId);

        return response.readEntity(User.class);
    }
}
