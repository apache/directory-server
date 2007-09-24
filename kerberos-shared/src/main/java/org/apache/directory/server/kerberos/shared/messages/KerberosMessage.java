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
package org.apache.directory.server.kerberos.shared.messages;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A common inherited class which contains the protocol version number and the
 * message type.
 * 
 * The part of ASN.1 grammar will be something like :
 *   pvno            [T1] INTEGER (5),
 *   msg-type        [T2] INTEGER (11 -- AS -- | 13 -- TGS --),
 *   
 * where T1 and T2 can differ from one message to another. 
 *  Encoding such a message won't be done in this upper class, but in each of
 * the inherited class, for this reason
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class KerberosMessage extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KerberosMessage.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /**
     * The Kerberos protocol version number (5).
     */
    public static final int PVNO = 5;

    /** The protocol version number */
    private int pvno;
    
    /** The message type */
    private MessageType messageType;
    
    /** The starting tag for the encoding and decoding
     * It can differ from one message to another. Default to 0xA0;
     **/
    private byte startingTag = (byte)0xA0;
    
    // Storage for computed lengths
    private transient int pvnoTagLength;
    private transient int pvnoLength;
    private transient int msgTypeTagLength;
    private transient int msgTypeLength;

    /**
     * Creates a new instance of KerberosMessage.
     *
     * @param type
     */
    public KerberosMessage( MessageType type )
    {
        this( PVNO, type );
    }


    /**
     * Creates a new instance of KerberosMessage.
     *
     * @param versionNumber
     * @param type
     */
    public KerberosMessage( int versionNumber, MessageType type )
    {
        pvno = versionNumber;
        messageType = type;
    }


    /**
     * Returns the {@link MessageType}.
     *
     * @return The {@link MessageType}.
     */
    public MessageType getMessageType()
    {
        return messageType;
    }


    /**
     * Sets the {@link MessageType}.
     *
     * @param type
     */
    public void setMessageType( MessageType type )
    {
        messageType = type;
    }


    /**
     * Returns the protocol version number.
     *
     * @return The protocol version number.
     */
    public int getProtocolVersionNumber()
    {
        return pvno;
    }


    /**
     * Sets the protocol version number.
     *
     * @param versionNumber
     */
    public void setProtocolVersionNumber( int versionNumber )
    {
        pvno = versionNumber;
    }

    /**
     * Set the starting tag if different to 0xA0
     * @param startingTag The starting tag
     */
    protected void setStartingTag( byte startingTag )
    {
        this.startingTag = startingTag;
    }

    /**
     * Return the length of this encoded part.
     * 
     * Ax L1
     *   0X02 0X01 pvno (default to 5)
     * A(x+1) L2
     *   0x02 0x01 messageType (Integer)
     *   
     * The Ax and A(x+1) can be different depending on the handled messages.
     */
    public int computeLength()
    {
        // This part's length is easy to compute :
        // 1 for each tag
        //   1 for each integer value
        //   1 for each value
        pvnoLength = Value.getNbBytes( pvno );
        pvnoTagLength = 1 + TLV.getNbBytes( pvnoLength ) + pvnoLength;
        
        msgTypeLength = Value.getNbBytes( messageType.getOrdinal() );
        msgTypeTagLength = 1 + TLV.getNbBytes( msgTypeLength ) + msgTypeLength;
        
        return 
            1 + TLV.getNbBytes( pvnoTagLength ) + pvnoTagLength +
            1 + TLV.getNbBytes( msgTypeTagLength ) + msgTypeTagLength;
    }
    
    /**
     * Encode the common KerberosMessage part.
     * 
     * 0xA0 L1
     *   0X02 0X01 pvno (default to 5)
     * 0xA1 L2
     *   0x02 0x01 messageType (Integer)
     * 
     * or
     * 0xA1 L1
     *   0X02 0X01 pvno (default to 5)
     * 0xA2 L2
     *   0x02 0x01 messageType (Integer)
     * 
     * depending on the startingTag value.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Null buffer not allowed" );
        }

        try
        {
            // The pvno tag
            buffer.put( startingTag );
            buffer.put( TLV.getBytes( pvnoTagLength ) );
            Value.encode( buffer, pvno );

            // The Ticket SEQUENCE Tag
            buffer.put( (byte)( startingTag + 1 ) );
            buffer.put( TLV.getBytes( msgTypeTagLength ) );
            Value.encode( buffer, messageType.getOrdinal() );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the KerberosMessage object, the PDU size is {} when only {} bytes has been allocated", 
                1 + TLV.getNbBytes( pvno ) + pvnoLength +
                1 + TLV.getNbBytes( messageType.getOrdinal() ) + msgTypeLength );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KerberosMessage encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "KerberosMessage initial value : {}", toString() );
        }

        return buffer;
    }

    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "NYI\n" );
        
        return sb.toString();
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
