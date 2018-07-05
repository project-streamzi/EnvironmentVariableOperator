package io.streamzi.ev.operator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.streamzi.ev.NoLabelException;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static io.streamzi.ev.operator.Util.sanitiseEnvVar;

/**
 * EnvironmentVariableOperator that will take the payload of a ConfigMap and set Environment Variables in a container.
 * Requires a label of streamzi.io/target=<TARGET_APP> to indicate which application will receive the Environment Variables.
 */
public class ConfigMapOperator implements EnvironmentVariableOperator<ConfigMap> {

    private final Logger logger = Logger.getLogger(ConfigMapOperator.class.getName());

    private static final String TARGET_KEY_LABEL = "streamzi.io/targetKey";

    private static final String TARGET_VALUE_LABEL = "streamzi.io/targetValue";

    private OpenShiftClient osClient;

    public ConfigMapOperator() {
        osClient = new DefaultOpenShiftClient();
    }

    @Override
    public void onAdded(ConfigMap configMap) throws NoLabelException {
        configMapToDeploymentConfig(configMap, false);
    }

    @Override
    public void onModified(ConfigMap configMap) throws NoLabelException {
        //TODO: It would be nice to remove values which previously came from this ConfigMap but I don't think that this is possible as we only receive the updated ConfigMap.
        configMapToDeploymentConfig(configMap, false);
    }

    @Override
    public void onDeleted(ConfigMap configMap) throws NoLabelException {
        configMapToDeploymentConfig(configMap, true);
    }

    /**
     * Attempt to keep a set of environment variables in a container in sync with a ConfigMap
     *
     * @param configMap The ConfigMap to take the variables from
     * @param remove    If true will remove the environment variable from the container
     */
    private void configMapToDeploymentConfig(ConfigMap configMap, boolean remove) throws NoLabelException {

        final String targetContainerLabelKey = Util.getLabelValue(configMap, TARGET_KEY_LABEL);
        final String targetContainerLabelValue = Util.getLabelValue(configMap, TARGET_VALUE_LABEL);

        //Only if we've got a valid container to target
        if (targetContainerLabelKey != null && targetContainerLabelValue != null) {


            //Deal with OpenShift Deployment Configs
            List<DeploymentConfig> dcs = osClient.deploymentConfigs().inNamespace(configMap.getMetadata().getNamespace())
                    .withLabel(targetContainerLabelKey, targetContainerLabelValue).list().getItems();

            for (DeploymentConfig dc : dcs) {

                boolean updated = false;

                //For each EnvVar
                final Map<String, String> data = configMap.getData();
                for (String key : data.keySet()) {

                    //Create a new sanitised EnvVar. x.y.z -> X_Y_Z
                    final EnvVar ev = new EnvVar(sanitiseEnvVar(key), data.get(key), null);

                    //For each container
                    final List<Container> containers = dc.getSpec().getTemplate().getSpec().getContainers();
                    for (Container container : containers) {

                        //Update the container if necessary
                        updated = updateContainer(container, ev, remove);
                    }
                }

                //Push change to OpenShift
                if (updated) {
                    osClient.deploymentConfigs().inNamespace(configMap.getMetadata().getNamespace()).createOrReplace(dc);
                }
            }

            //Deal with k8s Deployments
            List<Deployment> deployments = osClient.extensions().deployments().inNamespace(configMap.getMetadata().getNamespace())
                    .withLabel(targetContainerLabelKey, targetContainerLabelValue).list().getItems();

            for (Deployment deployment : deployments) {

                boolean updated = false;

                //For each EnvVar
                final Map<String, String> data = configMap.getData();
                for (String key : data.keySet()) {

                    //Create a new sanitised EnvVar. x.y.z -> X_Y_Z
                    final EnvVar ev = new EnvVar(sanitiseEnvVar(key), data.get(key), null);

                    //For each container
                    final List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
                    for (Container container : containers) {

                        updated = updateContainer(container, ev, remove);
                    }
                }

                //Push change to OpenShift
                if (updated) {
                    osClient.extensions().deployments().inNamespace(configMap.getMetadata().getNamespace()).createOrReplace(deployment);
                }
            }

        } else

        {
            throw new NoLabelException("Ignoring ConfigMap as it has not label (streamzi.io/target=<APP>) to identify container");
        }

    }

    /*
     * Add the Environment Variable to the container if it is different or doesn't exist.
     * Will remove the EnvironmentVariable if remove == true;
     */
    private boolean updateContainer(Container container, EnvVar ev, boolean remove) {

        boolean updated = false;
        //Remove the EnvVar if the CM has been deleted
        if (remove) {
            if (container.getEnv() != null && container.getEnv().contains(ev)) {
                logger.info("Removing " + ev);
                container.getEnv().remove(ev);
                updated = true;
            }
        } else {

            //Do not update the DC if the EnvVar is the same as an existing one
            if (container.getEnv().contains(ev)) {
                return false;
            } else {

                logger.info("Creating / updating " + ev);

                //Remove other EnvVars with the same name.
                //Necessary otherwise get multiple Environment Variables with the same key which would lead to unpredictable behaviour.
                container.getEnv().removeIf(existing ->
                        existing.getName().toUpperCase().equals(ev.getName().toUpperCase()));

                //Add
                container.getEnv().add(ev);
                updated = true;
            }
        }
        return updated;
    }

}