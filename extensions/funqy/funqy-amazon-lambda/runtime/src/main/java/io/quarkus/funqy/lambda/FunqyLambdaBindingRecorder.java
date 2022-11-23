package io.quarkus.funqy.lambda;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.jboss.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.amazon.lambda.runtime.AbstractLambdaPollLoop;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaContext;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaMapperRecorder;
import io.quarkus.amazon.lambda.runtime.JacksonInputReader;
import io.quarkus.amazon.lambda.runtime.JacksonOutputWriter;
import io.quarkus.amazon.lambda.runtime.LambdaInputReader;
import io.quarkus.amazon.lambda.runtime.LambdaOutputWriter;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.funqy.runtime.FunctionConstructor;
import io.quarkus.funqy.runtime.FunctionInvoker;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.funqy.runtime.FunqyServerResponse;
import io.quarkus.funqy.runtime.RequestContextImpl;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Provides the runtime methods to bootstrap Quarkus Funq
 */
@Recorder
public class FunqyLambdaBindingRecorder {
    private static final Logger log = Logger.getLogger(FunqyLambdaBindingRecorder.class);

    private static FunctionInvoker invoker;
    private static BeanContainer beanContainer;
    private static LambdaInputReader reader;
    private static LambdaOutputWriter writer;

    public void init(BeanContainer bc) {
        beanContainer = bc;
        FunctionConstructor.CONTAINER = bc;
        ObjectMapper objectMapper = AmazonLambdaMapperRecorder.objectMapper;
        for (FunctionInvoker invoker : FunctionRecorder.registry.invokers()) {
            if (invoker.hasInput() && !byte[].class.equals(invoker.getInputType())) {
                JavaType javaInputType = objectMapper.constructType(invoker.getInputType());
                ObjectReader reader = objectMapper.readerFor(javaInputType);
                invoker.getBindingContext().put(ObjectReader.class.getName(), reader);
            }
            if (invoker.hasOutput() && !byte[].class.equals(invoker.getOutputType())) {
                JavaType javaOutputType = objectMapper.constructType(invoker.getOutputType());
                ObjectWriter writer = objectMapper.writerFor(javaOutputType);
                invoker.getBindingContext().put(ObjectWriter.class.getName(), writer);
            }
        }
    }

    public void chooseInvoker(FunqyConfig config) {
        // this is done at Runtime so that we can change it with an environment variable.
        if (config.export.isPresent()) {
            invoker = FunctionRecorder.registry.matchInvoker(config.export.get());
            if (invoker == null) {
                throw new RuntimeException("quarkus.funqy.export does not match a function: " + config.export.get());
            }
        } else if (FunctionRecorder.registry.invokers().size() == 0) {
            throw new RuntimeException("There are no functions to process lambda");

        } else if (FunctionRecorder.registry.invokers().size() > 1) {
            throw new RuntimeException("Too many functions.  You need to set quarkus.funqy.export");
        } else {
            invoker = FunctionRecorder.registry.invokers().iterator().next();
        }
        if (invoker.hasInput()) {
            if (byte[].class.equals(invoker.getInputType())) {
                reader = new LambdaInputByteRead();
            } else {
                reader = new JacksonInputReader((ObjectReader) invoker.getBindingContext().get(ObjectReader.class.getName()));
            }
        }
        if (invoker.hasOutput()) {
            if (byte[].class.equals(invoker.getOutputType())) {
                writer = new LambdaOutputByteWriter();
            } else {
                writer = new JacksonOutputWriter((ObjectWriter) invoker.getBindingContext().get(ObjectWriter.class.getName()));
            }
        }

    }

    /**
     * Called by JVM handler wrapper
     *
     * @param inputStream
     * @param outputStream
     * @param context
     * @throws IOException
     */
    public static void handle(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        Object input = null;
        if (invoker.hasInput()) {
            if (byte[].class.equals(invoker.getInputType())) {
                input = inputStream.readAllBytes();
            } else {
                input = reader.readValue(inputStream);
            }
        }
        FunqyServerResponse response = dispatch(input);

        Object value = response.getOutput().await().indefinitely();
        if (value != null) {
            if (byte[].class.equals(invoker.getOutputType())) {
                outputStream.write((byte[]) value);
            } else {
                writer.writeValue(outputStream, value);
            }
        }

    }

    @SuppressWarnings("rawtypes")
    public void startPollLoop(ShutdownContext context, LaunchMode launchMode) {
        AbstractLambdaPollLoop loop = new AbstractLambdaPollLoop(AmazonLambdaMapperRecorder.objectMapper,
                AmazonLambdaMapperRecorder.cognitoIdReader, AmazonLambdaMapperRecorder.clientCtxReader, launchMode) {

            @Override
            protected Object processRequest(Object input, AmazonLambdaContext context) throws Exception {
                FunqyServerResponse response = dispatch(input);
                return response.getOutput().await().indefinitely();
            }

            @Override
            protected LambdaInputReader getInputReader() {
                return reader;
            }

            @Override
            protected LambdaOutputWriter getOutputWriter() {
                return writer;
            }

            @Override
            protected boolean isStream() {
                return false;
            }

            @Override
            protected void processRequest(InputStream input, OutputStream output, AmazonLambdaContext context)
                    throws Exception {
                throw new RuntimeException("Unreachable!");
            }
        };
        loop.startPollLoop(context);

    }

    private static FunqyServerResponse dispatch(Object input) {
        ManagedContext requestContext = beanContainer.requestContext();
        requestContext.activate();
        try {
            FunqyRequestImpl funqyRequest = new FunqyRequestImpl(new RequestContextImpl(), input);
            FunqyResponseImpl funqyResponse = new FunqyResponseImpl();
            invoker.invoke(funqyRequest, funqyResponse);
            return funqyResponse;
        } finally {
            if (requestContext.isActive()) {
                requestContext.terminate();
            }
        }
    }

    private static class LambdaInputByteRead implements LambdaInputReader<byte[]> {
        @Override
        public byte[] readValue(InputStream is) throws IOException {
            return is.readAllBytes();
        }
    }

    private static class LambdaOutputByteWriter implements LambdaOutputWriter {

        @Override
        public void writeValue(OutputStream os, Object obj) throws IOException {
            os.write((byte[])obj);
        }

        @Override
        public void writeHeaders(HttpURLConnection conn) {
            conn.setRequestProperty("Content-Type", "application/octet-stream");
        }
    }

}
