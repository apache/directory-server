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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.mavibot.btree.Tuple;
import org.apache.directory.mavibot.btree.util.TupleReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO LdifTupleReaderWriter.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifTupleReaderWriter<E> implements TupleReaderWriter<Dn, E>
{

    private LdifReader reader = null;

    private static final Logger LOG = LoggerFactory.getLogger( LdifTupleReaderWriter.class );

    private String ldifFile;
    
    private RandomAccessFile raf;
    
    private SchemaManager schemaManager;
    
    public LdifTupleReaderWriter( String ldifFile, SchemaManager schemaManager )
    {
        this.ldifFile = ldifFile;
        this.schemaManager = schemaManager;
        
        try
        {
            raf = new RandomAccessFile( ldifFile, "r" );
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
    }
    

    @Override
    public Tuple<Dn, E> readSortedTuple( DataInputStream in )
    {
        try
        {
            if( in.available() > 0 )
            {
                Tuple<Dn, E> t = new Tuple<Dn, E>();
                t.setKey( new Dn( in.readUTF() ) );
                
                String[] tokens = in.readUTF().split( ":" );
                
                long offset = Long.valueOf( tokens[0] );
                
                int length = Integer.valueOf( tokens[1] );
                
                raf.seek( offset );
                
                byte[] data = new byte[length];
                
                raf.read( data, 0, length );
                
                LdifReader reader = new LdifReader();
                
                LdifEntry ldifEntry = reader.parseLdif( new String( data ) ).get( 0 );
                Entry entry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );

                t.setValue( ( E ) entry );
                
                return t;
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to read a sorted tuple", e );
        }
        
        return null;
    }


    @Override
    public Tuple<Dn, E> readUnsortedTuple( DataInputStream in )
    {
        try
        {
            if ( reader == null )
            {
                reader = new LdifReader( in );
            }
        }
        catch ( Exception e )
        {
            String msg = "Failed to open the LDIF input stream";
            LOG.error( msg, e );

            throw new RuntimeException( msg, e );
        }

        Tuple<Dn, E> t = null;

        if ( reader.hasNext() )
        {
            LdifEntry ldifEntry = reader.next();

            if ( ldifEntry == null )
            {
                throw new IllegalStateException(
                    "Received null entry while parsing, check the LDIF file for possible incorrect/corrupted entries" );
            }

            t = new Tuple<Dn, E>();

            t.setKey( ldifEntry.getDn() );
            t.setValue( ( E ) ( ldifEntry.getOffset() + ":" + ldifEntry.getLengthBeforeParsing() ) );
        }

        return t;
    }


    @Override
    public void storeSortedTuple( Tuple<Dn, E> t, DataOutputStream out ) throws IOException
    {
        out.writeUTF( t.getKey().getName() );
        out.writeUTF( ( String ) t.getValue() );
    }

}
