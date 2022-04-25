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

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import io.dapr.Topic;

@Path("/dapr")
@ApplicationScoped
public class DaprResource {
    // add some rest methods here

    @GET
    public String hello() {
        return "Hello dapr";
    }

    @GET
    @Path("/1")
    @Topic(name = "test-topic1", pubsubName = "rocketmq")
    public String postTopic1() {
        return "Hello dapr";
    }

    @POST
    @Topic(name = "test-topic2", pubsubName = "rocketmq")
    public String postTopic2() {
        return "Hello dapr";
    }

    @POST
    @Path("/topic3")
    @Topic(name = "test-topic3", pubsubName = "rocketmq")
    public String postTopic3() {
        return "Hello dapr";
    }
}
