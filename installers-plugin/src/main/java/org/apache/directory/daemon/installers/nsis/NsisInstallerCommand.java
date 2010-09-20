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
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.directory.daemon.installers.AbstractMojoCommand;
import org.apache.directory.daemon.installers.MojoHelperUtils;
import org.apache.directory.daemon.installers.GenerateMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Touch;
import org.codehaus.plexus.util.FileUtils;


/**
 * Nullsoft INstaller System (NSIS) Installer command for Windows installers
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NsisInstallerCommand extends AbstractMojoCommand<NsisTarget>
{
    private final Properties filterProperties = new Properties( System.getProperties() );
    private final File nsisConfigurationFile;

    private File nsisCompiler;


    public NsisInstallerCommand( GenerateMojo mojo, NsisTarget target )
    {
        super( mojo, target );
        File imagesDir = target.getLayout().getInstallationDirectory().getParentFile();
        nsisConfigurationFile = new File( imagesDir, target.getId() + ".nsi" );
        initializeFiltering();
    }


    public Properties getFilterProperties()
    {
        return filterProperties;
    }


    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for windows</li>
     *   <li>Filter and copy project supplied .nsi file into place if it has been specified and exists</li>
     *   <li>If no .nsi file exists filter and deposite into place bundled nsis template & copy wrapper executables</li>
     *   <li>Bail if we cannot find the nsis compiler executable</li>
     *   <li>Execute nsis compiler it on the .nsi file</li>
     * </ol>
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // -------------------------------------------------------------------
        // Step 1 & 4: do some error checking first for compiler and OS
        // -------------------------------------------------------------------

        if ( !target.getOsFamily().equals( "windows" ) )
        {
            log.warn( "NSIS installer can only be targeted for windows platforms!" );
            log.warn( "The build will continue, but please check the the platform" );
            log.warn( "of this installer target" );
            return;
        }

        // @todo this should really be a parameter taken from the user's settings
        // because the compiler may be installed in different places and is specific
        if ( !target.getNsisCompiler().exists() )
        {
            log.warn( "Cannot find NSIS compiler at this location: " + target.getNsisCompiler() );
            log.warn( "The build will continue, but please check the location of your makensis executable " );
            return;
        }
        else
        {
            this.nsisCompiler = target.getNsisCompiler();
        }

        // -------------------------------------------------------------------
        // Step 2 & 3: copy nsis file and filter
        // -------------------------------------------------------------------

        // check first to see if the default install.iss file is present in src/main/installers
        File projectNsisFile = new File( mojo.getSourceDirectory(), "install.nsi" );
        if ( target.getNsisConfigurationFile() != null && target.getNsisConfigurationFile().exists() )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mojo, filterProperties, target.getNsisConfigurationFile(),
                        nsisConfigurationFile, true );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to filter and copy project provided "
                    + target.getNsisConfigurationFile() + " to " + nsisConfigurationFile );
            }
        }
        else if ( projectNsisFile.exists() )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mojo, filterProperties, projectNsisFile, nsisConfigurationFile, true );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to filter and copy project provided " + projectNsisFile
                    + " to " + nsisConfigurationFile );
            }
        }
        else
        {
            InputStream in = getClass().getResourceAsStream( "install.iss" );
            URL resource = getClass().getResource( "install.iss" );
            try
            {
                MojoHelperUtils.copyAsciiFile( mojo, filterProperties, in, nsisConfigurationFile, true );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to filter and copy bundled " + resource + " to "
                    + nsisConfigurationFile );
            }
        }

        // -------------------------------------------------------------------
        // 3: copy native files
        // -------------------------------------------------------------------

        // now copy over the Prunsrv and Prunmgr executables renaming them to the mojo.getApplicationName() + w for mgr
        if ( target.getOsFamily().equals( "windows" ) )
        {
            File executableTarget = new File( target.getLayout().getBinDirectory(), "apacheds.exe" );
            File override = new File( mojo.getSourceDirectory(), target.getWrapperExecutablePath() );
            if ( override.exists() )
            {
                mojo.getLog().info( "Using native launcher supplied by project: " + override.getAbsolutePath() );
                try
                {
                    FileUtils.copyFile( override, executableTarget );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException(
                        "Failed to copy project supplied native launcher executable override "
                            + override.getAbsolutePath() + " into position " + executableTarget.getAbsolutePath() );
                }
            }
            else
            {
                try
                {
                    MojoHelperUtils
                        .copyBinaryFile( getClass().getResourceAsStream( "../wrapper/bin/wrapper-windows-x86-32.exe" ),
                            executableTarget );
                    MojoHelperUtils.copyBinaryFile(
                        getClass().getResourceAsStream( "../wrapper/lib/wrapper-windows-x86-32.dll" ),
                            new File( target.getLayout().getLibDirectory(), "wrapper.dll" )
                        );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy native launcher executable file "
                        + getClass().getResource( "../wrapper/bin/wrapper-windows-x86-32.exe" ) + " into position "
                        + executableTarget.getAbsolutePath() );
                }
            }

        }

        processPackagedFiles( target, target.getPackagedFiles() );

        Execute task = new Execute();
        System.out.println( "nsisCompiler = " + nsisCompiler );
        System.out.println( "nsisConfigurationFile = " + nsisConfigurationFile );
        String[] cmd = new String[]
            { nsisCompiler.getAbsolutePath(), nsisConfigurationFile.getAbsolutePath() };
        task.setCommandline( cmd );
        task.setSpawn( true );
        task.setWorkingDirectory( target.getLayout().getInstallationDirectory() );
        try
        {
            task.execute();
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed while trying to execute " + nsisCompiler.getAbsolutePath() + ": "
                + e.getMessage() );
        }

        if ( task.getExitValue() != 0 )
        {
            throw new MojoFailureException( nsisCompiler.getAbsolutePath()
                + " execution resulted in a non-zero exit value: " + task.getExitValue() );
        }
    }


    private void initializeFiltering()
    {
        filterProperties.putAll( mojo.getProject().getProperties() );
        filterProperties.put( "app.base.dir", mojo.getProject().getBasedir().getAbsolutePath() );

        filterProperties.put( "app.displayname", "ApacheDS" );

        if ( mojo.getProject().getVersion() != null )
        {
            filterProperties.put( "app.version", mojo.getProject().getVersion() );
        }
        else
        {
            filterProperties.put( "app.version", "1.0" );
        }

        // -------------------------------------------------------------------
        // WARNING: hard code values just to for testing
        // -------------------------------------------------------------------

        if ( target.getFinalName() != null )
        {
            filterProperties.put( "app.final.name", target.getFinalName() );
        }
        else
        {
            String finalName = "apacheds-" + mojo.getProject().getVersion()
                + "-win32-setup.exe";
            filterProperties.put( "app.final.name", finalName );
        }

        //        filterProperties.put( "app.email", target.getApplication().getEmail() );
        //        filterProperties.put( "app.url", target.getApplication().getUrl() );
        filterProperties.put( "app.java.version", "1.5" );
        filterProperties.put( "app.license", target.getLayout().getLicenseFile().getPath() );
        filterProperties.put( "app.license.name", target.getLayout().getLicenseFile().getName() );
        filterProperties.put( "app.company.name", target.getCompanyName() );
        //        filterProperties.put( "app.description", target.getApplication().getDescription() );
        filterProperties.put( "app.copyright.year", target.getCopyrightYear() );

        // TODO FIXME
        //        if ( !target.getLayout().getReadmeFile().exists() )
        //        {
        //            touchFile( target.getLayout().getReadmeFile() );
        //        }
        //        filterProperties.put( "app.readme", target.getLayout().getReadmeFile().getPath() );
        //        filterProperties.put( "app.readme.name", target.getLayout().getReadmeFile().getName() );
        //        filterProperties.put( "app.icon", target.getLayout().getLogoIconFile().getPath() );
        //        filterProperties.put( "app.icon.name", target.getLayout().getLogoIconFile().getName() );
        //        filterProperties.put( "image.basedir", target.getLayout().getBaseDirectory().getPath() );
        //        filterProperties.put( "app.lib.jars", getApplicationLibraryJars() );
        //        filterProperties.put( "installer.output.directory", target.getLayout().getBaseDirectory().getParent() );

        File noticeFile = new File( target.getLayout().getInstallationDirectory(), "NOTICE.txt" );
        if ( noticeFile.exists() )
        {
            filterProperties.put( "notice.file", "Source: {#SourceBase}\\NOTICE.txt; DestDir: "
                + "{app}\\; Flags: ignoreversion recursesubdirs createallsubdirs" );
        }
        else
        {
            filterProperties.put( "notice.file", "" );
        }
    }


    static void touchFile( File file )
    {
        Touch touch = new Touch();
        touch.setProject( new Project() );
        touch.setFile( file );
        touch.execute();
    }
}
