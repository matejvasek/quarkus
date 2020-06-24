package io.quarkus.funqy.runtime.bindings.brasilia.worker;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public final class Message {

    public static final class Type {
        private static final String MessageNamesPrefix = "projectbrasilia.core.";
        private static final String ResponseSuffix = "response";

        public static final String Connect = Type.MessageNamesPrefix + "connect";
        public static final String ConnectResponse = Type.Connect + Type.ResponseSuffix;
        public static final String WorkerRegistration = Type.MessageNamesPrefix + "workerregistration";
        public static final String WorkerRegistrationResponse = Type.WorkerRegistration + Type.ResponseSuffix;
        public static final String WorkerMessagePolling = Type.MessageNamesPrefix + "messagepolling";
        public static final String WorkerMessagePollingResponse = Type.WorkerMessagePolling + Type.ResponseSuffix;
        public static final String EventResult = Type.MessageNamesPrefix + "eventresult";
        public static final String GetEvents = Type.MessageNamesPrefix + "getevents";
        public static final String GetEventsResponse = Type.GetEvents + Type.ResponseSuffix;
        public static final String Heartbeat = Type.MessageNamesPrefix + "heartbeat";
        public static final String Unregister = MessageNamesPrefix + "unregister";
        public static final String Disconnect = MessageNamesPrefix + "disconnect";
    }

    public static final Map<String, Class<?>> cloudEventTypeToClass = Collections
            .unmodifiableMap(new HashMap<String, Class<?>>() {
                {
                    put(Type.Connect, Connect.class);
                    put(Type.ConnectResponse, ConnectResult.class);
                    put(Type.WorkerRegistration, EventRegistrations.class);
                    put(Type.WorkerRegistrationResponse, EventRegistrationsResult.class);
                    put(Type.Heartbeat, HeartBeat.class);
                    put(Type.GetEvents, GetEvents.class);
                    put(Type.GetEventsResponse, Event[].class);
                    put(Type.EventResult, CompleteEvent.class);
                    put(Type.Unregister, Unregister.class);
                    put(Type.Disconnect, Disconnect.class);
                }
            });

    public static final Map<Class<?>, String> classToCloudEventType = Collections.unmodifiableMap(cloudEventTypeToClass
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));

    public static final class Connect {

        public final String applicationId;
        public final String applicationVersion;
        public final String workerInstanceId;
        public final List<String> capabilities;

        public Connect(String applicationId, String applicationVersion, String workerInstanceId, List<String> capabilities) {
            this.applicationId = applicationId;
            this.applicationVersion = applicationVersion;
            this.workerInstanceId = workerInstanceId;
            this.capabilities = Collections.unmodifiableList(new ArrayList<>(capabilities));
        }

        @Override
        public String toString() {

            return "Connect{" +
                    "applicationId='" + applicationId + '\'' +
                    ", applicationVersion='" + applicationVersion + '\'' +
                    ", workerInstanceId='" + workerInstanceId + '\'' +
                    ", capabilities=" + capabilities +
                    '}';
        }
    }

    public static final class ConnectResult {

        public final Map<String, Map<String, String>> channelConfigurations;
        public final int workerHearbeatTimeoutInSeconds;

        @JsonCreator
        public ConnectResult(@JsonProperty("channelConfigurations") Map<String, Map<String, String>> channelConfigurations,
                @JsonProperty("workerHearbeatTimeoutInSeconds") int workerHearbeatTimeoutInSeconds) {
            this.channelConfigurations = channelConfigurations;
            this.workerHearbeatTimeoutInSeconds = workerHearbeatTimeoutInSeconds;
        }

        @Override
        public String toString() {
            return "ConnectResponseData{" +
                    "channelConfigurations=" + channelConfigurations +
                    ", workerHearbeatTimeoutInSeconds=" + workerHearbeatTimeoutInSeconds +
                    '}';
        }
    }

    public static final class EventRegistrations {
        public final String workerInstanceId;
        public final List<EventRegistration> eventRegistrations;

        public EventRegistrations(String workerInstanceId, List<EventRegistration> eventRegistrations) {
            this.workerInstanceId = workerInstanceId;
            this.eventRegistrations = Collections.unmodifiableList(new ArrayList<>(eventRegistrations));
        }

        public static final class EventRegistration {
            public final String eventProviderId;
            public final String eventProviderVersion;
            public final String subscriberId;
            public final Map<String, String> config;

            public EventRegistration(String eventProviderId, String eventProviderVersion, String subscriberId,
                    Map<String, String> config) {
                this.eventProviderId = eventProviderId;
                this.eventProviderVersion = eventProviderVersion;
                this.subscriberId = subscriberId;
                this.config = Collections.unmodifiableMap(new HashMap<>(config));
            }
        }
    }

    public static final class EventRegistrationsResult {
        public final List<EventRegistrationResult> eventRegistrationResults;
        public final boolean success;
        public final String failureMessage;

        @JsonCreator
        public EventRegistrationsResult(
                @JsonProperty("eventRegistrationResults") List<EventRegistrationResult> eventRegistrationResults,
                @JsonProperty("success") boolean success,
                @JsonProperty("failureMessage") String failureMessage) {
            this.eventRegistrationResults = Collections.unmodifiableList(new ArrayList<>(eventRegistrationResults));
            this.success = success;
            this.failureMessage = failureMessage;
        }

        @Override
        public String toString() {
            return "EventRegistrationsResponse{" +
                    "eventRegistrationResults=" + eventRegistrationResults +
                    ", success=" + success +
                    ", failureMessage='" + failureMessage + '\'' +
                    '}';
        }

        public static final class EventRegistrationResult {
            public final String eventProviderId;
            public final String eventProviderVersion;
            public final String subscriberId;
            public final boolean success;
            public final String failureMessage;

            @JsonCreator
            public EventRegistrationResult(@JsonProperty("eventProviderId") String eventProviderId,
                    @JsonProperty("eventProviderVersion") String eventProviderVersion,
                    @JsonProperty("subscriberId") String subscriberId,
                    @JsonProperty("success") boolean success,
                    @JsonProperty("failureMessage") String failureMessage) {
                this.eventProviderId = eventProviderId;
                this.eventProviderVersion = eventProviderVersion;
                this.subscriberId = subscriberId;
                this.success = success;
                this.failureMessage = failureMessage;
            }

            @Override
            public String toString() {
                return "EventRegistrationResult{" +
                        "EventProviderId='" + eventProviderId + '\'' +
                        ", EventProviderVersion='" + eventProviderVersion + '\'' +
                        ", SubscriberId='" + subscriberId + '\'' +
                        ", Success=" + success +
                        ", FailureMessage='" + failureMessage + '\'' +
                        '}';
            }
        }
    }

    public static final class HeartBeat {
        public final String workerInstanceId;

        public HeartBeat(String workerInstanceId) {
            this.workerInstanceId = workerInstanceId;
        }
    }

    public static final class GetEvents {
        public final long count;
        public final String workerInstanceId;

        public GetEvents(String workerInstanceId, long count) {
            this.workerInstanceId = workerInstanceId;
            this.count = count;
        }
    }

    public static final class Event {
        public final String id;
        public final String name;

        @JsonCreator
        public Event(@JsonProperty("id") String id,
                @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public static final class CompleteEvent {
        public final String eventId;
        public final InvocationResult result;

        public CompleteEvent(String eventId, InvocationResult result) {
            this.eventId = eventId;
            this.result = result;
        }

        @Override
        public String toString() {
            return "CompleteEvent{" +
                    "eventId='" + eventId + '\'' +
                    ", result=" + result +
                    '}';
        }

        public static final class InvocationResult {

            public final Object data;
            public final InvocationStatus status;
            public final String timeoutReason;

            public InvocationResult(Object data, InvocationStatus status, String timeoutReason) {
                this.data = data;
                this.status = status;
                this.timeoutReason = timeoutReason;
            }

            @Override
            public String toString() {
                return "InvocationResult{" +
                        "data=" + data +
                        ", status=" + status +
                        ", timeoutReason='" + timeoutReason + '\'' +
                        '}';
            }

            public enum InvocationStatus {

                UNHANDLED(0b0),
                FAILED(0b1),
                SUCCEEDED(0b10),
                TIMEOUT(0b101);

                private int bits;

                InvocationStatus(int bits) {
                    this.bits = bits;
                }

                @JsonCreator
                public static InvocationStatus fromBits(int bits) {
                    for (InvocationStatus e : values()) {
                        if (e.bits == bits) {
                            return e;
                        }
                    }
                    return null;
                }

                @JsonValue
                public int toBits() {
                    return bits;
                }
            }
        }
    }

    public static final class Unregister {
        public final String workerInstanceId;

        public Unregister(String workerInstanceId) {
            this.workerInstanceId = workerInstanceId;
        }
    }

    public static final class Disconnect {
        public final String workerInstanceId;

        public Disconnect(String workerInstanceId) {
            this.workerInstanceId = workerInstanceId;
        }
    }
}
