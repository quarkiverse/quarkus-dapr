package io.quarkus.reactive.messaging.dapr;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.ConnectorAttribute;
import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;

@Connector(DaprConnector.NAME)
@ApplicationScoped
@ConnectorAttribute(name = "pubsubName", type = "string", description = "Dapr PubSub name", direction = ConnectorAttribute.Direction.INCOMING_AND_OUTGOING, mandatory = true)
@ConnectorAttribute(name = "topic", type = "string", description = "PubSub topic", direction = ConnectorAttribute.Direction.INCOMING_AND_OUTGOING, mandatory = true)
public class DaprConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {

    public static final String NAME = "quarkus-dapr";

    @Inject
    ReactiveDaprHandlerBean handler;

    DaprClient daprClient = new DaprClientBuilder().build();

    @Override
    public PublisherBuilder<DaprMessage<?>> getPublisherBuilder(Config config) {
        DaprConnectorIncomingConfiguration incomingConfig = new DaprConnectorIncomingConfiguration(config);
        Multi<DaprMessage<?>> processor = handler.getProcessor(incomingConfig);
        return ReactiveStreams.fromPublisher(AdaptersToReactiveStreams.publisher(processor));
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(Config config) {
        DaprConnectorOutgoingConfiguration outgoingConfig = new DaprConnectorOutgoingConfiguration(
                config);
        return new DaprSink(this.daprClient, outgoingConfig.getPubsubName(), outgoingConfig.getTopic())
                .sink();
    }
}
