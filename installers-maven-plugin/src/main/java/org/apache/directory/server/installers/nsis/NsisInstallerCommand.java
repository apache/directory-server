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
package org.apache.directory.server.installers.nsis;


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
 * Nullsoft INstaller System (NSIS) Installer command for Windows installers. It creates this layout :
 * 
 * <pre>
 * installers/
 *  |
 *  +-- target/
 *       |
 *       +-- installers/
 *            |
 *            +-- apacheds-win32/
 *                 |
 *                 +-- instancesFiles/
 *                 |    |
 *                 |    +-- default/
 *                 |         |
 *                 |         +-- run/
 *                 |         |
 *                 |         +-- partitions/
 *                 |         |
 *                 |         +-- log/
 *                 |         |
 *                 |         +-- cache/
 *                 |         |
 *                 |         +-- conf/
 *                 |              |
 *                 |              +-- wrapper-instance.conf
 *                 |              |
 *                 |              +-- log4j.properties
 *                 |              |
 *                 |              +-- config.ldif
 *                 |
 *                 +-- installationFiles/
 *                 |    |
 *                 |    +-- lib/
 *                 |    |    |
 *                 |    |    +-- wrapper.dll
 *                 |    |    |
 *                 |    |    +-- wrapper-3.2.3.jar
 *                 |    |    |
 *                 |    |    +-- apacheds-wrapper-2.0.0-M20-SNAPSHOT.jar
 *                 |    |    |
 *                 |    |    +-- apacheds-service-2.0.0-M20-SNAPSHOT.jar
 *                 |    |
 *                 |    +-- conf/
 *                 |    |    |
 *                 |    |    +-- wrapper.conf
 *                 |    |
 *                 |    +-- bin/
 *                 |    |    |
 *                 |    |    +-- wrapper.exe
 *                 |    |
 *                 |    +-- NOTICE
 *                 |    |
 *                 |    +-- LICENSE
 *                 |    |
 *                 |    +-- Manage ApacheDS.ex
 *                 |   
 *                 +-- header.bmp
 *                 |
 *                 +-- welcome.bmp
 *                 |
 *                 +-- installer.ico
 *                 |
 *                 +-- uninstaller.ico
 *                 |
 *                 +-- installer.nsi
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NsisInstallerCommand extends AbstractMojoCommand<NsisTarget>
{
    /** The list of windows files */
    private static final String WRAPPER_EXE_RESOURCE = INSTALLERS_PATH + "wrapper/bin/wrapper-windows-x86-32.exe";
    private static final String WRAPPER_EXE_FILE = "wrapper.exe";
    private static final String WRAPPER_DLL_RESOURCE = INSTALLERS_PATH + "wrapper/lib/wrapper-windows-x86-32.dll";
    private static final String WRAPPER_DLL_FILE = "wrapper.dll";
    private static final String MANAGE_APACHEDS_EXE = "Manage ApacheDS.exe";
    private static final String HEADER_BMP = "header.bmp";
    private static final String WELCOME_BMP = "welcome.bmp";
    private static final String INSTALLER_ICO = "installer.ico";
    private static final String UNINSTALLER_ICO = "uninstaller.ico";
    private static final String INSTALLER_NSI = "installer.nsi";
    private static final String EXE_EXTENSION = ".exe";
    private static final String INSTALLATION_FILES = "installationFiles";
    private static final String INSTANCES_FILES = "instancesFiles";


    /**
     * Creates a new instance of NsisInstallerCommand.
     *
     * @param mojo the Server Installers Mojo
     * @param target the NSIS target
     */
    public NsisInstallerCommand( GenerateMojo mojo, NsisTarget target )
    {
        super( mojo, target );
        initializeFilterProperties();
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for windows or the NSIS compiler utility can't be found.</li>
     *   <li>Execute NSIS compiler (makensis) to create the NSIS installer.</li>
     * </ol>
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Verifying the target
        if ( !verifyTarget() )
        {
            return;
        }

        log.info( "  Creating NSIS installer..." );

        // Creating the target directory
        File targetDirectory = getTargetDirectory();

        if ( !targetDirectory.mkdirs() )
        {
            Exception e = new IOException( I18n.err( I18n.ERR_00004_COULD_NOT_CREATE_DIRECTORY, targetDirectory ) );
            log.error( e.getLocalizedMessage() );
            throw new MojoFailureException( e.getMessage() );
        }

        log.info( "    Copying NSIS installer files" );

        File installerFile = new File( targetDirectory, INSTALLER_NSI );

        try
        {
            // Creating the installation and instance layouts
            createLayouts();

            // Copying the 'Manage ApacheDS' utility
            MojoHelperUtils.copyBinaryFile( mojo, MANAGE_APACHEDS_EXE,
                getClass().getResourceAsStream( MANAGE_APACHEDS_EXE ), new File( getInstallationDirectory(),
                    MANAGE_APACHEDS_EXE ) );

            // Copying the images and icons
            MojoHelperUtils.copyBinaryFile( mojo, HEADER_BMP, getClass().getResourceAsStream( HEADER_BMP ),
                new File(
                    targetDirectory, HEADER_BMP ) );

            MojoHelperUtils.copyBinaryFile( mojo, WELCOME_BMP, getClass().getResourceAsStream( WELCOME_BMP ),
                new File(
                    targetDirectory, WELCOME_BMP ) );

            MojoHelperUtils.copyBinaryFile( mojo, INSTALLER_ICO, getClass().getResourceAsStream( INSTALLER_ICO ),
                new File(
                    targetDirectory, INSTALLER_ICO ) );

            MojoHelperUtils.copyBinaryFile( mojo, UNINSTALLER_ICO,
                getClass().getResourceAsStream( UNINSTALLER_ICO ), new File( targetDirectory, UNINSTALLER_ICO ) );

            // Copying the 'installer.nsi' file
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLER_NSI, getClass().getResourceAsStream(
                INSTALLER_NSI ), installerFile, true );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy NSIS installer files." );
        }

        // Generating the NSIS installer
        log.info( "    Generating NSIS installer" );
        Execute createPkgTask = new Execute();
        String[] cmd = new String[]
            {
                mojo.getMakensisUtility().getAbsolutePath(),
                "-V2" /* V2 means 'only log warnings and errors' */,
                installerFile.getAbsolutePath() };
        createPkgTask.setCommandline( cmd );
        createPkgTask.setWorkingDirectory( targetDirectory );

        try
        {
            createPkgTask.execute();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the NSIS installer: " + e.getMessage() );
        }

        log.info( "=> NSIS installer generated at "
            + new File( mojo.getOutputDirectory(), filterProperties.getProperty( FINAL_NAME_PROP ) ) );
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
        // Verifying the target is Mac OS X
        if ( !target.getOsName().equalsIgnoreCase( Target.OS_NAME_WINDOWS ) )
        {
            log.warn( "NSIS installer can only be targeted for Windows platform!" );
            log.warn( "The build will continue, but please check the the platform of this installer target." );
            return false;
        }

        // Verifying the NSIS compiler utility exists
        if ( !mojo.getMakensisUtility().exists() )
        {
            log.warn( "Cannot find NSIS compiler at this location: " + mojo.getMakensisUtility() );
            log.warn( "The build will continue, but please check the location of your makensis executable." );
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

        String finalName = target.getFinalName();

        if ( !finalName.endsWith( EXE_EXTENSION ) )
        {
            finalName = finalName + EXE_EXTENSION;
        }

        filterProperties.put( FINAL_NAME_PROP, target.getFinalName() );
        filterProperties.put( INSTALLATION_FILES, INSTALLATION_FILES );
        filterProperties.put( INSTANCES_FILES, INSTANCES_FILES );
        filterProperties.put( WRAPPER_JAVA_COMMAND_PROP, "wrapper.java.command=@java.home@\\bin\\java.exe" );
        filterProperties.put( DOUBLE_QUOTE_PROP, "\"" );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstallationDirectory()
    {
        return new File( getTargetDirectory(), INSTALLATION_FILES );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstanceDirectory()
    {
        return new File( getTargetDirectory(), INSTANCES_FILES + File.separator + DEFAULT );
    }


    /**
     * Copies wrapper files to the installation layout.
     *
     * @param mojo The maven plugin Mojo
     * @throws MojoFailureException If the copy failed
     */
    public void copyWrapperFiles( GenerateMojo mojo ) throws MojoFailureException
    {
        try
        {
            MojoHelperUtils.copyBinaryFile( mojo, WRAPPER_EXE_RESOURCE,
                getClass().getResourceAsStream( WRAPPER_EXE_RESOURCE ),
                new File( getInstallationLayout().getBinDirectory(), WRAPPER_EXE_FILE ) );

            MojoHelperUtils.copyBinaryFile( mojo, WRAPPER_DLL_RESOURCE,
                getClass().getResourceAsStream( WRAPPER_DLL_RESOURCE ), new File( getInstallationLayout()
                    .getLibDirectory(), WRAPPER_DLL_FILE ) );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
        }
    }
}
