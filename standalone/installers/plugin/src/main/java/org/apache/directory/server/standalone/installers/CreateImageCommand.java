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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;


public class CreateImageCommand implements MojoCommand
{
    private final Properties filterProperties = new Properties( System.getProperties() );
    private final ServiceInstallersMojo mymojo;
    private final Target target;

    
    public CreateImageCommand( ServiceInstallersMojo mojo, Target target )
    {
        this.mymojo = mojo;
        this.target = target;
        initializeFiltering();
    }


    private void initializeFiltering() 
    {
        filterProperties.putAll( mymojo.getProject().getProperties() );
        filterProperties.put( "app" , mymojo.getApplicationName() );
        filterProperties.put( "app.caps" , mymojo.getApplicationName().toUpperCase() );
        filterProperties.put( "app.server.class", mymojo.getApplicationClass() );

        if ( mymojo.getApplicationVersion() != null )
        {
            filterProperties.put( "app.version", mymojo.getApplicationVersion() );
        }
        
        if ( mymojo.getApplicationDescription() != null )
        {
            filterProperties.put( "app.init.message", mymojo.getApplicationDescription() );
        }
    }

    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // make the layout directories
        File dir = new File( mymojo.getOutputDirectory(), target.getId() );
        InstallationLayout layout = new InstallationLayout( dir );
        layout.mkdirs();
        
        // copy over the REQUIRED bootstrapper.jar file 
        try
        {
            FileUtils.copyFile( mymojo.getBootstrapper().getFile(), layout.getBootstrapper() );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to copy bootstrapper.jar " + mymojo.getBootstrapper().getFile()
                + " into position " + layout.getBootstrapper() );
        }
        
        // copy over the REQUIRED bootstrapper configuration file
        try
        {
            FileUtils.copyFile( target.getBootstrapperConfiguraitonFile(), layout.getBootstrapperConfigurationFile() );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to copy bootstrapper configuration file "  
                + target.getBootstrapperConfiguraitonFile() 
                + " into position " + layout.getBootstrapperConfigurationFile() );
        }
        
        // copy over the optional logging configuration file
        if ( target.getLoggerConfigurationFile().exists() )
        {
            try
            {
                FileUtils.copyFile( target.getLoggerConfigurationFile(), 
                    layout.getLoggerConfigurationFile() );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy logger configuration file "  
                    + target.getLoggerConfigurationFile() 
                    + " into position " + layout.getLoggerConfigurationFile(), e );
            }
        }
        
        // copy over the optional server configuration file
        if ( target.getServerConfigurationFile().exists() )
        {
            try
            {
                FileUtils.copyFile( target.getServerConfigurationFile(), 
                    layout.getConfigurationFile() );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy server configuration file "  
                    + target.getServerConfigurationFile()
                    + " into position " + layout.getConfigurationFile(), e );
            }
        }
        
        // copy over the init script template
        if ( target.getOsFamily().equals( "unix" ) )
        {
            try
            {
                copyAsciiFile( getClass().getResourceAsStream( "template.init" ), layout.getInitScript(), true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy server configuration file "  
                    + target.getServerConfigurationFile()
                    + " into position " + layout.getInitScript(), e );
            }
        }
        
        // now copy over the jsvc executable renaming it to the applicationName 
        if ( target.getOsName().equals( "linux" ) && 
             target.getOsArch().equals( "i386" ) )
        {
            File executable = new File ( layout.getBinDirectory(), mymojo.getApplicationName() );
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
        
        // now copy over the jsvc executable renaming it to the mymojo.getApplicationName() 
        if ( target.getOsName().equals( "sunos" ) && 
             target.getOsArch().equals( "sparc" ) )
        {
            File executable = new File ( layout.getBinDirectory(), mymojo.getApplicationName() );
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
        
        // now copy over the jsvc executable renaming it to the mymojo.getApplicationName() 
        if ( target.getOsName().equals( "macosx" ) && target.getOsArch().equals( "ppc" ) )
        {
            File executable = new File ( layout.getBinDirectory(), mymojo.getApplicationName() );
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
        
        // now copy over the Prunsrv and Prunmgr executables renaming them to the mymojo.getApplicationName() + w for mgr
        if ( target.getOsFamily().equals( "windows" ) &&  target.getOsArch().equals( "x86" ) )
        {
            File executable = new File ( layout.getBinDirectory(), mymojo.getApplicationName() + ".exe" );
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

            executable = new File ( layout.getBinDirectory(), mymojo.getApplicationName() + "w.exe" );
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
            if ( mymojo.getEncoding() == null || mymojo.getEncoding().length() < 1 )
            {
                fileReader = new BufferedReader( new InputStreamReader( from ) );
                fileWriter = new FileWriter( to );
            }
            else
            {
                FileOutputStream outstream = new FileOutputStream( to );
                fileReader = new BufferedReader( new InputStreamReader( from, mymojo.getEncoding() ) );
                fileWriter = new OutputStreamWriter( outstream, mymojo.getEncoding() );
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
                    new ReflectionProperties( mymojo.getProject(), isPropertiesFile ), "${", "}" );
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


    private void copyDependencies( InstallationLayout layout ) throws MojoFailureException
    {
        Artifact artifact = null;
        Iterator artifacts = mymojo.getProject().getRuntimeArtifacts().iterator();
        while ( artifacts.hasNext() )
        {
            artifact = ( Artifact ) artifacts.next();
            if ( artifact.equals( mymojo.getBootstrapper() ) )
            {
                mymojo.getLog().info( "Not copying bootstrapper " + artifact );
            }
            else
            {
                String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
                if ( mymojo.getExcludes().contains( key ) )
                {
                    mymojo.getLog().info( "<<<=== excluded <<<=== " + key );
                    continue;
                }
                
                try
                {
                    FileUtils.copyFileToDirectory( artifact.getFile(), layout.getLibDirectory() );
                    mymojo.getLog().info( "===>>> included ===>>> " + key );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy dependency artifact "  
                        + artifact + " into position " + layout.getLibDirectory() );
                }
            }
        }
    }
}
