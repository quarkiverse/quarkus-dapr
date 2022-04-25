package io.quarkiverse.dapr.it;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class App2Application {
    public static void main(String... args) {
        Quarkus.run(args);
    }
}
