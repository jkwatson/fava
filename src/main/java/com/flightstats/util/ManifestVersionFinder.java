package com.flightstats.util;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * This class attempts to find the Implementation-Version field
 * from the manifest file of the jar file containing the given class.
 * In the event that the class is not running from a jar or if
 * the manifest attribute cannot be found, then the default value is used.
 */
public class ManifestVersionFinder {

    /**
     * Doesn't seem to handle inner classes.
     */
    public String findVersion(Class<?> clazz, String defaultVersion) {
        String className = clazz.getSimpleName() + ".class";
        URL classUri = clazz.getResource(className);
        if (classUri == null) {
            return defaultVersion;
        }

        String classPath = classUri.toString();
        if (!classPath.startsWith("jar")) {
            return defaultVersion;
        }
        return readVersionFromManifest(defaultVersion, classPath);
    }

    private String readVersionFromManifest(String defaultVersion, String classPath) {
        String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
        Manifest manifest;
        try {
            manifest = new Manifest(new URL(manifestPath).openStream());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        Attributes attr = manifest.getMainAttributes();
        String value = attr.getValue("Implementation-Version");
        return Objects.firstNonNull(value, defaultVersion);
    }
}
