package codestarts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog;
import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class QuarkusDaprCodestartsTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .languages(QuarkusCodestartCatalog.Language.JAVA)
            .setupStandaloneExtensionTest("io.quarkiverse.dapr:quarkus-dapr")
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.DaprResource");
    }

    @Test
    void testFile() throws Throwable {
        codestartTest
                .assertThatGeneratedFile(QuarkusCodestartCatalog.Language.JAVA,
                        "src/main/java/ilove/quark/us/DaprResource.java")
                .content()
                .contains("@Path");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}
