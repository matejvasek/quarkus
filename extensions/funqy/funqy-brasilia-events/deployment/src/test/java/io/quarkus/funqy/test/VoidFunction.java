package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.brasilia.BrasiliaEventMapping;
import io.smallrye.mutiny.Uni;

public class VoidFunction {

    @Funq("void-function1")
    @BrasiliaEventMapping(name = "Timer", version = "0.1", configJSON = "{ \"schedule\": \"*/1 * * * * *\" }")
    public Uni<String> voidFunction1() {
        System.out.println("Got event voidFunction1 ");
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Uni.createFrom().item(() -> "Hello World!!!");
    }

    @Funq("void-function2")
    public Uni<Void> voidFunction2() {
        System.out.println("Got event voidFunction2");
        return Uni.createFrom().failure(new RuntimeException());
    }
}
