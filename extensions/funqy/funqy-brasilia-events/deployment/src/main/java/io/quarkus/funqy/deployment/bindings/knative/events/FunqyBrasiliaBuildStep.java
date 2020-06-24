package io.quarkus.funqy.deployment.bindings.knative.events;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;
import java.util.Optional;

// import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.funqy.deployment.FunctionBuildItem;
import io.quarkus.funqy.deployment.FunctionInitializedBuildItem;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.funqy.runtime.bindings.brasilia.BrasiliaBindingRecorder;
import io.quarkus.funqy.runtime.bindings.brasilia.FunqyBrasiliaConfig;
import io.quarkus.jackson.ObjectMapperProducer;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;

public class FunqyBrasiliaBuildStep {
    //    private static final Logger log = Logger.getLogger(FunqyBrasiliaBuildStep.class);
    public static final String FUNQY_BRASILIA = "funqy-brasilia";

    @BuildStep
    public void markObjectMapper(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapper.class.getName())));
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapperProducer.class.getName())));
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public void staticInit(BrasiliaBindingRecorder binding,
            List<FunctionBuildItem> functions,
            Optional<FunctionInitializedBuildItem> hasFunctions,
            BeanContainerBuildItem beanContainer // make sure bc is initialized
    ) throws Exception {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;
        binding.init();
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            FunqyConfig funqyConfig,
            FunqyBrasiliaConfig eventsConfig,
            BrasiliaBindingRecorder binding,
            Optional<FunctionInitializedBuildItem> hasFunctions,
            BuildProducer<FeatureBuildItem> feature,
            CoreVertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            ExecutorBuildItem executorBuildItem) throws Exception {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;

        feature.produce(new FeatureBuildItem(FUNQY_BRASILIA));

        binding.start(funqyConfig,
                eventsConfig,
                vertx.getVertx(),
                shutdown,
                beanContainer.getValue(),
                executorBuildItem.getExecutorProxy());

    }
}
