/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.ldap.codec.extended;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapResponse;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A ExtendedResponse Message. Its syntax is :
 *   ExtendedResponse ::= [APPLICATION 24] SEQUENCE {
 *              COMPONENTS OF LDAPResult,
 *              responseName     [10] LDAPOID OPTIONAL,
 *              response         [11] OCTET STRING OPTIONAL }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExtendedResponse extends LdapResponse
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The name */
    private OID responseName;

    /** The response */
    private Object response;

    /** The extended response length */
    private transient int extendedResponseLength;

    /** The OID length */
    private transient int responseNameLength;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ExtendedResponse object.
     */
    public ExtendedResponse()
    {
        super( );
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the message type
     *
     * @return Returns the type.
     */
    public int getMessageType()
    {
        return LdapConstants.EXTENDED_RESPONSE;
    }

    /**
     * Get the extended response name
     *
     * @return Returns the name.
     */
    public String getResponseName()
    {
        return ( ( responseName == null ) ? "" : responseName.toString() );
    }

    /**
     * Set the extended response name
     *
     * @param responseName The name to set.
     */
    public void setResponseName( OID responseName )
    {
        this.responseName = responseName;
    }

    /**
     * Get the extended response
     *
     * @return Returns the response.
     */
    public Object getResponse()
    {
        return response;
    }

    /**
     * Set the extended response
     *
     * @param response The response to set.
     */
    public void setResponse( Object response )
    {
        this.response = response;
    }

    /**
     * Compute the ExtendedResponse length
     * 
     * ExtendedResponse :
     * 
     * 0x78 L1
     *  |
     *  +--> LdapResult
     * [+--> 0x8A L2 name
     * [+--> 0x8B L3 response]]
     * 
     * L1 = Length(LdapResult)
     *      [ + Length(0x8A) + Length(L2) + L2
     *       [ + Length(0x8B) + Length(L3) + L3]]
     * 
     * Length(ExtendedResponse) = Length(0x78) + Length(L1) + L1
     * @return The ExtendedResponse length
    */
    public int computeLength()
    {
        extendedResponseLength = super.computeLength();

        if ( responseName != null )
        {
            responseNameLength = responseName.toString().length();
            extendedResponseLength += 1 + Length.getNbBytes( responseNameLength ) + responseNameLength;

            if ( response != null )
            {
                if ( response instanceof String )
                {
                    int responseLength = StringTools.getBytesUtf8( (String)response ).length;
                    extendedResponseLength += 1 + Length.getNbBytes( responseLength ) + responseLength;
                }
                else
                {
                    extendedResponseLength += 1 + Length.getNbBytes( ( (byte[])response).length ) + ( (byte[])response).length;
                }
            }
        }

        return 1 + Length.getNbBytes( extendedResponseLength ) + extendedResponseLength;
    }

    /**
     * Encode the ExtendedResponse message to a PDU.
     * 
     * ExtendedResponse :
     * 
     * LdapResult.encode()
     * [0x8A LL response name]
     * [0x8B LL response]
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if (buffer == null)
        {
            throw new EncoderException("Cannot put a PDU in a null buffer !");
        }

        try
        {
            // The BindResponse Tag
            buffer.put( LdapConstants.EXTENDED_RESPONSE_TAG );
            buffer.put( Length.getBytes( extendedResponseLength ) );

            // The LdapResult
            super.encode(buffer);

            // The responseName, if any
            if ( responseName != null )
            {
                buffer.put( (byte) LdapConstants.EXTENDED_RESPONSE_RESPONSE_NAME_TAG );
                buffer.put( Length.getBytes( responseNameLength ) );

                if ( responseName.getOIDLength() != 0 )
                {
                    buffer.put( StringTools.getBytesUtf8( responseName.toString() ) );
                }
            }

            // The response, if any
            if ( response != null )
            {
                buffer.put( (byte)LdapConstants.EXTENDED_RESPONSE_RESPONSE_TAG );

                if ( response instanceof String )
                {
                    byte[] responseBytes = StringTools.getBytesUtf8( (String)response );
                    buffer.put( Length.getBytes( responseBytes.length ) );

                    if ( responseBytes.length != 0 )
                    {
                        buffer.put( responseBytes );
                    }
                }
                else
                {
                    buffer.put( Length.getBytes( ( (byte[])response).length ) );

                    if ( ( (byte[])response).length != 0 )
                    {
                        buffer.put( (byte[])response );
                    }
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException("The PDU buffer size is too small !");
        }

        return buffer;
    }

    /**
     * Get a String representation of an ExtendedResponse
     *
     * @return An ExtendedResponse String 
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Extended Response\n" );
        sb.append( super.toString() );

        if ( responseName != null )
        {
            sb.append( "        Response name :'" ).append( responseName.toString() ).append( "'\n" );
        }

        if ( response != null )
        {
            sb.append( "        Response :'" ).append( response.toString() ).append( "'\n" );
        }

        return sb.toString();
    }
}
