package io.streamzi.ev.operator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

import java.util.List;
import java.util.logging.Logger;

/**
 * EnvironmentVariableOperator that will take the payload of a DeploymentConfig and check to see if there are ConfigMaps
 * containing environment variables that should be applied to it
 */
public class DeploymentConfigOperator implements EnvironmentVariableOperator<DeploymentConfig> {

    private final Logger logger = Logger.getLogger(DeploymentConfigOperator.class.getName());

    private static final String TARGET_LABEL = "streamzi.io/target";

    private static final String KIND_LABEL = "streamzi.io/kind";

    @Override
    public void onAdded(DeploymentConfig dc) {
        deploymentConfigAdded(dc);
    }

    @Override
    public void onModified(DeploymentConfig dc) {
        deploymentConfigAdded(dc);
    }

    @Override
    public void onDeleted(DeploymentConfig dc) {
        //do nothing
    }

    /*
     * Finds all the ConfigMaps that might reference this DeploymentConfig and applies the Environment Variables to the
     * DeploymentConfig if it finds any.
     */
    private void deploymentConfigAdded(DeploymentConfig dc) {

        boolean updated = false;

        final String appName = dc.getMetadata().getLabels().get("app");

        final OpenShiftClient osClient = new DefaultOpenShiftClient();
        final List<ConfigMap> cms = osClient.configMaps().inNamespace(dc.getMetadata().getNamespace()).withLabel(TARGET_LABEL, appName).list().getItems();

        for (ConfigMap cm : cms) {

            if (cm.getMetadata().getLabels().containsKey(KIND_LABEL) && cm.getMetadata().getLabels().get(KIND_LABEL).equals("ev")) {

                for (String key : cm.getData().keySet()) {

                    final EnvVar ev = new EnvVar(Util.sanitiseEnvVar(key), cm.getData().get(key), null);

                    final List<Container> containers = dc.getSpec().getTemplate().getSpec().getContainers();

                    for (Container container : containers) {

                        if (container.getEnv().contains(ev)) {
                            break;
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
                }
            }

            if (updated) {
                osClient.deploymentConfigs().inNamespace(dc.getMetadata().getNamespace()).createOrReplace(dc);
            }
        }
    }


}
