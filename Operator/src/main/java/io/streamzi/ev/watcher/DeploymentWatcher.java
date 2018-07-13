/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.streamzi.ev.watcher;

import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.streamzi.ev.NoLabelException;
import io.streamzi.ev.operator.EnvironmentVariableOperator;
import org.apache.logging.log4j.LogManager;

import java.util.Map;

/**
 * Watch for changes in a Deployment and push them to an EnvironmentVariableOperator which will look to see if any
 * ConfigMaps contain Environment Variables that should be copied into this deployment
 */
public class DeploymentWatcher implements Watcher<Deployment>, Runnable {

    private final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(DeploymentWatcher.class);

    //EnvironmentVariableOperator for updating Environment variables
    private EnvironmentVariableOperator<Deployment> operator;


    public DeploymentWatcher(EnvironmentVariableOperator<Deployment> operator) {
        this.operator = operator;
    }

    @Override
    public void eventReceived(Action action, Deployment dc) {

        final Map<String, String> labels = dc.getMetadata().getLabels();
        final String name = dc.getMetadata().getName();
        logger.info("Deployment watch received event " + action + " on map " + name + " with labels" + labels);

        try {
            switch (action) {
                case ADDED:
                    operator.onAdded(dc);
                    break;
                case MODIFIED:
                    operator.onModified(dc);
                    break;
                case DELETED:
                    operator.onDeleted(dc);
                    break;
                case ERROR:
                    logger.warn("Watch received action=ERROR for Deployment " + name);
            }
        } catch (NoLabelException e) {
            logger.warn(e.getMessage());
        }


    }

    @Override
    public void run() {
        logger.info("Starting DeploymentWatcher");

        final OpenShiftClient osClient = new DefaultOpenShiftClient();
        osClient.extensions().deployments().inNamespace(osClient.getNamespace()).watch(this);
    }

    @Override
    public void onClose(KubernetesClientException e) {
        logger.info("Closing Watcher: " + this);
        logger.info(e.getMessage());
    }
}
