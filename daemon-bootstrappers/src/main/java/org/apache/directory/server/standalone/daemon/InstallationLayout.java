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
package org.apache.directory.server.standalone.daemon;


import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Convenience class to encapsulate paths to various folders and files within
 * an installation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class InstallationLayout
{
    private final static Logger log = LoggerFactory.getLogger( InstallationLayout.class );
    private final static FileFilter JAR_FILTER;
    
    static 
    {
        JAR_FILTER = new FileFilter() 
        {
            public boolean accept( File pathname )
            {
                return pathname.isFile() && pathname.getName().endsWith( ".jar" );
            }
        };
    }
    
    private final File baseDirectory;
    private transient File[] dirs;
    private transient File[] files;
    private transient URL[] allJars = null;
    private transient URL[] dependentJars = null;
    private transient URL[] extensionJars = null;

    
    public InstallationLayout( File baseDirectory )
    {
        this.baseDirectory = baseDirectory;
    }
    
    
    public InstallationLayout( String baseDirectoryPath )
    {
        this.baseDirectory = new File( baseDirectoryPath );
    }
    
    
    public File getBaseDirectory()
    {
        return baseDirectory;
    }
    
    
    public File getBinDirectory()
    {
        return new File( baseDirectory, "bin" );
    }
    
    
    public File getLibDirectory()
    {
        return new File( baseDirectory, "lib" );
    }
    
    
    public File getVarDirectory()
    {
        return new File( baseDirectory, "var" );
    }
    
    
    public File getLogDirectory()
    {
        return new File( getVarDirectory(), "log" );
    }
    
    
    public File getRunDirectory()
    {
        return new File( getVarDirectory(), "run" );
    }
    
    
    public File getPidFile()
    {
        return new File( getRunDirectory(), "server.pid" );
    }
    
    
    public File getBootstrapper()
    {
        return new File( getBinDirectory(), "bootstrapper.jar" );
    }
    
    
    public File getLogger()
    {
        return new File( getBinDirectory(), "logger.jar" );
    }
    
    
    public File getDaemon()
    {
        return new File( getBinDirectory(), "daemon.jar" );
    }
    
    
    public File getInitScript()
    {
        return getInitScript( "server.init" );
    }
    
    
    public File getInitScript( String name )
    {
        return new File( getBinDirectory(), name );
    }
    
    
    public File getExtensionsDirectory()
    {
        return new File( getLibDirectory(), "ext" );
    }
    
    
    public File getPartitionsDirectory()
    {
        return new File( getVarDirectory(), "partitions" );
    }
    
    
    public File getConfigurationDirectory()
    {
        return new File( baseDirectory, "conf" );
    }

    
    public File getConfigurationFile()
    {
        return getConfigurationFile( "server.xml" );
    }

    
    public File getConfigurationFile( String name )
    {
        return new File( getConfigurationDirectory(), name );
    }

    
    public File getLoggerConfigurationFile()
    {
        return getLoggerConfigurationFile( "log4j.properties" );
    }

    
    public File getLoggerConfigurationFile( String name )
    {
        return new File( getConfigurationDirectory(), name );
    }

    
    public File getLogoIconFile()
    {
        return getLogoIconFile( "logo.ico" );
    }

    
    public File getLogoIconFile( String name )
    {
        return new File( getBaseDirectory(), name );
    }

    
    public File getLicenseFile()
    {
        return getLicenseFile( "LICENSE.txt" );
    }

    
    public File getLicenseFile( String name )
    {
        return new File( getBaseDirectory(), name );
    }

    
    public File getReadmeFile()
    {
        return getReadmeFile( "README.txt" );
    }

    
    public File getReadmeFile( String name )
    {
        return new File( getBaseDirectory(), name );
    }

    
    public File getBootstrapperConfigurationFile()
    {
        return new File( getConfigurationDirectory(), "bootstrapper.properties" );
    }

    
    public void init()
    {
        if ( dirs == null )
        {
            dirs = new File[] {
                this.getBaseDirectory(),
                this.getBinDirectory(),
                this.getLibDirectory(),
                this.getExtensionsDirectory(),
                this.getConfigurationDirectory(),
                this.getVarDirectory(),
                this.getLogDirectory(),
                this.getPartitionsDirectory(),
                this.getRunDirectory()
            };
        }
        
        if ( files == null )
        {
            // only these files are requred to be present
            files = new File[] {
                this.getBootstrapper(),
                this.getBootstrapperConfigurationFile()
            };
        }
    }
    
    
    public void verifyInstallation()
    {
        init();
        
        for ( int ii = 0; ii < dirs.length; ii++ )
        {
            if ( ! dirs[ii].exists() )
            {
                throw new IllegalStateException( dirs[ii] + " does not exist!" );
            }
            
            if ( dirs[ii].isFile() )
            {
                throw new IllegalStateException( dirs[ii] + " is a file when it should be a directory." );
            }
            
            if ( ! dirs[ii].canWrite() )
            {
                throw new IllegalStateException( dirs[ii] + " is write protected from the current user: " 
                    + System.getProperty( "user.name" ) );
            }
        }
        
        for ( int ii = 0; ii < files.length; ii++ )
        {
            if ( ! files[ii].exists() )
            {
                throw new IllegalStateException( files[ii] + " does not exist!" );
            }
            
            if ( files[ii].isDirectory() )
            {
                throw new IllegalStateException( files[ii] + " is a directory when it should be a file." );
            }
            
            if ( ! dirs[ii].canRead() )
            {
                throw new IllegalStateException( files[ii] + " is not readable by the current user: " 
                    + System.getProperty( "user.name" ) );
            }
        }
    }
    
    
    public void mkdirs()
    {
        init();
        
        for ( int ii = 0; ii < dirs.length; ii++ )
        {
            dirs[ii].mkdirs();
        }
    }
    
    
    public URL[] getDependentJars()
    {
        if ( dependentJars == null )
        {
            File[] deps = getLibDirectory().listFiles( new FileFilter() {
                public boolean accept( File pathname )
                {
                    return pathname.isFile() && pathname.getName().endsWith( ".jar" );
                }
            });
            
            dependentJars = new URL[deps.length];
            for ( int ii = 0; ii < deps.length; ii++ )
            {
                try
                {
                    dependentJars[ii] = deps[ii].toURL();
                }
                catch ( MalformedURLException e )
                {
                    log.error( "Failed to generate a URL for " + deps[ii] + 
                    ".  It will not be added to the dependencies." );
                }
            }
        }
        
        return dependentJars;
    }


    public URL[] getExtensionJars()
    {
        if ( extensionJars == null )
        {
            File[] extensions = getExtensionsDirectory().listFiles( JAR_FILTER );
            
            extensionJars = new URL[extensions.length];
            for ( int ii = 0; ii < extensions.length; ii++ )
            {
                try
                {
                    extensionJars[ii] = extensions[ii].toURL();
                }
                catch ( MalformedURLException e )
                {
                    log.error( "Failed to generate a URL for " + extensions[ii] + 
                        ".  It will not be added to the extensions." );
                }
            }
        }
        
        return extensionJars;
    }


    public URL[] getAllJars()
    {
        if ( allJars == null )
        {
            int dependentLength = getDependentJars().length;
            int extensionLength = getExtensionJars().length;
            allJars = new URL[ dependentLength + extensionLength ];
            
            for ( int ii = 0; ii < allJars.length; ii++ )
            {
                if ( ii < dependentLength )
                {
                    allJars[ii] = dependentJars[ii];
                }
                else
                {
                    allJars[ii] = extensionJars[ii - dependentLength];
                }
            }
        }
        
        return allJars;
    }
}
