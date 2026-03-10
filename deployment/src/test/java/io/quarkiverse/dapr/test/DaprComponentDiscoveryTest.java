package io.quarkiverse.dapr.test;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.dapr.devui.DaprDashboardRPCService;
import io.quarkus.test.QuarkusUnitTest;

public class DaprComponentDiscoveryTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(DaprDashboardRPCService.class)
                    .addClass(DaprDashboardRPCService.DTOComponent.class)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addAsResource("components/statestore.yaml", "components/statestore.yaml"))
            .overrideConfigKey("quarkus.dapr.devservices.enabled", "false");

    @Inject
    DaprDashboardRPCService rpcService;

    @Test
    public void testComponentDiscovery() {
        List<DaprDashboardRPCService.DTOComponent> components = rpcService.getComponents();
        Assertions.assertNotNull(components);
        Assertions.assertFalse(components.isEmpty(), "Components list should not be empty");

        boolean found = components.stream().anyMatch(c -> "statestore".equals(c.name) && "state.redis".equals(c.type));
        Assertions.assertTrue(found, "Discovered components should include 'statestore' of type 'state.redis'");
    }
}
