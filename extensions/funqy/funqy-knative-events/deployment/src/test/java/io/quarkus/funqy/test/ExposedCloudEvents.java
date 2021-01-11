package io.quarkus.funqy.test;

import java.io.Serializable;
import java.util.Objects;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class ExposedCloudEvents {

    @Funq
    @CloudEventMapping(trigger = "test-type")
    public CloudEvent<TestBean> doubleIt(CloudEvent<TestBean> event) {

        if (event == null)
            throw new RuntimeException("Event is null!");
        if (!event.specVersion().equals("1.0"))
            throw new RuntimeException("Bad specversion!");
        if (!event.id().equals("test-id"))
            throw new RuntimeException("Bad id!");
        if (!event.source().toString().equals("/OfTest"))
            throw new RuntimeException("Bad source!");
        if (!event.type().equals("test-type"))
            throw new RuntimeException("Bad type!");
        if (!event.subject().equals("test-subj"))
            throw new RuntimeException("Bad subject!");
        if (event.time() == null)
            throw new RuntimeException("Bad time!");

        TestBean inBean = event.data();
        return CloudEventBuilder.<TestBean> create()
                .specVersion("1.0")
                .id("double-it-id")
                .type("double-it-type")
                .source("/OfDoubleIt")
                .dataContentType("application/json")
                .data(new TestBean(inBean.getI() * 2, inBean.getS() + inBean.getS()))
                .build();
    }

    public static class TestBean implements Serializable {
        private int i;
        private String s;

        public TestBean() {
        }

        public TestBean(int i, String s) {
            this.i = i;
            this.s = s;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestBean testBean = (TestBean) o;
            return i == testBean.i && Objects.equals(s, testBean.s);
        }

        @Override
        public int hashCode() {
            return Objects.hash(s, i);
        }

        @Override
        public String toString() {
            return "TestBean{" +
                    "i=" + i +
                    ", s='" + s + '\'' +
                    '}';
        }
    }
}
