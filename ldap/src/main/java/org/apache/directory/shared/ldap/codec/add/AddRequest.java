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
package org.apache.directory.shared.ldap.codec.add;


import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.util.LdapString;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;


/**
 * An AddRequest Message. Its syntax is : AddRequest ::= [APPLICATION 8]
 * SEQUENCE { entry LDAPDN, attributes AttributeList } AttributeList ::=
 * SEQUENCE OF SEQUENCE { type AttributeDescription, vals SET OF AttributeValue }
 * AttributeValue ::= OCTET STRING
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddRequest extends LdapMessage
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private transient static final Logger log = LoggerFactory.getLogger( AddRequest.class );

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The DN to be added. */
    private LdapDN entry;

    /** The attributes list. */
    private Attributes attributes;

    /** The current attribute being decoded */
    private transient Attribute currentAttribute;

    /** The add request length */
    private transient int addRequestLength;

    /** The attributes length */
    private transient int attributesLength;

    /** The list of all attributes length */
    private transient List attributeLength;

    /** The list of all vals length */
    private transient List valuesLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new AddRequest object.
     */
    public AddRequest()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the message type
     * 
     * @return Returns the type.
     */
    public int getMessageType()
    {
        return LdapConstants.ADD_REQUEST;
    }


    /**
     * Initialize the ArrayList for attributes.
     */
    public void initAttributes()
    {
        attributes = new BasicAttributes( true );
    }


    /**
     * Get the entry's attributes to be added
     * 
     * @return Returns the attributes.
     */
    public Attributes getAttributes()
    {
        return attributes;
    }


    /**
     * Create a new attributeValue
     * 
     * @param type
     *            The attribute's name (called 'type' in the grammar)
     */
    public void addAttributeType( LdapString type )
    {
        currentAttribute = new BasicAttribute( type.getString().toLowerCase() );
        attributes.put( currentAttribute );
    }

    /**
     * Create a new attributeValue
     * 
     * @param type
     *            The attribute's name (called 'type' in the grammar)
     */
    public void addAttributeType( String type )
    {
        currentAttribute = new BasicAttribute( type.toLowerCase() );
        attributes.put( currentAttribute );
    }


    /**
     * Add a new value to the current attribute
     * 
     * @param value
     *            The value to be added
     */
    public void addAttributeValue( Object value )
    {
        currentAttribute.add( value );
    }


    /**
     * Get the added DN
     * 
     * @return Returns the entry.
     */
    public LdapDN getEntry()
    {
        return entry;
    }


    /**
     * Set the added DN.
     * 
     * @param entry
     *            The entry to set.
     */
    public void setEntry( LdapDN entry )
    {
        this.entry = entry;
    }


    /**
     * Compute the AddRequest length AddRequest : 0x68 L1 | +--> 0x04 L2 entry
     * +--> 0x30 L3 (attributes) | +--> 0x30 L4-1 (attribute) | | | +--> 0x04
     * L5-1 type | +--> 0x31 L6-1 (values) | | | +--> 0x04 L7-1-1 value | +-->
     * ... | +--> 0x04 L7-1-n value | +--> 0x30 L4-2 (attribute) | | | +--> 0x04
     * L5-2 type | +--> 0x31 L6-2 (values) | | | +--> 0x04 L7-2-1 value | +-->
     * ... | +--> 0x04 L7-2-n value | +--> ... | +--> 0x30 L4-m (attribute) |
     * +--> 0x04 L5-m type +--> 0x31 L6-m (values) | +--> 0x04 L7-m-1 value +-->
     * ... +--> 0x04 L7-m-n value
     */
    public int computeLength()
    {
        // The entry
        addRequestLength = 1 + Length.getNbBytes( LdapDN.getNbBytes( entry ) ) + LdapDN.getNbBytes( entry );

        // The attributes sequence
        attributesLength = 0;

        if ( ( attributes != null ) && ( attributes.size() != 0 ) )
        {
            NamingEnumeration attributeIterator = attributes.getAll();
            attributeLength = new LinkedList();
            valuesLength = new LinkedList();

            // Compute the attributes length
            while ( attributeIterator.hasMoreElements() )
            {
                Attribute attribute = ( Attribute ) attributeIterator.nextElement();
                int localAttributeLength = 0;
                int localValuesLength = 0;

                // Get the type length
                int idLength = attribute.getID().getBytes().length;
                localAttributeLength = 1 + Length.getNbBytes( idLength ) + idLength;

                // The values
                try
                {
                    NamingEnumeration values = attribute.getAll();

                    if ( values.hasMoreElements() )
                    {
                        localValuesLength = 0;

                        while ( values.hasMoreElements() )
                        {
                            Object value = values.next();

                            if ( value instanceof String )
                            {
                                int valueLength = StringTools.getBytesUtf8( ( String ) value ).length;
                                localValuesLength += 1 + Length.getNbBytes( valueLength ) + valueLength;
                            }
                            else
                            {
                                int valueLength = ( ( byte[] ) value ).length;
                                localValuesLength += 1 + Length.getNbBytes( valueLength ) + valueLength;
                            }
                        }

                        localAttributeLength += 1 + Length.getNbBytes( localValuesLength ) + localValuesLength;
                    }

                }
                catch ( NamingException ne )
                {
                    return 0;
                }

                // add the attribute length to the attributes length
                attributesLength += 1 + Length.getNbBytes( localAttributeLength ) + localAttributeLength;

                attributeLength.add( new Integer( localAttributeLength ) );
                valuesLength.add( new Integer( localValuesLength ) );
            }
        }

        addRequestLength += 1 + Length.getNbBytes( attributesLength ) + attributesLength;

        // Return the result.
        int result = 1 + Length.getNbBytes( addRequestLength ) + addRequestLength;

        if ( log.isDebugEnabled() )
        {
            log.debug( "AddRequest PDU length = {}", new Integer( result ) );
        }

        return result;
    }


    /**
     * Encode the AddRequest message to a PDU. AddRequest : 0x68 LL 0x04 LL
     * entry 0x30 LL attributesList 0x30 LL attributeList 0x04 LL
     * attributeDescription 0x31 LL attributeValues 0x04 LL attributeValue ...
     * 0x04 LL attributeValue ... 0x30 LL attributeList 0x04 LL
     * attributeDescription 0x31 LL attributeValue 0x04 LL attributeValue ...
     * 0x04 LL attributeValue
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
            // The AddRequest Tag
            buffer.put( LdapConstants.ADD_REQUEST_TAG );
            buffer.put( Length.getBytes( addRequestLength ) );

            // The entry
            Value.encode( buffer, LdapDN.getBytes( entry ) );

            // The attributes sequence
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( Length.getBytes( attributesLength ) );

            // The partial attribute list
            if ( ( attributes != null ) && ( attributes.size() != 0 ) )
            {
                NamingEnumeration attributeIterator = attributes.getAll();
                int attributeNumber = 0;

                // Compute the attributes length
                while ( attributeIterator.hasMoreElements() )
                {
                    Attribute attribute = ( Attribute ) attributeIterator.nextElement();

                    // The attributes list sequence
                    buffer.put( UniversalTag.SEQUENCE_TAG );
                    int localAttributeLength = ( ( Integer ) attributeLength.get( attributeNumber ) ).intValue();
                    buffer.put( Length.getBytes( localAttributeLength ) );

                    // The attribute type
                    Value.encode( buffer, attribute.getID() );

                    // The values
                    buffer.put( UniversalTag.SET_TAG );
                    int localValuesLength = ( ( Integer ) valuesLength.get( attributeNumber ) ).intValue();
                    buffer.put( Length.getBytes( localValuesLength ) );

                    try
                    {
                        NamingEnumeration values = attribute.getAll();

                        if ( values.hasMoreElements() )
                        {
                            while ( values.hasMoreElements() )
                            {
                                Object value = values.next();

                                if ( value instanceof byte[] )
                                {
                                    Value.encode( buffer, ( byte[] ) value );
                                }
                                else
                                {
                                    Value.encode( buffer, ( String ) value );
                                }
                            }
                        }

                    }
                    catch ( NamingException ne )
                    {
                        throw new EncoderException( "Cannot enumerate the values" );
                    }

                    // Go to the next attribute number;
                    attributeNumber++;
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( log.isDebugEnabled() )
        {
            log.debug( "AddRequest encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "AddRequest initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * Return a String representing an AddRequest
     * 
     * @return A String representing the AddRequest
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Add Request\n" );
        sb.append( "        Entry : '" ).append( entry.toString() ).append( "'\n" );
        sb.append( "        Attributes\n" );

        if ( ( attributes == null ) || ( attributes.size() == 0 ) )
        {
            sb.append( "            No attributes\n" );
        }
        else
        {
            sb.append( AttributeUtils.toString( "            ", attributes ) );
        }

        return sb.toString();
    }


    /**
     * @return Returns the currentAttribute type.
     */
    public String getCurrentAttributeType()
    {
        return currentAttribute.getID();
    }
}
