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
package org.apache.directory.shared.ldap.codec.search;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;


/**
 * Or Filter Object to store the Or filter.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OrFilter extends ConnectorFilter
{
    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * The constructor. We wont initialize the ArrayList as they may not be
     * used.
     */
    public OrFilter()
    {
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the OrFilter
     * 
     * @return Returns the orFilter.
     */
    public ArrayList getOrFilter()
    {
        return filterSet;
    }


    /**
     * Compute the OrFilter length OrFilter : 0xA1 L1 super.computeLength()
     * Length(OrFilter) = Length(0xA1) + Length(super.computeLength()) +
     * super.computeLength()
     */
    public int computeLength()
    {
        filtersLength = super.computeLength();

        return 1 + Length.getNbBytes( filtersLength ) + filtersLength;
    }


    /**
     * Encode the OrFilter message to a PDU. OrFilter : 0xA1 LL filter.encode()
     * 
     * @param buffer
     *            The buffer where to put the PDU
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
            // The OrFilter Tag
            buffer.put( ( byte ) LdapConstants.OR_FILTER_TAG );
            buffer.put( Length.getBytes( filtersLength ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        super.encode( buffer );

        return buffer;
    }


    /**
     * Return a string compliant with RFC 2254 representing an OR filter
     * 
     * @return The OR filter string
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( '|' ).append( super.toString() );

        return sb.toString();
    }
}
