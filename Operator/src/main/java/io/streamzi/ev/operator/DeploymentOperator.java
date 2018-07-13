package io.streamzi.ev.operator;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.logging.log4j.LogManager;

import java.util.List;

/**
 * EnvironmentVariableOperator that will take the payload of a DeploymentConfig and check to see if there are ConfigMaps
 * containing environment variables that should be applied to it
 */
public class DeploymentOperator implements EnvironmentVariableOperator<Deployment> {

    private final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(DeploymentOperator.class);

    private static final String TARGET_LABEL = "streamzi.io/target";

    private static final String KIND_LABEL = "streamzi.io/kind";

    public static final String KIND_VALUE = "ev";

    @Override
    public void onAdded(Deployment d) {
        deploymentAdded(d);
    }

    @Override
    public void onModified(Deployment d) {
        deploymentAdded(d);
    }

    @Override
    public void onDeleted(Deployment d) {
        //do nothing
    }

    /*
     * Finds all the ConfigMaps that might reference this DeploymentConfig and applies the Environment Variables to the
     * DeploymentConfig if it finds any.
     */
    private void deploymentAdded(Deployment d) {

        boolean updated = false;

        final String appName = d.getMetadata().getName();

        final OpenShiftClient osClient = new DefaultOpenShiftClient();
        final List<ConfigMap> cms = osClient.configMaps().inNamespace(d.getMetadata().getNamespace()).withLabel(TARGET_LABEL, appName).list().getItems();

        for (ConfigMap cm : cms) {

            if (cm.getMetadata().getLabels().containsKey(KIND_LABEL) && cm.getMetadata().getLabels().get(KIND_LABEL).equals(KIND_VALUE)) {

                for (String key : cm.getData().keySet()) {

                    final EnvVar ev = new EnvVar(Util.sanitiseEnvVar(key), cm.getData().get(key), null);

                    final List<Container> containers = d.getSpec().getTemplate().getSpec().getContainers();

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
        }

        if (updated) {
            logger.info("Updating Deployment: " + d.getMetadata().getName());
            osClient.extensions().deployments().inNamespace(d.getMetadata().getNamespace()).createOrReplace(d);
        }

    }


}
