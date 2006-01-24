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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
 * @goal generate
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


    private Properties filterProperties;

    private Artifact bootstrapper;
    
    private List allTargets;
    
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // collect all targets 
        initializeAllTargets();
        
        // initialize filters
        initializeFiltering();
        
        // makes sure defaulted values are set to globals
        setDefaults();
        
        // bail if there is nothing to do 
        if ( izPackTargets == null )
        {
            getLog().info( "===================================================================" );
            getLog().info( "[installers:create]" );
            getLog().info( "No installer targets to create." );
            getLog().info( "===================================================================" );
            return;
        }
        
        // report what we have to build 
        reportSetup();

        // search for and find the bootstrapper artifact
        setBootstrapperArtifact();
        
        // creates installation images for all targets
        createImages();
    }
    
    
    private void initializeAllTargets()
    {
        allTargets = new ArrayList();
        
        if ( izPackTargets != null )
        {
            Collections.addAll( allTargets, izPackTargets );
        }
        
        if ( innoTargets != null )
        {
            Collections.addAll( allTargets, innoTargets );
        }
        
        if ( rpmTargets != null )
        {
            Collections.addAll( allTargets, rpmTargets );
        }
        
        if ( debTargets != null )
        {
            Collections.addAll( allTargets, debTargets );
        }
        
        if ( pkgTargets != null )
        {
            Collections.addAll( allTargets, pkgTargets );
        }
    }
    
    
    private void copyDependencies( InstallationLayout layout ) throws MojoFailureException
    {
        Artifact artifact = null;
        Iterator artifacts = project.getRuntimeArtifacts().iterator();
        while ( artifacts.hasNext() )
        {
            artifact = ( Artifact ) artifacts.next();
            if ( artifact.getArtifactId().equals( BOOTSTRAPPER_ARTIFACT_ID ) || artifact.getGroupId().equals( BOOTSTRAPPER_GROUP_ID ) )
            {
                getLog().info( "Not copying bootstrapper " + artifact );
            }
            else
            {
                String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
                if ( excludes.contains( key ) )
                {
                    getLog().info( "<<<=== excluded <<<=== " + key );
                    continue;
                }
                
                try
                {
                    FileUtils.copyFileToDirectory( artifact.getFile(), layout.getLibDirectory() );
                    getLog().info( "===>>> included ===>>> " + key );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy dependency artifact "  
                        + artifact + " into position " + layout.getLibDirectory() );
                }
            }
        }
    }
    

    private void createImages() throws MojoFailureException
    {
        for ( int ii = 0; ii < allTargets.size(); ii++ )
        {
            // make the layout directories
            File dir = new File( outputDirectory, ( ( Target ) allTargets.get( ii ) ).getId() );
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
                FileUtils.copyFile( ( ( Target ) allTargets.get( ii ) ).getBootstrapperConfiguraitonFile(), 
                    layout.getBootstrapperConfigurationFile() );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy bootstrapper configuration file "  
                    + ( ( Target ) allTargets.get( ii ) ).getBootstrapperConfiguraitonFile() 
                    + " into position " + layout.getBootstrapperConfigurationFile() );
            }
            
            // copy over the optional logging configuration file
            if ( ( ( Target ) allTargets.get( ii ) ).getLoggerConfigurationFile().exists() )
            {
                try
                {
                    FileUtils.copyFile( ( ( Target ) allTargets.get( ii ) ).getLoggerConfigurationFile(), 
                        layout.getLoggerConfigurationFile() );
                }
                catch ( IOException e )
                {
                    getLog().error( "Failed to copy logger configuration file "  
                        + ( ( Target ) allTargets.get( ii ) ).getLoggerConfigurationFile() 
                        + " into position " + layout.getLoggerConfigurationFile(), e );
                }
            }
            
            // copy over the optional server configuration file
            if ( ( ( Target ) allTargets.get( ii ) ).getServerConfigurationFile().exists() )
            {
                try
                {
                    FileUtils.copyFile( ( ( Target ) allTargets.get( ii ) ).getServerConfigurationFile(), 
                        layout.getConfigurationFile() );
                }
                catch ( IOException e )
                {
                    getLog().error( "Failed to copy server configuration file "  
                        + ( ( Target ) allTargets.get( ii ) ).getServerConfigurationFile()
                        + " into position " + layout.getConfigurationFile(), e );
                }
            }
            
            // copy over the init script template
            if ( ( ( Target ) allTargets.get( ii ) ).getOsFamily().equals( "unix" ) )
            {
                try
                {
                    copyAsciiFile( getClass().getResourceAsStream( "template.init" ), layout.getInitScript(), true );
                }
                catch ( IOException e )
                {
                    getLog().error( "Failed to copy server configuration file "  
                        + ( ( Target ) allTargets.get( ii ) ).getServerConfigurationFile()
                        + " into position " + layout.getInitScript(), e );
                }
            }
            
            // now copy over the jsvc executable renaming it to the applicationName 
            if ( ( ( Target ) allTargets.get( ii ) ).getOsName().equals( "linux" ) && 
                 ( ( Target ) allTargets.get( ii ) ).getOsArch().equals( "i386" ) )
            {
                File executable = new File ( layout.getBinDirectory(), applicationName );
                try
                {
                    copyBinaryFile( getClass().getResourceAsStream( "jsvc_linux_i386" ), executable );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy jsvc executable file "  
                        + getClass().getResource( "jsvc_linux_i386" )
                        + " into position " + executable.getAbsolutePath() );
                }
            }
            
            // now copy over the jsvc executable renaming it to the applicationName 
            if ( ( ( Target ) allTargets.get( ii ) ).getOsName().equals( "sunos" ) && 
                 ( ( Target ) allTargets.get( ii ) ).getOsArch().equals( "sparc" ) )
            {
                File executable = new File ( layout.getBinDirectory(), applicationName );
                try
                {
                    copyBinaryFile( getClass().getResourceAsStream( "jsvc_solaris_sparc" ), executable );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy jsvc executable file "  
                        + getClass().getResource( "jsvc_solaris_sparc" )
                        + " into position " + executable.getAbsolutePath() );
                }
            }
            
            // now copy over the jsvc executable renaming it to the applicationName 
            if ( ( ( Target ) allTargets.get( ii ) ).getOsName().equals( "macosx" ) && 
                 ( ( Target ) allTargets.get( ii ) ).getOsArch().equals( "ppc" ) )
            {
                File executable = new File ( layout.getBinDirectory(), applicationName );
                try
                {
                    copyBinaryFile( getClass().getResourceAsStream( "jsvc_macosx_ppc" ), executable );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy jsvc executable file "  
                        + getClass().getResource( "jsvc_macosx_ppc" )
                        + " into position " + executable.getAbsolutePath() );
                }
            }
            
            // now copy over the Prunsrv and Prunmgr executables renaming them to the applicationName + w for mgr
            if ( ( ( Target ) allTargets.get( ii ) ).getOsFamily().equals( "windows" ) && 
                 ( ( Target ) allTargets.get( ii ) ).getOsArch().equals( "x86" ) )
            {
                File executable = new File ( layout.getBinDirectory(), applicationName + ".exe" );
                try
                {
                    copyBinaryFile( getClass().getResourceAsStream( "prunsrv.exe" ), executable );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy prunsrv executable file "  
                        + getClass().getResource( "prunsrv.exe" )
                        + " into position " + executable.getAbsolutePath() );
                }

                executable = new File ( layout.getBinDirectory(), applicationName + "w.exe" );
                try
                {
                    copyBinaryFile( getClass().getResourceAsStream( "prunmgr.exe" ), executable );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy prunmgr executable file "  
                        + getClass().getResource( "prunmgr.exe" )
                        + " into position " + executable.getAbsolutePath() );
                }
            }
            
            copyDependencies( layout );
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
                
//                if ( ( Target ) allTargets.get( ii ).getDependencies() != null )
//                {
//                    StringBuffer buf = new StringBuffer();
//                    for ( int jj = 0; jj < ( Target ) allTargets.get( ii ).getDependencies().length; jj++ )
//                    {
//                        buf.append( ( Target ) allTargets.get( ii ).getDependencies()[jj] );
//                        buf.append( " " );
//                    }
//                    getLog().info( "DEPENDENCIES: " + buf.toString() );
//                }
                
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

    
    private void copyBinaryFile( InputStream from, File to ) throws IOException
    {
        FileOutputStream out = null;
        try 
        {
            out = new FileOutputStream( to );
            IOUtil.copy( from, out );
        }
        finally
        {
            IOUtil.close( from );
            IOUtil.close( out );
        }
    }
    
    
    private void copyAsciiFile( InputStream from, File to, boolean filtering ) throws IOException
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
                // support _${token}
                reader = new InterpolationFilterReader( fileReader, filterProperties, "_${", "}" );
                // support ${token}
                reader = new InterpolationFilterReader( reader, filterProperties, "${", "}" );
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
