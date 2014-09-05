package com.flightstats.util;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Properties;

/**
 * A utility to read a properties file from the classpath.
 */
public class PropertiesLoader {

    private final static Logger logger = LoggerFactory.getLogger(PropertiesLoader.class);

    public Properties readProperties(String propertiesFile) {
        URL propertyFileUrl = Resources.getResource(propertiesFile);
        logger.info("Loading properties file from " + propertyFileUrl);
        try (InputStream stream = propertyFileUrl.openStream()) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load properties", e);
        }
    }
}
