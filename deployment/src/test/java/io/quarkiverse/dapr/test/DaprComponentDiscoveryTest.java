package io.quarkiverse.dapr.test;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.dapr.devui.DaprComponent;
import io.quarkiverse.dapr.devui.DaprDashboardRPCService;
import io.quarkus.test.QuarkusUnitTest;

public class DaprComponentDiscoveryTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(DaprDashboardRPCService.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsResource("test-components/statestore.yaml", "components/statestore.yaml"))
            .overrideConfigKey("quarkus.dapr.devservices.enabled", "false");

    @Inject
    DaprDashboardRPCService rpcService;

    @Test
    public void testComponentDiscovery() {
        List<DaprComponent> components = rpcService.getComponents();
        Assertions.assertEquals(1, components.size(), "Exactly one component should be discovered");

        DaprComponent component = components.get(0);
        Assertions.assertEquals("statestore", component.getName());
        Assertions.assertEquals("state.redis", component.getType());
        Assertions.assertEquals("v1", component.getVersion());
        Assertions.assertEquals("localhost:6379", component.getMetadata().get("redisHost"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> components.add(component),
                "Components list should be immutable");
    }
}
