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
package org.apache.directory.server.core.api.changelog;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.LdapPrincipalSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A helper class which serialize and deserialize a ChangeLogEvent.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class ChangeLogEventSerializer
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( ChangeLogEventSerializer.class );


    /**
     * Private constructor.
     */
    private ChangeLogEventSerializer()
    {
    }


    /**
     * Serializes a ChangeLogEvent instance.
     * 
     * @param event The ChangeLogEvent instance to serialize
     * @param out The stream into which we will write the serialized instance
     * @throws IOException If the stream can't be written
     */
    public static void serialize( ChangeLogEvent event, ObjectOutput out ) throws IOException
    {
        // The date the change has been created, "yyyyMMddHHmmss'Z'" 
        out.writeUTF( event.getZuluTime() );

        // The committer's Principal
        LdapPrincipalSerializer.serialize( event.getCommitterPrincipal(), out );

        // The revision
        out.writeLong( event.getRevision() );

        // The forward LDIF
        event.getForwardLdif().writeExternal( out );

        // The reverse LDIFs number
        int nbReverses = event.getReverseLdifs().size();
        out.writeInt( nbReverses );

        for ( LdifEntry reverseLdif : event.getReverseLdifs() )
        {
            reverseLdif.writeExternal( out );
        }

        out.flush();
    }


    /**
     * Deserializes a ChangeLogEvent instance.
     * 
     * @param schemaManager The SchemaManager (can be null)
     * @param in The input stream from which the ChengaLogEvent is read
     * @return a deserialized ChangeLogEvent
     * @throws IOException If we had an issue processing the stream
     * @throws LdapInvalidDnException If the deserialization failed
     */
    public static ChangeLogEvent deserialize( SchemaManager schemaManager, ObjectInput in )
        throws IOException, LdapInvalidDnException
    {
        // The date the change has been created, "yyyyMMddHHmmss'Z'" 
        String zuluTime = in.readUTF();

        // The committer's Principal
        LdapPrincipal committerPrincipal = LdapPrincipalSerializer.deserialize( schemaManager, in );

        // The revision
        long revision = in.readLong();

        // The forward LDIF
        LdifEntry forwardEntry = new LdifEntry();

        try
        {
            forwardEntry.readExternal( in );
        }
        catch ( ClassNotFoundException cnfe )
        {
            IOException ioe = new IOException( cnfe.getMessage() );
            ioe.initCause( cnfe );
            throw ioe;
        }

        // The reverse LDIFs number
        int nbReverses = in.readInt();

        List<LdifEntry> reverses = new ArrayList<LdifEntry>( nbReverses );

        for ( int i = 0; i < nbReverses; i++ )
        {
            LdifEntry reverseEntry = new LdifEntry();

            try
            {
                reverseEntry.readExternal( in );
            }
            catch ( ClassNotFoundException cnfe )
            {
                IOException ioe = new IOException( cnfe.getMessage() );
                ioe.initCause( cnfe );
                throw ioe;
            }

            reverses.add( reverseEntry );
        }

        ChangeLogEvent changeLogEvent = new ChangeLogEvent( revision, zuluTime, committerPrincipal, forwardEntry,
            reverses );

        return changeLogEvent;
    }
}
