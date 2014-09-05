package com.flightstats.util;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * A utility to read a properties file from the classpath.
 */
public class PropertiesLoader {

    private final static Logger logger = LoggerFactory.getLogger(PropertiesLoader.class);

    public Properties readProperties(String propertiesFile) {
        URL propertyFileUrl = getClass().getClassLoader().getResource(propertiesFile);
        if (propertyFileUrl == null) {
            throw new RuntimeException("Property file '" + propertiesFile + "' not found");
        }
        logger.info("Loading properties file from " + propertyFileUrl);
        try (InputStream stream = propertyFileUrl.openStream()) {
            Properties props = new Properties();
            props.load(stream);
            return props;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
