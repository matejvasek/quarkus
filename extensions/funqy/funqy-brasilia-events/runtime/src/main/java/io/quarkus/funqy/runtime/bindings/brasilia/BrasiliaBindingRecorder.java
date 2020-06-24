package io.quarkus.funqy.runtime.bindings.brasilia;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.brasilia.BrasiliaEventMapping;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.funqy.runtime.bindings.brasilia.worker.Message;
import io.quarkus.funqy.runtime.bindings.brasilia.worker.Worker;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Vertx;

/**
 * Provides the runtime methods to bootstrap Quarkus Funq
 */
@Recorder
public class BrasiliaBindingRecorder {
    private static final Logger log = Logger.getLogger(BrasiliaBindingRecorder.class);

    private static ObjectMapper objectMapper;
    private static final List<BrasiliaEventRegistration> eventRegistrations = new ArrayList<>();

    public void init() {
        objectMapper = getObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            Method method = invoker.getMethod();
            BrasiliaEventMapping annotation = method.getAnnotation(BrasiliaEventMapping.class);
            if (annotation != null && !annotation.name().isEmpty()) {
                eventRegistrations.add(new BrasiliaEventRegistration(invoker.getName(), annotation.name(), annotation.version(),
                        annotation.configJSON()));
            }

            if (invoker.hasInput()) {
                ObjectReader reader = objectMapper.readerFor(invoker.getInputType());
                invoker.getBindingContext().put(ObjectReader.class.getName(), reader);
            }
            if (invoker.hasOutput()) {
                ObjectWriter writer = objectMapper.writerFor(invoker.getOutputType());
                invoker.getBindingContext().put(ObjectWriter.class.getName(), writer);
            }
        }

        //        for (Class<?> clazz : new Class[] {
        //                Message.Connect.class,
        //                Message.ConnectResult.class,
        //                Message.EventRegistrations.class,
        //                Message.EventRegistrations.EventRegistration.class,
        //                Message.EventRegistrationsResult.class,
        //                Message.EventRegistrationsResult.EventRegistrationResult.class,
        //                Message.Event.class,
        //                Message.CompleteEvent.class,
        //                Message.CompleteEvent.InvocationResult.class,
        //                Message.CompleteEvent.InvocationResult.InvocationStatus.class,
        //                Message.Disconnect.class,
        //                Message.HeartBeat.class,
        //                Message.Unregister.class,
        //                Message.GetEvents.class }) {
        //
        //            objectMapper.writerFor(clazz);
        //            objectMapper.readerFor(clazz);
        //        }
        for (Class aClass : getAllNested(Message.class)) {
            objectMapper.writerFor(aClass);
            objectMapper.readerFor(aClass);
        }
    }

    private List<Class> getAllNested(Class clazz) {
        ArrayList<Class> classes = new ArrayList<Class>();
        classes.add(clazz);
        for (Class declaredClass : clazz.getDeclaredClasses()) {
            classes.addAll(getAllNested(declaredClass));
        }
        return classes;
    }

    private ObjectMapper getObjectMapper() {
        InstanceHandle<ObjectMapper> instance = Arc.container().instance(ObjectMapper.class);
        if (instance.isAvailable()) {
            return instance.get().copy();
        }
        return new ObjectMapper();
    }

    public void start(
            FunqyConfig funqyConfig,
            FunqyBrasiliaConfig eventsConfig,
            Supplier<Vertx> vertx,
            ShutdownContext shutdown,
            BeanContainer beanContainer,
            ExecutorService executor) {

        FunctionConstructor.CONTAINER = beanContainer;

        if (eventsConfig.mapping != null) {
            for (Map.Entry<String, FunqyBrasiliaConfig.FunctionMapping> entry : eventsConfig.mapping.entrySet()) {
                String functionName = entry.getKey();
                FunctionInvoker invoker = FunctionRecorder.registry.matchInvoker(functionName);
                if (invoker == null) {
                    throw new RuntimeException("brasilia.function-mapping does not map to a function: " + functionName);
                }
                FunqyBrasiliaConfig.FunctionMapping mapping = entry.getValue();
                if (mapping.name.isPresent() && mapping.version.isPresent()) {
                    eventRegistrations.add(new BrasiliaEventRegistration(functionName, mapping.name.get(),
                            mapping.version.get(), mapping.config.orElse("{}")));
                }
            }
        }

        Worker worker = new Worker(eventsConfig.appId.orElse("QuarkusAppX"),
                eventsConfig.appVersion.orElse("0.0.1"),
                vertx.get(),
                executor,
                objectMapper,
                FunctionRecorder.registry.invokers(),
                eventRegistrations);
        worker.start().await().indefinitely();
        log.infof("Worker has started.");

        shutdown.addShutdownTask(() -> {
            FunctionConstructor.CONTAINER = null;
            objectMapper = null;
            eventRegistrations.clear();
            worker.stop().await().indefinitely();
            log.infof("Worker has stopped.");
        });
        return;
    }
}
