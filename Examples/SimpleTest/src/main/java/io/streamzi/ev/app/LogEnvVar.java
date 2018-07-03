package io.streamzi.ev.app;

import java.util.Map;
import java.util.logging.Logger;

public class LogEnvVar {

    private static final Logger logger = Logger.getLogger(LogEnvVar.class.getName());

    public static void main(String... args) {

        logger.info("Starting Example App");

        StringBuilder builder = new StringBuilder();
        Map<String, String> envVars = System.getenv();
        for(String key : envVars.keySet()){
            builder.append(key).append(": ").append(envVars.get(key)).append("\n");
        }

        logger.info(builder.toString());

        logger.info("Terminating Example App");
    }
}
