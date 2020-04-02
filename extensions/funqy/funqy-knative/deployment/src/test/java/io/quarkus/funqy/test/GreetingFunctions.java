package io.quarkus.funqy.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import io.quarkus.funqy.Funq;

public class GreetingFunctions {
    @Inject
    GreetingService service;

    @Funq
    public CompletionStage greet(Identity name) {
        String message = service.hello(name.getName());
        Greeting greeting = new Greeting();
        greeting.setMessage(message);
        greeting.setName(name.getName());
        return CompletableFuture.completedFuture(greeting);
    }

}
