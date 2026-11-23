package io.quarkiverse.dapr.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.dapr.config.DaprRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

public class DaprRuntimeConfigDefaultsTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.dapr.devservices.enabled", "false");

    @Inject
    DaprRuntimeConfig config;

    @Test
    void shouldNotOverrideSdkPropertiesWhenNothingIsConfigured() {
        // the Dapr SDK keeps resolving DAPR_* environment variables and dapr.* system properties
        assertThat(DaprRuntimeConfig.toPropertyOverrides(config)).isEmpty();
    }
}
