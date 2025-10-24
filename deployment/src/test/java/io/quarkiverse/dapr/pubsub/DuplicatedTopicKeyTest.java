package io.quarkiverse.dapr.pubsub;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.dapr.Topic;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DuplicatedTopicKeyTest {

    @RegisterExtension
    static final QuarkusDevModeTest devModeTest = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Subscriber.class));

    @Test
    void shouldNotHaveDuplicatedTopicKeys() {
        RestAssured.given()
                .get("/dapr/subscribe")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo(
                        "[{\"pubsubName\":\"mypubsub\",\"topic\":\"mytopic\",\"route\":\"/subscriber\",\"metadata\":{}}]"));
    }

    @Path("/subscriber")
    public static class Subscriber {

        @POST
        @Topic(name = "mytopic", pubsubName = "mypubsub")
        public void handleMyTopic(String message) {
            // Handle the message
        }

        @POST
        @Path("/duplicate")
        @Topic(name = "mytopic", pubsubName = "mypubsub")
        public void handleMyTopicDuplicate(String message) {
            // This is a duplicate topic key and should cause a startup failure
        }

    }
}
