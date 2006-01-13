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
package org.apache.ldap.common.codec.modify;

import org.apache.asn1.codec.EncoderException;
import org.apache.asn1.ber.tlv.Length;
import org.apache.asn1.ber.tlv.UniversalTag;
import org.apache.asn1.ber.tlv.Value;
import org.apache.ldap.common.codec.LdapConstants;
import org.apache.ldap.common.codec.LdapMessage;
import org.apache.ldap.common.codec.util.LdapString;
import org.apache.ldap.common.name.LdapDN;
import org.apache.ldap.common.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;


/**
 * A ModifyRequest Message. Its syntax is :
 *   ModifyRequest ::= [APPLICATION 6] SEQUENCE {
 *              object          LDAPDN,
 *              modification    SEQUENCE OF SEQUENCE {
 *                      operation       ENUMERATED {
 *                                              add     (0),
 *                                              delete  (1),
 *                                              replace (2) },
 *                      modification    AttributeTypeAndValues } }
 *
 *   AttributeTypeAndValues ::= SEQUENCE {
 *              type    AttributeDescription,
 *              vals    SET OF AttributeValue }
 * 
 *   AttributeValue ::= OCTET STRING
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ModifyRequest extends LdapMessage
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The logger */
    private static final transient Logger log = LoggerFactory.getLogger( ModifyRequest.class );

    //~ Instance fields ----------------------------------------------------------------------------

    /** The DN to be modified. */
    private Name object;

    /** The modifications list. This is an array of ModificationItem. */
    private ArrayList modifications;

    /** The current attribute being decoded */
    private transient Attribute currentAttribute;

    /** A local storage for the operation */
    private transient int currentOperation;

    /** The modify request length */
    private transient int modifyRequestLength;
    
    /** The modifications length */
    private transient int modificationsLength;
    
    /** The modification sequence length */
    private transient List modificationSequenceLength;
    
    /** The list of all modification length */
    private transient List modificationLength;
    
    /** The list of all vals length */
    private transient List valuesLength;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ModifyRequest object.
     */
    public ModifyRequest()
    {
        super( );
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Get the message type
     *
     * @return Returns the type.
     */
    public int getMessageType()
    {
        return LdapConstants.MODIFY_REQUEST;
    }

    /**
     * Initialize the ArrayList for modifications.
     */
    public void initModifications()
    {
        modifications = new ArrayList();
    }

    /**
     * Get the entry's attributes
     *
     * @return Returns the modifications.
     */
    public ArrayList getModifications()
    {
        return modifications;
    }

    /**
     * Add a new modification to the list
     *
     * @param operation The type of operation (add, delete or replace)
    */
    public void addModification( int operation )
    {
        currentOperation = operation;

        if ( currentAttribute == null )
        {
            modifications = new ArrayList();
        }
    }

    /**
     * Add a new attributeTypeAndValue
     * 
     * @param type The attribute's name
     */
    public void addAttributeTypeAndValues( LdapString type )
    {
        currentAttribute = new BasicAttribute( StringTools.lowerCase( type.getString() ) );

        int operation = 0;

        switch ( currentOperation )
        {

            case LdapConstants.OPERATION_ADD : // add
                operation = DirContext.ADD_ATTRIBUTE;
                break;

            case LdapConstants.OPERATION_DELETE : // delete
                operation = DirContext.REMOVE_ATTRIBUTE;
                break;

            case LdapConstants.OPERATION_REPLACE : // replace
                operation = DirContext.REPLACE_ATTRIBUTE;
                break;
        }

        ModificationItem modification = new ModificationItem( operation, currentAttribute );
        modifications.add( modification );
    }

    /**
     * Add a new value to the current attribute
     * 
     * @param value The value to add
     */
    public void addAttributeValue( Object value )
    {
        currentAttribute.add( value );
    }

    /**
     * Return the current attribute's type
     */
    public String getCurrentAttributeType()
    {
        return currentAttribute.getID();
    }

    /**
     * Get the modification's DN
     * 
     * @return Returns the object.
     */
    public String getObject()
    {
        return ( ( object == null ) ? "" : object.toString() );
    }

    /**
     * Set the modification DN.
     * 
     * @param object The DN to set.
     */
    public void setObject( Name object )
    {
        this.object = object;
    }

    /**
     * Get the current operation
     * 
     * @return Returns the currentOperation.
     */
    public int getCurrentOperation()
    {
        return currentOperation;
    }

    /**
     * Store the current operation
     * 
     * @param currentOperation The currentOperation to set.
     */
    public void setCurrentOperation( int currentOperation )
    {
        this.currentOperation = currentOperation;
    }

    /**
     * Compute the ModifyRequest length
     * 
     * ModifyRequest :
     * 
     * 0x66 L1
     *  |
     *  +--> 0x04 L2 object
     *  +--> 0x30 L3 modifications
     *        |
     *        +--> 0x30 L4-1 modification sequence
     *        |     |
     *        |     +--> 0x0A 0x01 (0..2) operation
     *        |     +--> 0x30 L5-1 modification
     *        |           |
     *        |           +--> 0x04 L6-1 type
     *        |           +--> 0x31 L7-1 vals
     *        |                 |
     *        |                 +--> 0x04 L8-1-1 attributeValue
     *        |                 +--> 0x04 L8-1-2 attributeValue
     *        |                 +--> ...
     *        |                 +--> 0x04 L8-1-i attributeValue
     *        |                 +--> ...
     *        |                 +--> 0x04 L8-1-n attributeValue
     *        |
     *        +--> 0x30 L4-2 modification sequence
     *        .     |
     *        .     +--> 0x0A 0x01 (0..2) operation
     *        .     +--> 0x30 L5-2 modification
     *                    |
     *                    +--> 0x04 L6-2 type
     *                    +--> 0x31 L7-2 vals
     *                          |
     *                          +--> 0x04 L8-2-1 attributeValue
     *                          +--> 0x04 L8-2-2 attributeValue
     *                          +--> ...
     *                          +--> 0x04 L8-2-i attributeValue
     *                          +--> ...
     *                          +--> 0x04 L8-2-n attributeValue
     * 
     * 
     */
    public int computeLength()
    {
        // Initialized with object
        modifyRequestLength = 1 + Length.getNbBytes( LdapDN.getNbBytes( object ) ) + LdapDN.getNbBytes( object );
        
        // Modifications
        modificationsLength = 0;
        
        if ( ( modifications != null ) && ( modifications.size() != 0 ) )
        {
            Iterator modificationsIterator = modifications.iterator();
            modificationSequenceLength = new LinkedList();
            modificationLength = new LinkedList();
            valuesLength = new LinkedList();
            
            while ( modificationsIterator.hasNext() )
            {
                // Modification sequence length initialized with the operation
                int localModificationSequenceLength = 1 + 1 + 1;
                int localValuesLength = 0;
                
                ModificationItem modification = (ModificationItem)modificationsIterator.next();
                
                // Modification length initialized with the type
                int typeLength = modification.getAttribute().getID().length();
                int localModificationLength = 1 + Length.getNbBytes( typeLength ) + typeLength;
                
                try
                {
                    
                    NamingEnumeration values = modification.getAttribute().getAll();

                    // Get all the values
                    if ( values.hasMoreElements() )
                    {
                        while ( values.hasMore() )
                        {
                            Object value = values.next();
                            
                            if ( value instanceof String )
                            {
                                int valueLength = StringTools.getBytesUtf8( (String)value ).length;
                                localValuesLength += 1 + Length.getNbBytes( valueLength ) + valueLength;
                            }
                            else
                            {
                                localValuesLength += 1 + Length.getNbBytes( ( (byte[])value).length ) + ( (byte[])value).length;
                            }
                        }
                    }
                    
                    localModificationLength += 1 + Length.getNbBytes( localValuesLength ) + localValuesLength;
                }
                catch (NamingException ne)
                {
                    continue;
                }
                
                // Compute the modificationSequenceLength
                localModificationSequenceLength += 1 + Length.getNbBytes( localModificationLength ) + localModificationLength;
                
                // Add the tag and the length
                modificationsLength += 1 + Length.getNbBytes( localModificationSequenceLength ) + localModificationSequenceLength;
                
                // Store the arrays of values
                valuesLength.add( new Integer( localValuesLength ) );
                modificationLength.add( new Integer( localModificationLength ) );
                modificationSequenceLength.add( new Integer( localModificationSequenceLength ) );
            }
            
            // Add the modifications length to the modificationRequestLength
            modifyRequestLength += 1 + Length.getNbBytes( modificationsLength ) + modificationsLength;
        }

        return 1 + Length.getNbBytes( modifyRequestLength ) + modifyRequestLength;
    }

    /**
     * Encode the ModifyRequest message to a PDU.
     * 
     * AddRequest :
     * 
     * 0x66 LL
     *   0x04 LL object
     *   0x30 LL modifiations
     *     0x30 LL modification sequence
     *       0x0A 0x01 operation
     *       0x30 LL modification
     *         0x04 LL type
     *         0x31 LL vals
     *           0x04 LL attributeValue
     *           ... 
     *           0x04 LL attributeValue
     *     ... 
     *     0x30 LL modification sequence
     *       0x0A 0x01 operation
     *       0x30 LL modification
     *         0x04 LL type
     *         0x31 LL vals
     *           0x04 LL attributeValue
     *           ... 
     *           0x04 LL attributeValue
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
            // The AddRequest Tag
            buffer.put( LdapConstants.MODIFY_REQUEST_TAG );
            buffer.put( Length.getBytes( modifyRequestLength ) ) ;
            
            // The entry
            Value.encode( buffer, LdapDN.getBytes( object ) );
            
            // The modifications sequence
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( Length.getBytes( modificationsLength ) ) ;

            // The modifications list
            if ( ( modifications != null ) && ( modifications.size() != 0 ) )
            {
                Iterator modificationIterator = modifications.iterator();
                int modificationNumber = 0;
                
                // Compute the modifications length
                while ( modificationIterator.hasNext() )
                {
                    ModificationItem modification = (ModificationItem)modificationIterator.next();
                    
                    // The modification sequence
                    buffer.put( UniversalTag.SEQUENCE_TAG );
                    int localModificationSequenceLength = ( (Integer)modificationSequenceLength.get( modificationNumber ) ).intValue();
                    buffer.put( Length.getBytes( localModificationSequenceLength ) );

                    // The operation. The value has to be changed, it's not
                    // the same value in DirContext and in RFC 2251.
                    buffer.put( UniversalTag.ENUMERATED_TAG );
                    buffer.put( (byte)1 );
                    
                    switch ( modification.getModificationOp() )
                    {

                        case DirContext.ADD_ATTRIBUTE : // add
                            buffer.put( (byte)LdapConstants.OPERATION_ADD );
                            break;

                        case DirContext.REMOVE_ATTRIBUTE : // delete
                            buffer.put( (byte)LdapConstants.OPERATION_DELETE );
                            break;

                        case DirContext.REPLACE_ATTRIBUTE : // replace
                            buffer.put( (byte)LdapConstants.OPERATION_REPLACE );
                            break;
                    }

                    // The modification
                    buffer.put( UniversalTag.SEQUENCE_TAG );
                    int localModificationLength = ( (Integer)modificationLength.get( modificationNumber ) ).intValue();
                    buffer.put( Length.getBytes( localModificationLength ) );
                    
                    // The modification type
                    Value.encode( buffer, modification.getAttribute().getID() );
                    
                    // The values
                    buffer.put( UniversalTag.SET_TAG );
                    int localValuesLength = ( (Integer)valuesLength.get( modificationNumber ) ).intValue();
                    buffer.put( Length.getBytes( localValuesLength ) );
                    
                    try
                    {
                        NamingEnumeration values = modification.getAttribute().getAll();
                        
                        if ( values.hasMoreElements() )
                        {
                            while ( values.hasMoreElements() )
                            {
                                Object value = values.next();
                                
                                if ( value instanceof String )
                                {
                                    Value.encode( buffer, (String)value );
                                }
                                else
                                {
                                    Value.encode( buffer, (byte[])value );
                                }
                            }
                        }
                        
                    }
                    catch (NamingException ne)
                    {
                        throw new EncoderException("Cannot enumerate the values");
                    }
                    
                    // Go to the next modification number;
                    modificationNumber++;
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException("The PDU buffer size is too small !"); 
        }

        return buffer;
    }

    /**
     * Get a String representation of a ModifyRequest
     *
     * @return A ModifyRequest String 
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Modify Request\n" );
        sb.append( "        Object : '" ).append( object ).append( "'\n" );

        if ( modifications != null )
        {

            for ( int i = 0; i < modifications.size(); i++ )
            {

                ModificationItem modification = ( ModificationItem ) modifications.get( i );

                sb.append( "            Modification[" ).append( i ).append( "]\n" );
                sb.append( "                Operation : " );

                switch ( modification.getModificationOp() )
                {

                    case DirContext.ADD_ATTRIBUTE :
                        sb.append( " add\n" );
                        break;

                    case DirContext.REPLACE_ATTRIBUTE :
                        sb.append( " replace\n" );
                        break;

                    case DirContext.REMOVE_ATTRIBUTE :
                        sb.append( " delete\n" );
                        break;
                }

                sb.append( "                Modification\n" );

                Attribute attribute = modification.getAttribute();

                try
                {
                    sb.append( "                    Type : '" ).append( attribute.getID() ).append(
                        "'\n" );
                    sb.append( "                    Vals\n" );

                    for ( int j = 0; j < attribute.size(); j++ )
                    {


                        Object attributeValue = attribute.get( j );
                        sb.append( "                        Val[" ).append( j ).append( "] : '" );
                        
                        if ( attributeValue instanceof String )
                        {
                            sb.append( attributeValue ).append( "' \n" );
                        }
                        else
                        {
                            sb.append( StringTools.utf8ToString( (byte[])attributeValue ) ).append( "' \n" );
                        }
                    }
                }
                catch ( NamingException ne )
                {
                    log.error( "Naming exception while printing the '" + attribute.getID() +
                        "'" );
                }
            }
        }

        return sb.toString();
    }
}
