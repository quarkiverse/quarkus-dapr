package org.acme;

import jakarta.enterprise.context.ApplicationScoped;

@QuarkusMain(name = "quarkus-dapr")
public class Dapr implements QuarkusApplication {

    public String hello() {
        return "My Example Hello Quarkus-Dapr Codestart";
    }
}