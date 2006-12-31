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
package org.apache.directory.server.schema.bootstrap.partition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Extracts dbfiles for the schema partition onto a destination directory.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaPartitionExtractor
{
    private DbFileListing listing;
    private File outputDirectory; 
    
    
    public SchemaPartitionExtractor( File outputDirectory ) throws IOException
    {
        this.outputDirectory = outputDirectory;
        this.listing = new DbFileListing();
    }
    
    
    public void extract() throws IOException
    {
        if ( ! outputDirectory.exists() )
        {
            outputDirectory.mkdirs();
        }
        
        File schemaDirectory = new File( outputDirectory, "schema" );
        if ( ! schemaDirectory.exists() )
        {
            schemaDirectory.mkdirs();
        }

        Iterator<String> ii = listing.iterator();
        while ( ii.hasNext() )
        {
            extract( ii.next() );
        }
    }
    
    
    public DbFileListing getDbFileListing()
    {
        return listing;
    }
    
    
    private void extract( String resource ) throws IOException
    {
        byte[] buf = new byte[512];
        InputStream in = getClass().getResourceAsStream( resource );
        FileOutputStream out = null;

        try
        {
            out = new FileOutputStream( new File( outputDirectory, resource ) );
            while( in.available() > 0 )
            {
                int readCount = in.read( buf );
                out.write( buf, 0, readCount );
            }
            out.flush();
        }
        finally
        {
            if ( out != null )
            {
                out.close();
            }
            
            if ( in != null )
            {
                in.close();
            }
        }
    }
}
