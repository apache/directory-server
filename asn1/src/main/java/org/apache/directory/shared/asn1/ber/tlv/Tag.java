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
 * The Tag component of a BER TLV Tuple.
 *
 * @author   <a href="mailto:dev@directory.apache.org">Apache
 *           Directory Project</a>
 */
public class Tag implements Cloneable, Serializable
{
	public static final long serialVersionUID = 1L;

	//~ Static fields/initializers -----------------------------------------------------------------

    /** tag flag for the primitive/constructed bit - 0010 0000 - 0x20 */
    public static final transient int CONSTRUCTED_FLAG = 0x20;

    /** tag mask for the short tag format - 0001 1111 - 0x1F */
    public static final transient int SHORT_MASK = 0x1F;

    /** tag mask for the long tag format - 0111 1111 - 0x7F */
    public static final transient int LONG_MASK = 0x7F;

    /** tag flag indicating the use of the long tag encoding form */
    public static final transient int LONG_FLAG = 0x80;

    /** the max id size with one tag octet */
    public static final transient int ONE_OCTET_IDMAX = 30;

    /** the max id size with two tag octets */
    public static final transient int TWO_OCTET_IDMAX = ( 1 << 7 ) - 1;

    /** the max id size with three tag octets */
    public static final transient int THREE_OCTET_IDMAX = ( 1 << 14 ) - 1;

    /** the max id size with four tag octets */
    public static final transient int FOUR_OCTET_IDMAX = ( 1 << 21 ) - 1;

    /** the bit that signal that the value will overflow */
    public static final transient int TAG_MAX_FLAG = ( 1 << 26 );

    /** value for the universal type class */
    public static final transient int TYPE_CLASS_UNIVERSAL = 0;

    /** value for the application type class */
    public static final transient int TYPE_CLASS_APPLICATION = 1;

    /** value for the context specific type class */
    public static final transient int TYPE_CLASS_CONTEXT_SPECIFIC = 2;

    /** value for the private type class */
    public static final transient int TYPE_CLASS_PRIVATE = 3;

    /** mask to get the type class value */
    public static final transient int TYPE_CLASS_MASK = 0xC0;

    /**
     * The maximum bytes number that could be used to hold the value. Actually,
     * it's five : - 7 bits x 4 bytes = 28 bits, which is not enough to
     * represent an int. - 7 bits x 5 bytes = 35 bits, which is just above int's
     * number of bits Note : the higher bit is not used.
     */
    public static final transient int MAX_TAG_BYTES = 5;

    /** array of the different Type classes */
    public static final int[] TYPE_CLASS =
    {
        Tag.TYPE_CLASS_UNIVERSAL, Tag.TYPE_CLASS_APPLICATION, Tag.TYPE_CLASS_CONTEXT_SPECIFIC,
        Tag.TYPE_CLASS_PRIVATE
    };

    //~ Instance fields ----------------------------------------------------------------------------

    /** the int used to store the tag octets */
    private int id;

    /** the number of octets currently read */
    private int size;

    /** whether or not this tag represents a primitive type */
    private boolean isPrimitive;

    /** the type class of this tag */
    private int typeClass;

    /** The bytes read from the PDU. We store only 5 bytes, so we can't have tag that are
     * above 2^28 */
    private byte[] tagBytes = new byte[] { 0, 0, 0, 0, 0 };

    /** Current position in the tagBytes */
    private int bytePos = 0;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new Tag object.
     */
    public Tag()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Reset the tag so that it can be reused. 
     */
    public void reset()
    {
        id          = 0;
        size        = 0;
        isPrimitive = false;
        typeClass   = Tag.TYPE_CLASS_APPLICATION;
        tagBytes[0] = 0;
        tagBytes[1] = 0;
        tagBytes[2] = 0;
        tagBytes[3] = 0;
        tagBytes[4] = 0;
        bytePos     = 0;
    }

    /**
     * Gets the id which represent the tag.
     *
     * @return  the id
     */
    public int getId()
    {
        return id;
    }

    /**
     * Set the id.
     *
     * @param id The id to be set
    */
    public void setId( int id )
    {
        this.id = id;
    }

    /**
     * Gets the number of octets of this Tag.
     *
     * @return  the number of octets of this Tag
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Gets the number of octets in this Tag.
     *
     * @param size The size of the tag
    */
    public void setSize( int size )
    {
        this.size = size;
    }

    /**
     * Gets the number of octets in this Tag.
     *
     * */
    public void incTagSize()
    {
        this.size++;
    }

    /**
     * Gets the type class for this Tag.
     *
     * @return  The typeClass for this Tag
     */
    public int getTypeClass()
    {
        return typeClass;
    }

    /**
     * Gets the type class for this Tag.
     *
     * @param typeClass The TypeClass to set
    */
    public void setTypeClass( int typeClass )
    {
        this.typeClass = typeClass;
    }

    /**
     * Checks to see if the tag is constructed.
     *
     * @return  true if constructed, false if primitive
     */
    public boolean isConstructed()
    {
        return ! isPrimitive;
    }

    /**
     * Checks to see if the tag represented by this Tag is primitive or
     * constructed.
     *
     * @return  true if it is primitive, false if it is constructed
     */
    public boolean isPrimitive()
    {
        return isPrimitive;
    }

    /**
     * Tells if the tag is Universal or not
     * @return  true if it is primitive, false if it is constructed
     */
    public boolean isUniversal()
    {
        return typeClass == TYPE_CLASS_UNIVERSAL;
    }

    /**
     * Tells if the tag class is Application or not
     *
     * @return  true if it is Application, false otherwise.
     */
    public boolean isApplication()
    {
        return typeClass == TYPE_CLASS_APPLICATION;
    }

    /**
     * Tells if the tag class is Private or not
     *
     * @return  true if it is Private, false otherwise.
     */
    public boolean isPrivate()
    {
        return typeClass == TYPE_CLASS_PRIVATE;
    }

    /**
     * Tells if the tag class is Contextual or not
     *
     * @return  true if it is Contextual, false otherwise.
     */
    public boolean isContextual()
    {
        return typeClass == TYPE_CLASS_CONTEXT_SPECIFIC;
    }

    /**
     * Set the tag type to Primitive or Constructed
     *
     * @param isPrimitive The type to set
    */
    public void setPrimitive( boolean isPrimitive )
    {
        this.isPrimitive = isPrimitive;
    }

    /**
     * Add a byte to the inner representation of the tag.
     *
     * @param octet The byte to add.
     */
    public void addByte( byte octet )
    {
        tagBytes[bytePos++] = octet;
    }

    /**
     * Get the first byte of the tag.
     *
     * @return The first byte of the tag.
     */
    public byte getTagByte()
    {
        return tagBytes[0];
    }

    /**
     * @return Get all the bytes of the tag
     */
    public byte[] getTagBytes()
    {
        return tagBytes;
    }

    /**
     * Get the byte at a specific position of the tag's bytes
     *
     * @param pos The position
     *
     * @return The byte found
     */
    public byte getTagBytes( int pos )
    {
        return tagBytes[pos];
    }

    /**
     * Clone the Tag
     *
     * @return A copy of the tag
     *
     * @throws CloneNotSupportedException Thrown if we have a cloning problem 
     */
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }

    /**
     * A string representation of a Tag
     *
     * @return A string representation of a Tag
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "TAG[" );

        if ( isPrimitive )
        {
            sb.append( "PRIMITIVE, " );
        }
        else
        {
            sb.append( "CONSTRUCTED, " );
        }

        switch ( typeClass )
        {

            case TYPE_CLASS_APPLICATION :
                sb.append( "APPLICATION, " );

                break;

            case TYPE_CLASS_UNIVERSAL :
                sb.append( "UNIVERSAL, " ).append( UniversalTag.toString( id ) );

                break;

            case TYPE_CLASS_PRIVATE :
                sb.append( "PRIVATE, " ).append( id );

                break;

            case TYPE_CLASS_CONTEXT_SPECIFIC :
                sb.append( "CONTEXTUAL, " ).append( id );

                break;
        }

        sb.append( "](size=" ).append( size ).append( ")" );

        return sb.toString();
    }
} // end interface ITag
