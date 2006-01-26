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
package org.apache.directory.server.standalone.installers.izpack;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.directory.server.standalone.daemon.InstallationLayout;
import org.apache.directory.server.standalone.installers.MojoCommand;
import org.apache.directory.server.standalone.installers.MojoHelperUtils;
import org.apache.directory.server.standalone.installers.ServiceInstallersMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Touch;

import com.izforge.izpack.ant.IzPackTask;


/**
 * The IzPack installer command.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class IzPackInstallerCommand implements MojoCommand
{
    private final static String UNIX_INSTALL = "install_unix.xml";
    private final static String UNIX_SHORTCUTS = "unix_shortcuts.xml";
    private final static String WINDOWS_INSTALL = "install_windows.xml";
    private final static String WINDSOWS_SHORTCUTS = "windows_shortcuts.xml";
    private final static String USER_INPUT = "user_input.xml";
    private final static String SHELLLINK_DLL = "ShellLink.dll";
    
    private final Properties filterProperties = new Properties( System.getProperties() );
    private final ServiceInstallersMojo mymojo;
    private final IzPackTarget target;
    private final InstallationLayout layout;
    
    private File izPackInput;
    private File izPackUserInput;
    private File izPackWindowsShortcuts;
    private File izPackUnixShortcuts;
    private File izPackOutput;
    private File shellLinkDll;
    private File izPackBase;
    
    
    public IzPackInstallerCommand( ServiceInstallersMojo mymojo, IzPackTarget target, InstallationLayout layout )
    {
        this.target = target;
        this.layout = layout;
        this.mymojo = mymojo;
        File imageDir = layout.getBaseDirectory().getParentFile();
        izPackBase = new File( imageDir, target.getId() );
        izPackOutput = new File( imageDir, target.getId() + "_izpack_installer.jar" );
        izPackInput = new File( imageDir, target.getId() + "_izpack_install.xml" );
        izPackUserInput = new File( imageDir, target.getId() + "_izpack_install_user_input.xml" );
        izPackWindowsShortcuts = new File( imageDir, target.getId() + "_izpack_windows_shortcuts.xml" );
        izPackUnixShortcuts = new File( imageDir, target.getId() + "_izpack_unix_shortcuts.xml" );
        shellLinkDll = new File( imageDir, SHELLLINK_DLL );
        initializeFiltering();
    }
    
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        doIzPackFiles();
        Project antProject = new Project();
        IzPackTask task = new IzPackTask();
        task.setBasedir( izPackBase.getPath() );
        task.setProject( antProject );
        task.setInput( izPackInput.getPath() );
        task.setOutput( izPackOutput.getPath() );
        task.setTaskName( "izpack" );
        task.execute();
    }
    
    
    private void doIzPackFiles() throws MojoFailureException
    {
        // -------------------------------------------------------------------
        // For windows we include use a different template file than for unix
        // if the project does not supply one.  We also add the windows short-
        // cuts file template as well in the same fashion.  Also a native dll
        // is deposited outside of the image folder to create windows shortcuts
        // -------------------------------------------------------------------

        if ( target.getOsFamily().equals( "windows" ) )
        {
            // handle the installer file
            if ( target.getIzPackInstallFile().exists() )
            {
                try
                {
                    MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                        target.getIzPackInstallFile(), izPackInput, true );
                }
                catch ( IOException e )
                {
                    mymojo.getLog().error( "Failed to copy project supplied izpack install file template "  
                        + target.getIzPackInstallFile() + " into position " + izPackInput, e );
                }
                
                if ( mymojo.getLog().isInfoEnabled() )
                {
                    mymojo.getLog().info( "Using project supplied installer configuration file: " 
                        + target.getIzPackInstallFile() );
                }
            }
            else
            {
                try
                {
                    MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                        getClass().getResourceAsStream( WINDOWS_INSTALL ), izPackInput, true );
                }
                catch ( IOException e )
                {
                    mymojo.getLog().error( "Failed to copy bundled izpack windows install file "  
                        + getClass().getResource( WINDOWS_INSTALL )
                        + " into position " + izPackInput, e );
                }
                
                if ( mymojo.getLog().isInfoEnabled() )
                {
                    mymojo.getLog().info( "Using bundled installer configuration file: " 
                        + getClass().getResource( WINDOWS_INSTALL ) );
                }
            }

            // handle the windows shortcuts file 
            if ( target.getIzPackShortcutsWindowsFile().exists() )
            {
                try
                {
                    MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                        target.getIzPackShortcutsWindowsFile(), izPackWindowsShortcuts, true );
                }
                catch ( IOException e )
                {
                    mymojo.getLog().error( "Failed to copy project supplied izpack windows shortcuts file "  
                        + target.getIzPackShortcutsWindowsFile()
                        + " into position " + izPackWindowsShortcuts, e );
                }
                
                if ( mymojo.getLog().isInfoEnabled() )
                {
                    mymojo.getLog().info( "Using project supplied windows shortcuts configuration file: " 
                        + target.getIzPackShortcutsWindowsFile() );
                }
            }
            else
            {
                try
                {
                    MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                        getClass().getResourceAsStream( WINDSOWS_SHORTCUTS ), izPackWindowsShortcuts, true );
                }
                catch ( IOException e )
                {
                    mymojo.getLog().error( "Failed to copy bundled izpack windows shortcuts file "  
                        + getClass().getResource( WINDSOWS_SHORTCUTS )
                        + " into position " + izPackWindowsShortcuts, e );
                }
                
                if ( mymojo.getLog().isInfoEnabled() )
                {
                    mymojo.getLog().info( "Using bundled windows shortcuts configuration file: " 
                        + getClass().getResource( WINDSOWS_SHORTCUTS ) );
                }
            }
            
            // copy the ShellLink creation dll into the images folder
            try
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( SHELLLINK_DLL ), shellLinkDll );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy izpack shellLinkDll file "  
                    + getClass().getResource( SHELLLINK_DLL )
                    + " into position " + shellLinkDll, e );
            }
        }
        else if ( target.getOsFamily().equals( "unix" ) || target.getOsFamily().equals( "mac" ) )
        {
            if ( target.getIzPackInstallFile().exists() )
            {
                try
                {
                    MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                        target.getIzPackInstallFile(), izPackInput, true );
                }
                catch ( IOException e )
                {
                    mymojo.getLog().error( "Failed to copy project supplied izpack input file "  
                        + target.getIzPackInstallFile()
                        + " into position " + izPackInput, e );
                }
                
                if ( mymojo.getLog().isInfoEnabled() )
                {
                    mymojo.getLog().info( "Using project supplied installer configuration file: " 
                        + target.getIzPackInstallFile() );
                }
            }
            else
            {
                try
                {
                    MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                        getClass().getResourceAsStream( UNIX_INSTALL ), izPackInput, true );
                }
                catch ( IOException e )
                {
                    mymojo.getLog().error( "Failed to copy bundled izpack input file for unix "  
                        + getClass().getResource( UNIX_INSTALL )
                        + " into position " + izPackInput, e );
                }
                
                if ( mymojo.getLog().isInfoEnabled() )
                {
                    mymojo.getLog().info( "Using bundled installer configuration file: " 
                        + getClass().getResource( UNIX_INSTALL ) );
                }
            }

            if ( target.getIzPackShortcutsUnixFile().exists() )
            {
                try
                {
                    MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                        target.getIzPackShortcutsUnixFile(), izPackUnixShortcuts, true );
                }
                catch ( IOException e )
                {
                    mymojo.getLog().error( "Failed to copy project supplied izpack unix shortcuts file "  
                        + target.getIzPackShortcutsUnixFile()
                        + " into position " + izPackUnixShortcuts, e );
                }
                
                if ( mymojo.getLog().isInfoEnabled() )
                {
                    mymojo.getLog().info( "Using project supplied unix shortcuts configuration file: " 
                        + target.getIzPackShortcutsUnixFile() );
                }
            }
            else
            {
                try
                {
                    MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                        getClass().getResourceAsStream( UNIX_SHORTCUTS ), izPackUnixShortcuts, true );
                }
                catch ( IOException e )
                {
                    mymojo.getLog().error( "Failed to copy bundled izpack unix shortcuts file "  
                        + getClass().getResource( UNIX_SHORTCUTS )
                        + " into position " + izPackUnixShortcuts, e );
                }
                
                if ( mymojo.getLog().isInfoEnabled() )
                {
                    mymojo.getLog().info( "Using bundled unix shortcuts configuration file: " 
                        + getClass().getResource( UNIX_SHORTCUTS ) );
                }
            }

            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                    getClass().getResourceAsStream( "../template.init" ), layout.getInitScript(), true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy init script "  
                    + getClass().getResource( "../template.init" )
                    + " into position " + layout.getInitScript(), e );
            }
        }

        if ( target.getIzPackUserInputFile().exists() )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                    target.getIzPackUserInputFile(), izPackUserInput, true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy project supplied izpack input file "  
                    + target.getIzPackUserInputFile()
                    + " into position " + izPackUserInput, e );
            }
            
            if ( mymojo.getLog().isInfoEnabled() )
            {
                mymojo.getLog().info( "Using project supplied user input configuration file: " 
                    + target.getIzPackUserInputFile() );
            }
        }
        else
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                    getClass().getResourceAsStream( USER_INPUT ), izPackUserInput, true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy bundled izpack input file "  
                    + getClass().getResource( USER_INPUT )
                    + " into position " + izPackUserInput, e );
            }
            
            if ( mymojo.getLog().isInfoEnabled() )
            {
                mymojo.getLog().info( "Using bundled user input configuration file: " 
                    + target.getIzPackUserInputFile() );
            }
        }
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

        // -------------------------------------------------------------------
        // WARNING: hard code values just to for testing
        // -------------------------------------------------------------------
        
        // optional properties from mojo but should default:
        filterProperties.put( "app.author" , "Directory Project Team" );
        filterProperties.put( "app.email" , "dev@directory.apache.org" );
        filterProperties.put( "app.url" , "http://directory.apache.org" );
        filterProperties.put( "app.java.version" , "1.4" );
        
        // izpack compiler will barf if these files are not present
        // files which are user specified also from mojo
        // these files need to be copied to the image folder of the target
        
        // this one is installed or a defualt is installed by create image command
        filterProperties.put( "app.license" , layout.getLicenseFile().getPath() );

        if ( ! layout.getReadmeFile().exists() )
        {
            touchFile( layout.getReadmeFile() );
        }
        filterProperties.put( "app.readme" , layout.getReadmeFile().getPath() );

        // this one is installed or a default is installed by create image command
        filterProperties.put( "app.icon" , layout.getLogoIconFile().getPath() );
        
        // generated files
        if ( target.getOsFamily().equals( "windows" ) )
        {
            filterProperties.put( "windows.shortcuts" , izPackWindowsShortcuts.getPath() );
        }
        
        if ( target.getOsFamily().equals( "unix" ) && target.getOsFamily().equals( "unix" ) )
        {
            filterProperties.put( "unix.shortcuts", izPackUnixShortcuts.getPath() );
        }
        filterProperties.put( "user.input", izPackUserInput.getPath() );
        filterProperties.put( "image.basedir", layout.getBaseDirectory().getPath() );

        if ( target.getOsFamily().equals( "mac" )|| target.getOsFamily().equals( "unix" ) )
        {
            filterProperties.put( "server.init", layout.getInitScript().getName() );
        }
        
        // for the substitution of the application's installation path done by izPack
        filterProperties.put( "app.install.base", "%INSTALL_PATH" );
    }
    
    
    static void touchFile( File file )
    {
        Touch touch = new Touch();
        touch.setProject( new Project() );
        touch.setFile( file );
        touch.execute();
    }
}
