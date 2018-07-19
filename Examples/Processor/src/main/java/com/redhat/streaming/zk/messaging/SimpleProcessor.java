package com.redhat.streaming.zk.messaging;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SimpleProcessor implements Runnable {

    private static final Logger logger = Logger.getLogger(SimpleProcessor.class.getName());

    private final AtomicBoolean running = new AtomicBoolean(Boolean.TRUE);
    private KafkaStreams streams;
    private CountDownLatch latch;
    private Properties props;

    private String inputTopic = "";
    private String outputTopic = "";

    public SimpleProcessor() {
    }

    public void init(String kafkaUrl, String inputTopic, String outputTopic) {
        this.inputTopic = inputTopic;
        this.outputTopic = outputTopic;

        props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, UUID.randomUUID().toString());
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

    }

    @Override
    public void run() {

        if (inputTopic == null || outputTopic == null) {
            System.exit(1);
        }
        final StreamsBuilder builder = new StreamsBuilder();

        logger.info("Connecting to: " + props.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG) + " - " + inputTopic);
        logger.info("Outputtng to: " + props.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG) + " - " + outputTopic);

        KStream<String, String> source = builder.stream(inputTopic);
        source.flatMapValues(value -> Arrays.asList(value.split("\\W+")))
                .to(outputTopic);

        final Topology topology = builder.build();
        streams = new KafkaStreams(topology, props);
        latch = new CountDownLatch(1);

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
    }

    /**
     * True when a consumer is running; otherwise false
     */
    private boolean isRunning() {
        return running.get();
    }

    /*
     * Shutdown hook which can be called from a separate thread.
     */
    public void shutdown() {
        if (isRunning()) {
            logger.info("Shutting down the processor.");
            if (streams != null) {
                streams.close();
            }
            if (latch != null) {
                latch.countDown();
            }

        }
    }
}
