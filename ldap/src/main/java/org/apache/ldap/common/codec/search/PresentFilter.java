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
import org.apache.ldap.common.codec.util.LdapString;


/**
 * Object to store the filter. A filter is seen as a tree with a root.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PresentFilter extends Filter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The attribute description. */
    private LdapString attributeDescription;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * The constructor. 
     */
    public PresentFilter()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the attribute
     *
     * @return Returns the attributeDescription.
     */
    public LdapString getAttributeDescription()
    {
        return attributeDescription;
    }

    /**
     * Set the attributeDescription
     *
     * @param attributeDescription The attributeDescription to set.
     */
    public void setAttributeDescription( LdapString attributeDescription )
    {
        this.attributeDescription = attributeDescription;
    }

    /**
     * Compute the PresentFilter length
     * 
     * PresentFilter :
     * 
     * 0x87 L1 present
     * 
     * Length(PresentFilter) = Length(0x87) + Length(super.computeLength()) + super.computeLength()
     * 
     */
    public int computeLength()
    {
        return 1 + Length.getNbBytes( attributeDescription.getNbBytes() ) + attributeDescription.getNbBytes();
    }

    /**
     * Encode the PresentFilter message to a PDU.
     * 
     * PresentFilter :
     * 
     * 0x87 LL attributeDescription 
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if (buffer == null)
        {
            throw new EncoderException("Cannot put a PDU in a null buffer !");
        }

        try
        {
            // The PresentFilter Tag
            buffer.put( (byte)LdapConstants.PRESENT_FILTER_TAG );
            buffer.put( Length.getBytes( attributeDescription.getNbBytes() ) );
            buffer.put( attributeDescription.getBytes() );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException("The PDU buffer size is too small !");
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

        sb.append( attributeDescription.toString() ).append( "=*" );

        return sb.toString();
    }
}
