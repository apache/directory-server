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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An authorization data container.
 * 
 * The ASN.1 grammar is :
 * 
 * -- NOTE: AuthorizationData is always used as an OPTIONAL field and
 * -- should not be empty.
 * AuthorizationData       ::= SEQUENCE OF AuthorizationDataEntry
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AuthorizationData extends AbstractAsn1Object implements Encodable
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( AuthorizationData.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The list of AuthrorizationData elements */
    private List<AuthorizationDataEntry> entries = new ArrayList<AuthorizationDataEntry>();

    // Storage for computed lengths
    private transient int authorizationDataLength;


    /**
     * Creates a new instance of AuthorizationData.
     */
    public AuthorizationData()
    {
        // used by ASN.1 decoder
    }


    /**
     * Adds all {@link AuthorizationData} entries to this {@link AuthorizationData}.
     *
     * @param data
     */
    public void add( AuthorizationData data )
    {
        entries.addAll( data.entries );
    }


    /**
     * Adds an {@link AuthorizationDataEntry} to this {@link AuthorizationData}.
     *
     * @param entry
     */
    public void add( AuthorizationDataEntry entry )
    {
        entries.add( entry );
    }


    /**
     * @return The AuthorizationdataEntry list
     */
    public List<AuthorizationDataEntry> getEntries()
    {
        return entries;
    }


    /**
     * Returns an {@link Iterator} over the entries in this {@link AuthorizationData}.
     *
     * @return An {@link Iterator} over the entries in this {@link AuthorizationData}.
     */
    public Iterator iterator()
    {
        return entries.iterator();
    }


    /**
     * Compute the AuthorizationData length
     * 
     * AuthorizationData :
     * 
     * 0x30 L1 AuthorizationData
     *  |
     *  +--> 0x30 L2 AuthorizationDataEntry
     *  |
     *  +--> 0x30 L2 AuthorizationDataEntry
     *  |
     *  ...
     *  |
     *  +--> 0x30 L2 AuthorizationDataEntry
     */
    public int computeLength()
    {
        if ( ( entries == null ) || ( entries.size() == 0 ) )
        {
            authorizationDataLength = 1;

            return authorizationDataLength + 1;
        }
        else
        {
            authorizationDataLength = 0;

            for ( AuthorizationDataEntry entry : entries )
            {
                authorizationDataLength += entry.computeLength();
            }

            return 1 + TLV.getNbBytes( authorizationDataLength ) + authorizationDataLength;
        }
    }


    /**
     * Encode the AuthorizationData message to a PDU. 
     * 
     * AuthorizationData :
     * 
     * 0x30 LL
     *   0x30 LL AuthorizationDataEntry 
     *   0x30 LL AuthorizationDataEntry
     *   ... 
     *   0x30 LL AuthorizationDataEntry 
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The AuthorizationData SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( authorizationDataLength ) );

            // Each entry, if any
            if ( ( entries != null ) && ( entries.size() != 0 ) )
            {
                for ( AuthorizationDataEntry entry : entries )
                {
                    entry.encode( buffer );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error(
                "Cannot encode the AuthorizationData object, the PDU size is {} when only {} bytes has been allocated",
                1 + TLV.getNbBytes( authorizationDataLength ) + authorizationDataLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "AuthorizationData encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "AuthorizationData initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        if ( ( entries == null ) || ( entries.size() == 0 ) )
        {
            sb.append( tabs ).append( "AuthorizationData : {}\n" );
        }
        else
        {
            sb.append( tabs ).append( "AuthorizationData : {\n" );
            boolean isFirst = true;

            for ( AuthorizationDataEntry entry : entries )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( '\n' );
                }

                sb.append( entry.toString( tabs + "    " ) ).append( '\n' );
            }

            sb.append( tabs + "}" );
        }

        return sb.toString();
    }
}
