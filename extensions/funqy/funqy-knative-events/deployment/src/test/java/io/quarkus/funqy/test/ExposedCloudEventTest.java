package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class ExposedCloudEventTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ExposedCloudEvents.class));

    @ParameterizedTest
    @MethodSource("provideBinaryEncodingTestArgs")
    public void testBinaryEncoding(Map<String, String> headers, String specversion, String dataSchemaHdrName) {

        RequestSpecification req = RestAssured.given().contentType("application/json");

        for (Map.Entry<String, String> h : headers.entrySet()) {
            req = req.header(h.getKey(), h.getValue());
        }

        req.body(BINARY_ENCODED_EVENT_BODY)
                .post("/")
                .then()
                .header("ce-specversion", equalTo(specversion))
                .header("ce-id", equalTo("double-it-id"))
                .header("ce-type", equalTo("double-it-type"))
                .header("ce-source", equalTo("/OfDoubleIt"))
                .header(dataSchemaHdrName, equalTo("dataschema-server"))
                .header("ce-extserver", equalTo("ext-server-val"))
                .body("i", equalTo(42))
                .body("s", equalTo("abcabc"))
                .statusCode(200);
    }

    @ParameterizedTest
    @MethodSource("provideStructuredEncodingTestArgs")
    public void testStructuredEncoding(String event, String specversion, String dataSchemaFieldName) {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(event)
                .post("/")
                .then()
                .body("specversion", equalTo(specversion))
                .body("id", equalTo("double-it-id"))
                .body("type", equalTo("double-it-type"))
                .body("source", equalTo("/OfDoubleIt"))
                .body(dataSchemaFieldName, equalTo("dataschema-server"))
                .body("extserver", equalTo("ext-server-val"))
                .body("data.i", equalTo(42))
                .body("data.s", equalTo("abcabc"))
                .statusCode(200);
    }

    static {
        Map<String, String> common = new HashMap<>();
        common.put("ce-id", "test-id");
        common.put("ce-type", "test-type");
        common.put("ce-source", "/OfTest");
        common.put("ce-subject", "test-subj");
        common.put("ce-time", "2018-04-05T17:31:00Z");
        common.put("ce-extclient", "ext-client-val");

        Map<String, String> v1 = new HashMap<>(common);
        v1.put("ce-specversion", "1.0");
        v1.put("ce-dataschema", "test-dataschema-client");
        BINARY_ENCODED_EVENT_V1_HEADERS = Collections.unmodifiableMap(v1);

        Map<String, String> v03 = new HashMap<>(common);
        v03.put("ce-specversion", "0.3");
        v03.put("ce-schemaurl", "test-dataschema-client");
        BINARY_ENCODED_EVENT_V03_HEADERS = Collections.unmodifiableMap(v03);
    }

    public static final Map<String, String> BINARY_ENCODED_EVENT_V1_HEADERS;
    public static final Map<String, String> BINARY_ENCODED_EVENT_V03_HEADERS;

    private static Stream<Arguments> provideBinaryEncodingTestArgs() {
        return Stream.<Arguments> builder()
                .add(Arguments.arguments(BINARY_ENCODED_EVENT_V1_HEADERS, "1.0", "ce-dataschema"))
                .add(Arguments.arguments(BINARY_ENCODED_EVENT_V03_HEADERS, "0.3", "ce-schemaurl"))
                .build();
    }

    public static final String BINARY_ENCODED_EVENT_BODY = " { \"i\" : 21, \"s\" : \"abc\" } ";

    static final String STRUCTURED_ENCODED_EVENT_V1_BODY = "{ \"id\" : \"test-id\", " +
            "  \"specversion\": \"1.0\", " +
            "  \"source\": \"/OfTest\", " +
            "  \"subject\": \"test-subj\", " +
            "  \"time\": \"2018-04-05T17:31:00Z\", " +
            "  \"type\": \"test-type\", " +
            "  \"extclient\": \"ext-client-val\", " +
            "  \"dataschema\": \"test-dataschema-client\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": { \"i\" : 21, \"s\" : \"abc\" } " +
            "}";

    static final String STRUCTURED_ENCODED_EVENT_V03_BODY = "{ \"id\" : \"test-id\", " +
            "  \"specversion\": \"0.3\", " +
            "  \"source\": \"/OfTest\", " +
            "  \"subject\": \"test-subj\", " +
            "  \"time\": \"2018-04-05T17:31:00Z\", " +
            "  \"type\": \"test-type\", " +
            "  \"extclient\": \"ext-client-val\", " +
            "  \"schemaurl\": \"test-dataschema-client\", " +
            "  \"datacontenttype\": \"application/json\", " +
            "  \"data\": { \"i\" : 21, \"s\" : \"abc\" } " +
            "}";

    private static Stream<Arguments> provideStructuredEncodingTestArgs() {
        return Stream.<Arguments> builder()
                .add(Arguments.arguments(STRUCTURED_ENCODED_EVENT_V1_BODY, "1.0", "dataschema"))
                .add(Arguments.arguments(STRUCTURED_ENCODED_EVENT_V03_BODY, "0.3", "schemaurl"))
                .build();
    }
}
