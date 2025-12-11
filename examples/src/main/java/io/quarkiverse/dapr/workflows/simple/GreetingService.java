package io.quarkiverse.dapr.workflows.simple;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.logging.Log;

@ApplicationScoped
public class GreetingService {

    public void sayHello() {
        Log.info("hello world");
    }
}
