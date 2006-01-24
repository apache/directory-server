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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;


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
    private String applicationName;
    
    /**
     * @parameter 
     * @required
     */
    private String applicationClass;

    /**
     * @parameter 
     * @required
     */
    private String applicationVersion;

    /**
     * @parameter 
     */
    private String applicationDescription;

    /**
     * @parameter
     */
    private String encoding;
    
    /**
     * @parameter
     */
    private Set excludes;

    private Artifact bootstrapper;
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
        setBootstrapperArtifact();
        
        // generate installers for all targets
        for ( int ii = 0; ii < allTargets.size(); ii++ )
        {
            Target target = ( Target ) allTargets.get( ii );
            
            // create the installation image first
            CreateImageCommand imgCommand = new CreateImageCommand( this, target );
            imgCommand.execute();
            
            // generate the installer
            if ( target instanceof IzPackTarget )
            {
                IzPackInstallerCommand izPackCmd = null;
                izPackCmd = new IzPackInstallerCommand( this, ( IzPackTarget ) target, imgCommand.getLayout() );
                izPackCmd.execute();
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
    
    
    private void setDefaults()
    {
        if ( allTargets == null )
        {
            return;
        }
        
        for ( int ii = 0; ii < allTargets.size(); ii++ )
        {
            Target target = ( Target ) allTargets.get( ii );

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
        }
    }
    
    
    private void setBootstrapperArtifact() throws MojoFailureException
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
                return;
            }
        }

        throw new MojoFailureException( "Bootstrapper dependency artifact required: " 
            + BOOTSTRAPPER_GROUP_ID + ":" + BOOTSTRAPPER_ARTIFACT_ID );
    }
    
    
    public void reportSetup()
    {
        getLog().info( "===================================================================" );
        getLog().info( "[installers:create]" );
        getLog().info( "applicationName = " + applicationName );
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


    public String getApplicationName()
    {
        return applicationName;
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


    public String getApplicationVersion()
    {
        return this.applicationVersion;
    }


    public String getApplicationClass()
    {
        return this.applicationClass;
    }


    public String getApplicationDescription()
    {
        return this.applicationDescription;
    }
}
