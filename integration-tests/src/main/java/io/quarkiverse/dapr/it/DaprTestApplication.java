package io.quarkiverse.dapr.demo;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class DaprTestApplication {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
