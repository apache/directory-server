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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.Expand;
import org.codehaus.plexus.util.FileUtils;


/**
 * A Mojo command pattern interface.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class MojoCommand
{
    protected final Map<String, Artifact> dependencyMap;
    protected final Log log;
    protected final ServiceInstallersMojo mymojo;
    
    public abstract void execute() throws MojoExecutionException, MojoFailureException;
    public abstract Properties getFilterProperties();
    

    public MojoCommand( ServiceInstallersMojo mymojo )
    {
        this.mymojo = mymojo;
        this.log = mymojo.getLog();
        this.dependencyMap = new HashMap<String, Artifact>();
        
        for ( Iterator ii = mymojo.getProject().getDependencyArtifacts().iterator(); ii.hasNext(); /* */ )
        {
            Artifact artifact = ( Artifact ) ii.next();
            dependencyMap.put( artifact.getGroupId() + ":" + artifact.getArtifactId(), artifact );
        }
    }
    
    
    public void reportProcessing( PackagedFile packagedFile )
    {
        if ( ! log.isInfoEnabled() )
        {
            return;
        }
        
        log.info( "\t\tProcessing packagedFile with source " + packagedFile.getSource() + " for destination " 
            + packagedFile.getDestinationPath() ); 
    }
    
    
    public void processPackagedFiles( Target target, PackagedFile[] packagedFiles )
    {
        if ( packagedFiles == null )
        {
            return;
        }
        
        if ( log.isInfoEnabled() )
        {
            log.info( "\tProcessing " + packagedFiles.length + " packagedFiles: " );
        }
        
        for ( int ii = 0; ii < packagedFiles.length; ii++ )
        {
            File source = null;
            reportProcessing( packagedFiles[ii ] );

            try
            {
                if ( packagedFiles[ii].isDependency() )
                {
                    Artifact artifact = dependencyMap.get( packagedFiles[ii].getSource() );
                    
                    if ( artifact == null )
                    {
                        throw new MojoFailureException( "The packaged file setup as a dependency on artifact "
                            + packagedFiles[ii].getSource() + " has not been found in the project.  " +
                                    "Check your <dependencies> in the project pom." );
                    }
                    source = artifact.getFile().getAbsoluteFile();
                }
                else
                {
                    source = new File( packagedFiles[ii].getSource() );
                }
                
                if ( ! source.isAbsolute() )
                {
                    File sourceDirectoryRelative = new File( mymojo.getSourceDirectory(), packagedFiles[ii].getSource() );
                    File baseRelative = new File( mymojo.getProject().getBasedir(), packagedFiles[ii].getSource() );
                    if ( sourceDirectoryRelative.exists() )
                    {
                        source = sourceDirectoryRelative;
                    }
                    else if ( baseRelative.exists() )
                    {
                        source = baseRelative;
                    }
                    else if ( ! source.exists() )
                    {
                        throw new MojoFailureException( "Failed to copy packagedFile. Cannot locate source: " + source );
                    }
                    
                    source = source.getAbsoluteFile();
                }
                
                if ( packagedFiles[ii].isExpandable() )
                {
                    File dest = new File( target.getLayout().getBaseDirectory(), packagedFiles[ii].getDestinationPath() );
                    if ( ! dest.exists() )
                    {
                        dest.mkdirs();
                    }
                    
                    String fileExtension = source.getName().substring( source.getName().lastIndexOf( '.' ) );
                    if ( fileExtension.equalsIgnoreCase( ".jar" ) || fileExtension.equalsIgnoreCase( ".zip" ) 
                        || fileExtension.equalsIgnoreCase( ".war" ) || fileExtension.equalsIgnoreCase( ".sar" ) )
                    {
                        log.info( "\t\t\t ... expanding " + source  + "\n\t\t\t => to " + dest );
                        Expand expand = new Expand();
                        expand.setSrc( source );
                        expand.setOverwrite( true );
                        expand.setDest( dest );
                        try
                        {
                            expand.execute();
                            continue;
                        }
                        catch ( Exception e )
                        {
                            throw new MojoFailureException( "Failed to expaned packagedFile " + source + ": " + e.getMessage() );
                        }
                    }
                    
                    throw new MojoFailureException( "Failed to expand packagedFile: " + source 
                        + ". It does not have a jar, war or zip extension" );
                }
                
                File dest = new File( target.getLayout().getBaseDirectory(), packagedFiles[ii].getDestinationPath() );
                
                if ( packagedFiles[ii].isDirectory() )
                {
                    try
                    {
                        FileUtils.copyDirectoryStructure( source, dest );
                    }
                    catch ( IOException e )
                    {
                        throw new MojoFailureException( "Failed to copy packagedFile [directory=true] from source " 
                            + source + " to destination " + dest );
                    }
                    continue;
                }
                else if ( packagedFiles[ii].isFiltered() )
                {
                    try
                    {
                        MojoHelperUtils.copyAsciiFile( mymojo, getFilterProperties(), source, dest, true );
                    }
                    catch ( IOException e )
                    {
                        throw new MojoFailureException( "Failed to copy packagedFile from source " + source +
                            " to destination " + dest );
                    }
                    continue;
                }
                
                try
                {
                    FileUtils.copyFile( source, dest );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy packagedFile from source " + source +
                        " to destination " + dest );
                }
            }
            catch ( Exception e ) 
            {
                log.error( "Failed while processing " + source, e );
            }
        }
    }
}
