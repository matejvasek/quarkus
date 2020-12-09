package io.quarkus.funqy.runtime.bindings.knative.events;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.funqy.knative.events.AbstractCloudEvent;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;

class HeaderCloudEventImpl<T> extends AbstractCloudEvent<T> implements CloudEvent<T> {
    String id;
    String specVersion;
    URI source;
    String type;

    String dataContentType;
    T data;
    String subject;
    OffsetDateTime time;
    Map<String, String> extensions;

    final MultiMap headers;
    final Buffer buffer;
    final Type dataType;
    final ObjectMapper mapper;
    private ObjectReader reader;

    HeaderCloudEventImpl(MultiMap headers, Buffer buffer, Type dataType, ObjectMapper mapper, ObjectReader reader) {
        this.headers = headers;
        this.buffer = buffer;
        this.dataType = dataType;
        this.mapper = mapper;
        this.reader = reader;
    }

    @Override
    public String id() {
        if (id == null) {
            id = headers.get("ce-id");
        }

        return id;
    }

    @Override
    public String specVersion() {
        if (specVersion == null) {
            specVersion = headers.get("ce-specversion");
        }

        return specVersion;
    }

    @Override
    public URI source() {
        if (source == null && headers.contains("ce-source")) {
            source = URI.create(headers.get("ce-source"));
        }

        return source;
    }

    @Override
    public String type() {
        if (type == null) {
            type = headers.get("ce-type");
        }

        return type;
    }

    @Override
    public String subject() {
        if (subject == null) {
            subject = headers.get("ce-subject");
        }

        return subject;
    }

    @Override
    public OffsetDateTime time() {
        if (time == null) {
            String t = headers.get("ce-time");
            if (t != null) {
                time = OffsetDateTime.parse(t);
            }
        }

        return time;
    }

    private static final Set<String> reservedHeaders;
    static {
        Set<String> ra = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ra.add("ce-specversion");
        ra.add("ce-id");
        ra.add("ce-source");
        ra.add("ce-type");

        ra.add("Content-Type");
        ra.add("ce-subject");
        ra.add("ce-time");

        ra.add("ce-datacontentencoding");
        ra.add("ce-schemaurl");

        ra.add("ce-dataschema");

        reservedHeaders = Collections.unmodifiableSet(ra);
    }

    private static boolean isCEHeader(String value) {
        return (value.charAt(0) == 'C' || value.charAt(0) == 'c') &&
                (value.charAt(1) == 'E' || value.charAt(1) == 'e') &&
                value.charAt(2) == '-';
    }

    @Override
    public Iterator<Map.Entry<String, String>> extensions() {
        return headers.entries()
                .stream()
                .filter(entry -> isCEHeader(entry.getKey()) && !reservedHeaders.contains(entry.getKey()))
                .iterator();
    }

    @Override
    public String dataContentType() {
        if (dataContentType == null) {
            dataContentType = headers.get("Content-Type");
        }

        return dataContentType;
    }

    @Override
    public T data() {
        if (data != null) {
            return data;
        }
        if (dataContentType() != null && dataContentType().startsWith("application/json") && !byte[].class.equals(dataType)) {
            try {
                data = reader.readValue(buffer.getBytes());
                return data;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else if (byte[].class.equals(dataType)) {
            data = (T) buffer.getBytes();
            return data;
        } else {
            String msg = String.format("don't know how to get event data (dataContentType: %s, javaType: %s)",
                    dataContentType(), dataType.getTypeName());
            throw new RuntimeException(msg);
        }
    }
}
