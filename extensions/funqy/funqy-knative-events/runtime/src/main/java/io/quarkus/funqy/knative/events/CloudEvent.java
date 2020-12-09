package io.quarkus.funqy.knative.events;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Map;

/**
 * CloudEvent.
 *
 */
public interface CloudEvent<T> {

    String id();

    String specVersion();

    URI source();

    String type();

    String subject();

    OffsetDateTime time();

    Iterator<Map.Entry<String, String>> extensions();

    String dataContentType();

    T data();

}
