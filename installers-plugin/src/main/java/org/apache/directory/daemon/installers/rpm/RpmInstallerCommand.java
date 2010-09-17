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
package org.apache.directory.daemon.installers.rpm;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.directory.daemon.installers.MojoCommand;
import org.apache.directory.daemon.installers.MojoHelperUtils;
import org.apache.directory.daemon.installers.ServiceInstallersMojo;
import org.apache.directory.daemon.installers.Target;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Touch;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;


/**
 * The IzPack installer command.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RpmInstallerCommand extends MojoCommand
{
    private final Properties filterProperties = new Properties( System.getProperties() );
    private final RpmTarget target;
    private final File rpmConfigurationFile;
    private final Log log;

    private File rpmBuilder;


    public RpmInstallerCommand(ServiceInstallersMojo mymojo, RpmTarget target)
    {
        super( mymojo );
        this.target = target;
        this.log = mymojo.getLog();
        File imagesDir = target.getLayout().getBaseDirectory().getParentFile();
        rpmConfigurationFile = new File( imagesDir, target.getId() + ".spec" );
        initializeFiltering();
    }


    public Properties getFilterProperties()
    {
        return filterProperties;
    }
    
    
    /**
     * Performs the following:
     * <ol>
     *   <li>Bail if target is not for linux or current machine is not linux (no rpm builder)</li>
     *   <li>Filter and copy project supplied spec file into place if it has been specified and exists</li>
     *   <li>If no spec file exists filter and deposite into place bundled spec template</li>
     *   <li>Bail if we cannot find the rpm builder executable</li>
     *   <li>Execute rpm build on the filtered spec file</li>
     * </ol> 
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // -------------------------------------------------------------------
        // Step 1 & 4: do some error checking first for builder and OS
        // -------------------------------------------------------------------

        if ( !target.getOsFamily().equals( "unix" ) || !target.getOsName().equalsIgnoreCase( "Linux" ) )
        {
            log.warn( "RPM target " + target.getId() + " cannot be built for an non-linux based machine!" );
            log.warn( "The target will not be built." );
            log.warn( "The rest of the build will not fail because of this acceptable situation." );
            return;
        }

        if ( !Os.isName( "linux" ) )
        {
            log.warn( "os name = " + System.getProperty( "os.name" ) );
            log.warn( "RPM target " + target.getId() + " cannot be built on a non-linux based machine!" );
            log.warn( "The target will not be built." );
            log.warn( "The rest of the build will not fail because of this acceptable situation." );
            return;
        }

        if ( target.getRpmTopDir() == null )
        {
            target.setRpmTopDir( new File( "target/rpmbuild" ) );
        }

        if ( !target.getRpmTopDir().exists() )
        {
            try
            {
                target.getRpmTopDir().mkdirs();
                String baseDir = target.getRpmTopDir().getAbsolutePath();
                new File( baseDir + "/BUILD" ).mkdirs();
                new File( baseDir + "/RPMS" ).mkdirs();
                new File( baseDir + "/SOURCES" ).mkdirs();
                new File( baseDir + "/SPECS" ).mkdirs();
                new File( baseDir + "/SRPMS" ).mkdirs();
            } catch ( Exception e )
            {
                log.warn( "Please set the rpmTopDir in the pom.xml to a directory where the build" );
                log.warn( "user has proper permissions to create dirs and files." );
                return;
            }
        }

        // @todo this should really be a parameter taken from the user's settings
        // because the compiler may be installed in different places and is specific
        if ( !target.getRpmBuilder().exists() )
        {
            log.warn( "Cannot find rpmbuild utility at this location: " + target.getRpmBuilder() );
            log.warn( "The build will continue, but please check the location of your rpmbuild " );
            log.warn( "utility." );
            return;
        }
        else
        {
            this.rpmBuilder = target.getRpmBuilder();
        }

        // -------------------------------------------------------------------
        // Step 2 & 3: copy rpm spec file and filter 
        // -------------------------------------------------------------------

        String version = target.getApplication().getVersion().replace( '-', '_' );
        
        if ( target.getScriptFile() != null && target.getScriptFile().exists() )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, target.getScriptFile(), 
                    target.getLayout().getInitScript(), true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy project supplied init script " + target.getScriptFile()
                    + " into position " + target.getLayout().getInitScript(), e );
            }

            if ( mymojo.getLog().isInfoEnabled() )
            {
                mymojo.getLog().info( "Using project supplied init script file: "
                        + target.getScriptFile() );
            }
        }
        else
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, getClass().getResourceAsStream(
                    "server.init" ), target.getLayout().getInitScript(), true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error(
                    "Failed to copy init script " + getClass().getResource( "server.init" ) + " into position "
                        + target.getLayout().getInitScript(), e );
            }
        }
        
        // check first to see if the default spec file is present in src/main/installers
        File projectRpmFile = new File( mymojo.getSourceDirectory(), "spec.template" );
        if ( target.getRpmSpecificationFile() != null && target.getRpmSpecificationFile().exists() )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, target.getRpmSpecificationFile(),
                    rpmConfigurationFile, true );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to filter and copy project provided "
                    + target.getRpmSpecificationFile() + " to " + rpmConfigurationFile );
            }
        }
        else if ( projectRpmFile.exists() )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, projectRpmFile, rpmConfigurationFile, true );
            }
           catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to filter and copy project provided " + projectRpmFile + " to "
                    + rpmConfigurationFile );
            }
        }
        else
        {
            InputStream in = getClass().getResourceAsStream( "spec.template" );
            URL resource = getClass().getResource( "spec.template" );
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, in, rpmConfigurationFile, true );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to filter and copy bundled " + resource + " to "
                    + rpmConfigurationFile );
            }
        }

        processPackagedFiles( target, target.getPackagedFiles() );

        buildSourceTarball();
        String[] cmd = new String[]
            { rpmBuilder.getAbsolutePath(), "-ba", "--define", "_topdir " + target.getRpmTopDir().getAbsolutePath(), rpmConfigurationFile.getAbsolutePath() };
        MojoHelperUtils.exec( cmd, target.getLayout().getBaseDirectory().getParentFile(), target.isDoSudo() );
        String rpmName = target.getApplication().getName() + "-" + version + "-0." + target.getOsArch() + ".rpm";
        File srcFile = new File( target.getRpmTopDir(), "RPMS/" + target.getOsArch() + "/" + rpmName );
        File dstFile = null;

        if ( target.getFinalName() == null )
        {
            dstFile = new File( mymojo.getOutputDirectory(), rpmName );
        }
        else
        {
            String finalName = target.getFinalName();
            if ( !finalName.endsWith( ".rpm" ) )
            {
                finalName = finalName + ".rpm";
            }

            dstFile = new File( mymojo.getOutputDirectory(), finalName );
        }

        try
        {
            FileUtils.copyFile( srcFile, dstFile );
            srcFile.delete();
        }
        catch ( IOException e )
        {
            // if this happens we don't stop since RPM could be somewhere else
            e.printStackTrace();
        }
    }


    private void initializeFiltering()
    {
        filterProperties.putAll( mymojo.getProject().getProperties() );
        filterProperties.put( "app", target.getApplication().getName() );
        filterProperties.put( "app.caps", target.getApplication().getName().toUpperCase() );
        filterProperties.put( "app.server.class", mymojo.getApplicationClass() );
        filterProperties.put( "app.java.home", "java");

        char firstChar = target.getApplication().getName().charAt( 0 );
        firstChar = Character.toUpperCase( firstChar );
        filterProperties.put( "app.display.name", firstChar + target.getApplication().getName().substring( 1 ) );
        filterProperties.put( "app.release", "0" );
        filterProperties.put( "app.license.type", target.getApplication().getLicenseType() );

        String version = target.getApplication().getVersion().replace( '-', '_' );
        if ( target.getApplication().getVersion() != null )
        {
            filterProperties.put( "app.version", version );
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
                + "-linux-" + target.getOsArch() + ".rpm";
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
        filterProperties.put( "app.icon", target.getLayout().getLogoIconFile().getName() );
        filterProperties.put( "app.icon.name", target.getLayout().getLogoIconFile().getName() );
        filterProperties.put( "image.basedir", target.getLayout().getBaseDirectory().getPath() );
        filterProperties.put( "install.append.libs", getInstallLibraryJars() );
        filterProperties.put( "verify.append.libs", getVerifyLibraryJars() );
        filterProperties.put( "installer.output.directory", target.getLayout().getBaseDirectory().getParent() );
        filterProperties.put( "server.init", target.getLayout().getInitScript().getName() );
        filterProperties.put( "app.install.base", "/opt/" + target.getApplication().getName() + "-" + version );

        if ( target.getDocsDirectory() != null )
        {
            File docRoot = new File( target.getLayout().getBaseDirectory(), target.getDocsTargetPath() );
            List<File> docList = new ArrayList<File>( 200 );
            listFiles( docList, docRoot );
            filterProperties.put( "mk.docs.dirs", getMkDocsDirs( docList, target ) );
            filterProperties.put( "install.docs", getInstallDocs( docList, target ) );
            filterProperties.put( "verify.docs", getVerifyDocs( docList, target ) );
        }
        else
        {
            filterProperties.put( "mk.docs.dirs", "" );
            filterProperties.put( "install.docs", "" );
            filterProperties.put( "verify.docs", "" );
        }

        if ( target.getSourcesDirectory() != null )
        {
            File srcRoot = new File( target.getLayout().getBaseDirectory(), target.getSourcesTargetPath() );
            List<File> srcList = new ArrayList<File>( 200 );
            listFiles( srcList, srcRoot );
            filterProperties.put( "mk.sources.dirs", getMkSourcesDirs( srcList, target ) );
            filterProperties.put( "install.sources", getInstallSources( srcList, target ) );
            filterProperties.put( "verify.sources", getVerifySources( srcList, target ) );
        }
        else
        {
            filterProperties.put( "mk.sources.dirs", "" );
            filterProperties.put( "install.sources", "" );
            filterProperties.put( "verify.sources", "" );
        }

        File noticeFile = new File( target.getLayout().getBaseDirectory(), "NOTICE.txt" );
        if ( noticeFile.exists() )
        {
            filterProperties.put( "install.notice.file", "install -m 644 " + target.getLayout().getBaseDirectory()
                + "/NOTICE.txt $RPM_BUILD_ROOT/opt/" + target.getApplication().getName() + "-%{version}" );
            filterProperties.put( "verify.notice.file", "/opt/" + target.getApplication().getName()
                + "-%{version}/NOTICE.txt" );
        }
        else
        {
            filterProperties.put( "install.notice.file", "" );
            filterProperties.put( "verify.notice.file", "" );
        }
    }


    static String getMkSourcesDirs( List srcList, Target target )
    {
        StringBuffer buf = new StringBuffer();
        File srcBase = target.getLayout().getBaseDirectory();
        srcBase = new File( srcBase, target.getSourcesTargetPath() );
        // +1 for '/' char 
        int basePathSize = target.getLayout().getBaseDirectory().getAbsolutePath().length() + 1;

        for ( int ii = 0; ii < srcList.size(); ii++ )
        {
            File file = ( File ) srcList.get( ii );
            if ( file.isFile() )
            {
                continue;
            }

            String path = file.getAbsolutePath().substring( basePathSize );
            buf.append( "mkdir -p $RPM_BUILD_ROOT/opt/" );
            buf.append( target.getApplication().getName() );
            buf.append( "-%{version}/" );
            buf.append( path );
            buf.append( "\n" );
        }
        return buf.toString();
    }


    static String getMkDocsDirs( List docList, Target target )
    {
        StringBuffer buf = new StringBuffer();
        File docsBase = target.getLayout().getBaseDirectory();
        docsBase = new File( docsBase, target.getDocsTargetPath() );
        // +1 for '/' char 
        int basePathSize = target.getLayout().getBaseDirectory().getAbsolutePath().length() + 1;

        for ( int ii = 0; ii < docList.size(); ii++ )
        {
            File file = ( File ) docList.get( ii );
            if ( file.isFile() )
            {
                continue;
            }

            String path = file.getAbsolutePath().substring( basePathSize );
            buf.append( "mkdir -p $RPM_BUILD_ROOT/opt/" );
            buf.append( target.getApplication().getName() );
            buf.append( "-%{version}/" );
            buf.append( path );
            buf.append( "\n" );
        }
        return buf.toString();
    }


    static void listFiles( List<File> fileList, File dir )
    {
        if ( dir.isFile() )
        {
            return;
        }

        fileList.add( dir );
        File[] files = dir.listFiles();
        
        for ( File file:files )
        {
            if ( file.isFile() )
            {
                fileList.add( file );
            }

            listFiles( fileList, file );
        }
    }


    static String getInstallDocs( List docList, Target target )
    {
        StringBuffer buf = new StringBuffer();
        File docsBase = target.getLayout().getBaseDirectory();
        docsBase = new File( docsBase, target.getDocsTargetPath() );
        // +1 for '/' char 
        int basePathSize = target.getLayout().getBaseDirectory().getAbsolutePath().length() + 1;

        for ( int ii = 0; ii < docList.size(); ii++ )
        {
            File file = ( File ) docList.get( ii );
            if ( file.isDirectory() )
            {
                continue;
            }

            String path = file.getAbsolutePath().substring( basePathSize );
            buf.append( "install -m 644 " );
            buf.append( target.getLayout().getBaseDirectory() ).append( "/" );
            buf.append( path );
            buf.append( " $RPM_BUILD_ROOT/opt/" );
            buf.append( target.getApplication().getName() );
            buf.append( "-%{version}/" );
            buf.append( path );
            buf.append( "\n" );
        }
        return buf.toString();
    }


    static String getVerifyDocs( List docList, Target target )
    {
        StringBuffer buf = new StringBuffer();
        File docBase = target.getLayout().getBaseDirectory();
        docBase = new File( docBase, target.getDocsTargetPath() );
        // +1 for '/' char 
        int basePathSize = target.getLayout().getBaseDirectory().getAbsolutePath().length() + 1;

        for ( int ii = 0; ii < docList.size(); ii++ )
        {
            File file = ( File ) docList.get( ii );
            String path = file.getAbsolutePath().substring( basePathSize );
            buf.append( target.getLayout().getBaseDirectory() );
            buf.append( target.getApplication().getName() );
            buf.append( "-%{version}/" );
            buf.append( path );
            buf.append( "\n" );
        }
        return buf.toString();
    }


    static String getInstallSources( List sourceList, Target target )
    {
        StringBuffer buf = new StringBuffer();
        File srcBase = target.getLayout().getBaseDirectory();
        srcBase = new File( srcBase, target.getSourcesTargetPath() );
        // +1 for '/' char 
        int basePathSize = target.getLayout().getBaseDirectory().getAbsolutePath().length() + 1;

        for ( int ii = 0; ii < sourceList.size(); ii++ )
        {
            File file = ( File ) sourceList.get( ii );
            if ( file.isDirectory() )
            {
                continue;
            }

            String path = file.getAbsolutePath().substring( basePathSize );
            buf.append( "install -m 644 " );
            buf.append( target.getLayout().getBaseDirectory() ).append( "/" );
            buf.append( path );
            buf.append( " $RPM_BUILD_ROOT/opt/" );
            buf.append( target.getApplication().getName() );
            buf.append( "-%{version}/" );
            buf.append( path );
            buf.append( "\n" );
        }
        return buf.toString();
    }


    static String getVerifySources( List sourceList, Target target )
    {
        StringBuffer buf = new StringBuffer();
        File srcBase = target.getLayout().getBaseDirectory();
        srcBase = new File( srcBase, target.getSourcesTargetPath() );
        // +1 for '/' char 
        int basePathSize = target.getLayout().getBaseDirectory().getAbsolutePath().length() + 1;

        for ( int ii = 0; ii < sourceList.size(); ii++ )
        {
            File file = ( File ) sourceList.get( ii );
            String path = file.getAbsolutePath().substring( basePathSize );
            buf.append( "/opt/" );
            buf.append( target.getApplication().getName() );
            buf.append( "-%{version}/" );
            buf.append( path );
            buf.append( "\n" );
        }
        return buf.toString();
    }


    private Object getVerifyLibraryJars()
    {
        StringBuffer buf = new StringBuffer();
        List artifacts = target.getLibArtifacts();
        for ( int ii = 0; ii < artifacts.size(); ii++ )
        {
            File artifact = ( ( Artifact ) artifacts.get( ii ) ).getFile();
            buf.append( "/opt/" );
            buf.append( target.getApplication().getName() );
            buf.append( "-%{version}/lib/" );
            buf.append( artifact.getName() );
            buf.append( "\n" );
        }

        return buf.toString();
    }


    private String getInstallLibraryJars()
    {
        StringBuffer buf = new StringBuffer();
        List artifacts = target.getLibArtifacts();
        for ( int ii = 0; ii < artifacts.size(); ii++ )
        {
            buf.append( "install -m 644 " );
            File artifact = ( ( Artifact ) artifacts.get( ii ) ).getFile();
            buf.append( artifact.getAbsoluteFile() );
            buf.append( " $RPM_BUILD_ROOT/opt/" );
            buf.append( target.getApplication().getName() );
            buf.append( "-%{version}/lib/" );
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


    private void buildSourceTarball() throws MojoFailureException
    {
        String version = target.getApplication().getVersion().replace( '-', '_' );
        String dirname = target.getApplication().getName() + "-" + version;
        File sourcesDir = new File( target.getLayout().getBaseDirectory().getParentFile(), dirname );
        try
        {
            FileUtils.copyDirectoryStructure( target.getLayout().getBaseDirectory(), sourcesDir );
        }
        catch ( IOException e1 )
        {
            throw new MojoFailureException( "failed to copy directory structure at " + target.getLayout() + " to "
                + sourcesDir );
        }

        String[] cmd = new String[]
            { "tar", "-zcvf",
            target.getRpmTopDir().getAbsolutePath() + "/SOURCES/" + target.getApplication().getName() + "-" + version + ".tar.gz",
                sourcesDir.getAbsolutePath() };

        MojoHelperUtils.exec( cmd, target.getLayout().getBaseDirectory().getParentFile(), target.isDoSudo() );
    }
}
