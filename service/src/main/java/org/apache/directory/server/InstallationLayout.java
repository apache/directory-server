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
 * an installation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InstallationLayout
{
    /** The logger*/
    private final static Logger log = LoggerFactory.getLogger( InstallationLayout.class );

    /** The required directories */
    private File[] requiredDirectories = new File[]
        {
            getInstallationDirectory(),
            getBinDirectory(),
            getConfDirectory(),
            getLibDirectory()
        };

    /** The required files */
    private File[] requiredFiles = new File[]
        {
            getScriptFile(),
            getWrapperFile(),
            getWrapperConfigurationFile()
        };

    /** The installation directory */
    private File installationDirectory;


    /**
     * Creates a new instance of InstallationLayout.
     *
     * @param installationDirectory
     *      the installation directory
     */
    public InstallationLayout( File installationDirectory )
    {
        this.installationDirectory = installationDirectory;
    }


    /**
     * Creates a new instance of InstallationLayout.
     *
     * @param installationDirectoryPath
     *      the path to the installation directory
     */
    public InstallationLayout( String installationDirectoryPath )
    {
        this.installationDirectory = new File( installationDirectoryPath );
    }


    /**
     * Gets the 'bin' directory in the installation directory.
     *
     * @return
     *      the 'bin' directory
     */
    public File getBinDirectory()
    {
        return new File( getInstallationDirectory(), "bin" );
    }


    /**
     * Gets the 'conf' directory in the installation directory.
     *
     * @return
     *      the 'conf' directory
     */
    public File getConfDirectory()
    {
        return new File( getInstallationDirectory(), "conf" );
    }


    /**
     * Gets the installation directory.
     *
     * @return
     *      the installation directory
     */
    public File getInstallationDirectory()
    {
        return installationDirectory;
    }


    /**
     * Gets the 'lib' directory in the installation directory.
     *
     * @return
     *      the 'lib' directory
     */
    public File getLibDirectory()
    {
        return new File( getInstallationDirectory(), "lib" );
    }


    /**
     * Gets the LICENSE file (<em>'/LICENSE'</em>).
     *
     * @return
     *      the LICENSE file
     */
    public File getLicenseFile()
    {
        return new File( getInstallationDirectory(), "LICENSE" );
    }


    /**
     * Gets the NOTICE file (<em>'/NOTICE'</em>).
     *
     * @return
     *      the NOTICE file
     */
    public File getNoticeFile()
    {
        return new File( getInstallationDirectory(), "NOTICE" );
    }


    /**
     * Gets the script file (<em>'/bin/apacheds'</em>).
     *
     * @return
     *      the script file
     */
    public File getScriptFile()
    {
        return new File( getBinDirectory(), "apacheds" );
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
     * Gets the wrapper file (<em>'/bin/wrapper'</em>).
     *
     * @return
     *      the wrapper file
     */
    public File getWrapperFile()
    {
        return new File( getBinDirectory(), "wrapper" );
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
