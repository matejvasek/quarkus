package io.quarkus.funqy.knative.events;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class CloudEventBuilder<T> {
    private String specVersion;
    private String id;
    private String type;
    private String source;
    private String dataContentType;
    private T data;
    private String subject;
    private OffsetDateTime time;
    private Map<String, String> extensions;

    private CloudEventBuilder() {
    }

    public static <T> CloudEventBuilder<T> create() {
        return new CloudEventBuilder<>();
    }

    public CloudEventBuilder<T> specVersion(String specVersion) {
        this.specVersion = specVersion;
        return this;
    }

    public CloudEventBuilder<T> id(String id) {
        this.id = id;
        return this;
    }

    public CloudEventBuilder<T> type(String type) {
        this.type = type;
        return this;
    }

    public CloudEventBuilder<T> source(String source) {
        this.source = source;
        return this;
    }

    public CloudEventBuilder<T> dataContentType(String dataContentType) {
        this.dataContentType = dataContentType;
        return this;
    }

    public CloudEventBuilder<T> data(T data) {
        this.data = data;
        return this;
    }

    public CloudEventBuilder<T> subject(String subject) {
        this.subject = subject;
        return this;
    }

    public CloudEventBuilder<T> time(OffsetDateTime time) {
        this.time = time;
        return this;
    }

    public CloudEventBuilder<T> extensions(Map<String, String> extensions) {
        this.extensions = extensions;
        return this;
    }

    public CloudEvent<T> build() {
        return new SimpleCloudEvent(specVersion, id, type, source, dataContentType, data, subject, time, extensions);
    }

    private class SimpleCloudEvent<T> extends AbstractCloudEvent<T> implements CloudEvent<T> {
        private final String specVersion;
        private final String id;
        private final String type;
        private final String source;
        private final String dataContentType;
        private final T data;
        private final String subject;
        private final OffsetDateTime time;
        private final Map<String, String> extensions;

        SimpleCloudEvent(String specVersion, String id, String type, String source, String dataContentType, T data,
                String subject, OffsetDateTime time, Map<String, String> extensions) {

            Objects.requireNonNull(specVersion);
            Objects.requireNonNull(id);
            Objects.requireNonNull(type);
            Objects.requireNonNull(source);

            if (extensions == null)
                extensions = Collections.emptyMap();

            this.specVersion = specVersion;
            this.id = id;
            this.type = type;
            this.source = source;
            this.dataContentType = dataContentType;
            this.data = data;
            this.subject = subject;
            this.time = time;
            this.extensions = extensions;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String specVersion() {
            return specVersion;
        }

        @Override
        public String source() {
            return source;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public String subject() {
            return subject;
        }

        @Override
        public OffsetDateTime time() {
            return time;
        }

        @Override
        public Map<String, String> extensions() {
            return extensions;
        }

        @Override
        public String dataContentType() {
            return dataContentType;
        }

        @Override
        public T data() {
            return data;
        }

    }
}
