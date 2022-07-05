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
import io.dapr.client.domain.CloudEvent;
import io.quarkiverse.dapr.core.SyncDaprClient;

@Path("/pubsub")
@ApplicationScoped
public class PubsubResource {
    private final AtomicInteger counter = new AtomicInteger(1);

    @Inject
    SyncDaprClient dapr;

    @GET
    public String hello() {
        return "Hello, this is quarkus-dapr demo app1";
    }

    @GET
    @Path("/trigger/topic1")
    public String triggerSendEvent2Topic1() {
        String content = counter.getAndIncrement() + "-app1";
        dapr.publishEvent("messagebus", "topic1", content.getBytes(StandardCharsets.UTF_8),
                new HashMap<>());
        System.out.println("App1 succeeds to send event to topic1 with content=" + content);

        return "App1 succeeds to send event to topic1 with content=" + content;
    }

    @POST
    @Path("/topic2")
    @Topic(name = "topic2", pubsubName = "messagebus")
    public String eventOnTopic2(String content) {
        System.out.println("App1 received event from topic2: content=" + content);

        return "App1 received event from topic2";
    }

    @POST
    @Path("/topic3")
    @Topic(name = "topic3", pubsubName = "messagebus")
    public String eventOnTopic3(CloudEvent<String> event) {
        String content = event.getData();
        System.out.println("App1 received event from topic3: content=" + content);

        return "App1 received event from topic3";
    }

    @POST
    @Path("/topic4")
    @Topic(name = "topic4", pubsubName = "messagebus")
    public String eventOnTopic4(CloudEvent<TestData> event) {
        TestData testData = event.getData();
        String content = testData.getContent();
        System.out.println("App1 received event from topic4: content=" + content);

        return "App1 received event from topic4";
    }

}
