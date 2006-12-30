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
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A ldapObject which stores the SASL authentication of a BindRequest.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SaslCredentials extends LdapAuthentication
{
    /** The logger */
    private static Logger log = LoggerFactory.getLogger( SimpleAuthentication.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /**
     * Any mechanism defined in RFC 2222 : KERBEROS_V4, GSSAPI, SKEY, EXTERNAL
     */
    private String mechanism;
    
    /** The mechanism bytes */
    private transient byte[] mechanismBytes;

    /** optional credentials of the user */
    private byte[] credentials;

    /** The mechanism length */
    private transient int mechanismLength;

    /** The credentials length */
    private transient int credentialsLength;


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the credentials
     * 
     * @return The credentials
     */
    public byte[] getCredentials()
    {
        return credentials;
    }


    /**
     * Set the credentials
     * 
     * @param credentials The credentials
     */
    public void setCredentials( byte[] credentials )
    {
        this.credentials = credentials;
    }


    /**
     * Get the mechanism
     * 
     * @return The mechanism
     */
    public String getMechanism()
    {

        return ( ( mechanism == null ) ? null : mechanism );
    }


    /**
     * Set the mechanism
     * 
     * @param mechanism The mechanism
     */
    public void setMechanism( String mechanism )
    {
        this.mechanism = mechanism;
    }


    /**
     * Compute the Sasl authentication length 
     * 
     * Sasl authentication :
     * 
     * 0xA3 L1 
     *   0x04 L2 mechanism
     *   [0x04 L3 credentials]
     * 
     * L2 = Length(mechanism)
     * L3 = Length(credentials)
     * L1 = L2 + L3
     * 
     * Length(Sasl authentication) = Length(0xA3) + Length(L1) + 
     *                               Length(0x04) + Length(L2) + Length(mechanism)
     *                               [+ Length(0x04) + Length(L3) + Length(credentials)]
     */
    public int computeLength()
    {
        mechanismBytes = StringTools.getBytesUtf8( mechanism );
        mechanismLength = 1 + TLV.getNbBytes( mechanismBytes.length ) + mechanismBytes.length;
        credentialsLength = 0;

        if ( credentials != null )
        {
            credentialsLength = 1 + TLV.getNbBytes( credentials.length ) + credentials.length;
        }

        int saslLength = 1 + TLV.getNbBytes( mechanismLength + credentialsLength ) + mechanismLength
            + credentialsLength;

        if ( IS_DEBUG )
        {
            log.debug( "SASL Authentication length : {}", Integer.valueOf( saslLength ) );
        }

        return saslLength;
    }


    /**
     * Encode the sasl authentication to a PDU. 
     * 
     * SimpleAuthentication : 
     * 0xA3 L1 
     *   0x04 L2 mechanism
     *   [0x04 L3 credentials]
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
            // The saslAuthentication Tag
            buffer.put( ( byte ) LdapConstants.BIND_REQUEST_SASL_TAG );

            buffer.put( TLV.getBytes( mechanismLength + credentialsLength ) );

            Value.encode( buffer, mechanism );

            if ( credentials != null )
            {
                Value.encode( buffer, credentials );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "The PDU buffer size is too small !" );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return buffer;
    }


    /**
     * Get a String representation of a SaslCredential
     * 
     * @return A SaslCredential String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "        Sasl credentials\n" );
        sb.append( "            Mechanism :'" ).append( mechanism ).append( "'\n" );

        if ( credentials != null )
        {
            sb.append( "            Credentials :'" ).
                append( StringTools.dumpBytes(  credentials ) ).
                append( "'\n" );
        }

        return sb.toString();
    }
}
