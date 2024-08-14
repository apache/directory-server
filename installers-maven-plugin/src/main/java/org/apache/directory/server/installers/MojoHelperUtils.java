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
package org.apache.directory.server.installers;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.directory.server.InstallationLayout;
import org.apache.directory.server.i18n.I18n;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.taskdefs.Execute;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.InterpolationFilterReader;


/**
 * Some helper/utility methods for this plugin.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class MojoHelperUtils
{

    private MojoHelperUtils()
    {
    }


    public static void copyBinaryFile( GenerateMojo mojo, String fileName, InputStream from, File to )
        throws IOException
    {
        mojo.getLog().info( "Copying " + fileName + " to " + to );

        try ( OutputStream out = Files.newOutputStream( to.toPath() ) )
        {
            IOUtil.copy( from, out );
        }
        finally
        {
            IOUtil.close( from );
        }
    }


    public static void copyAsciiFile( GenerateMojo mymojo, Properties filterProperties, String fileName,
        InputStream from, File to, boolean filtering ) throws IOException
    {
        // buffer so it isn't reading a byte at a time!
        try ( Reader fileReader = new BufferedReader( new InputStreamReader( from, StandardCharsets.UTF_8 ) );
            Writer writer = Files.newBufferedWriter( to.toPath(), StandardCharsets.UTF_8 ) )
        {
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
                reader = new InterpolationFilterReader( reader, new ReflectionProperties( mymojo.getProject(),
                    isPropertiesFile ), "${", "}" );
            }
            else
            {
                reader = fileReader;
            }
            IOUtil.copy( reader, writer );
        }
    }


    public static void copyAsciiFile( GenerateMojo mymojo, Properties filterProperties, File from, File to,
        boolean filtering ) throws IOException
    {
        InputStream input = Files.newInputStream( from.toPath() );
        copyAsciiFile( mymojo, filterProperties, from.getAbsolutePath(), input, to, filtering );
    }


    public static void copyDependencies( GenerateMojo myMojo, InstallationLayout layout,
        boolean includeWrapperDependencies )
        throws MojoFailureException
    {
        // Creating the excludes set
        Set<String> includes = new HashSet<>();

        // Always add the apacheds-service.jar
        includes.add( "org.apache.directory.server:apacheds-service" );

        // Adding the wrapper dependencies to the excludes set
        if ( includeWrapperDependencies )
        {
            includes.add( "org.apache.directory.server:apacheds-wrapper" );
            includes.add( "tanukisoft:wrapper" );
        }

        // Filtering and copying dependencies
        Set<Artifact> artifacts = myMojo.getProject().getArtifacts();

        for ( Artifact artifact : artifacts )
        {
            String key = artifact.getGroupId() + ":" + artifact.getArtifactId();

            if ( includes.contains( key ) )
            {
                try
                {
                    myMojo.getLog().info( "Copying " + artifact.getFile() + " to " + layout.getLibDirectory() );
                    FileUtils.copyFileToDirectory( artifact.getFile(), layout.getLibDirectory() );
                }
                catch ( IOException e )
                {
                    throw new MojoFailureException( "Failed to copy dependency artifact " + artifact
                        + " into position " + layout.getLibDirectory() );
                }
            }
        }
    }


    public static void exec( String[] cmd, File workDir, boolean doSudo ) throws MojoFailureException
    {
        Execute task = new Execute();
        task.setCommandline( cmd );
        task.setWorkingDirectory( workDir );

        if ( doSudo )
        {
            StringBuilder cmdString = new StringBuilder( " " );
            
            for ( String command : cmd )
            {
                cmdString.append( command ).append( " " );
            }

            String[] temp = new String[2];
            temp[0] = "sudo";
            temp[1] = cmdString.toString();
            cmd = temp;
        }

        StringBuilder cmdString = new StringBuilder( " " );
        
        for ( String command : cmd )
        {
            cmdString.append( command ).append( " " );
        }

        try
        {
            task.execute();
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed while trying to execute '" + cmdString + "': " + e.getMessage() );
        }

        if ( task.getExitValue() != 0 )
        {
            throw new MojoFailureException( "Execution of '" + cmdString + "' resulted in a non-zero exit value: "
                + task.getExitValue() );
        }
    }


    /**
     * Recursively copy files from the given source to the given destination.
     *
     * @param src
     *      the source
     * @param dest
     *      the destination
     * @throws IOException
     *      If an error occurs when copying a file
     */
    public static void copyFiles( File src, File dest ) throws IOException
    {
        if ( src.isDirectory() )
        {
            File[] files = src.listFiles();

            if ( !dest.mkdirs() )
            {
                throw new IOException( I18n.err( I18n.ERR_11000_COULD_NOT_CREATE_DIRECTORY, dest ) );
            }

            for ( File file : files )
            {
                copyFiles( file, new File( dest, file.getName() ) );
            }
        }
        else
        {
            FileUtils.copyFile( src, dest );
        }
    }
}
