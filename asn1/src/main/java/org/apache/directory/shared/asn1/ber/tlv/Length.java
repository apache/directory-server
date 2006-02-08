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
package org.apache.directory.shared.asn1.ber.tlv;

import java.io.Serializable;


/**
 * The Length part of a TLV. We are not dealing with indefinite length.
 * 
 * @author   <a href="mailto:dev@directory.apache.org">Apache
 *           Directory Project</a>
 */
public class Length implements Cloneable, Serializable
{
	public static final long serialVersionUID = 1L;
	
    //~ Static fields/initializers -----------------------------------------------------------------

    /** A mask to get the Length form */
    public static final transient int LENGTH_LONG_FORM = 0x0080;

    /** Value of the reserved extension */
    public static final transient int LENGTH_EXTENSION_RESERVED = 0x7F;

    /** A mask to get the long form value */
    public static final transient int SHORT_MASK = 0x007F;

    //~ Instance fields ----------------------------------------------------------------------------

    /** The length of the following value */
    private int length;

    /** The size of the Length part. */
    private int size;

    /** If the Length is in a long form, this variable store the expected
     * number of bytes to be read to obtain the length */
    private transient int expectedLength;

    /** Stores the number of bytes already read for a long Length form */
    private int currentLength;

    /** A flag used with definite forms length. */
    private boolean definiteForm;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new object.
    */
    public Length()
    {
        length         = 0;
        expectedLength = 1;
        currentLength  = 0;
        size           = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Reset the Length object
     */
    public void reset()
    {
        length         = 0;
        expectedLength = 0;
        currentLength  = 0;
        size           = 0;
    }

    /**
     * Get the Value length
     *
     * @return Returns the length of the value part.
     */
    public int getLength()
    {

        return length;
    }

    /**
     * Set the length of the Value part 
     *
     * @param length The length of the Value part.
     */
    public void setLength( int length )
    {
        this.length = length;
    }

    /**
     * Get the current number of bytes read
     *
     * @return Returns the currentLength.
     */
    public int getCurrentLength()
    {

        return currentLength;
    }

    /**
     * Set the current length of the Length
     *
     * @param currentLength The currentLength to set.
     */
    public void setCurrentLength( int currentLength )
    {
        this.currentLength = currentLength;
    }

    /**
     * Increment the Length being read
     */
    public void incCurrentLength()
    {
        this.currentLength++;
    }

    /**
     * Get the expected length
     *
     * @return Returns the expected Length of the long form Length.
     */
    public int getExpectedLength()
    {
        return expectedLength;
    }

    /**
     * Set the expected long form length
     *
     * @param expectedLength The long form expected length to set.
     */
    public void setExpectedLength( int expectedLength )
    {
        this.expectedLength = expectedLength;
    }

    /**
     * Clone the object
     *
     * @return A deep copy of the Length
     *
     * @throws CloneNotSupportedException Thrown if any problem occurs.
     */
    public Object clone() throws CloneNotSupportedException
    {

        return super.clone();
    }

    /**
     * Get the size of the Length element
     *
     * @return Returns the size of the Length element.
     */
    public int getSize()
    {

        return size;
    }

    /**
     * Increment the size of the Length element.
     */
    public void incSize()
    {
        this.size++;
    }

    /**
     * Return a String representing the Length
     *
     * @return The length
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();
        sb.append( "LENGTH[" ).append( length ).append( "](" )
          .append( definiteForm ? "definite)" : "indefinite)" ).append( "size=" ).append( size )
          .append(
            ")" );

        return sb.toString();
    }

    /**
     * Set the Length's size
     *
     * @param size The lengthSize to set.
     */
    public void setSize( int size )
    {
        this.size = size;
    }

    /**
     * Utility function that return the number of bytes necessary to store 
     * the length
     * @param length The length to store in a byte array
     * @return The number of bytes necessary to store the length.
     */
    public static int getNbBytes( int length )
    {

        if ( length >= 0 )
        {

            if ( length < 128 )
            {
                return 1;
            }
            else if ( length < 256 )
            {
                return 2;
            }
            else if ( length < 65536 )
            {
                return 3;
            }
            else if ( length < 16777216 )
            {
                return 4;
            }
            else
            {
                return 5;
            }
        }
        else
        {
            return 5;
        }
    }

    /**
     * Utility function that return a byte array representing the length
     * @param length The length to store in a byte array
     * @return The byte array representing the length.
     */
    public static byte[] getBytes( int length )
    {

        byte[] bytes = new byte[getNbBytes( length )];

        if ( length >= 0 )
        {

            if ( length < 128 )
            {
                bytes[0] = ( byte ) length;
            }
            else if ( length < 256 )
            {
                bytes[0] = ( byte ) 0x81;
                bytes[1] = ( byte ) length;
            }
            else if ( length < 65536 )
            {
                bytes[0] = ( byte ) 0x82;
                bytes[1] = ( byte ) ( length >> 8 );
                bytes[2] = ( byte ) ( length & 0x00FF );
            }
            else if ( length < 16777216 )
            {
                bytes[0] = ( byte ) 0x83;
                bytes[1] = ( byte ) ( length >> 16 );
                bytes[2] = ( byte ) ( ( length >> 8 ) & 0x00FF );
                bytes[3] = ( byte ) ( length & 0x00FF );
            }
            else
            {
                bytes[0] = ( byte ) 0x84;
                bytes[1] = ( byte ) ( length >> 24 );
                bytes[2] = ( byte ) ( ( length >> 16 ) & 0x00FF );
                bytes[3] = ( byte ) ( ( length >> 8 ) & 0x00FF );
                bytes[4] = ( byte ) ( length & 0x00FF );
            }
        }
        else
        {
            bytes[0] = ( byte ) 0x84;
            bytes[1] = ( byte ) ( length >> 24 );
            bytes[2] = ( byte ) ( ( length >> 16 ) & 0x00FF );
            bytes[3] = ( byte ) ( ( length >> 8 ) & 0x00FF );
            bytes[4] = ( byte ) ( length & 0x00FF );
        }

        return bytes;
    }

    /**
     * Get the length's type 
     * @return Returns the definiteForm flag.
     */
    public boolean isDefiniteForm()
    {
        return definiteForm;
    }

    /**
     * Set the length's form
     *
     * @param definiteForm The definiteForm flag to set.
     */
    public void setDefiniteForm( boolean definiteForm )
    {
        this.definiteForm = definiteForm;
    }
}
