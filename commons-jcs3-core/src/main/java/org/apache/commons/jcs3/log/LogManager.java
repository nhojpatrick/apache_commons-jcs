package org.apache.commons.jcs3.log;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * This is a borrowed and stripped-down version of the log4j2 LogManager class.
 *
 * The anchor point for the JCS logging system. The most common usage of this
 * class is to obtain a named {@link Log}.
 */
public class LogManager
{
    /**
     * The SPI LogFactory
     */
    private static final class LogFactoryHolder
    {
        static final LogFactory INSTANCE = createLogFactory();

        /**
         * Scans the classpath to find a logging implementation.
         *
         * @return the LogFactory
         * @throws RuntimeException, if no factory implementation could be found
         */
        private static LogFactory createLogFactory()
        {
            final ServiceLoader<LogFactory> factories = ServiceLoader.load(LogFactory.class);
            if (LogManager.logSystem == null)
            {
                LogManager.logSystem = System.getProperty("jcs.logSystem",
                        LOGSYSTEM_JAVA_UTIL_LOGGING);
            }

            // Store errors that may occur until log system is available
            final List<ServiceConfigurationError> errors = new ArrayList<>();
            LogFactory factory = null;
            for (final LogFactory instance : factories) {
                try
                {
                    if (logSystem.equalsIgnoreCase(instance.getName()))
                    {
                        factory = instance;
                        break;
                    }
                }
                catch (final ServiceConfigurationError e)
                {
                    errors.add(e);
                }
            }
            if (factory != null)
            {
                if (!errors.isEmpty())
                {
                    final Log log = factory.getLog(LogFactoryHolder.class);

                    for (final ServiceConfigurationError error : errors)
                    {
                        log.debug("Error loading LogFactory", error);
                    }
                    log.debug("Found LogFactory for " + logSystem);
                }
                return factory;
            }

            // No log system could be found --> report errors to stderr
            errors.forEach(e -> System.err.println(e.getMessage()));
            throw new IllegalStateException("Could not find factory implementation for log subsystem " + logSystem);
        }
    }

    /**
     * The name of log subsystem
     */
    private static String logSystem;
    /** Log systems currently known */
    public static final String LOGSYSTEM_JAVA_UTIL_LOGGING = "jul";

    public static final String LOGSYSTEM_LOG4J2 = "log4j2";

    /**
     * Returns a Log using the fully qualified name of the Class as the Log
     * name.
     *
     * @param clazz
     *            The Class whose name should be used as the Log name.
     * @return The Log.
     * @throws UnsupportedOperationException
     *             if {@code clazz} is {@code null}
     */
    public static Log getLog(final Class<?> clazz)
    {
        return getLogFactory().getLog(clazz);
    }

    /**
     * Returns a Log with the specified name.
     *
     * @param name
     *            The logger name.
     * @return The Log.
     * @throws UnsupportedOperationException
     *             if {@code name} is {@code null}
     */
    public static Log getLog(final String name)
    {
        return getLogFactory().getLog(name);
    }

    /**
     * Return the LogFactory
     */
    private static LogFactory getLogFactory()
    {
        return LogFactoryHolder.INSTANCE;
    }

    /**
     * Returns the root logger.
     *
     * @return the root logger, named {@link LogFactory#ROOT_LOGGER_NAME}.
     */
    public static Log getRootLogger()
    {
        return getLog(LogFactory.ROOT_LOGGER_NAME);
    }

    /**
     * Sets the log system. Must be called before getLog is called
     *
     * @param logSystem the logSystem to set
     */
    public static void setLogSystem(final String logSystem)
    {
        LogManager.logSystem = logSystem;
    }

    /**
     * Shutdown the logging system if the logging system supports it.
     */
    public static void shutdown()
    {
        getLogFactory().shutdown();
    }

    /**
     * Prevents instantiation
     */
    protected LogManager()
    {
    }
}
