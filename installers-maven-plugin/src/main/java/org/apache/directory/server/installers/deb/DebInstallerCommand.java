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
package org.apache.directory.server.installers.deb;


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
 * Deb Installer command for Linux.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DebInstallerCommand extends LinuxInstallerCommand<DebTarget>
{
    /** The debian directory */
    private static final String DEBIAN_DIR = "DEBIAN";

    /** The debian control file */
    private static final String CONTROL_FILE = "control";

    /** The debian preinst file */
    private static final String POSTINST_FILE = "postinst";

    /** The debian postints file for files */
    private static final String PRERM_FILE = "prerm";

    /** The debian extension for files */
    private static final String DOT_DEB_EXTENSION = ".deb";

    /** The debian extension for files */
    private static final String DEB_EXTENSION = "deb";

    /** The default extension */
    private static final String DASH_DEFAULT = "-" + DEFAULT;

    /** The etc/init.d directory */
    private static final String ETC_INITD = "etc/init.d";


    /**
     * Creates a new instance of DebInstallerCommand.
     *
     * @param mojo the Server Installers Mojo
     * @param target the DEB target
     */
    public DebInstallerCommand( GenerateMojo mojo, DebTarget target )
    {
        super( mojo, target );
        initializeFilterProperties();
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for linux or the dpkg utility could not be found.</li>
     *   <li>Creates the Debian DEB package for ApacheDS</li>
     * </ol>
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Verifying the target
        if ( !verifyTarget() )
        {
            return;
        }

        log.info( "  Creating Deb installer..." );

        // Creating the target directory, which uses the ID of the Target
        File targetDirectory = getTargetDirectory();

        log.info( "Creating target directory : " + targetDirectory.getAbsolutePath() );

        if ( !getTargetDirectory().mkdirs() )
        {
            Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, getTargetDirectory() ) );
            log.error( e.getLocalizedMessage() );
            throw new MojoFailureException( e.getMessage() );
        }

        log.info( "    Copying Deb installer files" );

        try
        {
            // Creating the installation and instance layouts
            createLayouts();

            // Copying the init script in /etc/init.d/
            File debEtcInitdDirectory = new File( getDebDirectory(), ETC_INITD );

            if ( !debEtcInitdDirectory.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, debEtcInitdDirectory ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }

            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + ETC_INITD_SCRIPT,
                getClass().getResourceAsStream( INSTALLERS_PATH + ETC_INITD_SCRIPT ),
                new File( debEtcInitdDirectory, APACHEDS_DASH + mojo.getProject().getVersion() + DASH_DEFAULT ), true );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy Deb installer files." );
        }

        // Create DEBIAN directory
        File debDebianDirectory = new File( getDebDirectory(), DEBIAN_DIR );

        if ( !debDebianDirectory.mkdirs() )
        {
            Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, debDebianDirectory ) );
            log.error( e.getLocalizedMessage() );
            throw new MojoFailureException( e.getMessage() );
        }

        // Copying the 'control', 'postinst' and 'prerm' files
        try
        {
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, CONTROL_FILE,
                getClass().getResourceAsStream( CONTROL_FILE ),
                new File( debDebianDirectory, CONTROL_FILE ), true );

            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, POSTINST_FILE,
                getClass().getResourceAsStream( POSTINST_FILE ),
                new File( debDebianDirectory, POSTINST_FILE ), true );

            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, PRERM_FILE,
                getClass().getResourceAsStream( PRERM_FILE ),
                new File( debDebianDirectory, PRERM_FILE ), true );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy DEB 'control' file." );
        }

        // Setting correct permission on the postinst script
        MojoHelperUtils.exec( new String[]
            { CHMOD, RWX_RX_RX, new File( debDebianDirectory, POSTINST_FILE ).toString() }, debDebianDirectory,
            false );
        MojoHelperUtils.exec( new String[]
            { CHMOD, RWX_RX_RX, new File( debDebianDirectory, PRERM_FILE ).toString() }, debDebianDirectory, false );

        // Generating the Deb
        log.info( "    Generating Deb installer" );

        String finalName = target.getFinalName();

        if ( !finalName.endsWith( DOT_DEB_EXTENSION ) )
        {
            finalName = finalName + DOT_DEB_EXTENSION;
        }

        Execute createDebTask = new Execute();

        String[] cmd = new String[]
            {
                mojo.getDpkgUtility().getAbsolutePath(),
                "-b",
                getTargetDirectory().getName() + "/" + getDebDirectory().getName(),
                finalName
        };

        StringBuilder antTask = new StringBuilder();

        for ( String command : cmd )
        {
            antTask.append( command ).append( " " );
        }

        log.info( "Executing the ant task with command : " + antTask.toString() + " into directory "
            + mojo.getOutputDirectory() );
        createDebTask.setCommandline( cmd );
        createDebTask.setWorkingDirectory( mojo.getOutputDirectory() );

        try
        {
            createDebTask.execute();
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed while trying to generate the DEB package: " + e.getMessage() );
        }

        log.info( "Debian DEB package generated at " + new File( mojo.getOutputDirectory(), finalName ) );
    }


    /**
     * Verifies the target. The OsName must be 'Linux', it must be executed on a Linux box
     * or a mac, the dpkg utility must be present locally.
     *
     * @return
     *      <code>true</code> if the target is correct, 
     *      <code>false</code> if not.
     */
    private boolean verifyTarget()
    {
        // Verifying the target is linux
        if ( !target.isOsNameLinux() )
        {
            log.warn( "Deb package installer can only be targeted for Linux platforms!" );
            log.warn( "The build will continue, but please check the the platform of this installer target" );

            return false;
        }

        // Verifying the currently used OS to build the installer is Linux or Mac OS X
        String osName = System.getProperty( OS_NAME );

        if ( !( Target.OS_NAME_LINUX.equalsIgnoreCase( osName ) || Target.OS_NAME_MAC_OS_X.equalsIgnoreCase( osName ) ) )
        {
            log.warn( "Deb package installer can only be built on a machine running Linux or Mac OS X!" );
            log.warn( "The build will continue, generation of this target is skipped." );

            return false;
        }

        // Verifying the dpkg utility exists
        if ( !mojo.getDpkgUtility().exists() )
        {
            log.warn( "Cannot find dpkg utility at this location: " + mojo.getDpkgUtility() );
            log.warn( "The build will continue, but please check the location of your dpkg utility." );

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

        String version = mojo.getProject().getVersion();

        if ( version != null )
        {
            if ( version.endsWith( "-SNAPSHOT" ) )
            {
                filterProperties.put( "version.debian", version + "1" );
            }
            else
            {
                filterProperties.put( "version.debian", version );
            }
        }

        filterProperties.put( ARCH_PROP, target.getOsArch() );
        filterProperties.put( INSTALLATION_DIRECTORY_PROP, OPT_APACHEDS_DIR + mojo.getProject().getVersion() );
        filterProperties.put( INSTANCES_DIRECTORY_PROP, VAR_LIB_APACHEDS_DIR + mojo.getProject().getVersion() );
        filterProperties.put( USER_PROP, APACHEDS );
        filterProperties.put( GROUP_PROP, APACHEDS );
        filterProperties.put( WRAPPER_JAVA_COMMAND_PROP, WRAPPER_JAVA_COMMAND );
        filterProperties.put( DOUBLE_QUOTE_PROP, "" );
        filterProperties.put( DEFAULT_INSTANCE_NAME_PROP, DEFAULT );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstallationDirectory()
    {
        return new File( getDebDirectory(), OPT_APACHEDS_DIR + mojo.getProject().getVersion() );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstanceDirectory()
    {
        return new File( getDebDirectory(), VAR_LIB_APACHEDS_DIR + mojo.getProject().getVersion() + "/" + DEFAULT );
    }


    /**
     * Gets the directory for the Deb installer.
     *
     * @return the directory for the Deb installer
     */
    private File getDebDirectory()
    {
        return new File( getTargetDirectory(), DEB_EXTENSION );
    }
}
