package io.quarkiverse.dapr.deployment;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.jandex.DotName;

import io.dapr.Topic;

public class DotNames {

    public static final DotName TOPIC_DOTNAME = DotName.createSimple(Topic.class);
    public static final DotName RESTEASY_PATH_DOTNAME = DotName.createSimple(Path.class);
    public static final DotName RESTEASY_POST_DOTNAME = DotName.createSimple(POST.class);

}
