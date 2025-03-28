package ilove.quark.us;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain(name = "quarkus-dapr")
public class Dapr implements QuarkusApplication {

    @Override public int run(String... args) throws Exception {
        System.out.println("Hello quarkus-dapr codestart");
        return 0;
    }
}