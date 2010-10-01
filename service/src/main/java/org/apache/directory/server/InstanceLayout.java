package org.apache.directory.server;


/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

import java.io.File;


/**
 * Convenience class to encapsulate paths to various directories and files within
 * an instance.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InstanceLayout extends AbstractLayout
{
    // Static final fields for system property names
    private static final String LOG_DIR = "apacheds.log.dir";
    private static final String RUN_DIR = "apacheds.run.dir";


    /**
     * Creates a new instance of InstanceLayout.
     *
     * @param instanceDirectory
     *      the instance directory
     */
    public InstanceLayout( File instanceDirectory )
    {
        super( instanceDirectory );
        init();
    }


    /**
     * Creates a new instance of InstanceLayout.
     *
     * @param instanceDirectoryPath
     *      the path to the instance directory
     */
    public InstanceLayout( String instanceDirectoryPath )
    {
        super( instanceDirectoryPath );
        init();
    }


    /**
     * Initializes the InstanceLayout.
     */
    private void init()
    {
        // The required directories
        File[] requiredDirectories = new File[]
            {
                getInstanceDirectory(),
                getConfDirectory(),
                getLogDirectory(),
                getPartitionsDirectory(),
                getRunDirectory()
            };
        setRequiredDirectories( requiredDirectories );

        // The required files
        File[] requiredFiles = new File[]
            {
                getWrapperConfigurationFile(),
                getLogConfigurationFile() /*,
                                          getApacheDsConfigurationLdifFile() */// TODO re-activate this when possible.
            };
        setRequiredFiles( requiredFiles );
    }


    /**
     * Gets the 'conf' directory.
     *
     * @return
     *      the 'conf' directory
     */
    public File getConfDirectory()
    {
        return new File( getInstanceDirectory(), "conf" );
    }


    /**
     * Gets the 'log' directory.
     *
     * @return
     *      the 'log' directory
     */
    public File getLogDirectory()
    {
        String logDir = System.getProperty( LOG_DIR );

        if ( logDir != null )
        {
            return new File( logDir );
        }

        return new File( getInstanceDirectory(), "log" );
    }


    /**
     * Gets the 'partitions' directory.
     *
     * @return
     *      the 'partitions' directory
     */
    public File getPartitionsDirectory()
    {
        return new File( getInstanceDirectory(), "partitions" );
    }


    /**
     * Gets the 'run' directory in the installation directory.
     *
     * @return
     *      the 'run' directory
     */
    public File getRunDirectory()
    {
        String runDir = System.getProperty( RUN_DIR );

        if ( runDir != null )
        {
            return new File( runDir );
        }

        return new File( getInstanceDirectory(), "run" );
    }


    /**
     * Gets the instance directory.
     *
     * @return
     *      the instance directory
     */
    public File getInstanceDirectory()
    {
        return getDirectory();
    }


    /**
     * Gets the log configuration file (<em>'/conf/log4j.properties'</em>).
     *
     * @return
     *      the log configuration file
     */
    public File getLogConfigurationFile()
    {
        return new File( getConfDirectory(), "log4j.properties" );
    }


    /**
     * Gets the wrapper configuration file (<em>'/conf/wrapper.conf'</em>).
     *
     * @return
     *      the wrapper configuration file
     */
    public File getWrapperConfigurationFile()
    {
        return new File( getConfDirectory(), "wrapper.conf" );
    }


    /**
     * Gets the apacheds configuration ldif file (<em>'/conf/wrapper.conf'</em>).
     *
     * @return
     *      the apacheds configuration ldif file
     */
    public File getApacheDsConfigurationLdifFile()
    {
        return new File( getConfDirectory(), "config.ldif" );
    }
}
