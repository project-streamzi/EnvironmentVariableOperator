package io.streamzi.ev.operator;

import io.fabric8.kubernetes.api.model.ConfigMap;

public class Util {

    /**
     * Returns an uppercase version of the input with '.' replaced with '_' suitable for use as UNIX environment variables.
     *
     * @param key String to sanitise
     * @return sanitised String
     */
    public static String sanitiseEnvVar(String key) {
        return key.replace('.', '_').toUpperCase();
    }

    //Split the label into the container name that we are targetting
    public static String getTargetContainerName(ConfigMap configMap, String targetLabel) throws IllegalArgumentException {

        if (configMap.getMetadata().getLabels() != null) {

            final String target = configMap.getMetadata().getLabels().get(targetLabel);

            if (target == null || target.equals("")) {
                return null;
            }

            return target;
        } else {
            return null;
        }
    }

}
