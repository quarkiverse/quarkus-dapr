package io.quarkiverse.dapr.demo;

import java.util.List;
import java.util.Map;

public class Subscription {

    private String pubsubName;
    private String topic;
    private String route;
    private Routes routes;
    private Map<String, String> metadata;

    public String getPubsubName() {
        return pubsubName;
    }

    public void setPubsubName(String pubsubName) {
        this.pubsubName = pubsubName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public Routes getRoutes() {
        return routes;
    }

    public void setRoutes(Routes routes) {
        this.routes = routes;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public static class Routes {
        private List<Rule> rules;

        public List<Rule> getRules() {
            return rules;
        }

        public void setRules(List<Rule> rules) {
            this.rules = rules;
        }
    }

    public static class Rule {
        private String match;
        private String path;

        public String getMatch() {
            return match;
        }

        public void setMatch(String match) {
            this.match = match;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
