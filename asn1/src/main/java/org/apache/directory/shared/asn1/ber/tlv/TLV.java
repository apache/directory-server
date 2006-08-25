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
package org.apache.directory.shared.asn1.ber.tlv;


import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.ber.tlv.Tag;


/**
 * This class is used to store Tag, Length and Value decoded from a PDU.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TLV
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The current Tag being processed */
    private Tag tag;

    /** The current Length being processed */
    private Length length;

    /** The current Value being processed */
    private Value value;

    /**
     * Reference the TLV which contains the current TLV, if any. As the
     * enclosing TLV of a PDU does not have parent, it can be null in this case.
     * Otherwise, it must point to a constructed TLV
     */
    private TLV parent;

    /**
     * The expected length of the TLV's elements, if the current TLV is a
     * constructed TLV.
     */
    private int expectedLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new TLV object.
     */
    public TLV()
    {
        tag = new Tag();
        length = new Length();
        value = new Value();

        expectedLength = 0;
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Reset the TLV, so it can be reused for the next PDU decoding.
     */
    public void reset()
    {
        tag.reset();
        length.reset();
        value.reset();

        expectedLength = 0;
    }


    /**
     * @return Returns the length.
     */
    public Length getLength()
    {
        return length;
    }


    /**
     * Add the TLV Length part
     * 
     * @param length
     *            The length to set.
     */
    public void setLength( Length length )
    {
        this.length = length;

        expectedLength = length.getLength();
    }


    /**
     * @return Returns the tag.
     */
    public Tag getTag()
    {
        return tag;
    }


    /**
     * @return Returns the value.
     */
    public Value getValue()
    {
        return value;
    }


    /**
     * Get a String representation of the TLV
     * 
     * @return A String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "TLV[ " );
        sb.append( tag.toString() ).append( ", " );
        sb.append( length.toString() ).append( ", " );
        sb.append( value.toString() );
        sb.append( "]" );

        return sb.toString();
    }


    /**
     * The TLV size is calculated by adding the Tag's size, the Length's size
     * and the Value's length, if any.
     * 
     * @return Returns the size of the TLV.
     */
    public int getSize()
    {
        return tag.getSize() + length.getSize() + length.getLength();
    }


    /**
     * @return Returns the parent.
     */
    public TLV getParent()
    {
        return parent;
    }


    /**
     * @param parent
     *            The parent to set.
     */
    public void setParent( TLV parent )
    {
        this.parent = parent;
    }


    /**
     * Get the TLV expected length.
     * 
     * @return Returns the expectedLength.
     */
    public int getExpectedLength()
    {
        return expectedLength;
    }


    /**
     * Set the new expected length of the current TLV.
     * 
     * @param expectedLength
     *            The expectedLength to set.
     */
    public void setExpectedLength( int expectedLength )
    {
        this.expectedLength = expectedLength;
    }
}
