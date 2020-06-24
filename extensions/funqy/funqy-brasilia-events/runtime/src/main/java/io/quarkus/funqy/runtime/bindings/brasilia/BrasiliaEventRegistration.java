package io.quarkus.funqy.runtime.bindings.brasilia;

public final class BrasiliaEventRegistration {
    public final String eventProviderId;
    public final String eventProviderVersion;
    public final String functionName;
    public final String configJSON;

    public BrasiliaEventRegistration(String functionName, String eventProviderId, String eventProviderVersion,
            String configJSON) {
        this.functionName = functionName;
        this.eventProviderId = eventProviderId;
        this.eventProviderVersion = eventProviderVersion;
        this.configJSON = configJSON;
    }

    @Override
    public String toString() {
        return "BrasiliaEventRegistration{" +
                "eventProviderId='" + eventProviderId + '\'' +
                ", eventProviderVersion='" + eventProviderVersion + '\'' +
                ", functionName='" + functionName + '\'' +
                ", configJSON='" + configJSON + '\'' +
                '}';
    }
}
