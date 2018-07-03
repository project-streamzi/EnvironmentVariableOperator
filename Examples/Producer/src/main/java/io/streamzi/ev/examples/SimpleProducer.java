package io.streamzi.ev.examples;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class SimpleProducer implements Runnable {
    private static final Logger logger = Logger.getLogger(SimpleProducer.class.getName());

    private String topic;

    private final AtomicBoolean running = new AtomicBoolean(Boolean.TRUE);

    private KafkaProducer<String, String> producer;

    public SimpleProducer() {
    }

    public void init(String kafkaUrl, String topic) {
        this.topic = topic;

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        producer = new KafkaProducer<>(props);
    }

    @Override
    public void run() {

        int messageNo = 1;
        while (isRunning()) {
            String messageStr = messageNo + " The quick brown fox jumped over the lazy dog.";

            try {
                producer.send(new ProducerRecord<>(topic,
                        String.valueOf(messageNo),
                        messageStr)).get();
                System.out.println("Sent message: (" + messageNo + ", " + messageStr + ")");

                Thread.sleep(5 * 1000);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                // handle the exception
            }
            ++messageNo;
        }
    }


    /**
     * True when a producer is running; otherwise false
     */
    private boolean isRunning() {
        return running.get();
    }

    /*
     * Shutdown hook which can be called from a separate thread.
     */
    public void shutdown() {
        if (isRunning()) {
            logger.info("Shutting down the Producer.");
            running.set(Boolean.FALSE);
        }
    }
}
