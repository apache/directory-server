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
package org.apache.directory.daemon.installers.solarispkg;


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
 * PKG Installer command for creating Solaris packages.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SolarisPkgInstallerCommand extends MojoCommand
{
    private final Properties filterProperties = new Properties( System.getProperties() );
    /** The PKG target */
    private final SolarisPkgTarget target;
    /** The Maven logger */
    private final Log log;

    private File pkgMaker;
    private File pkgTranslator;


    /**
     * Creates a new instance of SolarisPkgInstallerCommand.
     *
     * @param mymojo
     *      the Server Installers Mojo
     * @param target
     *      the PKG target
     */
    public SolarisPkgInstallerCommand( ServiceInstallersMojo mymojo, SolarisPkgTarget target )
    {
        super( mymojo );
        this.target = target;
        this.log = mymojo.getLog();
        initializeFiltering();
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for solaris or the pkgmk or pkgtrans utilities coud not be found.</li>
     *   <li>Creates the Solaris PKG Installer for Apache DS</li>
     * </ol>
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Verifying the target is solaris
        if ( !target.getOsName().equals( "solaris" ) )
        {
            log.warn( "Solaris PKG installer can only be targeted for Solaris platform!" );
            log.warn( "The build will continue, but please check the the platform of this installer " );
            log.warn( "target" );
            return;
        }

        // Verifying the 'pkgmk' utility exists
        if ( !target.getPkgMaker().exists() )
        {
            log.warn( "Cannot find 'pkgmk' utility at this location: " + target.getPkgMaker() );
            log.warn( "The build will continue, but please check the location of your Package Maker " );
            log.warn( "utility." );
            return;
        }
        else
        {
            pkgMaker = target.getPkgMaker();
        }

        // Verifying the 'pkgtrans' utility exists
        if ( !target.getPkgTranslator().exists() )
        {
            log.warn( "Cannot find 'pkgtrans' utility at this location: " + target.getPkgTranslator() );
            log.warn( "The build will continue, but please check the location of your Package Maker " );
            log.warn( "utility." );
            return;
        }
        else
        {
            pkgTranslator = target.getPkgTranslator();
        }

        File baseDirectory = target.getLayout().getBaseDirectory();
        File imagesDirectory = baseDirectory.getParentFile();

        log.info( "Creating Solaris PKG Installer..." );

        // Creating the package directory
        File pkgDirectory = new File( imagesDirectory, target.getId() + "-pkg" );
        pkgDirectory.mkdirs();

        log.info( "Copying Solaris PKG installer files" );

        // Creating the root directories hierarchy
        File pkgRootDirectory = new File( pkgDirectory, "root" );
        pkgRootDirectory.mkdirs();

        // Copying the apacheds files in the '/opt/apacheds/' directory
        File apacheDsHomeDirectory = new File( pkgRootDirectory, "opt/apacheds" );
        try
        {
            // Copying the generated layout
            MojoHelperUtils.copyFiles( baseDirectory, apacheDsHomeDirectory );

            // Replacing the apacheds.conf file
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds.conf" ),
                new File( apacheDsHomeDirectory, "conf/apacheds.conf" ), false );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy image (" + target.getLayout().getBaseDirectory()
                + ") to the PKG directory (" + apacheDsHomeDirectory + ")" );
        }

        // Copying the instances in the '/var/opt/apacheds/default' directory
        File defaultInstanceDirectory = new File( pkgRootDirectory, "var/opt/apacheds" + "/default" );
        defaultInstanceDirectory.mkdirs();
        File debDefaultInstanceConfDirectory = new File( defaultInstanceDirectory, "conf" );
        debDefaultInstanceConfDirectory.mkdirs();
        new File( defaultInstanceDirectory, "ldif" ).mkdirs();
        new File( defaultInstanceDirectory, "log" ).mkdirs();
        new File( defaultInstanceDirectory, "partitions" ).mkdirs();
        new File( defaultInstanceDirectory, "run" ).mkdirs();
        File etcInitdDirectory = new File( pkgRootDirectory, "etc/init.d" );
        etcInitdDirectory.mkdirs();
        new File( pkgRootDirectory, "/var/run/apacheds" ).mkdirs();
        try
        {
            // Copying the apacheds.conf file in the default instance conf directory
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream(
                "apacheds-default.conf" ), new File( debDefaultInstanceConfDirectory, "apacheds.conf" ), false );

            // Copying the log4j.properties file in the default instance conf directory
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, new File( apacheDsHomeDirectory,
                "conf/log4j.properties" ), new File( debDefaultInstanceConfDirectory, "log4j.properties" ), false );

            // Copying the server.xml file in the default instance conf directory
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties,
                new File( apacheDsHomeDirectory, "conf/server.xml" ), new File( debDefaultInstanceConfDirectory,
                    "server.xml" ), false );

            // Copying the init script in /etc/init.d/
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds-init" ),
                new File( etcInitdDirectory, "apacheds" + "-default" ), true );

            // Removing the redundant server.xml file (see DIRSERVER-1112)
            new File( apacheDsHomeDirectory, "conf/server.xml" ).delete();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy resources files to the PKG directory ("
                + defaultInstanceDirectory + ")" );
        }

        // Copying the 'pkg' files 
        try
        {
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "Prototype" ),
                new File( pkgDirectory, "Prototype" ), true );

            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "pkginfo" ),
                new File( pkgDirectory, "pkginfo" ), true );

            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "preinstall" ),
                new File( pkgDirectory, "preinstall" ), true );

            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "postinstall" ),
                new File( pkgDirectory, "postinstall" ), true );

            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "preremove" ),
                new File( pkgDirectory, "preremove" ), true );

            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "postremove" ),
                new File( pkgDirectory, "postremove" ), true );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy PKG 'control' file." );
        }

        // Creating the target folder
        new File( pkgDirectory, "target" ).mkdir();

        // Generating the PKG
        log.info( "Generating Solaris PKG Installer" );
        String finalName = target.getFinalName();
        if ( !finalName.endsWith( ".pkg" ) )
        {
            finalName = finalName + ".pkg";
        }
        try
        {
            // Generating the PKG
            Execute executeTask = new Execute();
            executeTask.setCommandline( new String[]
                { pkgMaker.getAbsolutePath(), "-o", "-r", "root", "-d", "target", "apacheds" } );
            executeTask.setSpawn( true );
            executeTask.setWorkingDirectory( pkgDirectory );
            executeTask.execute();

            // Packaging it as a single file
            executeTask.setCommandline( new String[]
                { pkgTranslator.getAbsolutePath(), "-s", "target", "../../" + finalName, "apacheds" } );
            executeTask.execute();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the PKG: " + e.getMessage() );
        }

        log.info( "Solaris PKG generated at " + new File( imagesDirectory, finalName ) );
    }


    private void initializeFiltering()
    {
        filterProperties.putAll( mymojo.getProject().getProperties() );
        filterProperties.put( "app", target.getApplication().getName() );
        filterProperties.put( "app.name", target.getApplication().getName() );
        filterProperties.put( "osArch", target.getOsArch() );
        if ( target.getApplication().getVersion() != null )
        {
            filterProperties.put( "app.version", target.getApplication().getVersion() );
        }
        else
        {
            filterProperties.put( "app.version", "1.0" );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.daemon.installers.MojoCommand#getFilterProperties()
     */
    public Properties getFilterProperties()
    {
        return filterProperties;
    }
}
