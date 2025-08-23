package io.quarkus.reactive.messaging.dapr.config;

import static java.util.regex.Pattern.quote;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.reactive.messaging.dapr.DaprConfig;
import io.quarkus.reactive.messaging.dapr.DaprConnector;

@Singleton
public class ConfigReader {

    private static final String CONNECTOR = ".connector";

    private static final String MP_MSG_IN = "mp.messaging.incoming.";
    private static final String IN_KEY = "mp.messaging.incoming.%s.%s";
    private static final Pattern IN_PATTERN = Pattern.compile(quote(MP_MSG_IN) + "[^.]+" + quote(CONNECTOR));

    private static final String MP_MSG_OUT = "mp.messaging.outgoing.";
    private static final String OUT_KEY = "mp.messaging.outgoing.%s.%s";
    private static final Pattern OUT_PATTERN = Pattern.compile(quote(MP_MSG_OUT) + "[^.]+" + quote(CONNECTOR));

    private List<DaprConfig> configs;

    @PostConstruct
    void init() {
        configs = readIncomingHttpConfigs();
    }

    public List<DaprConfig> getConfigs() {
        return configs;
    }

    /**
     * Reads Dapr config, can be used in the build time
     *
     * @return list of Dapr configurations
     */
    public static List<DaprConfig> readIncomingHttpConfigs() {
        List<DaprConfig> daprConfigs = new ArrayList<>();
        Config config = ConfigProviderResolver.instance().getConfig();
        for (String propertyName : config.getPropertyNames()) {
            String connectorName = getConnectorNameIfMatching(IN_PATTERN, propertyName, IN_KEY, MP_MSG_IN,
                    DaprConnector.NAME);
            if (connectorName != null) {
                String pubsubName = getConfigProperty(IN_KEY, connectorName, "pubsubName", String.class);
                String topic = getConfigProperty(IN_KEY, connectorName, "topic", String.class);
                daprConfigs.add(new DaprConfig(pubsubName, topic));
            }
        }
        return daprConfigs;
    }

    private static String getConnectorNameIfMatching(Pattern connectorPropertyPattern,
            String propertyName, String format, String prefix, String expectedConnectorType) {
        Matcher matcher = connectorPropertyPattern.matcher(propertyName);
        if (matcher.matches()) {
            String connectorName = propertyName.substring(prefix.length(), propertyName.length() - CONNECTOR.length());
            String connectorType = getConfigProperty(format, connectorName, "connector", String.class);
            boolean matches = expectedConnectorType.equals(connectorType);
            return matches ? connectorName : null;
        } else {
            return null;
        }
    }

    private static <T> T getConfigProperty(String format, String connectorName, String property, Class<T> type) {
        String key = String.format(format, connectorName, property);
        return ConfigProvider.getConfig().getOptionalValue(key, type)
                .orElseThrow(() -> noPropertyFound(connectorName, property));
    }

    private static IllegalStateException noPropertyFound(String key, String propertyName) {
        String message = String.format("No %s defined for reactive dapr connector '%s'", propertyName, key);
        return new IllegalStateException(message);
    }
}
