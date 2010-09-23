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
package org.apache.directory.daemon.installers.nsis;


import java.io.File;
import java.io.IOException;

import org.apache.directory.daemon.installers.AbstractMojoCommand;
import org.apache.directory.daemon.installers.GenerateMojo;
import org.apache.directory.daemon.installers.MojoHelperUtils;
import org.apache.directory.daemon.installers.Target;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.taskdefs.Execute;


/**
 * Nullsoft INstaller System (NSIS) Installer command for Windows installers
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NsisInstallerCommand extends AbstractMojoCommand<NsisTarget>
{
    private static final String INSTALLATION_FILES = "installationFiles";
    private static final String INSTANCES_FILES = "instancesFiles/default";


    /**
     * Creates a new instance of NsisInstallerCommand.
     *
     * @param mojo
     *      the Server Installers Mojo
     * @param target
     *      the NSIS target
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

        log.info( "Creating NSIS installer..." );

        // Creating the target directory
        File targetDirectory = getTargetDirectory();
        targetDirectory.mkdirs();

        log.info( "  Copying NSIS installer files" );

        File installerFile = new File( targetDirectory, "installer.nsi" );

        try
        {
            copyCommonFiles( mojo );

            // Copying the 'installer.nsi' file
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream(
                "installer.nsi" ), installerFile, true );

            // Copying the images and icon
            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "installer.ico" ), new File(
                targetDirectory, "installer.ico" ) );
            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "header.bmp" ), new File(
                targetDirectory, "header.bmp" ) );
            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "welcome.bmp" ), new File(
                targetDirectory, "welcome.bmp" ) );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy NSIS installer files." );
        }

        // Generating the NSIS installer
        log.info( "  Generating NSIS installer" );
        Execute createPkgTask = new Execute();
        String[] cmd = new String[]
            { target.getNsisCompiler().getAbsolutePath(), installerFile.getAbsolutePath() };
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
        if ( !target.getNsisCompiler().exists() )
        {
            log.warn( "Cannot find NSIS compiler at this location: " + target.getNsisCompiler() );
            log.warn( "The build will continue, but please check the location of your makensis executable." );
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

        String finalName = target.getFinalName();
        if ( !finalName.endsWith( ".exe" ) )
        {
            finalName = finalName + ".exe";
        }
        filterProperties.put( "finalname", target.getFinalName() );
        filterProperties.put( "installationFiles", INSTALLATION_FILES );
        filterProperties.put( "instancesFiles", INSTANCES_FILES );

        filterProperties.put( "wrapper.java.command", "wrapper.java.command=@java.home@\\bin\\java.exe" );
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
        return new File( getTargetDirectory(), INSTANCES_FILES );
    }
}
