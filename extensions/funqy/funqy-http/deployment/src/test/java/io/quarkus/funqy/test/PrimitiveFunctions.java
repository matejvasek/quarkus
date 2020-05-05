package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.smallrye.mutiny.Uni;

public class PrimitiveFunctions {
    @Funq
    public String toLowerCase(String val) {
        return val.toLowerCase();
    }

    @Funq
    public Uni<String> toLowerCaseAsync(String val) {
        return Uni.createFrom().item(val.toLowerCase());
    }

    @Funq
    public int doubleIt(int val) {
        return val * 2;
    }

    @Funq
    public Uni<Integer> doubleItAsync(int val) {
        return Uni.createFrom().item(val * 2);
    }

}
