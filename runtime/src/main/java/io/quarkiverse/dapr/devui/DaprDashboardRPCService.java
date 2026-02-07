package io.quarkiverse.dapr.devui;

import jakarta.annotation.PostConstruct;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.JsonRpcDescription;

public class DaprDashboardRPCService {

    public static final String DAPR_DASHBOARD_WORKFLOW_URL = "quarkus.dapr.devservices.dashboard.url";
    private String url;

    @PostConstruct
    void init() {
        url = ConfigProvider.getConfig().getValue(DAPR_DASHBOARD_WORKFLOW_URL, String.class);
    }

    @JsonRpcDescription("Get the Dapr Dashboard Workflow URL")
    public String getDashboardWorkflowUrl() {
        return url;
    }
}
