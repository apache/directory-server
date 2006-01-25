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
import java.io.IOException;
import java.util.Properties;

import org.apache.directory.server.standalone.daemon.InstallationLayout;
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
    
    
    IzPackInstallerCommand( ServiceInstallersMojo mymojo, IzPackTarget target, InstallationLayout layout )
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
        shellLinkDll = new File( imageDir, "ShellLink.dll" );
        initializeFiltering();
    }
    
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        generateInstallerFiles( target, layout );
        Project antProject = new Project();
        IzPackTask task = new IzPackTask();
        task.setBasedir( izPackBase.getPath() );
        task.setProject( antProject );
        task.setInput( izPackInput.getPath() );
        task.setOutput( izPackOutput.getPath() );
        task.setTaskName( "izpack" );
        task.execute();
    }


    private void generateInstallerFiles( IzPackTarget target, InstallationLayout layout ) throws MojoFailureException
    {
        if ( target.getOsFamily().equals( "windows" ) )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                    getClass().getResourceAsStream( "izpack_install_windows_template.xml" ), izPackInput, true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy izpack input file "  
                    + getClass().getResource( "izpack_install_windows_template.xml" )
                    + " into position " + izPackInput, e );
            }

            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                    getClass().getResourceAsStream( "izpack_install_shortcuts_windows.xml" ), izPackWindowsShortcuts, true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy izpack windows shortcuts file "  
                    + getClass().getResource( "izpack_install_shortcuts_windows.xml" )
                    + " into position " + izPackWindowsShortcuts, e );
            }

            try
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream( "ShellLink.dll" ), shellLinkDll );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy izpack shellLinkDll file "  
                    + getClass().getResource( "ShellLink.dll" )
                    + " into position " + shellLinkDll, e );
            }
        }
        else if ( target.getOsFamily().equals( "unix" ) || target.getOsFamily().equals( "mac" ) )
        {
            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                    getClass().getResourceAsStream( "izpack_install_unix_template.xml" ), izPackInput, true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy izpack input file "  
                    + getClass().getResource( "izpack_install_unix_template.xml" )
                    + " into position " + izPackInput, e );
            }

            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                    getClass().getResourceAsStream( "izpack_install_shortcuts_unix.xml" ), izPackUnixShortcuts, true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy izpack unix shortcuts file "  
                    + getClass().getResource( "izpack_install_shortcuts_unix.xml" )
                    + " into position " + izPackUnixShortcuts, e );
            }

            try
            {
                MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                    getClass().getResourceAsStream( "template.init" ), layout.getInitScript(), true );
            }
            catch ( IOException e )
            {
                mymojo.getLog().error( "Failed to copy init script "  
                    + getClass().getResource( "template.init" )
                    + " into position " + layout.getInitScript(), e );
            }
        }

        try
        {
            MojoHelperUtils.copyAsciiFile( mymojo, filterProperties, 
                getClass().getResourceAsStream( "izpack_install_user_input.xml" ), izPackUserInput, true );
        }
        catch ( IOException e )
        {
            mymojo.getLog().error( "Failed to copy izpack input file "  
                + getClass().getResource( "izpack_install_user_input.xml" )
                + " into position " + izPackUserInput, e );
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
        if ( ! layout.getLicenseFile().exists() )
        {
            touchFile( layout.getLicenseFile() );
        }
        filterProperties.put( "app.license" , layout.getLicenseFile().getPath() );
        if ( ! layout.getReadmeFile().exists() )
        {
            touchFile( layout.getReadmeFile() );
        }
        filterProperties.put( "app.readme" , layout.getReadmeFile().getPath() );
        if ( ! layout.getLogoIconFile().exists() )
        {
            touchFile( layout.getLogoIconFile() );
        }
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
            System.out.println( " ***************************************** " );
            System.out.println( layout.getInitScript().getName() );
            System.out.println( " ***************************************** " );
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
