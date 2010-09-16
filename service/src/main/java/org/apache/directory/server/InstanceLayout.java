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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Convenience class to encapsulate paths to various directories and files within
 * an instance.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InstanceLayout
{
    /** The logger*/
    private final static Logger log = LoggerFactory.getLogger( InstanceLayout.class );

    private static final String LOG_DIR = "apacheds.log.dir";
    private static final String RUN_DIR = "apacheds.run.dir";

    /** The required directories */
    private File[] requiredDirectories = new File[]
        {
            getInstanceDirectory(),
            getConfDirectory(),
            getLdifDirectory(),
            getLogDirectory(),
            getPartitionsDirectory(),
            getRunDirectory()
        };

    /** The required files */
    private File[] requiredFiles = new File[]
        {
            getWrapperConfigurationFile(),
            getLogConfigurationFile() /*,
                                      getApacheDsConfigurationLdifFile() */// TODO re-activate this when possible.
        };

    /** The instance directory */
    private File instanceDirectory;


    /**
     * Creates a new instance of InstanceLayout.
     *
     * @param instanceDirectory
     *      the instance directory
     */
    public InstanceLayout( File instanceDirectory )
    {
        this.instanceDirectory = instanceDirectory;
    }


    /**
     * Creates a new instance of InstanceLayout.
     *
     * @param instanceDirectoryPath
     *      the path to the instance directory
     */
    public InstanceLayout( String instanceDirectoryPath )
    {
        this.instanceDirectory = new File( instanceDirectoryPath );
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
     * Gets the 'ldif' directory.
     *
     * @return
     *      the 'ldif' directory
     */
    public File getLdifDirectory()
    {
        return new File( getInstanceDirectory(), "ldif" );
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
        return instanceDirectory;
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


    /**
     * Creates the required directories (if they don't already exist).
     */
    public void mkdirs()
    {
        for ( File requiredDirectory : requiredDirectories )
        {
            if ( !requiredDirectory.exists() )
            {
                requiredDirectory.mkdirs();
            }
        }
    }


    /**
     * Verifies the installation by checking required directories and files.
     */
    public void verifyInstallation()
    {
        log.debug( "Verifying required directories" );

        // Verifying required directories
        for ( File requiredDirectory : requiredDirectories )
        {
            // Exists?
            if ( !requiredDirectory.exists() )
            {
                String message = "The required '" + requiredDirectory + " directory does not exist!";
                log.error( message );
                throw new IllegalStateException( message );
            }

            // Directory?
            if ( requiredDirectory.isFile() )
            {
                String message = "'" + requiredDirectory + "' is a file when it should be a directory.";
                log.error( message );
                throw new IllegalStateException( message );
            }

            // Writable?
            if ( !requiredDirectory.canWrite() )
            {
                String message = "'" + requiredDirectory
                    + "' is write protected from the current user '"
                    + System.getProperty( "user.name" ) + "'";
                log.error( message );
                throw new IllegalStateException( message );
            }
        }

        log.debug( "Required directories verification finished successfully." );

        log.debug( "Verifying required files" );

        // Verifying required files
        for ( File requiredFile : requiredFiles )
        {
            // Exists?
            if ( !requiredFile.exists() )
            {
                String message = "The required'" + requiredFile + "' file does not exist!";
                log.error( message );
                throw new IllegalStateException( message );
            }

            // File?
            if ( requiredFile.isDirectory() )
            {
                String message = "'" + requiredFile + "' is a directory when it should be a file.";
                log.error( message );
                throw new IllegalStateException( message );
            }

            // Writable?
            if ( !requiredFile.canRead() )
            {
                String message = "'" + requiredFile + "' is not readable by the current user '"
                    + System.getProperty( "user.name" ) + "'.";
                log.error( message );
                throw new IllegalStateException( message );
            }
        }

        log.debug( "Required files verification finished successfully." );
    }
}
