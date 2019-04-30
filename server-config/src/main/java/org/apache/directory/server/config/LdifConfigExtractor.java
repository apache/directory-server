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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Pattern;

import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.ResourceMap;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class to copy the default config to the work directory of a DirectoryService instance.
 * 
 * NOTE: much of this class code is duplicated from DefaultSchemaLdifExtractor class
 *       We should create a AbstractLdifExtractor class and move the reusable code there
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class LdifConfigExtractor
{

    public static final String LDIF_CONFIG_FILE = "config.ldif";

    private static final String CONFIG_SUBDIR = "config";

    private static final Logger LOG = LoggerFactory.getLogger( LdifConfigExtractor.class );

    // java.util.regex.Pattern is immutable so only one instance is needed for all uses.
    private static final Pattern EXTRACT_PATTERN = Pattern.compile( ".*config"
        + "[/\\Q\\\\E]" + "ou=config.*\\.ldif" );


    private LdifConfigExtractor()
    {
    }


    /**
     * Extracts the LDIF files from a Jar file or copies exploded LDIF resources.
     *
     * @param outputDirectory The directory where to extract the configuration
     * @param overwrite over write extracted structure if true, false otherwise
     * @throws IOException if schema already extracted and on IO errors
     */
    public static void extract( File outputDirectory, boolean overwrite ) throws IOException
    {
        if ( !outputDirectory.exists() )
        {
            LOG.debug( "creating non existing output directory {}", outputDirectory.getAbsolutePath() );
            if ( !outputDirectory.mkdir() )
            {
                throw new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, outputDirectory ) );
            }
        }

        File configDirectory = new File( outputDirectory, CONFIG_SUBDIR );

        if ( !configDirectory.exists() )
        {
            LOG.debug( "creating non existing config directory {}", configDirectory.getAbsolutePath() );
            if ( !configDirectory.mkdir() )
            {
                throw new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, configDirectory ) );
            }
        }
        else if ( !overwrite )
        {
            throw new IOException( I18n.err( I18n.ERR_508, configDirectory.getAbsolutePath() ) );
        }

        LOG.debug( "extracting the configuration to the directory at {}", configDirectory.getAbsolutePath() );

        Map<String, Boolean> list = ResourceMap.getResources( EXTRACT_PATTERN );

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

        if ( !destination.getParentFile().exists() && !destination.getParentFile().mkdirs() )
        {
            throw new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, destination.getParentFile() ) );
        }

        if ( !source.getParentFile().exists() )
        {
            throw new FileNotFoundException( I18n.err( I18n.ERR_509, source.getAbsolutePath() ) );
        }

        try ( FileWriter out = new FileWriter( destination ) )
        {
            try ( BufferedReader in = new BufferedReader( new FileReader( source ) ) )
            {
                String line;
                
                while ( null != ( line = in.readLine() ) )
                {
                    out.write( line + "\n" );
                }
            }
        
            out.flush();
        }
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

        try ( InputStream in = DefaultSchemaLdifExtractor.getUniqueResourceAsStream( resource,
            "LDIF file in config repository" ) ) 
        {
            File destination = new File( outputDirectory, resource );

            /*
             * Do not overwrite an LDIF file if it has already been extracted.
             */
            if ( destination.exists() )
            {
                return;
            }

            if ( !destination.getParentFile().exists() && !destination.getParentFile().mkdirs() )
            {
                throw new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY,
                    destination.getParentFile() ) );
            }

            try ( OutputStream out = Files.newOutputStream( destination.toPath() ) )
            {
                while ( in.available() > 0 )
                {
                    int readCount = in.read( buf );
                    out.write( buf, 0, readCount );
                }
                out.flush();
            }
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
        Stack<String> fileComponentStack = new Stack<>();
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

            if ( parent.equals( parent.getParentFile() ) || parent.getParentFile() == null )
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

        while ( !fileComponentStack.isEmpty() )
        {
            destinationFile = new File( destinationFile, fileComponentStack.pop() );
        }

        return destinationFile;
    }


    /**
     * extracts or overwrites the configuration LDIF file and returns the absolute path of this file
     *
     * @param configDir the directory where the config file should be extracted to
     * @param file The file containing the configuration
     * @param overwrite flag to indicate to overwrite the config file if already present in the given config directory
     * @return complete path of the config file on disk
     */
    public static String extractSingleFileConfig( File configDir, String file, boolean overwrite )
    {
        if ( file == null )
        {
            file = LDIF_CONFIG_FILE;
        }

        File configFile = new File( configDir, file );

        if ( !configDir.exists() )
        {
            LOG.debug( "creating non existing config directory {}", configDir.getAbsolutePath() );
            if ( !configDir.mkdir() )
            {
                throw new RuntimeException(
                    new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, configDir ) ) );
            }
        }
        else
        {
            if ( configFile.exists() && !overwrite )
            {
                LOG.warn( "config file already exists, returning, cause overwrite flag was set to false" );
                return configFile.getAbsolutePath();
            }
        }

        try
        {

            URL configUrl = LdifConfigExtractor.class.getClassLoader().getResource( file );

            LOG.debug( "URL of the config ldif file {}", configUrl );

            byte[] buf = new byte[1024 * 1024];

            try ( InputStream in = configUrl.openStream();
                FileWriter fw = new FileWriter( configFile ) )
            {
                while ( true )
                {
                    int read = in.read( buf );

                    if ( read <= 0 )
                    {
                        break;
                    }

                    String s = Strings.utf8ToString( buf, 0, read );
                    fw.write( s );
                }
            }

            LOG.info( "successfully extracted the config file {}", configFile.getAbsoluteFile() );

            return configFile.getAbsolutePath();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}
