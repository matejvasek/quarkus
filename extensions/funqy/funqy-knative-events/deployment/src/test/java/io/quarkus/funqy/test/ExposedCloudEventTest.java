package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ExposedCloudEventTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ExposedCloudEvents.class));

    @Test
    public void testMapping() {
        RestAssured.given().contentType("application/json")
                .header("ce-id", "test-id")
                .header("ce-specversion", "1.0")
                .header("ce-type", "test-type")
                .header("ce-source", "/OfTest")
                .header("ce-subject", "test-subj")
                .header("ce-dataschema", "test-dataschema-client")
                .header("ce-time", "2018-04-05T17:31:00Z")
                .header("ce-extclient", "ext-client-val")
                .body(" { \"i\" : 21, \"s\" : \"abc\" } ")
                .post("/")
                .then()
                .header("ce-specversion", equalTo("1.0"))
                .header("ce-id", equalTo("double-it-id"))
                .header("ce-type", equalTo("double-it-type"))
                .header("ce-source", equalTo("/OfDoubleIt"))
                .header("ce-dataschema", equalTo("dataschema-server"))
                .header("ce-extserver", equalTo("ext-server-val"))
                .body("i", equalTo(42))
                .body("s", equalTo("abcabc"))
                .statusCode(200);
    }

    static final String contextEvent = "{ \"id\" : \"test-id\", " +
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

    @Test
    public void testStructuredMapping() {
        RestAssured.given().contentType("application/cloudevents+json")
                .body(contextEvent)
                .post("/")
                .then()
                .body("specversion", equalTo("1.0"))
                .body("id", equalTo("double-it-id"))
                .body("type", equalTo("double-it-type"))
                .body("source", equalTo("/OfDoubleIt"))
                .body("dataschema", equalTo("dataschema-server"))
                .body("extserver", equalTo("ext-server-val"))
                .body("data.i", equalTo(42))
                .body("data.s", equalTo("abcabc"))
                .statusCode(200);
    }

}
