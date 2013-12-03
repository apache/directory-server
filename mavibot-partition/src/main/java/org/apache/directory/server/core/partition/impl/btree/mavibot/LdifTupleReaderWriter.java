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

import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.mavibot.btree.Tuple;
import org.apache.directory.mavibot.btree.util.TupleReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO LdifTupleReaderWriter.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifTupleReaderWriter implements TupleReaderWriter<Dn, String>
{

    private LdifReader reader = null;

    private static final Logger LOG = LoggerFactory.getLogger( LdifTupleReaderWriter.class );


    @Override
    public Tuple<Dn, String> readSortedTuple( DataInputStream in )
    {
        try
        {
            if( in.available() > 0 )
            {
                Tuple<Dn, String> t = new Tuple<Dn, String>();
                t.setKey( new Dn( in.readUTF() ) );
                t.setValue( in.readUTF() );
                
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
    public Tuple<Dn, String> readUnsortedTuple( DataInputStream in )
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

        Tuple<Dn, String> t = null;

        if ( reader.hasNext() )
        {
            LdifEntry ldifEntry = reader.next();

            if ( ldifEntry == null )
            {
                throw new IllegalStateException(
                    "Received null entry while parsing, check the LDIF file for possible incorrect/corrupted entries" );
            }

            t = new Tuple<Dn, String>();

            t.setKey( ldifEntry.getDn() );
            t.setValue( ldifEntry.getOffset() + ":" + ldifEntry.getLengthBeforeParsing() );
        }

        return t;
    }


    @Override
    public void storeSortedTuple( Tuple<Dn, String> t, DataOutputStream out ) throws IOException
    {
        out.writeUTF( t.getKey().getName() );
        out.writeUTF( t.getValue() );
    }

}
