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
package org.apache.directory.server.core.api;


import java.io.File;
import java.io.IOException;

import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Convenience class to encapsulate paths to various directories and files within
 * an layout.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractLayout
{
    /** The logger*/
    private static final Logger LOG = LoggerFactory.getLogger( AbstractLayout.class );

    /** The required directories */
    private File[] requiredDirectories = new File[0];

    /** The required files */
    private File[] requiredFiles = new File[0];

    /** The base directory */
    private File directory;


    /**
     * Creates a new instance of AbstractLayout.
     *
     * @param directory the base directory
     */
    protected AbstractLayout( File directory )
    {
        this.directory = directory;
    }


    /**
     * Creates a new instance of AbstractLayout.
     *
     * @param directoryPath the path to the base directory
     */
    protected AbstractLayout( String directoryPath )
    {
        this.directory = new File( directoryPath );
    }


    /**
     * Gets the base directory.
     *
     * @return the base directory
     */
    protected File getDirectory()
    {
        return directory;
    }


    /**
     * Gets the required directories.
     *
     * @return the required directories
     */
    public File[] getRequiredDirectories()
    {
        return requiredDirectories;
    }


    /**
     * Gets the required files.
     *
     * @return the required files
     */
    public File[] getRequiredFiles()
    {
        return requiredFiles;
    }


    /**
     * Creates the required directories (if they don't already exist).
     */
    public void mkdirs() throws IOException
    {
        for ( File requiredDirectory : requiredDirectories )
        {
            if ( !requiredDirectory.exists() )
            {
                if ( !requiredDirectory.mkdirs() )
                {
                    throw new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, requiredDirectory ) );
                }
            }
        }
    }


    /**
     * Sets the required directories.
     *
     * @param requiredDirectories an array of required directories
     */
    protected void setRequiredDirectories( File[] requiredDirectories )
    {
        this.requiredDirectories = requiredDirectories;
    }


    /**
     * Sets the required files.
     *
     * @param requiredFiles an array of required files
     */
    protected void setRequiredFiles( File[] requiredFiles )
    {
        this.requiredFiles = requiredFiles;
    }


    /**
     * Verifies the installation by checking required directories and files.
     */
    public void verifyInstallation()
    {
        LOG.debug( "Verifying required directories" );

        // Verifying required directories
        for ( File requiredDirectory : requiredDirectories )
        {
            // Exists?
            if ( !requiredDirectory.exists() )
            {
                String message = "The required '" + requiredDirectory + " directory does not exist!";
                LOG.error( message );
                throw new IllegalStateException( message );
            }

            // Directory?
            if ( requiredDirectory.isFile() )
            {
                String message = "'" + requiredDirectory + "' is a file when it should be a directory.";
                LOG.error( message );
                throw new IllegalStateException( message );
            }

            // Writable?
            if ( !requiredDirectory.canWrite() )
            {
                String message = "'" + requiredDirectory
                    + "' is write protected from the current user '"
                    + System.getProperty( "user.name" ) + "'";
                LOG.error( message );
                throw new IllegalStateException( message );
            }
        }

        LOG.debug( "Required directories verification finished successfully." );

        LOG.debug( "Verifying required files" );

        // Verifying required files
        for ( File requiredFile : requiredFiles )
        {
            // Exists?
            if ( !requiredFile.exists() )
            {
                String message = "The required'" + requiredFile + "' file does not exist!";
                LOG.error( message );
                throw new IllegalStateException( message );
            }

            // File?
            if ( requiredFile.isDirectory() )
            {
                String message = "'" + requiredFile + "' is a directory when it should be a file.";
                LOG.error( message );
                throw new IllegalStateException( message );
            }

            // Writable?
            if ( !requiredFile.canRead() )
            {
                String message = "'" + requiredFile + "' is not readable by the current user '"
                    + System.getProperty( "user.name" ) + "'.";
                LOG.error( message );
                throw new IllegalStateException( message );
            }
        }

        LOG.debug( "Required files verification finished successfully." );
    }
}
