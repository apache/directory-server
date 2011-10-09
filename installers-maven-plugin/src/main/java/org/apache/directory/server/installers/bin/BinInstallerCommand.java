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
package org.apache.directory.server.installers.bin;


import java.io.File;
import java.io.IOException;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.installers.AbstractMojoCommand;
import org.apache.directory.server.installers.GenerateMojo;
import org.apache.directory.server.installers.MojoHelperUtils;
import org.apache.directory.server.installers.Target;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.taskdefs.Execute;


/**
 * Bin (Binary) Installer command for Linux.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BinInstallerCommand extends AbstractMojoCommand<BinTarget>
{
    /** The sh utility executable */
    private File shUtility = new File( "/bin/sh" );

    /** The final name of the installer */
    private String finalName;


    /**
     * Creates a new instance of BinInstallerCommand.
     *
     * @param mojo
     *      the Server Installers Mojo
     * @param target
     *      the Bin target
     */
    public BinInstallerCommand( GenerateMojo mojo, BinTarget target )
    {
        super( mojo, target );
        initializeFilterProperties();
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for Linux</li>
     *   <li>Creates the Mac OS X PKG Installer for Apache DS</li>
     *   <li>Package it in a Mac OS X DMG (Disk iMaGe)</li>
     * </ol>
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Verifying the target
        if ( !verifyTarget() )
        {
            return;
        }

        log.info( "  Creating Bin installer..." );

        // Creating the target directory
        if ( !getTargetDirectory().mkdirs() )
        {
            Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, getTargetDirectory() ) );
            log.error( e.getLocalizedMessage() );
            throw new MojoFailureException( e.getMessage() );
        }

        log.info( "    Copying Bin installer files" );

        try
        {
            // Creating the installation layouts
            createInstallationLayout();

            // Creating the instance directory
            File instanceDirectory = getInstanceDirectory();
            if ( !instanceDirectory.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, instanceDirectory ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            // Copying configuration files to the instance directory
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
                getClass().getResourceAsStream( "/org/apache/directory/server/installers/log4j.properties" ),
                new File( instanceDirectory, "log4j.properties" ), true );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
                    getClass().getResourceAsStream( "/org/apache/directory/server/installers/wrapper-instance.conf" ),
                    new File( instanceDirectory, "wrapper.conf" ), true );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
                    getClass().getResourceAsStream( "/org/apache/directory/server/installers/config.ldif" ),
                    new File( instanceDirectory, "config.ldif" ), false );

            // Copying the init script to the instance directory
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
                    getClass().getResourceAsStream( "/org/apache/directory/server/installers/etc-initd-script" ),
                    new File( instanceDirectory, "etc-initd-script" ), true );

            // Creating the sh directory for the shell scripts
            File binShDirectory = new File( getBinInstallerDirectory(), "sh" );
            if ( !binShDirectory.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, binShDirectory ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            // Copying shell script utilities for the installer
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream( "bootstrap.sh" ),
                        new File( getBinInstallerDirectory(), "bootstrap.sh" ), true );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream(
                        "createInstaller.sh" ), new File( getBinInstallerDirectory(), "createInstaller.sh" ), true );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream( "functions.sh" ),
                        new File( binShDirectory, "functions.sh" ), false );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream( "install.sh" ),
                        new File( binShDirectory, "install.sh" ), false );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream( "variables.sh" ),
                        new File( binShDirectory, "variables.sh" ), false );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy bin installer files." );
        }

        // Generating the Bin
        log.info( "    Generating Bin installer" );
        Execute createBinTask = new Execute();
        String[] cmd = new String[]
                    { shUtility.getAbsolutePath(), "createInstaller.sh" };
        createBinTask.setCommandline( cmd );
        createBinTask.setWorkingDirectory( getBinInstallerDirectory() );
        try
        {
            createBinTask.execute();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the Bin: " + e.getMessage() );
        }

        log.info( "Bin Installer generated at " + new File( getTargetDirectory(), finalName ) );
    }


    /**
     * Verifies the target.
     *
     * @return
     *      <code>true</code> if the target is correct, 
     *      <code>false</code> if not.
     */
    private boolean verifyTarget()
    {
        // Verifying the target is Linux
        if ( !target.isOsNameLinux() )
        {
            log.warn( "Bin installer can only be targeted for Linux platforms!" );
            log.warn( "The build will continue, but please check the the platform of this installer target" );
            return false;
        }

        // Verifying the currently used OS to build the installer is Linux or Mac OS X
        if ( !( Target.OS_NAME_LINUX.equalsIgnoreCase( System.getProperty( "os.name" ) ) || Target.OS_NAME_MAC_OS_X
            .equalsIgnoreCase( System.getProperty( "os.name" ) ) ) )
        {
            log.warn( "Bin package installer can only be built on a machine running Linux or Mac OS X!" );
            log.warn( "The build will continue, generation of this target is skipped." );
            return false;
        }

        // Verifying the sh utility exists
        if ( !shUtility.exists() )
        {
            log.warn( "Cannot find sh utility at this location: " + shUtility );
            log.warn( "The build will continue, but please check the location of your sh utility." );
            return false;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    protected void initializeFilterProperties()
    {
        super.initializeFilterProperties();

        filterProperties.put( "tmpArchive", "__tmp.tar.gz" );
        finalName = target.getFinalName();
        if ( !finalName.endsWith( ".bin" ) )
        {
            finalName = finalName + ".bin";
        }
        filterProperties.put( "finalName", finalName );
        filterProperties.put( "apacheds.version", mojo.getProject().getVersion() );
        filterProperties.put( "wrapper.java.command", "# wrapper.java.command=<path-to-java-executable>" );
        filterProperties.put( "double.quote", "" );
    }


    /**
     * Gets the directory for the Bin installer.
     *
     * @return
     *      the directory for the Bin installer.
     */
    private File getBinInstallerDirectory()
    {
        return new File( getTargetDirectory(), "bin" );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstallationDirectory()
    {
        return new File( getBinInstallerDirectory(), "server" );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstanceDirectory()
    {
        return new File( getBinInstallerDirectory(), "instance" );
    }
}
