package io.quarkiverse.dapr.devui;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.All;
import io.quarkus.runtime.annotations.JsonRpcDescription;

@Singleton
public class DaprDashboardRPCService {

    public static final String DAPR_DASHBOARD_WORKFLOW_URL = "quarkus.dapr.devservices.dashboard.url";

    private final List<DaprComponent> components;
    private String url;

    @Inject
    public DaprDashboardRPCService(@All List<DaprComponent> components) {
        this.components = List.copyOf(components);
    }

    @PostConstruct
    void init() {
        url = ConfigProvider.getConfig().getOptionalValue(DAPR_DASHBOARD_WORKFLOW_URL, String.class).orElse("");
    }

    @JsonRpcDescription("Get the Dapr Dashboard Workflow URL")
    public String getDashboardWorkflowUrl() {
        return url;
    }

    @JsonRpcDescription("Get the Dapr components discovered in the classpath")
    public List<DaprComponent> getComponents() {
        return components;
    }
}
