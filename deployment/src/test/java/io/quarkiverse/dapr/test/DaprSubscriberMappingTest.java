package io.quarkiverse.dapr.test;

import java.util.List;
import java.util.function.Consumer;

import io.quarkiverse.dapr.core.DaprRuntime;
import io.quarkiverse.dapr.core.DaprTopicSubscription;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.assertj.core.api.SoftAssertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import io.quarkiverse.dapr.deployment.DaprTopicBuildItem;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DaprSubscriberMappingTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DaprSubscriberMappingTest.class);

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder buildChainBuilder) {
                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        List<DaprTopicBuildItem> buildItems = context.consumeMulti(DaprTopicBuildItem.class);

                        for (DaprTopicBuildItem buildItem : buildItems) {
                            LOGGER.info("buildItem route: {}", buildItem.getRoute());
                        }
                    }
                });
            }
        };
    }

    @Path("/")
    @ApplicationScoped
    public static class Subscriber {

        @POST
        @Path("/events")
        @Topic(name = "sub", pubsubName = "pubsub")
        public Response subscribe(CloudEvent<String> event) {
            return Response.ok().build();
        }
    }

    @Test
    void testing() {
        DaprTopicSubscription[] daprTopicSubscriptions = DaprRuntime.getInstance().listSubscribedTopics();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(daprTopicSubscriptions).hasSize(1);
            softly.assertThat(daprTopicSubscriptions[0].getRoute()).isEqualTo("/events");
        });
    }
}
