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

package org.apache.directory.server.core.partition.ldif;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.util.List;

import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.messages.AddResponse;
import org.apache.directory.shared.ldap.client.api.messages.SearchResponse;
import org.apache.directory.shared.ldap.client.api.messages.SearchResultEntry;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple utility to dump the DIT data into LDIF files on FS.
 * 
 * The pattern followed is as follows on FS
 * Each entry will have a directory with its DN as name and an LDIF file with the same DN'.ldif'
 * as its name.
 * 
 * NOTE: this file depends on ldap-client-api module
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DitToLdifWriter
{

    private LdapConnection connection;

    private LdifReader ldifParser = new LdifReader();
    
    private FileFilter dirFilter = new FileFilter()
    {
        public boolean accept( File dir )
        {
            return dir.isDirectory();
        }
    };
    
    private static Logger LOG = LoggerFactory.getLogger( DitToLdifWriter.class );    
    
    public DitToLdifWriter()
    {
        connection = new LdapConnection( "localhost", 10389 );
        try
        {
            connection.bind( "uid=admin,ou=system", "secret" );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Failed to connect to the server", e );
        }
    }

    
    public void dumpDitToFS( String dn, File baseDir ) throws Exception
    {
        Cursor<SearchResponse> cursor = connection.search( dn, "(objectclass=*)", SearchScope.SUBTREE, "*", "+" );
        
        StringBuilder filePath = new StringBuilder();

        while( cursor.next() )
        {
            SearchResponse searchRes = cursor.get();
            if( searchRes instanceof SearchResultEntry )
            {
                SearchResultEntry searchResultEntry = ( SearchResultEntry ) searchRes;
                Entry entry = searchResultEntry.getEntry();
         
                LdapDN entryDn = entry.getDn();
                int size = entryDn.size();
                
                filePath.append( baseDir.getAbsolutePath() ).append( File.separator );
                for( int i =0; i< size; i++ )
                {
                    filePath.append( entryDn.getRdn( i ).getUpName().toLowerCase() ).append( File.separator );
                }
                
                File dir = new File( filePath.toString() );
                dir.mkdirs();
                filePath.setLength( 0 );
                String ldif = LdifUtils.convertEntryToLdif( entry );
                
                File ldifFile = new File( dir, entryDn.getRdn().getUpName().toLowerCase() + ".ldif" );
                FileWriter fw = new FileWriter( ldifFile );
                fw.write( ldif );
                fw.close();
            }
        }
        
        cursor.close();
    }

    
    public void loadConfig( File baseDir, String partitionSuffix ) throws Exception
    {
        File dir = new File( baseDir, partitionSuffix );
        
        if( ! dir.exists() )
        {
            throw new Exception( "The specified configuration dir" + partitionSuffix + " doesn't exist under " + baseDir.getAbsolutePath() );
        }
        
        loadEntry( dir );
    }

    
    private void loadEntry( File entryDir ) throws Exception
    {
        LOG.warn( "processing dir {}", entryDir.getName() );
        File ldifFile = new File( entryDir, entryDir.getName() + ".ldif" );
        
        if( ldifFile.exists() )
        {
            LOG.warn( "ldif file {} exists", ldifFile.getName() );
            List<LdifEntry> entries = ldifParser.parseLdifFile( ldifFile.getAbsolutePath() );
            if( entries != null && !entries.isEmpty() )
            {
                LOG.warn( "adding entry {}", entries.get( 0 ) );
                // this ldif will have only one entry
                AddResponse resp = connection.add( entries.get( 0 ).getEntry() );
                LOG.warn( "{}", resp.getLdapResult().getResultCode() );
            }
        }
        else
        {
            LOG.warn( "ldif file doesn't exist {}", ldifFile.getAbsolutePath() );
        }
        
        File[] dirs = entryDir.listFiles( dirFilter );
        if( dirs != null )
        {
            for( File f : dirs )
            {
                loadEntry( f );
            }
        }
    }

    
    public void close() throws Exception
    {
        connection.close();
    }
    
    public static void main( String[] args ) throws Exception
    {
        DitToLdifWriter configWriter = new DitToLdifWriter();
        String partitionSuffix = "ou=config";
        File baseDir = new File( "/tmp" );
        configWriter.dumpDitToFS( partitionSuffix, baseDir );
//        configWriter.loadConfig( baseDir, partitionSuffix );
        configWriter.close();

        /*
        LdifReader reader = new LdifReader( new File( "src/main/resources/ads-2.ldif" ) );
        Iterator<LdifEntry> itr = reader.iterator();
        while( itr.hasNext() )
        {
            try
            {
                LdifEntry entry = itr.next();
                configWriter.connection.add( entry.getEntry() );
            }
            catch( Exception e )
            {
                System.exit( 1 );
            }
        }
        */
    }
}
