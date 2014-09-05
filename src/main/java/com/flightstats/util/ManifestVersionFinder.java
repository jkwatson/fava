package com.flightstats.util;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;

import java.io.IOException;
import java.io.UncheckedIOException;
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
     * Doesn't handle inner classes.
     */
    public String findVersion(Class<?> contextClass, String defaultVersion) {
        String className = contextClass.getSimpleName() + ".class";
        URL classUri = contextClass.getResource(className);
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
        try {
            Manifest manifest = new Manifest(new URL(manifestPath).openStream());
            Attributes attr = manifest.getMainAttributes();
            String value = attr.getValue("Implementation-Version");
            return Objects.firstNonNull(value, defaultVersion);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
