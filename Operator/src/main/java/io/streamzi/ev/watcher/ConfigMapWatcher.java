/*
 * Copyright 2017-2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.streamzi.ev.watcher;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.streamzi.ev.NoLabelException;
import io.streamzi.ev.operator.EnvironmentVariableOperator;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Watch for changes in a ConfigMap and push them to an EnvironmentVariableOperator for applying changes to the Environment Variables of containers
 */
public class ConfigMapWatcher implements Watcher<ConfigMap>, Runnable {

    private final static Logger logger = Logger.getLogger(ConfigMapWatcher.class.getName());

    //Label that we're going to watch. e.g. streamzi.io/kind=ev
    private String cmPredicate;

    //EnvironmentVariableOperator for updating Environment variables
    private EnvironmentVariableOperator<ConfigMap> operator;


    public ConfigMapWatcher(EnvironmentVariableOperator<ConfigMap> operator, String cmPredicate) {
        this.operator = operator;
        this.cmPredicate = cmPredicate;
    }

    @Override
    public void eventReceived(Action action, ConfigMap configMap) {

        ObjectMeta metadata = configMap.getMetadata();
        Map<String, String> labels = metadata.getLabels();

        if (labelValid(cmPredicate, configMap)) {
            String name = metadata.getName();

            logger.info("ConfigMap watch received event " + action + " on map " + name + " with labels" + labels);

            try {
                switch (action) {
                    case ADDED:
                        operator.onAdded(configMap);
                        break;
                    case MODIFIED:
                        operator.onModified(configMap);
                        break;
                    case DELETED:
                        operator.onDeleted(configMap);
                        break;
                    case ERROR:
                        logger.warning("Watch received action=ERROR for ConfigMap " + name);
                }
            } catch (NoLabelException e) {
                logger.warning(e.getMessage());
            }
        }
    }

    /**
     * Thread that's running the CM Watcher
     */
    @Override
    public void run() {
        logger.info("Starting ConfigMapWatcher");

        final KubernetesClient client = new DefaultKubernetesClient();

        client.configMaps().inNamespace(client.getNamespace()).watch(this);
    }


    @Override
    public void onClose(KubernetesClientException e) {
        logger.info("Closing Watcher: " + this);
    }

    /**
     * Is the label valid according to the predicate that we're 'listening' to?
     *
     * @param predictate label in the form of key=value e.g. streamzi.io/kind=ev
     * @param configMap  ConfigMap to test
     * @return true if the key=value label exists, false if not
     */
    private boolean labelValid(String predictate, ConfigMap configMap) {
        String[] parts = predictate.split("=");
        String labelKey = parts[0];
        String labelValue = parts[1];

        if (configMap.getMetadata().getLabels() != null && configMap.getMetadata().getLabels().containsKey(labelKey)) {
            return configMap.getMetadata().getLabels().get(labelKey).equals(labelValue);
        }
        return false;
    }
}
