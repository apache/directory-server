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
package org.apache.directory.daemon.installers.bin;


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
import org.codehaus.plexus.util.FileUtils;


/**
 * Bin (Binary) Installer command for Linux.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BinInstallerCommand extends MojoCommand
{
    private final Properties filterProperties = new Properties( System.getProperties() );
    /** The PKG target */
    private final BinTarget target;
    /** The Maven logger */
    private final Log log;
    /** The sh utility executable */
    private File shUtility = new File( "/bin/sh" );
    /** The final name of the installer */
    private String finalName;


    /**
     * Creates a new instance of BinInstallerCommand.
     *
     * @param mymojo
     *      the Server Installers Mojo
     * @param target
     *      the Bin target
     */
    public BinInstallerCommand( ServiceInstallersMojo mymojo, BinTarget target )
    {
        super( mymojo );
        this.target = target;
        this.log = mymojo.getLog();
        initializeFiltering();
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
        // Verifying the target is macosx
        if ( !target.getOsFamily().equals( "linux" ) )
        {
            log.warn( "Bin installer can only be targeted for linux platforms!" );
            log.warn( "The build will continue, but please check the the platform of this installer " );
            log.warn( "target" );
            return;
        }

        // Verifying the hdiutil utility exists
        if ( !shUtility.exists() )
        {
            log.warn( "Cannot find sh utility at this location: " + shUtility );
            log.warn( "The build will continue, but please check the location of your sh " );
            log.warn( "utility." );
            return;
        }

        File baseDirectory = target.getLayout().getBaseDirectory();
        File imagesDirectory = baseDirectory.getParentFile();

        log.info( "Creating Bin Installer..." );

        // Creating the package directory
        File binDirectory = new File( imagesDirectory, target.getId() + "-bin" );
        binDirectory.mkdirs();

        log.info( "Copying Bin installer files" );

        // Copying the apacheds files in the rootFolder directory
        File binRootFolderDirectory = new File( binDirectory, "rootFolder" );
        binRootFolderDirectory.mkdirs();
        File binRootFolderServerDirectory = new File( binRootFolderDirectory, "server" );
        try
        {
            copyFiles( baseDirectory, binRootFolderServerDirectory );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy image (" + target.getLayout().getBaseDirectory()
                + ") to the Bin directory (" + binRootFolderDirectory + ")" );
        }

        // Create instance and sh directory
        File binRootFolderInstanceDirectory = new File( binRootFolderDirectory, "instance" );
        binRootFolderInstanceDirectory.mkdirs();
        File binShDirectory = new File( binDirectory, "sh" );
        binShDirectory.mkdirs();

        // Copying the resources files
        try
        {
            // Copying the apacheds.conf file to the server installation layout
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds.conf" ),
                new File( binRootFolderServerDirectory, "conf/apacheds.conf" ), false );

            // Copying the default instance apacheds.conf file
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream(
                "apacheds-default.conf" ), new File( binRootFolderInstanceDirectory, "apacheds.conf" ), false );

            // Copying the log4j.properties file for the default instance
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, new File( binRootFolderServerDirectory,
                "conf/log4j.properties" ), new File( binRootFolderInstanceDirectory, "log4j.properties" ), false );

            // Copying the server.xml file for the default instance
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, new File( binRootFolderServerDirectory,
                "conf/server.xml" ), new File( binRootFolderInstanceDirectory, "server.xml" ), false );

            // Copying the apacheds-init script file for the default instance
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds-init" ),
                new File( binRootFolderInstanceDirectory, "apacheds-init" ), true );

            // Copying shell script utilities for the installer
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "bootstrap.sh" ),
                new File( binDirectory, "bootstrap.sh" ), true );
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream(
                "createInstaller.sh" ), new File( binDirectory, "createInstaller.sh" ), true );
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "functions.sh" ),
                new File( binShDirectory, "functions.sh" ), false );
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "install.sh" ),
                new File( binShDirectory, "install.sh" ), false );
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "variables.sh" ),
                new File( binShDirectory, "variables.sh" ), false );

            // Removing the redundant server.xml file (see DIRSERVER-1112)
            new File( binRootFolderServerDirectory, "conf/server.xml" ).delete();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy Bin resources files." );
        }

        // Generating the Bin
        log.info( "Generating Bin Installer" );
        Execute createBinTask = new Execute();
        String[] cmd = new String[]
            { shUtility.getAbsolutePath(), "createInstaller.sh" };
        createBinTask.setCommandline( cmd );
        createBinTask.setSpawn( true );
        createBinTask.setWorkingDirectory( binDirectory );
        try
        {
            createBinTask.execute();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the Bin: " + e.getMessage() );
        }

        log.info( "Bin Installer generated at " + new File( imagesDirectory, finalName ) );
    }


    /**
     * Recursively copy files from the given source to the given destination.
     *
     * @param src
     *      the source
     * @param dest
     *      the destination
     * @throws IOException
     *      If an error occurs when copying a file
     */
    public void copyFiles( File src, File dest ) throws IOException
    {
        if ( src.isDirectory() )
        {
            dest.mkdirs();

            for ( File file : src.listFiles() )
            {
                copyFiles( file, new File( dest, file.getName() ) );
            }
        }
        else
        {
            FileUtils.copyFile( src, dest );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.daemon.installers.MojoCommand#getFilterProperties()
     */
    public Properties getFilterProperties()
    {
        return filterProperties;
    }


    private void initializeFiltering()
    {
        filterProperties.putAll( mymojo.getProject().getProperties() );
        filterProperties.put( "app", target.getApplication().getName() );
        String version = target.getApplication().getVersion();
        if ( version != null )
        {
            filterProperties.put( "app.version", version );
        }
        else
        {
            filterProperties.put( "app.version", "1.0" );
        }
        filterProperties.put( "tmpArchive", "__tmp.tar.gz" );
        finalName = target.getFinalName();
        if ( !finalName.endsWith( ".bin" ) )
        {
            finalName = finalName + ".bin";
        }
        filterProperties.put( "finalName", finalName );
        filterProperties.put( "apacheds.version", target.getApplication().getVersion() );
    }
}
