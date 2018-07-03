/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.streamzi.ev.watcher;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.streamzi.ev.NoLabelException;
import io.streamzi.ev.operator.EnvironmentVariableOperator;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Watch for changes in a ConfigMap and push them to an EnvironmentVariableOperator for applying changes to the Environment Variables of containers
 */
public class DeploymentConfigWatcher implements Watcher<DeploymentConfig>, Runnable {

    private final static Logger logger = Logger.getLogger(DeploymentConfigWatcher.class.getName());

    //EnvironmentVariableOperator for updating Environment variables
    private EnvironmentVariableOperator<DeploymentConfig> operator;


    public DeploymentConfigWatcher(EnvironmentVariableOperator<DeploymentConfig> operator) {
        this.operator = operator;
    }

    @Override
    public void eventReceived(Action action, DeploymentConfig dc) {

        final Map<String, String> labels = dc.getMetadata().getLabels();
        final String name = dc.getMetadata().getName();
        logger.info("DeploymentConfig watch received event " + action + " on map " + name + " with labels" + labels);

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
                    logger.warning("Watch received action=ERROR for DeploymentConfig " + name);
            }
        } catch (NoLabelException e) {
            logger.warning(e.getMessage());
        }


    }

    @Override
    public void run() {
        logger.info("Starting DeploymentConfigWatcher");

        final OpenShiftClient osClient = new DefaultOpenShiftClient();
        osClient.deploymentConfigs().inNamespace(osClient.getNamespace()).watch(this);
    }

    @Override
    public void onClose(KubernetesClientException e) {
        logger.info("Closing Watcher: " + this);
    }
}
