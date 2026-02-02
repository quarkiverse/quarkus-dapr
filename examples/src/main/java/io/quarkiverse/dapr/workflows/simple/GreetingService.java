package io.quarkiverse.dapr.workflows.simple;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GreetingService {

    public String saySomething() {
        return "Hello, how are you?";
    }
}
