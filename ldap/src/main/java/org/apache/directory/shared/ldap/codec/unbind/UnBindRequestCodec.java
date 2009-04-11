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
package org.apache.directory.shared.ldap.codec.unbind;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;


/**
 * A UnBindRequest ldapObject. 
 * 
 * Its syntax is : 
 * UnbindRequest ::= [APPLICATION 2] NULL 
 * 
 * This ldapObject is empty.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class UnBindRequestCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new BindRequest object.
     */
    public UnBindRequestCodec()
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
        return LdapConstants.UNBIND_REQUEST;
    }


    /**
     * Compute the UnBindRequest length 
     * 
     * UnBindRequest : 
     * 0x42 00
     */
    public int computeLength()
    {
        return 2; // Always 2
    }


    /**
     * Encode the UnbindRequest message to a PDU.
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
            buffer.put( LdapConstants.UNBIND_REQUEST_TAG );

            // The length is always null.
            buffer.put( ( byte ) 0 );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return buffer;
    }


    /**
     * Get a String representation of a UnBindRequest
     * 
     * @return A UnBindRequest String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    UnBind Request\n" );

        return sb.toString();
    }
}
