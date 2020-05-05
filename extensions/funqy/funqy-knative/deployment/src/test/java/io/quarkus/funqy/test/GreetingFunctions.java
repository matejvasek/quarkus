package io.quarkus.funqy.test;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

import io.quarkus.funqy.Funq;
import io.smallrye.mutiny.Uni;

public class GreetingFunctions {
    @Inject
    GreetingService service;

    @Funq
    public Greeting greet(Identity name) {
        if (name == null) {
            throw new IllegalArgumentException("Identity cannot be null.");
        }
        String message = service.hello(name.getName());
        Greeting greeting = new Greeting();
        greeting.setMessage(message);
        greeting.setName(name.getName());
        return greeting;
    }

    @Funq
    public Uni<Greeting> greetAsync(Identity name) {
        if (name == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Identity cannot be null."));
        }
        return Uni.createFrom().emitter(emitter -> {

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String message = service.hello(name.getName());
                    Greeting greeting = new Greeting();
                    greeting.setMessage(message);
                    greeting.setName(name.getName());
                    emitter.complete(greeting);
                    timer.cancel();
                }
            }, Duration.ofMillis(1).toMillis());
        });
    }

}
