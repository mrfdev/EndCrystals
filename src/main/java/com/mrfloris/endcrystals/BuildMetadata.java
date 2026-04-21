package com.mrfloris.endcrystals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record BuildMetadata(
        String pluginVersion,
        String buildNumber,
        String targetPaperVersion,
        String targetMinecraftVersion,
        String javaTarget
) {

    public static BuildMetadata load() {
        Properties properties = new Properties();

        try (InputStream stream = BuildMetadata.class.getClassLoader().getResourceAsStream("build-info.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ignored) {
            // Fall back to defaults below when the resource cannot be read.
        }

        return new BuildMetadata(
                properties.getProperty("pluginVersion", "unknown"),
                properties.getProperty("buildNumber", "unknown"),
                properties.getProperty("targetPaperVersion", "unknown"),
                properties.getProperty("targetMinecraftVersion", "unknown"),
                properties.getProperty("javaTarget", "unknown")
        );
    }
}
