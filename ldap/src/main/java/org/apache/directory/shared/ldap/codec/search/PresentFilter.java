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
package org.apache.directory.shared.ldap.codec.search;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Object to store the filter. A filter is seen as a tree with a root.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PresentFilter extends Filter
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The attribute description. */
    private String attributeDescription;
    
    /** Temporary storage for attribute description bytes */
    private byte[] attributeDescriptionBytes; 


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * The constructor.
     */
    public PresentFilter()
    {
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the attribute
     * 
     * @return Returns the attributeDescription.
     */
    public String getAttributeDescription()
    {
        return attributeDescription;
    }


    /**
     * Set the attributeDescription
     * 
     * @param attributeDescription The attributeDescription to set.
     */
    public void setAttributeDescription( String attributeDescription )
    {
        this.attributeDescription = attributeDescription;
    }


    /**
     * Compute the PresentFilter length 
     * PresentFilter : 
     * 0x87 L1 present
     * 
     * Length(PresentFilter) = Length(0x87) + Length(super.computeLength()) +
     *      super.computeLength()
     */
    public int computeLength()
    {
        attributeDescriptionBytes = StringTools.getBytesUtf8( attributeDescription );
        return 1 + TLV.getNbBytes( attributeDescriptionBytes.length ) + attributeDescriptionBytes.length;
    }


    /**
     * Encode the PresentFilter message to a PDU. PresentFilter : 0x87 LL
     * attributeDescription
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
            // The PresentFilter Tag
            buffer.put( ( byte ) LdapConstants.PRESENT_FILTER_TAG );
            buffer.put( TLV.getBytes( attributeDescriptionBytes.length ) );
            buffer.put( attributeDescriptionBytes );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        super.encode( buffer );

        return buffer;
    }


    /**
     * Return a string compliant with RFC 2254 representing a Present filter
     * 
     * @return The Present filter string
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( attributeDescription ).append( "=*" );

        return sb.toString();
    }
}
