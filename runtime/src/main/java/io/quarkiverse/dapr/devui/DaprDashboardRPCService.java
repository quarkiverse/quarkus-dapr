package io.quarkiverse.dapr.devui;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.JsonRpcDescription;

@Singleton
public class DaprDashboardRPCService {

    public static final String DAPR_DASHBOARD_WORKFLOW_URL = "quarkus.dapr.devservices.dashboard.url";
    private String url;
    private List<DTOComponent> components = Collections.emptyList();

    @PostConstruct
    void init() {
        url = ConfigProvider.getConfig().getOptionalValue(DAPR_DASHBOARD_WORKFLOW_URL, String.class).orElse("");
    }

    @JsonRpcDescription("Get the Dapr Dashboard Workflow URL")
    public String getDashboardWorkflowUrl() {
        return url;
    }

    public void setComponents(List<DTOComponent> components) {
        this.components = components;
    }

    @JsonRpcDescription("Get the discovered Dapr components")
    public List<DTOComponent> getComponents() {
        return components;
    }

    public static class DTOComponent {
        public String name;
        public String type;
        public String version;
        public Map<String, String> metadata;

        public DTOComponent() {
        }

        public DTOComponent(String name, String type, String version, Map<String, String> metadata) {
            this.name = name;
            this.type = type;
            this.version = version;
            this.metadata = metadata;
        }
    }
}
