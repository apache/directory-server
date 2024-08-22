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

import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.installers.GenerateMojo;
import org.apache.directory.server.installers.LinuxInstallerCommand;
import org.apache.directory.server.installers.MojoHelperUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.BZip2;
import org.apache.tools.ant.taskdefs.GZip;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.taskdefs.Tar.TarFileSet;
import org.apache.tools.ant.taskdefs.Zip;
import org.codehaus.plexus.util.FileUtils;


/**
 * Archive Installer command for any platform. We will create the following layout :
 * <pre>
 * apacheds-archive-zip/
 *  |
 *  +-- apacheds-&lt;version&gt;
 *       |
 *       +-- lib/
 *       |    |
 *       |    +-- apacheds-service-&lt;version&gt;.jar
 *       |
 *       +-- instances/
 *       |    |
 *       |    +-- default/
 *       |         |    
 *       |         +-- run/
 *       |         |    
 *       |         +-- partitions/
 *       |         |    
 *       |         +-- log/
 *       |         |    
 *       |         +-- conf/
 *       |         |    |
 *       |         |    +-- log4j.properties
 *       |         |    |
 *       |         |    +-- config.ldif
 *       |         |
 *       |         +-- cache/
 *       |
 *       +-- bin/
 *       |    |
 *       |    +-- apacheds.bat
 *       |    |
 *       |    +-- apacheds.sh
 *       |    |
 *       |    +-- cpappend.bat
 *       |    
 *       +-- NOTICE
 *       |
 *       +-- LICENSE
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ArchiveInstallerCommand extends LinuxInstallerCommand<ArchiveTarget>
{
    /** The apacheds.bat file */
    private static final String APACHE_BAT_FILE = "apacheds.bat";

    /** The apacheds.sh file */
    private static final String APACHE_SH_FILE = "apacheds.sh";

    /** The cpappend.bat file */
    private static final String CPAPPEND_BAT_FILE = "cpappend.bat";

    /** Some extensions */
    private static final String DOT_TAR = ".tar";
    private static final String ALL_FILES = "/**";


    /**
     * Creates a new instance of ArchiveInstallerCommand.
     *
     * @param mojo the Server Installers Mojo
     * @param target the target
     */
    public ArchiveInstallerCommand( GenerateMojo mojo, ArchiveTarget target )
    {
        super( mojo, target );
        initializeFilterProperties();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void createInstanceLayout() throws IOException
    {
        // Getting the instance layout and creating directories
        InstanceLayout instanceLayout = getInstanceLayout();
        instanceLayout.mkdirs();

        // Copying the log4j.properties file
        MojoHelperUtils
            .copyAsciiFile(
                mojo,
                filterProperties,
                INSTALLERS_PATH + "archive/" + LOG4J_PROPERTIES_FILE,
                getClass().getResourceAsStream(
                    INSTALLERS_PATH + "archive/" + LOG4J_PROPERTIES_FILE ),
                new File( instanceLayout.getConfDirectory(), LOG4J_PROPERTIES_FILE ), true );

        // Copying the wrapper configuration file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + WRAPPER_INSTANCE_CONF_FILE,
            getClass().getResourceAsStream( INSTALLERS_PATH + WRAPPER_INSTANCE_CONF_FILE ),
            new File( instanceLayout.getConfDirectory(), WRAPPER_INSTANCE_CONF_FILE ), true );

        // Copying ApacheDS LDIF configuration file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + CONFIG_LDIF_FILE,
            getClass().getResourceAsStream( INSTALLERS_PATH + CONFIG_LDIF_FILE ),
            new File( instanceLayout.getConfDirectory(), CONFIG_LDIF_FILE ), false );
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if the archive type is unknown</li>
     *   <li>Creates the Archive Installer for ApacheDS</li>
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

        // Creating the target directory, where we will store the files which
        // will be packaged to form the installer
        if ( !getTargetDirectory().mkdirs() )
        {
            Exception e = new IOException( I18n.err( I18n.ERR_00004_COULD_NOT_CREATE_DIRECTORY, getTargetDirectory() ) );
            log.error( e.getLocalizedMessage() );
            throw new MojoFailureException( e.getMessage() );
        }

        log.info( "    Copying archive files" );

        try
        {
            // Creating the installation and instance layouts.
            createLayouts( false );

            // Copy bat and sh scripts to bin
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, APACHE_BAT_FILE, getClass().getResourceAsStream(
                APACHE_BAT_FILE ), new File( getInstallationLayout().getBinDirectory(), APACHE_BAT_FILE ), false );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, APACHE_SH_FILE, getClass().getResourceAsStream(
                APACHE_SH_FILE ), new File( getInstallationLayout().getBinDirectory(), APACHE_SH_FILE ), false );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, CPAPPEND_BAT_FILE, getClass().getResourceAsStream(
                CPAPPEND_BAT_FILE ), new File( getInstallationLayout().getBinDirectory(),
                CPAPPEND_BAT_FILE ), false );

            // Removing unnecessary directories and files
            FileUtils.deleteDirectory( getInstallationLayout().getConfDirectory() );
            File wrapperConf = new File( getInstanceLayout().getConfDirectory(), WRAPPER_INSTANCE_CONF_FILE );

            if ( !wrapperConf.delete() )
            {
                throw new IOException( I18n.err( I18n.ERR_11000_COULD_NOT_DELETE_FILE_OR_DIRECTORY, wrapperConf ) );
            }
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
        ArchiveType type = ArchiveType.getType( archiveType );

        switch ( type )
        {
            case ZIP:
                // ZIP Archive
                Zip zipTask = new Zip();
                zipTask.setProject( project );
                zipTask.setDestFile( finalFile );
                zipTask.setBasedir( getTargetDirectory() );
                zipTask.setIncludes( getArchiveDirectory().getName() + ALL_FILES );
                zipTask.execute();
                break;

            case TAR:
                // TAR Archive
                createTarFile( project, finalFile );
                break;

            case TAR_GZ:
                // TAR.GZ Archive
                File tarFile = new File( mojo.getOutputDirectory(), target.getId() + DOT_TAR );

                // First create the tar file that will be gzipped
                createTarFile( project, tarFile );

                // And gzip it
                GZip gzipTask = new GZip();
                gzipTask.setProject( project );
                gzipTask.setDestfile( finalFile );
                gzipTask.setSrc( tarFile );
                gzipTask.execute();

                if ( !tarFile.delete() )
                {
                    Exception e = new IOException( I18n.err( I18n.ERR_11000_COULD_NOT_DELETE_FILE_OR_DIRECTORY, tarFile ) );
                    log.error( e.getLocalizedMessage() );
                    throw new MojoFailureException( e.getMessage() );
                }

                break;

            case TAR_BZ2:
                // TAR.BZ2 Archive
                tarFile = new File( mojo.getOutputDirectory(), target.getId() + DOT_TAR );

                // First create the tar file that will be zipped
                createTarFile( project, finalFile );

                // And bzip it
                BZip2 bzip2Task = new BZip2();
                bzip2Task.setProject( project );
                bzip2Task.setDestfile( finalFile );
                bzip2Task.setSrc( tarFile );
                bzip2Task.execute();

                if ( !tarFile.delete() )
                {
                    Exception e = new IOException( I18n.err( I18n.ERR_11000_COULD_NOT_DELETE_FILE_OR_DIRECTORY, tarFile ) );
                    log.error( e.getLocalizedMessage() );
                    throw new MojoFailureException( e.getMessage() );
                }

                break;

            default:
                // Unkown archive type
                Exception e = new IOException(
                    "Cannot determinate the archive type. Only \"tar\", \"tar.gz\", \"tar.bz2\" and \"zip\" are accepted : "
                        + archiveType );
                log.error( e.getLocalizedMessage() );
                throw new MojoFailureException( e.getMessage() );
        }

        log.info( "=> Archive Installer (" + archiveType + ") archive generated at "
            + finalFile );
    }


    /**
     * Create a tar file will all the files in the project
     */
    private void createTarFile( Project project, File tarFile )
    {
        Tar tarTask = new Tar();
        tarTask.setProject( project );
        tarTask.setDestFile( tarFile );

        TarFileSet nonExecutables = tarTask.createTarFileSet();
        nonExecutables.setDir( getTargetDirectory() );
        nonExecutables.setIncludes( getArchiveDirectory().getName() + ALL_FILES );
        nonExecutables.setExcludes( getArchiveDirectory().getName() + "/**/*.sh" );

        TarFileSet executables = tarTask.createTarFileSet();
        executables.setDir( getTargetDirectory() );
        executables.setIncludes( getArchiveDirectory().getName() + "/**/*.sh" );
        executables.setFileMode( "755" );

        tarTask.execute();
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
        ArchiveType type = ArchiveType.getType( archiveType );

        if ( ( type != ArchiveType.ZIP ) && ( type != ArchiveType.TAR )
            && ( type != ArchiveType.TAR_GZ ) && ( type != ArchiveType.TAR_BZ2 ) )
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
    public File getInstallationDirectory()
    {
        return getArchiveDirectory();
    }


    /**
     * {@inheritDoc}
     */
    public File getInstanceDirectory()
    {
        return new File( getArchiveDirectory(), INSTANCE_DEFAULT_DIR );
    }


    /**
     * Gets the directory for the archive.
     *
     * @return the directory for the archive
     */
    private File getArchiveDirectory()
    {
        return new File( getTargetDirectory(), APACHEDS_DASH + mojo.getProject().getVersion() );
    }
}
