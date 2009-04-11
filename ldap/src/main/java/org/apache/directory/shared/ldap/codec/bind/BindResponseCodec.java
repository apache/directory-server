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
package org.apache.directory.shared.ldap.codec.bind;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A BindResponse Message. 
 * 
 * Its syntax is : 
 * BindResponse ::= [APPLICATION 1] SEQUENCE { 
 *   COMPONENTS OF LDAPResult, 
 *   serverSaslCreds [7] OCTET STRING OPTIONAL } 
 *   
 * LdapResult ::= resultCode matchedDN errorMessage (referrals)*
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class BindResponseCodec extends LdapResponseCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The server credentials */
    private byte[] serverSaslCreds;

    /** The bind response length */
    private int bindResponseLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new BindResponse object.
     */
    public BindResponseCodec()
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
        return LdapConstants.BIND_RESPONSE;
    }


    /**
     * @return Returns the serverSaslCreds.
     */
    public byte[] getServerSaslCreds()
    {
        if ( serverSaslCreds == null )
        {
            return null;
        }

        final byte[] copy = new byte[ serverSaslCreds.length ];
        System.arraycopy( serverSaslCreds, 0, copy, 0, serverSaslCreds.length );
        return copy;
    }


    /**
     * Set the server sasl credentials
     * 
     * @param serverSaslCreds The serverSaslCreds to set.
     */
    public void setServerSaslCreds( byte[] serverSaslCreds )
    {
        if ( serverSaslCreds != null )
        {
            this.serverSaslCreds = new byte[ serverSaslCreds.length ];
            System.arraycopy( serverSaslCreds, 0, this.serverSaslCreds, 0, serverSaslCreds.length );
        } else {
            this.serverSaslCreds = null;
        }
    }


    /**
     * Compute the BindResponse length 
     * 
     * BindResponse : 
     * 0x61 L1 
     *   | 
     *   +--> LdapResult
     *   +--> [serverSaslCreds] 
     *   
     * L1 = Length(LdapResult) [ + Length(serverSaslCreds) ] 
     * Length(BindResponse) = Length(0x61) + Length(L1) + L1
     */
    public int computeLength()
    {
        int ldapResponseLength = super.computeLength();

        bindResponseLength = ldapResponseLength;

        if ( serverSaslCreds != null )
        {
            bindResponseLength += 1 + TLV.getNbBytes( serverSaslCreds.length )
                + serverSaslCreds.length;
        }

        return 1 + TLV.getNbBytes( bindResponseLength ) + bindResponseLength;
    }


    /**
     * Encode the BindResponse message to a PDU. BindResponse :
     * LdapResult.encode [0x87 LL serverSaslCreds]
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
            buffer.put( LdapConstants.BIND_RESPONSE_TAG );
            buffer.put( TLV.getBytes( bindResponseLength ) );

            // The LdapResult
            super.encode( buffer );

            // The serverSaslCredential, if any
            if ( serverSaslCreds != null )
            {
                buffer.put( ( byte ) LdapConstants.SERVER_SASL_CREDENTIAL_TAG );

                buffer.put( TLV.getBytes( serverSaslCreds.length ) );

                if ( serverSaslCreds.length != 0 )
                {
                    buffer.put( serverSaslCreds );
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
     * Get a String representation of a BindResponse
     * 
     * @return A BindResponse String
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    BindResponse\n" );
        sb.append( super.toString() );

        if ( serverSaslCreds != null )
        {
            sb.append( "        Server sasl credentials : '" ).
                append( StringTools.dumpBytes( serverSaslCreds ) ).
                append( "'\n" );
        }

        return sb.toString();
    }
}
