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
package org.apache.directory.shared.ldap.codec.bind;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A ldapObject which stores the Simple authentication for a BindRequest.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleAuthentication extends LdapAuthentication
{
    /** The logger */
    private static Logger log = LoggerFactory.getLogger( SimpleAuthentication.class );

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The simple authentication password */
    private byte[] simple;


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the simple password
     * 
     * @return The password
     */
    public byte[] getSimple()
    {
        return simple;
    }


    /**
     * Set the simple password
     * 
     * @param simple
     *            The simple password
     */
    public void setSimple( byte[] simple )
    {
        this.simple = simple;
    }


    /**
     * Compute the Simple authentication length Simple authentication : 0x80 L1
     * simple L1 = Length(simple) Length(Simple authentication) = Length(0x80) +
     * Length(L1) + Length(simple)
     */
    public int computeLength()
    {
        int length = 1;

        length += Length.getNbBytes( simple.length ) + simple.length;

        if ( log.isDebugEnabled() )
        {
            log.debug( "Simple Authentication length : {}", new Integer( length ) );
        }

        return length;
    }


    /**
     * Encode the simple authentication to a PDU. SimpleAuthentication : 0x80 LL
     * simple
     * 
     * @param buffer
     *            The buffer where to put the PDU
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
            // The simpleAuthentication Tag
            buffer.put( ( byte ) LdapConstants.BIND_REQUEST_SIMPLE_TAG );
            buffer.put( Length.getBytes( simple.length ) );

            if ( simple.length != 0 )
            {
                buffer.put( simple );
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
     * Return the simple authentication as a string
     * 
     * @return The simple authentication string.
     */
    public String toString()
    {
        return ( ( simple == null ) ? "null" : simple.toString() );
    }
}
