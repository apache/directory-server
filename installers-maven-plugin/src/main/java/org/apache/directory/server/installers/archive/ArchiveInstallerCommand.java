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
package org.apache.directory.server.installers.archive;


import java.io.File;
import java.io.IOException;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.installers.AbstractMojoCommand;
import org.apache.directory.server.installers.GenerateMojo;
import org.apache.directory.server.installers.MojoHelperUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.BZip2;
import org.apache.tools.ant.taskdefs.GZip;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.taskdefs.Zip;
import org.codehaus.plexus.util.FileUtils;


/**
 * Archive Installer command for any platform.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ArchiveInstallerCommand extends AbstractMojoCommand<ArchiveTarget>
{
    /**
     * Creates a new instance of ArchiveInstallerCommand.
     *
     * @param mojo
     *      the Server Installers Mojo
     * @param target
     *      the target
     */
    public ArchiveInstallerCommand( GenerateMojo mojo, ArchiveTarget target )
    {
        super( mojo, target );
        initializeFilterProperties();
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if the archive type is unknown</li>
     *   <li>Creates the Archive Installer for Apache DS</li>
     * </ol>
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Verifying the target
        if ( !verifyTarget() )
        {
            return;
        }

        // Getting the archive type
        String archiveType = target.getArchiveType();

        log.info( "  Creating " + archiveType + " archive..." );

        // Creating the target directory
        if ( !getTargetDirectory().mkdirs() )
        {
            Exception e = new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, getTargetDirectory() ) );
            log.error( e.getLocalizedMessage() );
            throw new MojoFailureException( e.getMessage() );
        }

        log.info( "    Copying archive files" );

        try
        {
            // Creating the installation and instance layouts
            createLayouts( false );

            // Copy bat and sh scripts to bin
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream(
                "apacheds.bat" ), new File( getInstallationLayout().getBinDirectory(),
                "apacheds.bat" ), false );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream(
                "apacheds.sh" ), new File( getInstallationLayout().getBinDirectory(),
                "apacheds.sh" ), false );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, getClass().getResourceAsStream(
                "cpappend.bat" ), new File( getInstallationLayout().getBinDirectory(),
                "cpappend.bat" ), false );

            // Removing unnecessary directories and files
            FileUtils.deleteDirectory( getInstallationLayout().getConfDirectory() );
            File wrapperConf = new File( getInstanceLayout().getConfDirectory(), "wrapper.conf" );
            if ( !wrapperConf.delete() )
            {
                throw new IOException(I18n.err( I18n.ERR_113_COULD_NOT_DELETE_FILE_OR_DIRECTORY, wrapperConf ) );
            }
            FileUtils.deleteDirectory( getInstanceLayout().getRunDirectory() );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy archive files." );
        }

        // Generating the archive
        log.info( "    Generating archive" );

        // Creating the final file
        String finalName = target.getFinalName();
        if ( !finalName.endsWith( archiveType ) )
        {
            finalName = finalName + archiveType;
        }
        File finalFile = new File( mojo.getOutputDirectory(), finalName );

        // Preparing the Ant project
        Project project = new Project();
        project.setBaseDir( mojo.getOutputDirectory() );

        // ZIP Archive
        if ( archiveType.equalsIgnoreCase( "zip" ) )
        {
            Zip zipTask = new Zip();
            zipTask.setProject( project );
            zipTask.setDestFile( finalFile );
            zipTask.setBasedir( getTargetDirectory() );
            zipTask.setIncludes( getArchiveDirectory().getName() + "/**" );
            zipTask.execute();
        }
        // TAR Archive
        else if ( archiveType.equalsIgnoreCase( "tar" ) )
        {
            Tar tarTask = new Tar();
            tarTask.setProject( project );
            tarTask.setDestFile( finalFile );
            tarTask.setBasedir( getTargetDirectory() );
            tarTask.setIncludes( getArchiveDirectory().getName() + "/**" );
            tarTask.execute();
        }
        // TAR.GZ Archive
        else if ( archiveType.equalsIgnoreCase( "tar.gz" ) )
        {
            File tarFile = new File( mojo.getOutputDirectory(), target.getId() + ".tar" );

            Tar tarTask = new Tar();
            tarTask.setProject( project );
            tarTask.setDestFile( tarFile );
            tarTask.setBasedir( getTargetDirectory() );
            tarTask.setIncludes( getArchiveDirectory().getName() + "/**" );
            tarTask.execute();

            GZip gzipTask = new GZip();
            gzipTask.setProject( project );
            gzipTask.setDestfile( finalFile );
            gzipTask.setSrc( tarFile );
            gzipTask.execute();

            if ( !tarFile.delete() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_113_COULD_NOT_DELETE_FILE_OR_DIRECTORY, tarFile ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }
        }
        // TAR.BZ2 Archive
        else if ( archiveType.equalsIgnoreCase( "tar.bz2" ) )
        {
            File tarFile = new File( mojo.getOutputDirectory(), target.getId() + ".tar" );

            Tar tarTask = new Tar();
            tarTask.setProject( project );
            tarTask.setDestFile( tarFile );
            tarTask.setBasedir( getTargetDirectory() );
            tarTask.setIncludes( getArchiveDirectory().getName() + "/**" );
            tarTask.execute();

            BZip2 bzip2Task = new BZip2();
            bzip2Task.setProject( project );
            bzip2Task.setDestfile( finalFile );
            bzip2Task.setSrc( tarFile );
            bzip2Task.execute();

            if ( !tarFile.delete() )
            {
                Exception e = new IOException( I18n.err( I18n.ERR_113_COULD_NOT_DELETE_FILE_OR_DIRECTORY, tarFile ) );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
            }
        }

        log.info( "=> Archive Installer (" + archiveType + ") archive generated at "
            + finalFile );
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
        // Getting the archive type
        String archiveType = target.getArchiveType();

        // Checking for a null archive type
        if ( archiveType == null )
        {
            log.warn( "Archive type is null!" );
            log.warn( "The build will continue, but please check the archive type of this installer target" );
            return false;
        }

        // Checking for a known archive type
        if ( !archiveType.equalsIgnoreCase( "zip" ) && !archiveType.equalsIgnoreCase( "tar" )
            && !archiveType.equalsIgnoreCase( "tar.gz" ) && !archiveType.equalsIgnoreCase( "tar.bz2" ) )
        {
            log.warn( "Archive type is unknwown (" + archiveType + ")!" );
            log.warn( "The build will continue, but please check the archive type of this installer target" );
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
    }


    /**
     * {@inheritDoc}
     */
    public File getInstallationDirectory()
    {
        return getArchiveDirectory();
    }


    /**
     * {@inheritDoc}
     */
    public File getInstanceDirectory()
    {
        return new File( getArchiveDirectory(), "instances/default" );
    }


    /**
     * Gets the directory for the archive.
     *
     * @return
     *      the directory for the archive
     */
    private File getArchiveDirectory()
    {
        return new File( getTargetDirectory(), "apacheds-" + mojo.getProject().getVersion() );
    }
}
