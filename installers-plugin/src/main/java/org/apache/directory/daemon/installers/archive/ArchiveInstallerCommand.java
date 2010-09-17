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
package org.apache.directory.daemon.installers.archive;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.directory.daemon.installers.MojoCommand;
import org.apache.directory.daemon.installers.MojoHelperUtils;
import org.apache.directory.daemon.installers.ServiceInstallersMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
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
public class ArchiveInstallerCommand extends MojoCommand
{
    private final Properties filterProperties = new Properties( System.getProperties() );
    /** The archive target */
    private final ArchiveTarget target;
    /** The Maven logger */
    private final Log log;


    /**
     * Creates a new instance of ArchiveInstallerCommand.
     *
     * @param mymojo
     *      the Server Installers Mojo
     * @param target
     *      the target
     */
    public ArchiveInstallerCommand( ServiceInstallersMojo mymojo, ArchiveTarget target )
    {
        super( mymojo );
        this.target = target;
        this.log = mymojo.getLog();
        initializeFiltering();
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
        // Getting the archive type
        String archiveType = target.getArchiveType();

        // Checking for a null archive type
        if ( archiveType == null )
        {
            log.warn( "Archive type is null!" );
            log.warn( "The build will continue, but please check the archive type of this installer " );
            log.warn( "target" );
            return;
        }

        // Checking for a known archive type
        if ( !archiveType.equalsIgnoreCase( "zip" ) && !archiveType.equalsIgnoreCase( "tar" )
            && !archiveType.equalsIgnoreCase( "tar.gz" ) && !archiveType.equalsIgnoreCase( "tar.bz2" ) )
        {
            log.warn( "Archive type is unknwown (" + archiveType + ")!" );
            log.warn( "The build will continue, but please check the archive type of this installer " );
            log.warn( "target" );
            return;
        }

        File baseDirectory = target.getLayout().getBaseDirectory();
        File imagesDirectory = baseDirectory.getParentFile();

        log.info( "Creating Archive Installer..." );

        // Creating the archive directory
        File targetDirectory = new File( imagesDirectory, target.getId() );
        File archiveDirectory = new File( targetDirectory, target.getApplication().getName() + "_"
            + target.getApplication().getVersion() );

        log.info( "Copying Archive Installer files" );

        // Copying the resources files
        try
        {
            // Copying the apacheds.bat file
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds.bat" ),
                new File( targetDirectory, "apacheds.bat" ), false );

            // Copying the cpappend.bat file
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "cpappend.bat" ),
                new File( targetDirectory, "cpappend.bat" ), false );

            // Copying the apacheds.sh file
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream( "apacheds.sh" ),
                new File( targetDirectory, "apacheds.sh" ), false );

            // Copying all the files in the final archive directory
            MojoHelperUtils.copyFiles( targetDirectory, archiveDirectory );
        }
        catch ( IOException e )
        {
            log.error( e.getMessage() );
            throw new MojoFailureException( "Failed to copy Archive Installer resources files." );
        }

        // Generating the Bin
        log.info( "Generating Archive Installer" );

        Project project = new Project();
        project.setBaseDir( targetDirectory );

        // ZIP Archive
        if ( archiveType.equalsIgnoreCase( "zip" ) )
        {
            Zip zipTask = new Zip();
            zipTask.setProject( project );
            zipTask.setDestFile( new File( imagesDirectory, target.getFinalName() ) );
            zipTask.setBasedir( targetDirectory );
            zipTask.setIncludes( archiveDirectory.getName() + "/**" );
            zipTask.execute();
        }
        // TAR Archive
        else if ( archiveType.equalsIgnoreCase( "tar" ) )
        {
            Tar tarTask = new Tar();
            tarTask.setProject( project );
            tarTask.setDestFile( new File( imagesDirectory, target.getFinalName() ) );
            tarTask.setBasedir( targetDirectory );
            tarTask.setIncludes( archiveDirectory.getName() + "/**" );
            tarTask.execute();
        }
        // TAR.GZ Archive
        else if ( archiveType.equalsIgnoreCase( "tar.gz" ) )
        {
            File tarFile = new File( imagesDirectory, target.getId() + ".tar" );

            Tar tarTask = new Tar();
            tarTask.setProject( project );
            tarTask.setDestFile( tarFile );
            tarTask.setBasedir( targetDirectory );
            tarTask.setIncludes( archiveDirectory.getName() + "/**" );
            tarTask.execute();

            GZip gzipTask = new GZip();
            gzipTask.setProject( project );
            gzipTask.setDestfile( new File( imagesDirectory, target.getFinalName() ) );
            gzipTask.setSrc( tarFile );
            gzipTask.execute();

            tarFile.delete();
        }
        // TAR.BZ2 Archive
        else if ( archiveType.equalsIgnoreCase( "tar.bz2" ) )
        {
            File tarFile = new File( imagesDirectory, target.getId() + ".tar" );

            Tar tarTask = new Tar();
            tarTask.setProject( project );
            tarTask.setDestFile( tarFile );
            tarTask.setBasedir( targetDirectory );
            tarTask.setIncludes( archiveDirectory.getName() + "/**" );
            tarTask.execute();

            BZip2 bzip2Task = new BZip2();
            bzip2Task.setProject( project );
            bzip2Task.setDestfile( new File( imagesDirectory, target.getFinalName() ) );
            bzip2Task.setSrc( tarFile );
            bzip2Task.execute();

            tarFile.delete();
        }

        log.info( "Archive Installer generated at " + new File( imagesDirectory, target.getFinalName() ) );
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
    }
}
