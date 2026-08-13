package io.quarkiverse.dapr.devui;

import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DaprDashboardRecorder {

    public void setComponents(List<DaprDashboardRPCService.DTOComponent> components) {
        DaprDashboardRPCService service = Arc.container().instance(DaprDashboardRPCService.class).get();
        if (service != null) {
            service.setComponents(components);
        }
    }
}
