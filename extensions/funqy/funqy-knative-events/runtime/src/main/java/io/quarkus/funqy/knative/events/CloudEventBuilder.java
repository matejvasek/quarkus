package io.quarkus.funqy.knative.events;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class CloudEventBuilder<T> {
    private String specVersion;
    private String id;
    private String type;
    private URI source;
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

    public CloudEventBuilder<T> setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
        return this;
    }

    public CloudEventBuilder<T> setId(String id) {
        this.id = id;
        return this;
    }

    public CloudEventBuilder<T> setType(String type) {
        this.type = type;
        return this;
    }

    public CloudEventBuilder<T> setSource(URI source) {
        this.source = source;
        return this;
    }

    public CloudEventBuilder<T> setDataContentType(String dataContentType) {
        this.dataContentType = dataContentType;
        return this;
    }

    public CloudEventBuilder<T> setData(T data) {
        this.data = data;
        return this;
    }

    public CloudEventBuilder<T> setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public CloudEventBuilder<T> setTime(OffsetDateTime time) {
        this.time = time;
        return this;
    }

    public CloudEventBuilder<T> setExtensions(Map<String, String> extensions) {
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
        private final URI source;
        private final String dataContentType;
        private final T data;
        private final String subject;
        private final OffsetDateTime time;
        private final Map<String, String> extensions;

        SimpleCloudEvent(String specVersion, String id, String type, URI source, String dataContentType, T data,
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
        public URI source() {
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
