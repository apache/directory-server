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

import org.apache.directory.server.core.api.AbstractLayout;


/**
 * Convenience class to encapsulate paths to various directories and files within
 * an installation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InstallationLayout extends AbstractLayout
{
    /**
     * Creates a new instance of InstallationLayout.
     *
     * @param installationDirectory
     *      the installation directory
     */
    public InstallationLayout( File installationDirectory )
    {
        super( installationDirectory );
        init();
    }


    /**
     * Creates a new instance of InstallationLayout.
     *
     * @param installationDirectoryPath
     *      the path to the installation directory
     */
    public InstallationLayout( String installationDirectoryPath )
    {
        super( installationDirectoryPath );
        init();
    }


    /**
     * Initializes the InstallationLayout.
     */
    private void init()
    {
        // The required directories
        File[] requiredDirectories = new File[]
            {
                getInstallationDirectory(),
                getBinDirectory(),
                getConfDirectory(),
                getLibDirectory()
        };
        setRequiredDirectories( requiredDirectories );

        // The required files
        File[] requiredFiles = new File[]
            {
                getScriptFile(),
                getWrapperFile(),
                getWrapperConfigurationFile()
        };
        setRequiredFiles( requiredFiles );
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
        return getDirectory();
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
}
