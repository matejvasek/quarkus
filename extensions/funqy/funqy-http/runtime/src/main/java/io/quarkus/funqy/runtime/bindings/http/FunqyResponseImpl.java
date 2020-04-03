package io.quarkus.funqy.runtime.bindings.http;

import io.quarkus.funqy.runtime.FunqyServerResponse;

import java.util.concurrent.CompletionStage;

public class FunqyResponseImpl implements FunqyServerResponse {
    protected CompletionStage<?> output;

    @Override
    public CompletionStage<?> getOutput() {
        return output;
    }

    @Override
    public void setOutput(CompletionStage<?> output) {
        this.output = output;
    }
}
