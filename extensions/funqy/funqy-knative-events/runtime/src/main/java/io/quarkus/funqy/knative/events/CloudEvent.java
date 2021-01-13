package io.quarkus.funqy.knative.events;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * CloudEvent.
 *
 */
public interface CloudEvent<T> {

    String id();

    SpecVersion specVersion();

    String source();

    String type();

    String subject();

    OffsetDateTime time();

    Map<String, String> extensions();

    String dataSchema();

    String dataContentType();

    T data();

    enum SpecVersion {
        V03("0.3"),
        V1("1.0");

        private final String value;

        SpecVersion(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static SpecVersion fromString(String value) {
            switch (value) {
                case "1.0":
                    return SpecVersion.V1;
                case "0.3":
                    return SpecVersion.V03;
                default:
                    throw new RuntimeException("Unsupported CloudEvent spec-version: " + value + ".");
            }
        }
    }

}
