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
package org.apache.directory.shared.ldap.codec.search.controls;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A request/response control used to implement a simple paging of search
 * results. This is an implementation of RFC 2696 :
 * <a href="http://www.faqs.org/rfcs/rfc2696.html">LDAP Control Extension for Simple Paged Results Manipulation</a>
 * <br/>
 * <pre>
 *    This control is included in the searchRequest and searchResultDone
 *    messages as part of the controls field of the LDAPMessage, as defined
 *    in Section 4.1.12 of [LDAPv3]. The structure of this control is as
 *    follows:
 *
 * pagedResultsControl ::= SEQUENCE {
 *         controlType     1.2.840.113556.1.4.319,
 *         criticality     BOOLEAN DEFAULT FALSE,
 *         controlValue    searchControlValue
 * }
 * 
 * The searchControlValue is an OCTET STRING wrapping the BER-encoded
 * version of the following SEQUENCE:
 * 
 * realSearchControlValue ::= SEQUENCE {
 *         size            INTEGER (0..maxInt),
 *                                 -- requested page size from client
 *                                 -- result set size estimate from server
 *         cookie          OCTET STRING
 * }
 * 
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:  $
 */
public class PagedSearchControlCodec extends AbstractAsn1Object
{
    /** The number of entries to return, or returned */
    private int size;
    
    /** The exchanged cookie */
    private byte[] cookie;

    /** The entry change global length */
    private int pscSeqLength;


    /**
     * @see Asn1Object#Asn1Object
     */
    public PagedSearchControlCodec()
    {
        super();
    }

    
    /**
     * Compute the PagedSearchControl length 
     * 
     * 0x30 L1 
     *   | 
     *   +--> 0x02 0x0(1-4) [0..2^63-1] (size) 
     *   +--> 0x04 L2 (cookie) 
     */
    public int computeLength()
    {
        int sizeLength = 1 + 1 + Value.getNbBytes( size );;

        int cookieLength = 0;
        
        if ( cookie != null )
        {
            cookieLength = 1 + TLV.getNbBytes( cookie.length ) + cookie.length;
        }
        else
        {
            cookieLength = 1 + 1;
        }

        pscSeqLength = sizeLength + cookieLength;

        return 1 + TLV.getNbBytes( pscSeqLength ) + pscSeqLength;
    }


    /**
     * Encodes the paged search control.
     * 
     * @param buffer The encoded sink
     * @return A ByteBuffer that contains the encoded PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        // Allocate the bytes buffer.
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );
        bb.put( UniversalTag.SEQUENCE_TAG );
        bb.put( TLV.getBytes( pscSeqLength ) );

        Value.encode( bb, size );
        Value.encode( bb, cookie );
        
        return bb;
    }


    /**
     * Return a String representing this PagedSearchControl.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Paged Search Control\n" );
        sb.append( "        size   : '" ).append( size ).append( "'\n" );
        sb.append( "        cookie   : '" ).append( StringTools.dumpBytes( cookie ) ).append( "'\n" );
        
        return sb.toString();
    }


    /**
     * @return The requested or returned number of entries
     */
    public int getSize()
    {
        return size;
    }


    /**
     * Set the number of entry requested or returned
     *
     * @param size The number of entries 
     */
    public void setSize( int size )
    {
        this.size = size;
    }


    /**
     * @return The stored cookie
     */
    public byte[] getCookie()
    {
        return cookie;
    }


    /**
     * Set the cookie
     *
     * @param cookie The cookie to store in this control
     */
    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }
}
