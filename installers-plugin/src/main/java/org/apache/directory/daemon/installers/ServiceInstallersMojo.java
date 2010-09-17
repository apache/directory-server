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
package org.apache.directory.daemon.installers;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.daemon.installers.archive.ArchiveInstallerCommand;
import org.apache.directory.daemon.installers.archive.ArchiveTarget;
import org.apache.directory.daemon.installers.bin.BinInstallerCommand;
import org.apache.directory.daemon.installers.bin.BinTarget;
import org.apache.directory.daemon.installers.deb.DebInstallerCommand;
import org.apache.directory.daemon.installers.deb.DebTarget;
import org.apache.directory.daemon.installers.inno.InnoInstallerCommand;
import org.apache.directory.daemon.installers.inno.InnoTarget;
import org.apache.directory.daemon.installers.izpack.IzPackInstallerCommand;
import org.apache.directory.daemon.installers.izpack.IzPackTarget;
import org.apache.directory.daemon.installers.macosxpkg.MacOsXPkgInstallerCommand;
import org.apache.directory.daemon.installers.macosxpkg.MacOsXPkgTarget;
import org.apache.directory.daemon.installers.nsis.NsisInstallerCommand;
import org.apache.directory.daemon.installers.nsis.NsisTarget;
import org.apache.directory.daemon.installers.rpm.RpmInstallerCommand;
import org.apache.directory.daemon.installers.rpm.RpmTarget;
import org.apache.directory.daemon.installers.solarispkg.SolarisPkgInstallerCommand;
import org.apache.directory.daemon.installers.solarispkg.SolarisPkgTarget;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Developer;
import org.apache.maven.model.MailingList;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;


/**
 * Maven 2 mojo creating the platform specific installation layout images.
 * 
 * @goal generate
 * @description Creates platform specific installation layout images.
 * @phase package
 * @requiresDependencyResolution runtime
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ServiceInstallersMojo extends AbstractMojo
{
    static final String BOOTSTRAPPER_ARTIFACT_ID = "daemon-bootstrappers";
    static final String BOOTSTRAPPER_GROUP_ID = "org.apache.directory.daemon";
    static final String LOGGER_ARTIFACT_ID = "slf4j-api";
    static final String LOGGER_GROUP_ID = "org.slf4j";
    static final String DAEMON_ARTIFACT_ID = "wrapper";
    static final String DAEMON_GROUP_ID = "tanukisoft";

    /**
     * The target directory into which the mojo creates os and platform 
     * specific images.
     * @parameter default-value="${project.build.directory}/images"
     */
    private File outputDirectory;

    /**
     * The source directory where various configuration files for the installer 
     * are stored.
     * 
     * @parameter default-value="${project.basedir}/src/main/installers"
     */
    private File sourceDirectory;

    /**
     * @parameter expression="${project}" default-value="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * @parameter
     */
    private IzPackTarget[] izPackTargets;

    /**
     * @parameter
     */
    private RpmTarget[] rpmTargets;

    /**
     * @parameter
     */
    private MacOsXPkgTarget[] macOsXPkgTargets;

    /**
     * @parameter
     */
    private SolarisPkgTarget[] solarisPkgTargets;

    /**
     * @parameter
     */
    private InnoTarget[] innoTargets;

    /**
     * @parameter
     */
    private NsisTarget[] nsisTargets;

    /**
     * @parameter
     */
    private DebTarget[] debTargets;

    /**
     * @parameter
     */
    private BinTarget[] binTargets;
    /**
     * @parameter
     */
    private ArchiveTarget[] archiveTargets;

    /**
     * @parameter 
     * @required
     */
    private Application application;

    /**
     * @parameter 
     * @required
     */
    private String applicationClass;

    /**
     * @parameter
     */
    private String encoding;

    /**
     * @parameter
     */
    private String svnBaseUrl;

    /**
     * @parameter default-value="false"
     */
    private boolean packageSources;

    /**
     * @parameter default-value="false"
     */
    private boolean packageDocs;

    /**
     * @parameter default-value="src"
     */
    private String sourcesTargetPath;

    /**
     * @parameter default-value="docs"
     */
    private String docsTargetPath;

    /**
     * @parameter
     */
    private PackagedFile[] packagedFiles;

    /**
     * @parameter
     */
    private Set excludes;

    /** daemon bootstrapper */
    private Artifact bootstrapper;
    /** logging API need by bootstraper */
    private Artifact logger;
    /** commons-daemon dependency needed by native daemon */
    private Artifact daemon;

    private File exportedSources;
    private File docsBase;
    private List<Target> allTargets;


    public void execute() throws MojoExecutionException, MojoFailureException
    {
        FileUtils.mkdir( outputDirectory.getAbsolutePath() );

        // collect all targets 
        initializeAllTargets();

        // setup exports and docs if specified for installers
        setupSourcesAndDocs();

        // makes sure defaulted values are set to globals
        setDefaults();

        // bail if there is nothing to do 
        if ( allTargets.isEmpty() )
        {
            getLog().info( "===================================================================" );
            getLog().info( "[installers:generate]" );
            getLog().info( "No installers to generate." );
            getLog().info( "===================================================================" );
            return;
        }

        // report what we have to build 
        reportSetup();

        // search for and find the bootstrapper artifact
        setBootstrapArtifacts();

        // generate installers for all targets
        for ( Target target : allTargets )
        {
            // create the installation image first
            CreateImageCommand imgCmd = new CreateImageCommand( this, target );
            imgCmd.execute();

            // ---------------------------------------------------------------
            // Generate all installers
            // ---------------------------------------------------------------

            if ( target instanceof IzPackTarget )
            {
                IzPackInstallerCommand izPackCmd = null;
                izPackCmd = new IzPackInstallerCommand( this, ( IzPackTarget ) target );
                izPackCmd.execute();
            }

            if ( target instanceof InnoTarget )
            {
                InnoInstallerCommand innoCmd = null;
                innoCmd = new InnoInstallerCommand( this, ( InnoTarget ) target );
                innoCmd.execute();
            }

            if ( target instanceof NsisTarget )
            {
                NsisInstallerCommand nsisCmd = null;
                nsisCmd = new NsisInstallerCommand( this, ( NsisTarget ) target );
                nsisCmd.execute();
            }

            if ( target instanceof RpmTarget )
            {
                RpmInstallerCommand rpmCmd = null;
                rpmCmd = new RpmInstallerCommand( this, ( RpmTarget ) target );
                rpmCmd.execute();
            }

            if ( target instanceof MacOsXPkgTarget )
            {
                MacOsXPkgInstallerCommand pkgCmd = null;
                pkgCmd = new MacOsXPkgInstallerCommand( this, ( MacOsXPkgTarget ) target );
                pkgCmd.execute();
            }

            if ( target instanceof SolarisPkgTarget )
            {
                SolarisPkgInstallerCommand pkgCmd = null;
                pkgCmd = new SolarisPkgInstallerCommand( this, ( SolarisPkgTarget ) target );
                pkgCmd.execute();
            }

            if ( target instanceof DebTarget )
            {
                DebInstallerCommand debCmd = null;
                debCmd = new DebInstallerCommand( this, ( DebTarget ) target );
                debCmd.execute();
            }

            if ( target instanceof BinTarget )
            {
                BinInstallerCommand binCmd = null;
                binCmd = new BinInstallerCommand( this, ( BinTarget ) target );
                binCmd.execute();
            }

            if ( target instanceof ArchiveTarget )
            {
                ArchiveInstallerCommand archiveCmd = null;
                archiveCmd = new ArchiveInstallerCommand( this, ( ArchiveTarget ) target );
                archiveCmd.execute();
            }
        }
    }


    private void initializeAllTargets()
    {
        allTargets = new ArrayList<Target>();
        addAll( allTargets, izPackTargets );
        addAll( allTargets, innoTargets );
        addAll( allTargets, nsisTargets );
        addAll( allTargets, rpmTargets );
        addAll( allTargets, debTargets );
        addAll( allTargets, macOsXPkgTargets );
        addAll( allTargets, solarisPkgTargets );
        addAll( allTargets, binTargets );
        addAll( allTargets, archiveTargets );
    }


    private void addAll( List<Target> list, Target[] array )
    {
        if ( array == null )
        {
            return;
        }

        list.addAll( Arrays.asList( array ) );
    }


    private void setDefaults() throws MojoFailureException
    {
        if ( allTargets == null )
        {
            return;
        }

        if ( application.getName() == null )
        {
            throw new MojoFailureException( "Installed application name cannot be null." );
        }

        if ( application.getCompany() == null )
        {
            if ( project.getOrganization() != null )
            {
                application.setCompany( project.getOrganization().getName() );
            }
            else
            {
                application.setCompany( "Apache Software Foundation" );
            }
        }

        if ( application.getDescription() == null )
        {
            if ( project.getDescription() != null )
            {
                application.setDescription( project.getDescription() );
            }
            else
            {
                application.setDescription( "No description of this application is available." );
            }
        }

        if ( project.getInceptionYear() != null )
        {
            application.setCopyrightYear( project.getInceptionYear() );
        }

        if ( application.getUrl() == null )
        {
            if ( project.getUrl() != null )
            {
                application.setUrl( project.getUrl() );
            }
            else if ( project.getOrganization() != null )
            {
                application.setUrl( project.getOrganization().getUrl() );
            }
            else
            {
                application.setUrl( "http://www.apache.org" );
            }
        }

        if ( application.getVersion() == null )
        {
            application.setVersion( project.getVersion() );
        }

        if ( application.getMinimumJavaVersion() == null )
        {
            application.setMinimumJavaVersion( JavaEnvUtils.getJavaVersion() );
        }

        if ( application.getAuthors() == null )
        {
            List<String> authors = new ArrayList<String>();
            @SuppressWarnings(value =
                { "unchecked" })
            List<Developer> developers = project.getDevelopers();

            for ( Developer developer : developers )
            {
                if ( developer.getEmail() != null )
                {
                    authors.add( developer.getEmail() );
                }
                else
                {
                    authors.add( developer.getName() );
                }
            }

            application.setAuthors( authors );
        }

        if ( application.getEmail() == null )
        {
            if ( !project.getMailingLists().isEmpty() )
            {
                application.setEmail( ( ( MailingList ) project.getMailingLists().get( 0 ) ).getPost() );
            }

            application.setEmail( "general@apache.org" );
        }

        if ( application.getIcon() == null )
        {
            application.setIcon( new File( "src/main/installers/logo.ico" ) );
        }

        if ( application.getReadme() == null )
        {
            application.setReadme( new File( "README" ) );
        }

        if ( application.getLicense() == null )
        {
            application.setLicense( new File( "LICENSE" ) );
        }

        for ( Target target : allTargets )
        {
            if ( target.getApplication() == null )
            {
                target.setApplication( this.application );
            }

            if ( target.getLoggerConfigurationFile() == null )
            {
                target.setLoggerConfigurationFile( new File( sourceDirectory, "log4j.properties" ) );
            }

            if ( target.getBootstrapperConfigurationFile() == null )
            {
                target.setBootstrapperConfigurationFile( new File( sourceDirectory, "bootstrapper.properties" ) );
            }

            if ( target.getServerConfigurationFile() == null )
            {
                target.setServerConfigurationFile( new File( sourceDirectory, "server.xml" ) );
            }

            if ( target.getOsVersion() == null )
            {
                target.setOsVersion( "*" );
            }

            if ( packageSources && exportedSources != null && target.getSourcesDirectory() == null )
            {
                target.setSourcesDirectory( exportedSources );
            }

            if ( packageDocs && docsBase != null && target.getDocsDirectory() == null )
            {
                target.setDocsDirectory( docsBase );
            }

            if ( target.getSourcesTargetPath() == null )
            {
                target.setSourcesTargetPath( sourcesTargetPath );
            }

            if ( target.getDocsTargetPath() == null )
            {
                target.setDocsTargetPath( docsTargetPath );
            }
        }
    }


    private void setupSourcesAndDocs() throws MojoFailureException
    {
        File generatedDocs = null;

        if ( svnBaseUrl != null )
        {
            exportedSources = new File( outputDirectory, "src" );
            exportSvnSources( exportedSources );

            if ( packageDocs )
            {
                generatedDocs = new File( outputDirectory, "docs" );
                generateDocs( exportedSources, generatedDocs );
                docsBase = new File( generatedDocs, "target" );
                docsBase = new File( docsBase, "site" );
            }
        }
    }


    private void setBootstrapArtifacts() throws MojoFailureException
    {
        Artifact artifact = null;
        Iterator artifacts = project.getDependencyArtifacts().iterator();

        while ( artifacts.hasNext() )
        {
            artifact = ( Artifact ) artifacts.next();
            if ( artifact.getArtifactId().equals( BOOTSTRAPPER_ARTIFACT_ID )
                && artifact.getGroupId().equals( BOOTSTRAPPER_GROUP_ID ) )
            {
                getLog().info( "Found bootstrapper dependency with version: " + artifact.getVersion() );
                bootstrapper = artifact;
            }
            else if ( artifact.getArtifactId().equals( LOGGER_ARTIFACT_ID )
                && artifact.getGroupId().equals( LOGGER_GROUP_ID ) )
            {
                getLog().info( "Found logger dependency with version: " + artifact.getVersion() );
                logger = artifact;
            }
            else if ( artifact.getArtifactId().equals( DAEMON_ARTIFACT_ID )
                && artifact.getGroupId().equals( DAEMON_GROUP_ID ) )
            {
                getLog().info( "Found daemon dependency with version: " + artifact.getVersion() );
                daemon = artifact;
            }
        }

        if ( bootstrapper == null )
        {
            throw new MojoFailureException( "Bootstrapper dependency artifact required: " + BOOTSTRAPPER_GROUP_ID + ":"
                + BOOTSTRAPPER_ARTIFACT_ID );
        }
        if ( logger == null )
        {
            throw new MojoFailureException( "Logger dependency artifact required: " + LOGGER_GROUP_ID + ":"
                + LOGGER_ARTIFACT_ID );
        }
        if ( daemon == null )
        {
            throw new MojoFailureException( "Daemon dependency artifact required: " + DAEMON_GROUP_ID + ":"
                + DAEMON_ARTIFACT_ID );
        }
    }


    public void reportSetup()
    {
        getLog().info( "===================================================================" );
        getLog().info( "[installers:create]" );
        getLog().info( "applicationName = " + application.getName() );
        getLog().info( "sourceDirectory = " + sourceDirectory );
        getLog().info( "outputDirectory = " + outputDirectory );
        getLog().info( "----------------------------- allTargets -----------------------------" );

        boolean isFirst = true;

        if ( allTargets != null )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                getLog().info( "" );
            }

            for ( Target target : allTargets )
            {
                getLog().info( "id: " + target.getId() );
                getLog().info( "osName: " + target.getOsName() );
                getLog().info( "osArch: " + target.getOsArch() );
                getLog().info( "osVersion: " + target.getOsVersion() );
                getLog().info( "daemonFramework: " + target.getDaemonFramework() );
                getLog().info( "loggerConfigurationFile: " + target.getLoggerConfigurationFile() );
                getLog().info( "bootstrapperConfigurationFiles: " + target.getBootstrapperConfigurationFile() );
                getLog().info( "serverConfigurationFile: " + target.getServerConfigurationFile() );
            }
        }

        getLog().info( "===================================================================" );
    }


    private void exportSvnSources( File exportTarget ) throws MojoFailureException
    {
        String[] cmd = new String[]
            { "svn", "export", svnBaseUrl, exportTarget.getAbsolutePath() };
        MojoHelperUtils.exec( cmd, outputDirectory, false );
    }


    private void generateDocs( File exportTarget, File docsTarget ) throws MojoFailureException
    {
        try
        {
            FileUtils.copyDirectoryStructure( exportTarget, docsTarget );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to copy exported sources from svn here "
                + exportTarget.getAbsolutePath() + " to " + docsTarget.getAbsolutePath() );
        }

        String[] cmd = null;
        if ( Os.isFamily( "windows" ) )
        {
            cmd = new String[]
                { "mvn.bat", "site", "--non-recursive" };
        }
        else
        {
            cmd = new String[]
                { "mvn", "site", "--non-recursive" };
        }
        MojoHelperUtils.exec( cmd, docsTarget, false );
    }


    public File getOutputDirectory()
    {
        return outputDirectory;
    }


    public Artifact getBootstrapper()
    {
        return bootstrapper;
    }


    public Artifact getDaemon()
    {
        return daemon;
    }


    public Artifact getLogger()
    {
        return logger;
    }


    public String getEncoding()
    {
        return this.encoding;
    }


    public MavenProject getProject()
    {
        return project;
    }


    public Set getExcludes()
    {
        return this.excludes;
    }


    public String getApplicationClass()
    {
        return this.applicationClass;
    }


    public File getSourceDirectory()
    {
        return this.sourceDirectory;
    }


    public void setPackagedFiles( PackagedFile[] packagedFiles )
    {
        this.packagedFiles = packagedFiles;
    }


    public PackagedFile[] getPackagedFiles()
    {
        return packagedFiles;
    }
}
