package io.streamzi.ev.examples;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Runner {

    private static final Logger logger = Logger.getLogger(Runner.class.getName());

    private static final String BOOTSTRAP_SERVERS_KEY = "BOOTSTRAP_SERVERS";
    private static final String TOPIC_KEY = "TOPIC";

    public static void main(String... args) {
        logger.info("Starting Producer");

        final String bootstrapServers = resolve(BOOTSTRAP_SERVERS_KEY);
        final String topic = resolve(TOPIC_KEY);

        if (bootstrapServers != null && topic != null) {
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            SimpleProducer producer = new SimpleProducer();
            producer.init(bootstrapServers, topic);
            executor.submit(producer);


            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                executor.shutdown();
                try {
                    executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ie) {
                    logger.log(Level.SEVERE, "Error on close", ie);
                }
            }));
        }else{
            logger.warning("Not starting producer as either BOOTSTRAP_SERVERS or TOPIC unassigned");
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
