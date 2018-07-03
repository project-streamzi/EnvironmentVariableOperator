package io.streamzi.ev;

import io.streamzi.ev.operator.ConfigMapOperator;
import io.streamzi.ev.operator.DeploymentConfigOperator;
import io.streamzi.ev.watcher.DeploymentConfigWatcher;
import io.streamzi.ev.watcher.ConfigMapWatcher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Start the ConfigMap Watcher
 */
public class Manager {

    private static final Logger logger = Logger.getLogger(Manager.class.getName());

    private static final String CM_PREDICATE = "streamzi.io/kind=ev";

    public Manager() {
    }

    public static void main(String[] args) {

        DeploymentConfigWatcher dcw = new DeploymentConfigWatcher(new DeploymentConfigOperator());
        ConfigMapWatcher cmw = new ConfigMapWatcher(new ConfigMapOperator(), CM_PREDICATE);

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(dcw);executor.submit(cmw);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down");
            executor.shutdown();
            try {
                executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                logger.log(Level.SEVERE, "Error on close", ie);
            }
        }));


    }

}
