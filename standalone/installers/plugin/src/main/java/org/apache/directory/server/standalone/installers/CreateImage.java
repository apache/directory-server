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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.Properties;

import org.apache.directory.server.standalone.daemon.InstallationLayout;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;


/**
 * Maven 2 mojo creating the platform specific installation layout images.
 * 
 * @goal create
 * @description Creates platform specific installation layout images.
 * @phase package
 * @requiresDependencyResolution runtime
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CreateImage extends AbstractMojo
{
    private static final String BOOTSTRAPPER_ARTIFACT_ID = "org.apache.directory.server.standalone.daemon";
    private static final String BOOTSTRAPPER_GROUP_ID = "org.apache.directory.server.standalone.daemon";

    
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
    private Target[] targets;

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


    private Properties filterProperties;

    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // initialize filters
        initializeFiltering();
        
        // makes sure global values and report targets if have any
        setDefaults();
        if ( targets == null )
        {
            getLog().info( "===================================================================" );
            getLog().info( "[installers:create]" );
            getLog().info( "No installer targets to create." );
            getLog().info( "===================================================================" );
            return;
        }
        reportSetup();

        Artifact bootstrapper = getBootstrapperArtifact();
        for ( int ii = 0; ii < targets.length; ii++ )
        {
            // make the layout directories
            File dir = new File( outputDirectory, targets[ii].getId() );
            InstallationLayout layout = new InstallationLayout( dir );
            layout.mkdirs();
            
            // copy over the REQUIRED bootstrapper.jar file 
            try
            {
                FileUtils.copyFile( bootstrapper.getFile(), layout.getBootstrapper() );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy bootstrapper.jar " + bootstrapper.getFile() 
                    + " into position " + layout.getBootstrapper() );
            }
            
            // copy over the REQUIRED bootstrapper configuration file
            try
            {
                FileUtils.copyFile( targets[ii].getBootstrapperConfiguraitonFile(), layout.getBootstrapperConfigurationFile() );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy bootstrapper configuration file "  
                    + targets[ii].getBootstrapperConfiguraitonFile() 
                    + " into position " + layout.getBootstrapperConfigurationFile() );
            }
            
            // copy over the optional logging configuration file
            if ( targets[ii].getLoggerConfigurationFile().exists() )
            {
                try
                {
                    FileUtils.copyFile( targets[ii].getLoggerConfigurationFile(), layout.getLoggerConfigurationFile() );
                }
                catch ( IOException e )
                {
                    getLog().error( "Failed to copy logger configuration file "  
                        + targets[ii].getLoggerConfigurationFile() 
                        + " into position " + layout.getLoggerConfigurationFile(), e );
                }
            }
            
            // copy over the optional server configuration file
            if ( targets[ii].getServerConfigurationFile().exists() )
            {
                try
                {
                    FileUtils.copyFile( targets[ii].getServerConfigurationFile(), layout.getConfigurationFile() );
                }
                catch ( IOException e )
                {
                    getLog().error( "Failed to copy server configuration file "  
                        + targets[ii].getServerConfigurationFile()
                        + " into position " + layout.getConfigurationFile(), e );
                }
            }
            
            // copy over the init script template
            if ( targets[ii].isFamily( "unix" ) )
            {
                try
                {
                    copyFile( getClass().getResourceAsStream( "template.init" ), layout.getInitScript(), true );
                }
                catch ( IOException e )
                {
                    getLog().error( "Failed to copy server configuration file "  
                        + targets[ii].getServerConfigurationFile()
                        + " into position " + layout.getConfigurationFile(), e );
                }
            }
        }        
    }
    
    
    private void setDefaults()
    {
        if ( targets == null )
        {
            return;
        }
        
        for ( int ii = 0; ii < targets.length; ii++ )
        {
            Target target = targets[ii];

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
        }
    }
    
    
    private Artifact getBootstrapperArtifact() throws MojoFailureException
    {
        Artifact artifact = null;
        Iterator artifacts = project.getDependencyArtifacts().iterator();
        while ( artifacts.hasNext() )
        {
            artifact = ( Artifact ) artifacts.next();
            if ( artifact.getArtifactId().equals( BOOTSTRAPPER_ARTIFACT_ID ) || artifact.getGroupId().equals( BOOTSTRAPPER_GROUP_ID ) )
            {
                getLog().info( "Found bootstrapper dependency with version: " + artifact.getVersion() );
                return artifact;
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
        getLog().info( "----------------------------- targets -----------------------------" );
        
        if ( targets != null )
        {
            for ( int ii = 0; ii < targets.length; ii++ )
            {
                getLog().info( "id: " + targets[ii].getId() );
                
//                if ( targets[ii].getDependencies() != null )
//                {
//                    StringBuffer buf = new StringBuffer();
//                    for ( int jj = 0; jj < targets[ii].getDependencies().length; jj++ )
//                    {
//                        buf.append( targets[ii].getDependencies()[jj] );
//                        buf.append( " " );
//                    }
//                    getLog().info( "DEPENDENCIES: " + buf.toString() );
//                }
                
                getLog().info( "osName: " + targets[ii].getOsName() );
                getLog().info( "osArch: " + targets[ii].getOsArch() );
                getLog().info( "osVersion: " + targets[ii].getOsVersion() );
                getLog().info( "installer: " + targets[ii].getInstaller() );
                getLog().info( "daemonFramework: " + targets[ii].getDaemonFramework() );
                getLog().info( "log4jProperties: " + targets[ii].getLoggerConfigurationFile() );
                getLog().info( "bootstrapperProperties: " + targets[ii].getBootstrapperConfiguraitonFile() );
                getLog().info( "serverXml: " + targets[ii].getServerConfigurationFile() );
                
                if ( ii + 1 < targets.length )
                {
                    getLog().info( "" );
                }
            }
        }
        
        getLog().info( "===================================================================" );
    }

    
    private void initializeFiltering() throws MojoExecutionException
    {
        // System properties
        filterProperties = new Properties( System.getProperties() );
        // Project properties
        filterProperties.putAll( project.getProperties() );

        filterProperties.put( "app" , applicationName );
        filterProperties.put( "app.caps" , applicationName.toUpperCase() );
        filterProperties.put( "app.server.class", applicationClass );
        if ( applicationVersion != null )
        {
            filterProperties.put( "app.version", applicationVersion );
        }
        if ( applicationDescription != null )
        {
            filterProperties.put( "app.init.message", applicationDescription );
        }
    }

    
    private void copyFile( InputStream from, File to, boolean filtering ) throws IOException
    {
        // buffer so it isn't reading a byte at a time!
        Reader fileReader = null;
        Writer fileWriter = null;
        try
        {
            if ( encoding == null || encoding.length() < 1 )
            {
                fileReader = new BufferedReader( new InputStreamReader( from ) );
                fileWriter = new FileWriter( to );
            }
            else
            {
                FileOutputStream outstream = new FileOutputStream( to );
                fileReader = new BufferedReader( new InputStreamReader( from, encoding ) );
                fileWriter = new OutputStreamWriter( outstream, encoding );
            }

            Reader reader = null;
            if ( filtering )
            {
                // support ${token}
                reader = new InterpolationFilterReader( fileReader, filterProperties, "${", "}" );
                // support @token@
                reader = new InterpolationFilterReader( reader, filterProperties, "@", "@" );
    
                boolean isPropertiesFile = false;
                if ( to.isFile() && to.getName().endsWith( ".properties" ) )
                {
                    isPropertiesFile = true;
                }
                reader = new InterpolationFilterReader( reader, 
                    new ReflectionProperties( project, isPropertiesFile ), "${", "}" );
            }
            else
            {
                reader = fileReader;
            }
            IOUtil.copy( reader, fileWriter );
        }
        finally
        {
            IOUtil.close( fileReader );
            IOUtil.close( fileWriter );
        }
    }
}
