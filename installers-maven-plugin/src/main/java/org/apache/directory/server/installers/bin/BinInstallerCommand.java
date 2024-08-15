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
import org.apache.directory.server.installers.GenerateMojo;
import org.apache.directory.server.installers.LinuxInstallerCommand;
import org.apache.directory.server.installers.MojoHelperUtils;
import org.apache.directory.server.installers.Target;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.taskdefs.Execute;


/**
 * Bin (Binary) Installer command for Linux. This creates a pure Linux installer, that can be used on any
 * linux or unix box. This is an alternative for boxes not supporting RPM or DEB format.
 * 
 * The way it works is that it creates 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BinInstallerCommand extends LinuxInstallerCommand<BinTarget>
{
    /** The /bin/sh executable */
    private static final String BIN_SH_EXE = "/bin/sh";

    /** The sh utility executable */
    private File shUtility = new File( BIN_SH_EXE );

    /** The final name of the installer */
    private String finalName;


    /**
     * Creates a new instance of BinInstallerCommand.
     *
     * @param mojo the Server Installers Mojo
     * @param target the Bin target
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
     *   <li>Creates the Mac OS X PKG Installer for ApacheDS</li>
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
            Exception e = new IOException( I18n.err( I18n.ERR_00004_COULD_NOT_CREATE_DIRECTORY, getTargetDirectory() ) );
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
                Exception e = new IOException( I18n.err( I18n.ERR_00004_COULD_NOT_CREATE_DIRECTORY, instanceDirectory ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            // Copying configuration files to the instance directory
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + LOG4J_PROPERTIES_FILE,
                getClass().getResourceAsStream( INSTALLERS_PATH + LOG4J_PROPERTIES_FILE ),
                new File( instanceDirectory, LOG4J_PROPERTIES_FILE ), true );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + WRAPPER_INSTANCE_CONF_FILE,
                getClass().getResourceAsStream( INSTALLERS_PATH + WRAPPER_INSTANCE_CONF_FILE ),
                new File( instanceDirectory, WRAPPER_INSTANCE_CONF_FILE ), true );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + CONFIG_LDIF_FILE,
                getClass().getResourceAsStream( INSTALLERS_PATH + CONFIG_LDIF_FILE ),
                new File( instanceDirectory, CONFIG_LDIF_FILE ), false );

            // Copying the init script to the instance directory
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + ETC_INITD_SCRIPT,
                getClass().getResourceAsStream( INSTALLERS_PATH + ETC_INITD_SCRIPT ),
                new File( instanceDirectory, ETC_INITD_SCRIPT ), true );

            // Creating the sh directory for the shell scripts
            File binShDirectory = new File( getBinInstallerDirectory(), "sh" );

            if ( !binShDirectory.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_00004_COULD_NOT_CREATE_DIRECTORY, binShDirectory ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            // Copying shell script utilities for the installer
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, "bootstrap.sh",
                getClass().getResourceAsStream( "bootstrap.sh" ),
                new File( getBinInstallerDirectory(), "bootstrap.sh" ), true );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, "createInstaller.sh",
                getClass().getResourceAsStream( "createInstaller.sh" ),
                new File( getBinInstallerDirectory(), "createInstaller.sh" ), true );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, "functions.sh",
                getClass().getResourceAsStream( "functions.sh" ),
                new File( binShDirectory, "functions.sh" ), false );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, "install.sh",
                getClass().getResourceAsStream( "install.sh" ),
                new File( binShDirectory, "install.sh" ), false );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, "variables.sh",
                getClass().getResourceAsStream( "variables.sh" ),
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
        String osName = System.getProperty( OS_NAME );

        if ( !( Target.OS_NAME_LINUX.equalsIgnoreCase( osName ) || Target.OS_NAME_MAC_OS_X.equalsIgnoreCase( osName ) ) )
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
    @Override
    protected void initializeFilterProperties()
    {
        super.initializeFilterProperties();

        filterProperties.put( "tmpArchive", "__tmp.tar.gz" );
        finalName = target.getFinalName();

        if ( !finalName.endsWith( ".bin" ) )
        {
            finalName = finalName + ".bin";
        }

        filterProperties.put( FINAL_NAME_PROP, finalName );
        filterProperties.put( "apacheds.version", mojo.getProject().getVersion() );
        filterProperties.put( WRAPPER_JAVA_COMMAND_PROP, WRAPPER_JAVA_COMMAND );
        filterProperties.put( DOUBLE_QUOTE_PROP, "" );
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
