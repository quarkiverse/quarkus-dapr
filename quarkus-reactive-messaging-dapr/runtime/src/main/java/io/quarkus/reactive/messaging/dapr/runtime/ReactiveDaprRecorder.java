package io.quarkus.reactive.messaging.dapr.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.reactive.messaging.dapr.ReactiveDaprHandler;
import io.quarkus.reactive.messaging.dapr.ReactiveDaprHandlerBean;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ReactiveDaprRecorder {
    public Handler<RoutingContext> createHttpHandler() {
        ReactiveDaprHandlerBean bean = Arc.container().instance(ReactiveDaprHandlerBean.class).get();
        return new ReactiveDaprHandler(bean);
    }
}
