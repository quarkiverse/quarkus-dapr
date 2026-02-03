package io.quarkiverse.dapr.deployment;

import static io.quarkiverse.dapr.deployment.DaprProcessor.FEATURE;

import java.util.function.Supplier;

import io.dapr.config.Properties;
import io.quarkiverse.dapr.config.DaprDevServiceBuildTimeConfig;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;

public class DevServicesDaprProcessor {

    @BuildStep(onlyIfNot = { IsNormal.class })
    DevServicesResultBuildItem devServices(
            DaprDevServiceBuildTimeConfig config,
            LaunchModeBuildItem launchMode) {

        if (!config.enabled().orElse(false)) {
            return null;
        }
        return DevServicesResultBuildItem.owned()
                .serviceName(FEATURE)
                .feature(FEATURE)
                .startable(new Supplier<Startable>() {
                    @Override
                    public Startable get() {
                        return new DaprContainerStartable(config.daprdImage(),
                                launchMode.getLaunchMode());
                    }
                })
                .postStartHook(startable -> {
                    DaprContainerStartable daprContainerStartable = (DaprContainerStartable) startable;
                    System.setProperty(Properties.GRPC_PORT.getName(), Integer.toString(daprContainerStartable.getGrpcPort()));
                    System.setProperty(Properties.HTTP_PORT.getName(), Integer.toString(daprContainerStartable.getHttpPort()));
                })
                .build();
    }
}
