package com.redhat.streaming.zk.messaging;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Runner {

    private static final Logger logger = Logger.getLogger(Runner.class.getName());


    private static final String BOOTSTRAP_SERVERS_KEY = "BOOTSTRAP_SERVERS";
    private static final String INPUT_TOPIC_KEY = "INPUT_TOPIC";
    private static final String OUTPUT_TOPIC_KEY = "OUTPUT_TOPIC";

    public static void main(String... args) {
        logger.info("Starting Producer");

        final String bootstrapServers = resolve(BOOTSTRAP_SERVERS_KEY);
        final String inputTopic = resolve(INPUT_TOPIC_KEY);
        final String outputTopic = resolve(OUTPUT_TOPIC_KEY);

        if (bootstrapServers != null && inputTopic != null && outputTopic != null) {
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            SimpleProcessor producer = new SimpleProcessor();
            producer.init(bootstrapServers, inputTopic, outputTopic);

            executor.submit(producer);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                producer.shutdown();
                executor.shutdown();
                try {
                    executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ie) {
                    logger.log(Level.SEVERE, "Error on close", ie);
                }
            }));
        } else {
            logger.warning("Not starting producer as either BOOTSTRAP_SERVERS, INPUT_TOPIC or OUTPUT_TOPIC unassigned");
        }
    }

    private static String resolve(final String variable) {

        String value = System.getProperty(variable);
        if (value == null) {
            value = System.getenv(variable);
        }
        return value;
    }


}
