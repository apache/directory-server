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
import org.apache.directory.server.installers.AbstractMojoCommand;
import org.apache.directory.server.installers.GenerateMojo;
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
public class DebInstallerCommand extends AbstractMojoCommand<DebTarget>
{
    /**
     * Creates a new instance of DebInstallerCommand.
     *
     * @param mojo
     *      the Server Installers Mojo
     * @param target
     *      the DEB target
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
     *   <li>Creates the Debian DEB package for Apache DS</li>
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

        // Creating the target directory
        if ( !getTargetDirectory().mkdirs() )
        {
            Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, getTargetDirectory() ) );
            log.error( e.getLocalizedMessage() );
            throw new MojoFailureException( e.getMessage() );
        }

        log.info( "    Copying Deb installer files" );

        try
        {
            // Creating the installation and instance layouts
            createLayouts();

            // Copying the init script in /etc/init.d/
            File debEtcInitdDirectory = new File( getDebDirectory(), "etc/init.d" );
            if ( !debEtcInitdDirectory.mkdirs() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, debEtcInitdDirectory ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
                getClass().getResourceAsStream( "/org/apache/directory/server/installers/etc-initd-script" ),
                new File( debEtcInitdDirectory, "apacheds-" + mojo.getProject().getVersion() + "-default" ), true );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy Deb installer files." );
        }

        // Create DEBIAN directory
        File debDebianDirectory = new File( getDebDirectory(), "DEBIAN" );
        if ( !debDebianDirectory.mkdirs() )
        {
            Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, debDebianDirectory ) );
            log.error( e.getLocalizedMessage() );
            throw new MojoFailureException( e.getMessage() );
        }

        // Copying the 'control' file
        try
        {
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream( "control" ),
                        new File( debDebianDirectory, "control" ), true );

            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream( "postinst" ),
                        new File( debDebianDirectory, "postinst" ), true );

            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream( "prerm" ),
                        new File( debDebianDirectory, "prerm" ), true );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy DEB 'control' file." );
        }

        // Setting correct permission on the postinst script
        MojoHelperUtils.exec( new String[]
                    { "chmod", "755", new File( debDebianDirectory, "postinst" ).toString() }, debDebianDirectory,
            false );
        MojoHelperUtils.exec( new String[]
                    { "chmod", "755", new File( debDebianDirectory, "prerm" ).toString() }, debDebianDirectory, false );

        // Generating the Deb
        log.info( "    Generating Deb installer" );

        String finalName = target.getFinalName();
        if ( !finalName.endsWith( ".deb" ) )
        {
            finalName = finalName + ".deb";
        }

        Execute createDebTask = new Execute();
        String[] cmd = new String[]
                    {
                        mojo.getDpkgUtility().getAbsolutePath(),
                        "-b",
                        getTargetDirectory().getName() + "/" + getDebDirectory().getName(),
                        finalName
                    };
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
     * Verifies the target.
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
        if ( !( Target.OS_NAME_LINUX.equalsIgnoreCase( System.getProperty( "os.name" ) ) || Target.OS_NAME_MAC_OS_X
            .equalsIgnoreCase( System.getProperty( "os.name" ) ) ) )
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
        filterProperties.put( "arch", target.getOsArch() );

        filterProperties.put( "installation.directory", "/opt/apacheds-" + mojo.getProject().getVersion() );
        filterProperties.put( "instances.directory", "/var/lib/apacheds-" + mojo.getProject().getVersion() );
        filterProperties.put( "user", "apacheds" );
        filterProperties.put( "wrapper.java.command", "# wrapper.java.command=<path-to-java-executable>" );
        filterProperties.put( "double.quote", "" );
        filterProperties.put( "default.instance.name", "default" );
        filterProperties.put( "installation.directory", "/opt/apacheds-" + mojo.getProject().getVersion() );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstallationDirectory()
    {
        return new File( getDebDirectory(), "opt/apacheds-" + mojo.getProject().getVersion() );
    }


    /**
     * {@inheritDoc}
     */
    public File getInstanceDirectory()
    {
        return new File( getDebDirectory(), "var/lib/apacheds-" + mojo.getProject().getVersion() + "/default" );
    }


    /**
     * Gets the directory for the Deb installer.
     *
     * @return
     *      the directory for the Deb installer
     */
    private File getDebDirectory()
    {
        return new File( getTargetDirectory(), "deb" );
    }
}
