package com.graphhopper;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.openjdk.jmh.annotations.*;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class JettyBenchmark {
    @Param({"10000000", "20000000", "40000000"})
    int size;
    private int[] array;
    OkHttpClient client;
    private Tomcat tomcatServer;

    @Setup
    public void setup() throws Exception {
        Random rnd = new Random(123);
        array = new int[size];
        for (int i = 0; i < array.length; ++i)
            array[i] = rnd.nextInt(100);

        client = new OkHttpClient.Builder().build();
        tomcatServer = new Tomcat();
        Connector connector = new Connector();
        connector.setPort(8989);
        tomcatServer.getService().addConnector(connector);
        startServer(tomcatServer, array);
    }

    @TearDown
    public void tearDown() throws Exception {
        tomcatServer.stop();
        tomcatServer.destroy();
    }

    @State(Scope.Thread)
    public static class SumState {
        double checksum;

        @TearDown(Level.Iteration)
        public void finish() {
            LoggerFactory.getLogger(JettyBenchmark.class).info("checksum: " + checksum);
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void measureBaseline() {
        // do nothing
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public double measureBaselineHttp(SumState state) throws IOException {
        Call call = client.newCall(new Request.Builder().url("http://localhost:8989/baseline").build());
        ResponseBody body = call.execute().body();
        double result = state.checksum = Double.parseDouble(body.string());
        body.close();
        return result;
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public double measureSumArray(SumState state) {
        double result = sumArray(array);
        return state.checksum = result;
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public double measureSumArrayHttp(SumState state) throws IOException {
        Call call = client.newCall(new Request.Builder().url("http://localhost:8989/sumarray").build());
        ResponseBody body = call.execute().body();
        double result = state.checksum = Double.parseDouble(body.string());
        body.close();
        return result;
    }

    public static void startServer(Tomcat tomcatServer, int[] array) {
        Context context = tomcatServer.addWebapp("", new File(".").getAbsolutePath());
        ResourceConfig rc = new ResourceConfig();
        rc.register(new AbstractBinder() {
            @Override
            public void configure() {
                bind(new SumArrayResource(array)).to(SumArrayResource.class);
            }
        });
        rc.register(SumArrayResource.class);
        rc.register(BaselineResource.class);
        Tomcat.addServlet(context, "embedded", new ServletContainer(rc));
        context.addServletMappingDecoded("/*", "embedded");

        new Thread(() ->
        {
            try {
                tomcatServer.start();
                tomcatServer.getServer().await();
            } catch (LifecycleException e) {
                e.printStackTrace();
            }
        }).start();

        while (!tomcatServer.getServer().getState().equals(LifecycleState.STARTED)) {

        }

    }

    @Path("sumarray")
    public static class SumArrayResource {
        final int[] array;

        public SumArrayResource(int[] array) {
            this.array = array;
        }

        @GET
        @Produces({MediaType.APPLICATION_JSON})
        public double sumArray() {
            return JettyBenchmark.sumArray(array);
        }
    }

    @Path("baseline")
    public static class BaselineResource {
        @GET
        @Produces({MediaType.APPLICATION_JSON})
        public double baseline() {
            return 42;
        }
    }

    private static double sumArray(int[] array) {
        double result = 0;
        for (int i = 0; i < array.length; i++)
            result += array[i] * ((i % 2 == 0) ? 1 : -1);
        return result;
    }
}
