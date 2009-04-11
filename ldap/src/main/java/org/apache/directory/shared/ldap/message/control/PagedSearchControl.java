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
package org.apache.directory.shared.ldap.message.control;


import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.search.controls.pagedSearch.PagedSearchControlCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 * @version $Rev: 678621 $
 */
public class PagedSearchControl extends InternalAbstractControl
{
    private static final long serialVersionUID = -2356861450876343999L;

    /** This class logger */
    private static final Logger log = LoggerFactory.getLogger( PagedSearchControl.class );

    /** The Paged Search Control OID */
    public static final String CONTROL_OID = "1.2.840.113556.1.4.319";
    
    /** The number of entries to return, or returned */
    private int size;
    
    /** The exchanged cookie */
    private byte[] cookie;

    /**
     * 
     * Creates a new instance of PagedSearchControl.
     *
     */
    public PagedSearchControl()
    {
        super();
        setID( CONTROL_OID );
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
     * @return The integer value for the current cookie
     */
    public int getCookieValue()
    {
        int value = ((cookie[0]&0x00FF)<<24) + ((cookie[1]&0x00FF)<<16) + ((cookie[2]&0x00FF)<<8) + (cookie[3]&0x00FF);
        
        return value;
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


    /**
     * Encode the control.
     */
    public byte[] getEncodedValue() 
    {
        // should call this codec or something
        PagedSearchControlCodec pscc = new PagedSearchControlCodec();
        pscc.setSize( size );
        pscc.setCookie( cookie );

        try
        {
            return pscc.encode( null ).array();
        }
        catch ( EncoderException e )
        {
            log.error( "Failed to encode paged search control", e );
            throw new IllegalStateException( "Failed to encode control with encoder.", e );
        }
    }
}
