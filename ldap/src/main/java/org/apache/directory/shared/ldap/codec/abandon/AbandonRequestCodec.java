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
package org.apache.directory.shared.ldap.codec.abandon;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A AbandonRequest Message. 
 * 
 * Its syntax is : 
 * AbandonRequest ::= [APPLICATION 16] MessageID 
 * 
 * MessageID ::= INTEGER (0 .. maxInt) 
 * 
 * maxInt INTEGER ::= 2147483647 -- (2^^31 - 1) --
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class AbandonRequestCodec extends LdapMessageCodec
{
    /** The logger */
    private static Logger log = LoggerFactory.getLogger( AbandonRequestCodec.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The abandoned message ID */
    private int abandonedMessageId;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new AbandonRequest object.
     */
    public AbandonRequestCodec()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the abandoned message ID
     * 
     * @return Returns the abandoned MessageId.
     */
    public int getAbandonedMessageId()
    {
        return abandonedMessageId;
    }


    /**
     * Get the message type
     * 
     * @return Returns the type.
     */
    public int getMessageType()
    {
        return LdapConstants.ABANDON_REQUEST;
    }


    /**
     * Set the abandoned message ID
     * 
     * @param abandonedMessageId The abandoned messageID to set.
     */
    public void setAbandonedMessageId( int abandonedMessageId )
    {
        this.abandonedMessageId = abandonedMessageId;
    }


    /**
     * Compute the AbandonRequest length 
     * 
     * AbandonRequest : 
     * 0x50 0x0(1..4) abandoned MessageId 
     * 
     * Length(AbandonRequest) = Length(0x50) + 1 + Length(abandoned MessageId)
     */
    public int computeLength()
    {
        int length = 1 + 1 + Value.getNbBytes( abandonedMessageId );

        if ( IS_DEBUG )
        {
            log.debug( "Message length : {}", Integer.valueOf( length ) );
        }

        return length;
    }


    /**
     * Encode the AbandonRequest message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            log.error( "Cannot put a PDU in a null buffer !" );
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The tag
            buffer.put( LdapConstants.ABANDON_REQUEST_TAG );

            // The length. It has to be evaluated depending on
            // the abandoned messageId value.
            buffer.put( ( byte ) Value.getNbBytes( abandonedMessageId ) );

            // The abandoned messageId
            buffer.put( Value.getBytes( abandonedMessageId ) );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "The PDU buffer size is too small !" );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return buffer;
    }


    /**
     * Return a String representing an AbandonRequest
     * 
     * @return A String representing the AbandonRequest
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Abandon Request :\n" );
        sb.append( "        Message Id : " ).append( abandonedMessageId ).append( '\n' );

        return sb.toString();
    }
}
