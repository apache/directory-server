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
package org.apache.directory.shared.ldap.codec.extended;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A ExtendedRequest Message. Its syntax is :
 *   ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
 *              requestName      [0] LDAPOID,
 *              requestValue     [1] OCTET STRING OPTIONAL }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class ExtendedRequestCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The name */
    private OID requestName;

    /** The value */
    private byte[] requestValue;

    /** The extended request length */
    private int extendedRequestLength;

    /** The OID length */
    private int oidLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new ExtendedRequest object.
     */
    public ExtendedRequestCodec()
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
        return LdapConstants.EXTENDED_REQUEST;
    }


    /**
     * Get the extended request name
     * 
     * @return Returns the request name.
     */
    public String getRequestName()
    {
        return ( ( requestName == null ) ? "" : requestName.toString() );
    }


    /**
     * Set the extended request name
     * 
     * @param requestName The request name to set.
     */
    public void setRequestName( OID requestName )
    {
        this.requestName = requestName;
    }


    /**
     * Get the extended request value
     * 
     * @return Returns the request value.
     */
    public byte[] getRequestValue()
    {
        if ( requestValue == null )
        {
            return null;
        }

        final byte[] copy = new byte[ requestValue.length ];
        System.arraycopy( requestValue, 0, copy, 0, requestValue.length );
        return copy;
    }


    /**
     * Set the extended request value
     * 
     * @param requestValue The request value to set.
     */
    public void setRequestValue( byte[] requestValue )
    {
        if ( requestValue != null )
        {
            this.requestValue = new byte[ requestValue.length ];
            System.arraycopy( requestValue, 0, this.requestValue, 0, requestValue.length );
        } else {
            this.requestValue = null;
        }
    }


    /**
     * Compute the ExtendedRequest length
     * 
     * ExtendedRequest :
     * 
     * 0x77 L1
     *  |
     *  +--> 0x80 L2 name
     *  [+--> 0x81 L3 value]
     * 
     * L1 = Length(0x80) + Length(L2) + L2
     *      [+ Length(0x81) + Length(L3) + L3]
     * 
     * Length(ExtendedRequest) = Length(0x77) + Length(L1) + L1
     */
    public int computeLength()
    {
        oidLength = requestName.toString().length();
        extendedRequestLength = 1 + TLV.getNbBytes( oidLength ) + oidLength;

        if ( requestValue != null )
        {
            extendedRequestLength += 1 + TLV.getNbBytes( requestValue.length )
                + requestValue.length;
        }

        return 1 + TLV.getNbBytes( extendedRequestLength ) + extendedRequestLength;
    }


    /**
     * Encode the ExtendedRequest message to a PDU. 
     * 
     * ExtendedRequest :
     * 
     * 0x80 LL resquest name
     * [0x81 LL request value]
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
            // The BindResponse Tag
            buffer.put( LdapConstants.EXTENDED_REQUEST_TAG );
            buffer.put( TLV.getBytes( extendedRequestLength ) );

            // The requestName, if any
            if ( requestName == null )
            {
                throw new EncoderException( "The request name must not be null" );
            }

            buffer.put( ( byte ) LdapConstants.EXTENDED_REQUEST_NAME_TAG );
            buffer.put( TLV.getBytes( oidLength ) );

            if ( requestName.getOIDLength() != 0 )
            {
                buffer.put( StringTools.getBytesUtf8( requestName.toString() ) );
            }

            // The requestValue, if any
            if ( requestValue != null )
            {
                buffer.put( ( byte ) LdapConstants.EXTENDED_REQUEST_VALUE_TAG );

                buffer.put( TLV.getBytes( requestValue.length ) );

                if ( requestValue.length != 0 )
                {
                    buffer.put( requestValue );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return buffer;
    }


    /**
     * Get a String representation of an Extended Request
     * 
     * @return an Extended Request String
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Extended request\n" );
        sb.append( "        Request name : '" ).append( requestName ).append( "'\n" );

        if ( requestValue != null )
        {
            sb.append( "        Request value : '" ).
                append( StringTools.dumpBytes( requestValue ) ).
                append( "'\n" );
        }

        return sb.toString();
    }
}
