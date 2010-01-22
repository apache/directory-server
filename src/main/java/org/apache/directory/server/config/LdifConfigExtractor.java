/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.config;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.ResourceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class to copy the default config to the work directory of a DirectoryService instance.
 * 
 * NOTE: much of this class code is duplicated from DefaultSchemaLdifExtractor class
 *       We should create a AbstractLdifExtractor class and move the reusable code there 
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdifConfigExtractor
{
    
    private static final String CONFIG_SUBDIR = "config";
    
    private static final Logger LOG = LoggerFactory.getLogger( LdifConfigExtractor.class );
    
    
    /**
     * Extracts the LDIF files from a Jar file or copies exploded LDIF resources.
     *
     * @param overwrite over write extracted structure if true, false otherwise
     * @throws IOException if schema already extracted and on IO errors
     */
    public static void extract( File outputDirectory, boolean overwrite ) throws IOException
    {
        if ( !outputDirectory.exists() )
        {
            LOG.debug( "creating non existing output directory {}", outputDirectory.getAbsolutePath() );
            outputDirectory.mkdir();
        }
        
        File configDirectory = new File( outputDirectory, CONFIG_SUBDIR );

        if ( !configDirectory.exists() )
        {
            LOG.debug( "creating non existing config directory {}", configDirectory.getAbsolutePath() );
            configDirectory.mkdir();
        }
        else if ( !overwrite )
        {
            throw new IOException( I18n.err( I18n.ERR_508, configDirectory.getAbsolutePath() ) );
        }

        LOG.info( "extracting the configuration to the directory at {}", configDirectory.getAbsolutePath() );

        Pattern pattern = Pattern.compile( ".*config/ou=config.*\\.ldif" );
        Map<String, Boolean> list = ResourceMap.getResources( pattern );

        System.out.println( list );
        
        for ( Entry<String, Boolean> entry : list.entrySet() )
        {
            if ( entry.getValue() )
            {
                extractFromJar( outputDirectory, entry.getKey() );
            }
            else
            {
                File resource = new File( entry.getKey() );
                copyFile( resource, getDestinationFile( outputDirectory, resource ) );
            }
        }
    }

    
    /**
     * Copies a file line by line from the source file argument to the 
     * destination file argument.
     *
     * @param source the source file to copy
     * @param destination the destination to copy the source to
     * @throws IOException if there are IO errors or the source does not exist
     */
    private static void copyFile( File source, File destination ) throws IOException
    {
        LOG.debug( "copyFile(): source = {}, destination = {}", source, destination );
        
        if ( ! destination.getParentFile().exists() )
        {
            destination.getParentFile().mkdirs();
        }
        
        if ( ! source.getParentFile().exists() )
        {
            throw new FileNotFoundException( I18n.err( I18n.ERR_509, source.getAbsolutePath() ) );
        }
        
        FileWriter out = new FileWriter( destination );
        BufferedReader in = new BufferedReader( new FileReader( source ) );
        String line;
        while ( null != ( line = in.readLine() ) )
        {
            out.write( line + "\n" ); 
        }
        
        in.close();
        out.flush();
        out.close();
    }

    /**
     * Extracts the LDIF schema resource from a Jar.
     *
     * @param resource the LDIF schema resource
     * @throws IOException if there are IO errors
     */
    private static void extractFromJar( File outputDirectory, String resource ) throws IOException
    {
        byte[] buf = new byte[512];
        InputStream in = DefaultSchemaLdifExtractor.getUniqueResourceAsStream( resource,
            "LDIF file in config repository" );

        try
        {
            File destination = new File( outputDirectory, resource );

            /*
             * Do not overwrite an LDIF file if it has already been extracted.
             */
            if ( destination.exists() )
            {
                return;
            }
            
            if ( ! destination.getParentFile().exists() )
            {
                destination.getParentFile().mkdirs();
            }
            
            FileOutputStream out = new FileOutputStream( destination );
            try
            {
                while ( in.available() > 0 )
                {
                    int readCount = in.read( buf );
                    out.write( buf, 0, readCount );
                }
                out.flush();
            } 
            finally
            {
                out.close();
            }
        }
        finally
        {
            in.close();
        }
    }

    
    /**
     * Calculates the destination file.
     *
     * @param resource the source file
     * @return the destination file's parent directory
     */
    private static File getDestinationFile( File outputDirectory, File resource )
    {
        File parent = resource.getParentFile();
        Stack<String> fileComponentStack = new Stack<String>();
        fileComponentStack.push( resource.getName() );
        
        while ( parent != null )
        {
            if ( parent.getName().equals( "config" ) )
            {
                // All LDIF files besides the config.ldif are under the 
                // config/config base path. So we need to add one more 
                // schema component to all LDIF files minus this config.ldif
                fileComponentStack.push( "config" );
                
                return assembleDestinationFile( outputDirectory, fileComponentStack );
            }

            fileComponentStack.push( parent.getName() );
            
            if ( parent.equals( parent.getParentFile() )
                    || parent.getParentFile() == null )
            {
                throw new IllegalStateException( I18n.err( I18n.ERR_510 ) );
            }
            
            parent = parent.getParentFile();
        }

        throw new IllegalStateException( I18n.err( I18n.ERR_511 ) );
    }

    /**
     * Assembles the destination file by appending file components previously
     * pushed on the fileComponentStack argument.
     *
     * @param fileComponentStack stack containing pushed file components
     * @return the assembled destination file
     */
    private static File assembleDestinationFile( File outputDirectory, Stack<String> fileComponentStack )
    {
        File destinationFile = outputDirectory.getAbsoluteFile();
        
        while ( ! fileComponentStack.isEmpty() )
        {
            destinationFile = new File( destinationFile, fileComponentStack.pop() );
        }
        
        return destinationFile;
    }

}
