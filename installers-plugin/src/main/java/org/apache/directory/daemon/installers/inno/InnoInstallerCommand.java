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
package org.apache.directory.daemon.installers.inno;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.directory.daemon.installers.MojoCommand;
import org.apache.directory.daemon.installers.MojoHelperUtils;
import org.apache.directory.daemon.installers.ServiceInstallersMojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.Touch;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;


/**
 * The IzPack installer command.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InnoInstallerCommand extends MojoCommand
{
    private final Properties filterProperties = new Properties( System.getProperties() );
    private final InnoTarget target;
    private final File innoConfigurationFile;
    private final Log log;

    private File innoCompiler;


    public InnoInstallerCommand( ServiceInstallersMojo mymojo, InnoTarget target )
    {
        super( mymojo );
        this.target = target;
        this.log = mymojo.getLog();
        File imagesDir = target.getLayout().getBaseDirectory().getParentFile();
        innoConfigurationFile = new File( imagesDir, target.getId() + ".iss" );
        initializeFiltering();
    }


    public Properties getFilterProperties()
    {
        return filterProperties;
    }
    
    
    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for windows or current machine is not windows (no inno compiler)</li>
     *   <li>Filter and copy project supplied inno file into place if it has been specified and exists</li>
     *   <li>If no inno file exists filter and deposite into place bundled inno template & copy procrun executables</li>
     *   <li>Bail if we cannot find the inno compiler executable</li>
     *   <li>Execute inno compiler it on the inno file</li>
     * </ol> 
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // -------------------------------------------------------------------
        // Step 1 & 4: do some error checking first for compiler and OS
        // -------------------------------------------------------------------

        if ( !target.getOsFamily().equals( "windows" ) )
        {
            throw new MojoFailureException( "Inno installers can only be targeted for windows platforms!" );
        }

        if ( !Os.isFamily( "windows" ) )
        {
            log.warn( "Inno target " + target.getId() + " cannot be built on a non-windows machine!" );
            log.warn( "The build will not fail because of this acceptable situation." );
            return;
        }

        // @todo this should really be a parameter taken from the user's settings
        // because the compiler may be installed in different places and is specific
        if ( !target.getInnoCompiler().exists() )
        {
            throw new MojoFailureException( "Cannot find Inno compiler: " + target.getInnoCompiler() );
        }
        else
        {
            this.innoCompiler = target.getInnoCompiler();
        }

        // -------------------------------------------------------------------
        // Step 2 & 3: copy inno file and filter 
        // -------------------------------------------------------------------

        // check first to see if the default install.iss file is present in src/main/installers
        File projectInnoFile = new File( mymojo.getSourceDirectory(), "install.iss" );
        if ( target.getInnoConfigurationFile() != null && target.getInnoConfigurationFile().exists() )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, target.getInnoConfigurationFile(),
                    innoConfigurationFile, true );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to filter and copy project provided "
                    + target.getInnoConfigurationFile() + " to " + innoConfigurationFile );
            }
        }
        else if ( projectInnoFile.exists() )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, projectInnoFile, innoConfigurationFile, true );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to filter and copy project provided " + projectInnoFile
                    + " to " + innoConfigurationFile );
            }
        }
        else
        {
            InputStream in = getClass().getResourceAsStream( "install.iss" );
            URL resource = getClass().getResource( "install.iss" );
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, in, innoConfigurationFile, true );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to filter and copy bundled " + resource + " to "
                    + innoConfigurationFile );
            }
        }

        // -------------------------------------------------------------------
        // 3: copy procrun files 
        // -------------------------------------------------------------------

        // now copy over the Prunsrv and Prunmgr executables renaming them to the mymojo.getApplicationName() + w for mgr
        if ( target.getOsFamily().equals( "windows" ) && target.getOsArch().equals( "x86" ) )
        {
            // ---------------------------------------------------------------
            // Handle the server process: prunsrv.exe 
            // ---------------------------------------------------------------

            File executableTarget = new File( target.getLayout().getBinDirectory(), 
                target.getApplication().getName() + ".exe" );
            File override = new File( mymojo.getSourceDirectory(), target.getPrunsrvExecutablePath() );
            if ( override.exists() )
            {
                mymojo.getLog().info( "Using procrun prunsrv.exe supplied by project: " + override.getAbsolutePath() );
                try
                {
                    FileUtils.copyFile( override, executableTarget );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy project supplied prunsrv executable override "
                        + override.getAbsolutePath() + " into position " + executableTarget.getAbsolutePath() );
                }
            }
            else
            {
                try
                {
                    MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "../wrapper/bin/wrapper-windows-x86-32.exe" ), executableTarget );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy prunsrv executable file "
                        + getClass().getResource( "../wrapper/bin/wrapper-windows-x86-32.exe" ) + " into position " + executableTarget.getAbsolutePath() );
                }
            }
            

            // ---------------------------------------------------------------
            // Handle the manager process: prunmgr.exe 
            // ---------------------------------------------------------------

/*
            executableTarget = new File( target.getLayout().getBinDirectory(),
                target.getApplication().getName() + "w.exe" );
            override = new File( mymojo.getSourceDirectory(), target.getWrapperExecutablePath() );
            if ( override.exists() )
            {
                mymojo.getLog().info( "Using procrun prunmgr.exe supplied by project: " + override.getAbsolutePath() );
                try
                {
                    FileUtils.copyFile( override, executableTarget );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy project supplied prunmgr executable override "
                        + override.getAbsolutePath() + " into position " + executableTarget.getAbsolutePath() );
                }
            }
            else
            {
                try
                {
                    MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "../prunmgr.exe" ), executableTarget );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy prunmgr executable file "
                        + getClass().getResource( "../prunmgr.exe" ) + " into position " + executableTarget.getAbsolutePath() );
                }
            }
*/
        }

        processPackagedFiles( target, target.getPackagedFiles() );

        Execute task = new Execute();
        System.out.println( "innoCompiler = " + innoCompiler );
        System.out.println( "innoConfigurationFile = " + innoConfigurationFile );
        String[] cmd = new String[]
            { innoCompiler.getAbsolutePath(), innoConfigurationFile.getAbsolutePath() };
        task.setCommandline( cmd );
        task.setSpawn( true );
        task.setWorkingDirectory( target.getLayout().getBaseDirectory() );
        try
        {
            task.execute();
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed while trying to execute " + innoCompiler.getAbsolutePath() + ": "
                + e.getMessage() );
        }

        if ( task.getExitValue() != 0 )
        {
            throw new MojoFailureException( innoCompiler.getAbsolutePath()
                + " execution resulted in a non-zero exit value: " + task.getExitValue() );
        }
    }


    private void initializeFiltering()
    {
        filterProperties.putAll( mymojo.getProject().getProperties() );
        filterProperties.put( "app", target.getApplication().getName() );

        char firstChar = target.getApplication().getName().charAt( 0 );
        firstChar = Character.toUpperCase( firstChar );
        filterProperties.put( "app.displayname", firstChar + target.getApplication().getName().substring( 1 ) );

        if ( target.getApplication().getVersion() != null )
        {
            filterProperties.put( "app.version", target.getApplication().getVersion() );
        }
        else
        {
            filterProperties.put( "app.version", "1.0" );
        }

        // -------------------------------------------------------------------
        // WARNING: hard code values just to for testing
        // -------------------------------------------------------------------

        // @todo use the list of committers and add multiple authors to inno
        if ( target.getApplication().getAuthors().isEmpty() )
        {
            filterProperties.put( "app.author", "Apache Software Foundation" );
        }
        else
        {
            filterProperties.put( "app.author", target.getApplication().getAuthors().get( 0 ) );
        }

        if ( target.getFinalName() != null )
        {
            filterProperties.put( "app.final.name", target.getFinalName() );
        }
        else
        {
            String finalName = target.getApplication().getName() + "-" + target.getApplication().getVersion()
                + "-win32-setup.exe";
            filterProperties.put( "app.final.name", finalName );
        }

        filterProperties.put( "app.email", target.getApplication().getEmail() );
        filterProperties.put( "app.url", target.getApplication().getUrl() );
        filterProperties.put( "app.java.version", target.getApplication().getMinimumJavaVersion() );
        filterProperties.put( "app.license", target.getLayout().getLicenseFile().getPath() );
        filterProperties.put( "app.license.name", target.getLayout().getLicenseFile().getName() );
        filterProperties.put( "app.company.name", target.getCompanyName() );
        filterProperties.put( "app.description", target.getApplication().getDescription() );
        filterProperties.put( "app.copyright.year", target.getCopyrightYear() );

        if ( !target.getLayout().getReadmeFile().exists() )
        {
            touchFile( target.getLayout().getReadmeFile() );
        }
        filterProperties.put( "app.readme", target.getLayout().getReadmeFile().getPath() );
        filterProperties.put( "app.readme.name", target.getLayout().getReadmeFile().getName() );
        filterProperties.put( "app.icon", target.getLayout().getLogoIconFile().getPath() );
        filterProperties.put( "app.icon.name", target.getLayout().getLogoIconFile().getName() );
        filterProperties.put( "image.basedir", target.getLayout().getBaseDirectory().getPath() );
        filterProperties.put( "app.lib.jars", getApplicationLibraryJars() );
        filterProperties.put( "installer.output.directory", target.getLayout().getBaseDirectory().getParent() );

        if ( target.getDocsDirectory() != null )
        {
            filterProperties.put( "docs.directive", getDocsDirective() );
        }
        else
        {
            filterProperties.put( "docs.directive", "" );
        }

        if ( target.getSourcesDirectory() != null )
        {
            filterProperties.put( "sources.directive", getSourcesDirective() );
        }
        else
        {
            filterProperties.put( "sources.directive", "" );
        }

        File noticeFile = new File( target.getLayout().getBaseDirectory(), "NOTICE.txt" );
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


    private String getSourcesDirective()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "Source: {#SourceBase}\\" ).append( target.getSourcesTargetPath() );
        buf.append( "\\*; DestDir: {app}\\" ).append( target.getSourcesTargetPath() );
        buf.append( "\\; Flags: ignoreversion recursesubdirs createallsubdirs" );
        return buf.toString();
    }


    private String getDocsDirective()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "Source: {#SourceBase}\\" ).append( target.getDocsTargetPath() );
        buf.append( "\\*; DestDir: {app}\\" ).append( target.getDocsTargetPath() );
        buf.append( "\\; Flags: ignoreversion recursesubdirs createallsubdirs" );
        return buf.toString();
    }


    private String getApplicationLibraryJars()
    {
        StringBuffer buf = new StringBuffer();
        List artifacts = target.getLibArtifacts();

        for ( int ii = 0; ii < artifacts.size(); ii++ )
        {
            // "Source: {#SourceBase}\lib\${artifact.file.name}; DestDir: {app}; DestName: ${app.file.name}"
            buf.append( "Source: {#SourceBase}\\lib\\" );
            File artifact = ( ( Artifact ) artifacts.get( ii ) ).getFile();
            buf.append( artifact.getName() );
            buf.append( "; DestDir: {app}\\lib; DestName: " );
            buf.append( artifact.getName() );
            buf.append( "\n" );
        }

        return buf.toString();
    }


    static void touchFile( File file )
    {
        Touch touch = new Touch();
        touch.setProject( new Project() );
        touch.setFile( file );
        touch.execute();
    }
}
