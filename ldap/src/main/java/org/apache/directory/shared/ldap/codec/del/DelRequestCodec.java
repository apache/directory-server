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
package org.apache.directory.shared.ldap.codec.del;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A DelRequest Message. 
 * 
 * Its syntax is : 
 * 
 * DelRequest ::= [APPLICATION 10] LDAPDN
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class DelRequestCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The entry to be deleted */
    private LdapDN entry;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new DelRequest object.
     */
    public DelRequestCodec()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the message type
     * 
     * @return Returns the type.
     */
    public int getMessageType()
    {
        return LdapConstants.DEL_REQUEST;
    }


    /**
     * Get the entry to be deleted
     * 
     * @return Returns the entry.
     */
    public LdapDN getEntry()
    {
        return entry;
    }


    /**
     * Set the entry to be deleted
     * 
     * @param entry The entry to set.
     */
    public void setEntry( LdapDN entry )
    {
        this.entry = entry;
    }


    /**
     * Compute the DelRequest length 
     * 
     * DelRequest : 
     * 0x4A L1 entry 
     * 
     * L1 = Length(entry) 
     * Length(DelRequest) = Length(0x4A) + Length(L1) + L1
     */
    public int computeLength()
    {
        // The entry
        return 1 + TLV.getNbBytes( LdapDN.getNbBytes( entry ) ) + LdapDN.getNbBytes( entry );
    }


    /**
     * Encode the DelRequest message to a PDU. 
     * 
     * DelRequest : 
     * 0x4A LL entry
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The DelRequest Tag
            buffer.put( LdapConstants.DEL_REQUEST_TAG );

            // The entry
            buffer.put( TLV.getBytes( LdapDN.getNbBytes( entry ) ) );
            buffer.put( LdapDN.getBytes( entry ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return buffer;
    }


    /**
     * Return a String representing a DelRequest
     * 
     * @return A DelRequest String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Del request\n" );
        sb.append( "        Entry : '" ).append( entry ).append( "'\n" );

        return sb.toString();
    }
}
