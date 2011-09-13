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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.directory.server.InstallationLayout;
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
public class MojoHelperUtils
{
    public static void copyBinaryFile( InputStream from, File to ) throws IOException
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


    public static void copyAsciiFile( GenerateMojo mymojo, Properties filterProperties, InputStream from,
        File to, boolean filtering ) throws IOException
    {
        // buffer so it isn't reading a byte at a time!
        Reader fileReader = null;
        Writer fileWriter = null;
        try
        {
            fileReader = new BufferedReader( new InputStreamReader( from ) );
            fileWriter = new OutputStreamWriter( new FileOutputStream( to ) );

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
            IOUtil.copy( reader, fileWriter );
        }
        finally
        {
            IOUtil.close( fileReader );
            IOUtil.close( fileWriter );
        }
    }


    public static void copyAsciiFile( GenerateMojo mymojo, Properties filterProperties, File from, File to,
        boolean filtering ) throws IOException
    {
        copyAsciiFile( mymojo, filterProperties, new FileInputStream( from ), to, filtering );
    }


    public static void copyDependencies( GenerateMojo mymojo, InstallationLayout layout )
        throws MojoFailureException
    {
        copyDependencies( mymojo, layout, true );
    }


    public static void copyDependencies( GenerateMojo mymojo, InstallationLayout layout,
        boolean includeWrapperDependencies )
        throws MojoFailureException
    {
        // Creating the excludes set
        Set<String> excludes = new HashSet<String>();
        if ( mymojo.getExcludes() != null )
        {
            excludes.addAll( mymojo.getExcludes() );
        }

        // Adding the wrapper dependencies to the excludes set
        if ( !includeWrapperDependencies )
        {
            excludes.add( "org.apache.directory.server:apacheds-wrapper" );
            excludes.add( "tanukisoft:wrapper" );
        }

        // Filtering and copying dependencies
        Iterator<?> artifacts = mymojo.getProject().getRuntimeArtifacts().iterator();
        while ( artifacts.hasNext() )
        {
            Artifact artifact = ( Artifact ) artifacts.next();
            String key = artifact.getGroupId() + ":" + artifact.getArtifactId();

            if ( !excludes.contains( key ) )
            {
                try
                {
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
            StringBuffer cmdString = new StringBuffer( " " );
            for ( int ii = 0; ii < cmd.length; ii++ )
            {
                cmdString.append( cmd[ii] ).append( " " );
            }

            String[] temp = new String[2];
            temp[0] = "sudo";
            temp[1] = cmdString.toString();
            cmd = temp;
        }

        StringBuffer cmdString = new StringBuffer( " " );
        for ( int ii = 0; ii < cmd.length; ii++ )
        {
            cmdString.append( cmd[ii] ).append( " " );
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

            dest.mkdirs();

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
