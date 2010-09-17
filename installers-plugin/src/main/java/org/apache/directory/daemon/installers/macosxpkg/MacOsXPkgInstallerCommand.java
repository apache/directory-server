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
package org.apache.directory.daemon.installers.macosxpkg;


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
 * PKG Installer command for creating Mac OS X packages.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MacOsXPkgInstallerCommand extends MojoCommand
{
    private final Properties filterProperties = new Properties( System.getProperties() );
    /** The PKG target */
    private final MacOsXPkgTarget target;
    /** The Maven logger */
    private final Log log;
    /** The PackageMaker utility executable */
    private File packageMakerUtility;
    /** The hdiutil utility executable */
    private File hdiutilUtility = new File( "/usr/bin/hdiutil" );


    /**
     * Creates a new instance of MacOsXPkgInstallerCommand.
     *
     * @param mymojo
     *      the Server Installers Mojo
     * @param target
     *      the PKG target
     */
    public MacOsXPkgInstallerCommand( ServiceInstallersMojo mymojo, MacOsXPkgTarget target )
    {
        super( mymojo );
        this.target = target;
        this.log = mymojo.getLog();
        initializeFiltering();
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for macosx or the PackageMaker or hdiutil utilities coud not be found.</li>
     *   <li>Creates the Mac OS X PKG Installer for Apache DS</li>
     *   <li>Package it in a Mac OS X DMG (Disk iMaGe)</li>
     * </ol>
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Verifying the target is macosx
        if ( !target.getOsFamily().equals( "macosx" ) )
        {
            log.warn( "Mac OS X PKG installer can only be targeted for Mac OS X platform!" );
            log.warn( "The build will continue, but please check the the platform of this installer " );
            log.warn( "target" );
            return;
        }

        // Verifying the PackageMaker utility exists
        if ( !target.getPackageMakerUtility().exists() )
        {
            log.warn( "Cannot find PackageMaker utility at this location: " + target.getPackageMakerUtility() );
            log.warn( "The build will continue, but please check the location of your Package Maker " );
            log.warn( "utility." );
            return;
        }
        else
        {
            packageMakerUtility = target.getPackageMakerUtility();
        }

        // Verifying the hdiutil utility exists
        if ( !hdiutilUtility.exists() )
        {
            log.warn( "Cannot find hdiutil utility at this location: " + hdiutilUtility );
            log.warn( "The build will continue, but please check the location of your hdiutil " );
            log.warn( "utility." );
            return;
        }

        File baseDirectory = target.getLayout().getBaseDirectory();
        File imagesDirectory = baseDirectory.getParentFile();

        log.info( "Creating Mac OS X PKG Installer..." );

        // Creating the package directory
        File pkgDirectory = new File( imagesDirectory, target.getId() + "-pkg" );
        pkgDirectory.mkdirs();

        log.info( "Copying PKG installer files" );

        // Creating the root directories hierarchy
        File pkgRootDirectory = new File( pkgDirectory, "root" );
        pkgRootDirectory.mkdirs();
        File pkgRootUsrBinDirectory = new File( pkgRootDirectory, "usr/bin" );
        pkgRootUsrBinDirectory.mkdirs();
        File pkgRootUsrLocalApachedsDirectory = new File( pkgRootDirectory, "usr/local/"
            + target.getApplication().getName() + "-" + target.getApplication().getVersion() );
        pkgRootUsrLocalApachedsDirectory.mkdirs();
        File pkgRootInstancesDirectory = new File( pkgRootUsrLocalApachedsDirectory, "instances" );
        pkgRootInstancesDirectory.mkdirs();
        File pkgRootInstancesDefaultDirectory = new File( pkgRootInstancesDirectory, "default" );
        pkgRootInstancesDefaultDirectory.mkdirs();
        File pkgRootInstancesDefaultConfDirectory = new File( pkgRootInstancesDefaultDirectory, "conf" );
        pkgRootInstancesDefaultConfDirectory.mkdirs();
        File pkgRootInstancesDefaultLdifDirectory = new File( pkgRootInstancesDefaultDirectory, "ldif" );
        pkgRootInstancesDefaultLdifDirectory.mkdirs();
        new File( pkgRootInstancesDefaultDirectory, "log" ).mkdirs();
        new File( pkgRootInstancesDefaultDirectory, "partitions" ).mkdirs();
        new File( pkgRootInstancesDefaultDirectory, "run" ).mkdirs();
        File pkgRootLibraryLaunchDaemons = new File( pkgRootDirectory, "Library/LaunchDaemons" );
        pkgRootLibraryLaunchDaemons.mkdirs();

        // Copying the apacheds files in the root directory
        try
        {
            // Copying the generated layout
            MojoHelperUtils.copyFiles( baseDirectory, pkgRootUsrLocalApachedsDirectory );

            // Copying the apacheds.init file
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds.init" ),
                new File( pkgRootUsrLocalApachedsDirectory, "bin/" + target.getApplication().getName() + ".init" ),
                true );

            // Replacing the apacheds.conf file
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds.conf" ),
                new File( pkgRootUsrLocalApachedsDirectory, "conf/apacheds.conf" ), true );

            // Copying the apacheds.conf file in the default instance conf directory
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream(
                "apacheds-default.conf" ), new File( pkgRootInstancesDefaultConfDirectory, "apacheds.conf" ), false );

            // Copying the log4j.properties file in the default instance conf directory
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, new File( pkgRootUsrLocalApachedsDirectory,
                "conf/log4j.properties" ), new File( pkgRootInstancesDefaultConfDirectory, "log4j.properties" ), false );

            // Copying the server.xml file in the default instance conf directory
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, new File( pkgRootUsrLocalApachedsDirectory,
                "conf/server.xml" ), new File( pkgRootInstancesDefaultConfDirectory, "server.xml" ), false );

            // Copying the apacheds command to /usr/bin
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream(
                "apacheds-usr-bin.sh" ), new File( pkgRootUsrBinDirectory, target.getApplication().getName() ), true );

            // Copying the org.apache.directory.server.plist file to /Library/LaunchDaemons/
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream(
                "org.apache.directory.server.plist" ), new File( pkgRootLibraryLaunchDaemons,
                "org.apache.directory.server.plist" ), true );

            // Removing the redundant server.xml file (see DIRSERVER-1112)
            new File( pkgRootUsrLocalApachedsDirectory, "conf/server.xml" ).delete();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy image (" + target.getLayout().getBaseDirectory()
                + ") to the PKG directory (" + pkgRootDirectory + ")" );
        }

        // Create Resources folder and sub-folder
        // Copying the resources files and Info.plist file needed for the 
        // generation of the PKG
        File pkgResourcesEnglishDirectory = new File( pkgDirectory, "Resources/en.lproj" );
        pkgResourcesEnglishDirectory.mkdirs();
        File pkgScriptsDirectory = new File( pkgDirectory, "scripts" );
        pkgScriptsDirectory.mkdirs();

        try
        {
            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "pkg-background.tiff" ), new File(
                pkgResourcesEnglishDirectory, "background.tiff" ) );

            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "License.rtf" ), new File(
                pkgResourcesEnglishDirectory, "License.rtf" ) );

            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "Info.plist" ), new File( pkgDirectory,
                "Info.plist" ) );

            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "postflight" ), new File(
                pkgScriptsDirectory, "postflight" ) );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy PKG resources files." );
        }

        // Generating the PKG
        log.info( "Generating Mac OS X PKG Installer" );
        Execute createPkgTask = new Execute();
        String[] cmd = new String[]
            { packageMakerUtility.getAbsolutePath(), "--root", "root/", "--resources", "Resources/", "--info",
                "Info.plist", "--title", "Apache Directory Server " + target.getApplication().getVersion(),
                "--version", target.getApplication().getVersion(), "--scripts", "scripts", "--out",
                "Apache Directory Server Installer.pkg" };
        createPkgTask.setCommandline( cmd );
        createPkgTask.setSpawn( true );
        createPkgTask.setWorkingDirectory( pkgDirectory );
        try
        {
            createPkgTask.execute();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the PKG: " + e.getMessage() );
        }

        log.info( "Mac OS X PKG Installer generated at "
            + new File( pkgDirectory, "Apache Directory Server Installer.pkg" ) );

        log.info( "Creating Mac OS X DMG..." );

        // Creating the disc image directory
        File dmgDirectory = new File( imagesDirectory, target.getId() + "-dmg" );
        dmgDirectory.mkdirs();

        log.info( "Copying DMG files" );

        // Create dmg directory and its sub-directory
        File dmgDmgBackgroundDirectory = new File( dmgDirectory, "dmg/.background" );
        dmgDmgBackgroundDirectory.mkdirs();

        // Copying the files
        try
        {
            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "dmg-background.png" ), new File(
                dmgDirectory, "dmg/.background/background.png" ) );

            MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "DS_Store" ), new File( dmgDirectory,
                "dmg/.DS_Store" ) );

            MojoHelperUtils.copyFiles( new File( pkgDirectory, "Apache Directory Server Installer.pkg" ), new File(
                dmgDirectory, "dmg/Apache Directory Server Installer.pkg" ) );

        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy DMG resources files." );
        }

        // Setting execution permission to the postflight script
        // (unfortunately, the execution permission has been lost after the 
        // copy of the PKG to the dmg folder)
        MojoHelperUtils.exec( new String[]
            {
                "chmod",
                "755",
                new File( dmgDirectory, "dmg/Apache Directory Server Installer.pkg/Contents/Resources/postflight" )
                    .toString() }, dmgDirectory, false );

        // Generating the DMG
        log.info( "Generating Mac OS X DMG Installer" );
        String finalName = target.getFinalName();
        if ( !finalName.endsWith( ".dmg" ) )
        {
            finalName = finalName + ".dmg";
        }
        try
        {
            Execute createDmgTask = new Execute();
            createDmgTask.setCommandline( new String[]
                { hdiutilUtility.getAbsolutePath(), "makehybrid", "-hfs", "-hfs-volume-name",
                    "Apache Directory Server Installer", "-hfs-openfolder", "dmg/", "dmg/", "-o", "TMP.dmg" } );
            createDmgTask.setSpawn( true );
            createDmgTask.setWorkingDirectory( dmgDirectory );
            createDmgTask.execute();

            createDmgTask.setCommandline( new String[]
                { hdiutilUtility.getAbsolutePath(), "convert", "-format", "UDZO", "TMP.dmg", "-o", "../" + finalName } );
            createDmgTask.execute();

        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the DMG: " + e.getMessage() );
        }

        log.info( "Mac OS X DMG generated at " + new File( imagesDirectory, finalName ) );
    }


    private void initializeFiltering()
    {
        filterProperties.putAll( mymojo.getProject().getProperties() );
        filterProperties.put( "app.name", target.getApplication().getName() );
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
