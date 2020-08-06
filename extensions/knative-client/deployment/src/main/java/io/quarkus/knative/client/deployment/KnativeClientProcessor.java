package io.quarkus.knative.client.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.*;

class KnativeClientProcessor {

    private static final String FEATURE = "knative-client";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.fabric8", "knative-client"));
        indexDependency.produce(new IndexDependencyBuildItem("io.fabric8", "knative-model"));
    }

}
