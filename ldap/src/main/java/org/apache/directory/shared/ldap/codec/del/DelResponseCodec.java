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
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;


/**
 * An DelResponse Message. Its syntax is : 
 * 
 * DelResponse ::= [APPLICATION 11] LDAPResult
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class DelResponseCodec extends LdapResponseCodec
{
    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new DelResponse object.
     */
    public DelResponseCodec()
    {
        super();
    }


    /**
     * Get the message type
     * 
     * @return Returns the type.
     */
    public int getMessageType()
    {
        return LdapConstants.DEL_RESPONSE;
    }


    /**
     * Compute the DelResponse length 
     * 
     * DelResponse :
     * 
     * 0x6B L1
     *  |
     *  +--> LdapResult
     * 
     * L1 = Length(LdapResult)
     * 
     * Length(DelResponse) = Length(0x6B) + Length(L1) + L1
     */
    public int computeLength()
    {
        int ldapResponseLength = super.computeLength();

        return 1 + TLV.getNbBytes( ldapResponseLength ) + ldapResponseLength;
    }


    /**
     * Encode the DelResponse message to a PDU.
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
            // The tag
            buffer.put( LdapConstants.DEL_RESPONSE_TAG );
            buffer.put( TLV.getBytes( getLdapResponseLength() ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        // The ldapResult
        return super.encode( buffer );
    }


    /**
     * Get a String representation of a DelResponse
     * 
     * @return A DelResponse String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Del Response\n" );
        sb.append( super.toString() );

        return sb.toString();
    }
}
