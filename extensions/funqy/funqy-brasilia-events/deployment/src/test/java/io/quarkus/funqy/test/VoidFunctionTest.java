package io.quarkus.funqy.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class VoidFunctionTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("trigger.properties", "application.properties")
                    .addClasses(VoidFunction.class));

    @Test
    public void noOpTest() throws InterruptedException {
        Thread.sleep(10_000);
    }

}
