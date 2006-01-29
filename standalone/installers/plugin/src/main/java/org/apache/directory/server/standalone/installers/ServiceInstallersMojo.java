/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.standalone.installers;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.standalone.installers.inno.InnoInstallerCommand;
import org.apache.directory.server.standalone.installers.inno.InnoTarget;
import org.apache.directory.server.standalone.installers.izpack.IzPackInstallerCommand;
import org.apache.directory.server.standalone.installers.izpack.IzPackTarget;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Developer;
import org.apache.maven.model.MailingList;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.util.JavaEnvUtils;


/**
 * Maven 2 mojo creating the platform specific installation layout images.
 * 
 * @goal generate
 * @description Creates platform specific installation layout images.
 * @phase package
 * @requiresDependencyResolution runtime
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServiceInstallersMojo extends AbstractMojo
{
    static final String BOOTSTRAPPER_ARTIFACT_ID = "org.apache.directory.server.standalone.daemon";
    static final String BOOTSTRAPPER_GROUP_ID = "org.apache.directory.server.standalone.daemon";
    static final String LOGGER_ARTIFACT_ID = "nlog4j";
    static final String LOGGER_GROUP_ID = "org.slf4j";
    static final String DAEMON_ARTIFACT_ID = "commons-daemon";
    static final String DAEMON_GROUP_ID = "commons-daemon";

    /**
     * The target directory into which the mojo creates os and platform 
     * specific images.
     * @parameter expression="target/images"
     */
    private File outputDirectory;

    /**
     * The source directory where various configuration files for the installer 
     * are stored.
     * 
     * @parameter expression="src/main/installers"
     */
    private File sourceDirectory;

    /**
     * @parameter expression="${project}"
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
    private PkgTarget[] pkgTargets;

    /**
     * @parameter
     */
    private InnoTarget[] innoTargets;

    /**
     * @parameter
     */
    private DebTarget[] debTargets;

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
    private Set excludes;

    /** daemon bootstrapper */
    private Artifact bootstrapper;
    /** logging API need by bootstraper */
    private Artifact logger;
    /** commons-daemon dependency needed by native daemon */
    private Artifact daemon;
    private List allTargets;
    
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // collect all targets 
        initializeAllTargets();
        
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
        for ( int ii = 0; ii < allTargets.size(); ii++ )
        {
            Target target = ( Target ) allTargets.get( ii );
            
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
        }
    }
    
    
    private void initializeAllTargets()
    {
        allTargets = new ArrayList();
        addAll( allTargets, izPackTargets );
        addAll( allTargets, innoTargets );
        addAll( allTargets, rpmTargets );
        addAll( allTargets, debTargets );
        addAll( allTargets, pkgTargets );
    }
    

    private void addAll( List list, Object[] array )
    {
        if ( array == null ) return;
        for ( int ii = 0; ii < array.length; ii++ )
        {
            list.add( array[ii] );
        }
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
            List authors = new ArrayList();
            List developers = project.getDevelopers();
            for ( int ii = 0; ii < developers.size(); ii++ )
            {
                Developer developer = ( Developer ) developers.get( ii );
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
            if ( ! project.getMailingLists().isEmpty() )
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
            application.setReadme( new File( "README.txt" ) );
        }
        
        if ( application.getLicense() == null )
        {
            application.setLicense( new File( "LICENSE.txt" ) );
        }
        
        for ( int ii = 0; ii < allTargets.size(); ii++ )
        {
            Target target = ( Target ) allTargets.get( ii );
            
            if ( target.getApplication() == null)
            {
                target.setApplication( this.application );
            }

            if ( target.getLoggerConfigurationFile() == null )
            {
                target.setLoggerConfigurationFile( new File( sourceDirectory, "log4j.properties" ) );
            }
            if ( target.getBootstrapperConfiguraitonFile() == null )
            {
                target.setBootstrapperConfiguraitonFile( new File( sourceDirectory, "bootstrapper.properties" ) );
            }
            if ( target.getServerConfigurationFile() == null )
            {
                target.setServerConfigurationFile( new File( sourceDirectory, "server.xml" ) );
            }
            if ( target.getOsVersion() == null )
            {
                target.setOsVersion( "*" );
            }
            if ( target.getApplicationAuthors() == null )
            {
                target.setApplicationAuthors( application.getAuthors() );
            }
            if ( target.getApplicationEmail() == null )
            {
                target.setApplicationEmail( application.getEmail() );
            }
            if ( target.getApplicationJavaVersion() == null )
            {
                target.setApplicationJavaVersion( application.getMinimumJavaVersion() );
            }
            if ( target.getApplicationUrl() == null )
            {
                target.setApplicationUrl( application.getUrl() );
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
            if ( artifact.getArtifactId().equals( BOOTSTRAPPER_ARTIFACT_ID ) || artifact.getGroupId().equals( BOOTSTRAPPER_GROUP_ID ) )
            {
                getLog().info( "Found bootstrapper dependency with version: " + artifact.getVersion() );
                bootstrapper = artifact;
            }
            if ( artifact.getArtifactId().equals( LOGGER_ARTIFACT_ID ) || artifact.getGroupId().equals( LOGGER_GROUP_ID ) )
            {
                getLog().info( "Found logger dependency with version: " + artifact.getVersion() );
                logger = artifact;
            }
            if ( artifact.getArtifactId().equals( DAEMON_ARTIFACT_ID ) || artifact.getGroupId().equals( DAEMON_GROUP_ID ) )
            {
                getLog().info( "Found daemon dependency with version: " + artifact.getVersion() );
                daemon = artifact;
            }
        }

        if ( bootstrapper == null )
        {
            throw new MojoFailureException( "Bootstrapper dependency artifact required: " 
                + BOOTSTRAPPER_GROUP_ID + ":" + BOOTSTRAPPER_ARTIFACT_ID );
        }
        if ( logger == null )
        {
            throw new MojoFailureException( "Logger dependency artifact required: " 
                + LOGGER_GROUP_ID + ":" + LOGGER_ARTIFACT_ID );
        }
        if ( daemon == null )
        {
            throw new MojoFailureException( "Daemon dependency artifact required: " 
                + DAEMON_GROUP_ID + ":" + DAEMON_ARTIFACT_ID );
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
        
        if ( allTargets != null )
        {
            for ( int ii = 0; ii < allTargets.size(); ii++ )
            {
                getLog().info( "id: " + ( ( Target ) allTargets.get( ii ) ).getId() );
                getLog().info( "osName: " + ( ( Target ) allTargets.get( ii ) ).getOsName() );
                getLog().info( "osArch: " + ( ( Target ) allTargets.get( ii ) ).getOsArch() );
                getLog().info( "osVersion: " + ( ( Target ) allTargets.get( ii ) ).getOsVersion() );
                getLog().info( "daemonFramework: " + ( ( Target ) allTargets.get( ii ) ).getDaemonFramework() );
                getLog().info( "loggerConfigurationFile: " + 
                    ( ( Target ) allTargets.get( ii ) ).getLoggerConfigurationFile() );
                getLog().info( "bootstrapperConfiguraitonFiles: " + 
                    ( ( Target ) allTargets.get( ii ) ).getBootstrapperConfiguraitonFile() );
                getLog().info( "serverConfigurationFil: " + 
                    ( ( Target ) allTargets.get( ii ) ).getServerConfigurationFile() );
                
                if ( ii + 1 < allTargets.size() )
                {
                    getLog().info( "" );
                }
            }
        }
        
        getLog().info( "===================================================================" );
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
}
