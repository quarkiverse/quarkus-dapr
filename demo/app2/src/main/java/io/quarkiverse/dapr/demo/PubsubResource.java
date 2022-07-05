/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.dapr.demo;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.dapr.Topic;
import io.quarkiverse.dapr.core.SyncDaprClient;

@Path("/pubsub")
@ApplicationScoped
public class PubsubResource {
    private final AtomicInteger counter = new AtomicInteger(1);

    @Inject
    SyncDaprClient dapr;

    @GET
    public String hello() {
        return "Hello, this is quarkus-dapr demo app2";
    }

    @POST
    @Path("/topic1")
    @Topic(name = "topic1", pubsubName = "messagebus")
    public String eventOnTopic1(String content) {
        System.out.println("App2 received event from topic1: content=" + content);

        content = "content" + "-app2";
        dapr.publishEvent("messagebus", "topic2", content.getBytes(StandardCharsets.UTF_8),
                new HashMap<>());
        System.out.println("App1 sent event to topic2 with content=" + content);

        content = "content" + "-app3";
        dapr.publishEvent("messagebus", "topic3", content.getBytes(StandardCharsets.UTF_8),
                new HashMap<>());
        System.out.println("App1 sent event to topic3 with content=" + content);

        content = "content" + "-app4";
        TestData testData = new TestData();
        testData.setContent(content);
        dapr.publishEvent("messagebus", "topic4", testData,
                new HashMap<>());
        System.out.println("App1 sent event to topic4 with object content=" + content);

        return "App2 received event from topic1";
    }

}
