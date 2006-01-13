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
package org.apache.ldap.common.codec.search;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.tlv.Length;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapResponse;


/**
 * A SearchResultDone Message. Its syntax is :
 *   SearchResultDone ::= [APPLICATION 5] LDAPResult
 * 
 * It's a Response, so it inherites from LdapResponse.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchResultDone extends LdapResponse
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SearchResultDone object.
     */
    public SearchResultDone()
    {
        super( );
    }

    /**
     * Get the message type
     *
     * @return Returns the type.
     */
    public int getMessageType()
    {
        return LdapConstants.SEARCH_RESULT_DONE;
    }

    /**
     * Compute the SearchResultDone length
     * 
     * SearchResultDone :
     * 
     * 0x65 L1
     *  |
     *  +--> LdapResult
     * 
     * L1 = Length(LdapResult)
     * 
     * Length(SearchResultDone) = Length(0x65) + Length(L1) + L1
     */
    public int computeLength()
    {
        int ldapResponseLength = super.computeLength();

        return 1 + Length.getNbBytes( ldapResponseLength ) + ldapResponseLength;
    }

    /**
     * Encode the SearchResultDone message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer )  throws EncoderException
    {
        if (buffer == null)
        {
            throw new EncoderException("Cannot put a PDU in a null buffer !");
        }

        try
        {
            // The tag
            buffer.put( LdapConstants.SEARCH_RESULT_DONE_TAG );
            buffer.put( Length.getBytes( getLdapResponseLength() ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException("The PDU buffer size is too small !");
        }

        // The ldapResult
        return super.encode( buffer);
    }

    /**
     * Get a String representation of a SearchResultDone
     *
     * @return A SearchResultDone String 
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Search Result Done\n" );
        sb.append( super.toString() );

        return sb.toString();
    }
}
