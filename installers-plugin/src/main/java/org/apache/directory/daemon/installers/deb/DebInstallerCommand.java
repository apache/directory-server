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
package org.apache.directory.daemon.installers.deb;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.directory.daemon.installers.MojoCommand;
import org.apache.directory.daemon.installers.MojoHelperUtils;
import org.apache.directory.daemon.installers.ServiceInstallersMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.taskdefs.Execute;


/**
 * Nullsoft INstaller System (NSIS) Installer command for Windows installers
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DebInstallerCommand extends MojoCommand
{
    private final Properties filterProperties = new Properties( System.getProperties() );
    /** The Deb target */
    private final DebTarget target;
    /** The Maven logger */
    private final Log log;
    /** The dpkg utility*/
    private File dpkgUtility;


    /**
     * Creates a new instance of DebInstallerCommand.
     *
     * @param mymojo
     *      the Server Installers Mojo
     * @param target
     *      the DEB target
     */
    public DebInstallerCommand( ServiceInstallersMojo mymojo, DebTarget target )
    {
        super( mymojo );
        this.target = target;
        this.log = mymojo.getLog();
        initializeFiltering();
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for linux or the dpkg utility could not be found.</li>
     *   <li>Creates the Debian DEB package for Apache DS</li>
     * </ol>
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Verifying the target is linux
        if ( !target.getOsFamily().equals( "linux" ) )
        {
            log.warn( "DEB package installer can only be targeted for Linux platforms!" );
            log.warn( "The build will continue, but please check the the platform of this installer " );
            log.warn( "target" );
            return;
        }

        // Verifying the dpkg utility exists
        if ( !target.getDpkgUtility().exists() )
        {
            log.warn( "Cannot find dpkg utility at this location: " + target.getDpkgUtility() );
            log.warn( "The build will continue, but please check the location of your dpkg " );
            log.warn( "utility." );
            return;
        }
        else
        {
            dpkgUtility = target.getDpkgUtility();
        }

        File baseDirectory = target.getLayout().getBaseDirectory();
        File imagesDirectory = baseDirectory.getParentFile();

        log.info( "Creating Debian DEB Package..." );

        // Creating the package directory
        File debDirectory = new File( imagesDirectory, target.getId() + "-deb" );
        debDirectory.mkdirs();

        log.info( "Copying DEB Package files" );

        // Copying the apacheds files in the '/opt/apacheds-$VERSION/' directory
        File debApacheDsHomeDirectory = new File( debDirectory, "opt/apacheds-" + target.getApplication().getVersion() );
        try
        {
            // Copying the generated layout
            MojoHelperUtils.copyFiles( baseDirectory, debApacheDsHomeDirectory );

            // Replacing the apacheds.conf file
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds.conf" ),
                new File( debApacheDsHomeDirectory, "conf/apacheds.conf" ), false );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy image (" + target.getLayout().getBaseDirectory()
                + ") to the DEB directory (" + debApacheDsHomeDirectory + ")" );
        }

        // Copying the instances in the '/var/lib/apacheds-$VERSION/default' directory
        File debDefaultInstanceDirectory = new File( debDirectory, "var/lib/apacheds-"
            + target.getApplication().getVersion() + "/default" );
        debDefaultInstanceDirectory.mkdirs();
        File debDefaultInstanceConfDirectory = new File( debDefaultInstanceDirectory, "conf" );
        debDefaultInstanceConfDirectory.mkdirs();
        new File( debDefaultInstanceDirectory, "ldif" ).mkdirs();
        new File( debDefaultInstanceDirectory, "log" ).mkdirs();
        new File( debDefaultInstanceDirectory, "partitions" ).mkdirs();
        new File( debDefaultInstanceDirectory, "run" ).mkdirs();
        File debEtcInitdDirectory = new File( debDirectory, "etc/init.d" );
        debEtcInitdDirectory.mkdirs();
        new File( debDirectory, "/var/run/apacheds-" + target.getApplication().getVersion() ).mkdirs();
        try
        {
            // Copying the apacheds.conf file in the default instance conf directory
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream(
                "apacheds-default.conf" ), new File( debDefaultInstanceConfDirectory, "apacheds.conf" ), false );

            // Copying the log4j.properties file in the default instance conf directory
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, new File( debApacheDsHomeDirectory,
                "conf/log4j.properties" ), new File( debDefaultInstanceConfDirectory, "log4j.properties" ), false );

            // Copying the server.xml file in the default instance conf directory
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, new File( debApacheDsHomeDirectory,
                "conf/server.xml" ), new File( debDefaultInstanceConfDirectory, "server.xml" ), false );

            // Copying the init script in /etc/init.d/
            MojoHelperUtils
                .copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds-init" ), new File(
                    debEtcInitdDirectory, "apacheds-" + target.getApplication().getVersion() + "-default" ), true );

            // Removing the redundant server.xml file (see DIRSERVER-1112)
            new File( debApacheDsHomeDirectory, "conf/server.xml" ).delete();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy resources files to the DEB directory ("
                + debDefaultInstanceDirectory + ")" );
        }

        // Create DEBIAN directory
        File debDebianDirectory = new File( debDirectory, "DEBIAN" );
        debDebianDirectory.mkdirs();

        // Copying the 'control' file
        try
        {
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "control" ),
                new File( debDebianDirectory, "control" ), true );

            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "postinst" ),
                new File( debDebianDirectory, "postinst" ), true );

            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "prerm" ),
                new File( debDebianDirectory, "prerm" ), true );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy DEB 'control' file." );
        }

        // Setting correct permission on the postinst script
        MojoHelperUtils.exec( new String[]
            { "chmod", "755", new File( debDebianDirectory, "postinst" ).toString() }, debDebianDirectory, false );
        MojoHelperUtils.exec( new String[]
            { "chmod", "755", new File( debDebianDirectory, "prerm" ).toString() }, debDebianDirectory, false );

        // Generating the DEB
        log.info( "Generating Debian DEB Package" );
        String finalName = target.getFinalName();
        if ( !finalName.endsWith( ".deb" ) )
        {
            finalName = finalName + ".deb";
        }
        Execute createDebTask = new Execute();
        String[] cmd = new String[]
            { dpkgUtility.getAbsolutePath(), "-b", target.getId() + "-deb", finalName };
        createDebTask.setCommandline( cmd );
        createDebTask.setSpawn( true );
        createDebTask.setWorkingDirectory( imagesDirectory );

        try
        {
            createDebTask.execute();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the DEB package: " + e.getMessage() );
        }

        log.info( "Debian DEB package generated at " + new File( imagesDirectory, finalName ) );
    }


    private void initializeFiltering()
    {
        filterProperties.putAll( mymojo.getProject().getProperties() );
        filterProperties.put( "app", target.getApplication().getName() );
        String version = target.getApplication().getVersion();
        if ( target.getApplication().getVersion() != null )
        {
            if ( version.endsWith( "-SNAPSHOT" ) )
            {
                filterProperties.put( "version.debian", version + "1" );
            }
            else
            {
                filterProperties.put( "version.debian", version );
            }

            filterProperties.put( "app.version", version );
        }
        else
        {
            filterProperties.put( "app.version", "1.0" );
        }
        filterProperties.put( "arch", target.getOsArch() );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.daemon.installers.MojoCommand#getFilterProperties()
     */
    public Properties getFilterProperties()
    {
        return filterProperties;
    }
}
