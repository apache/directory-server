/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.entry;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientBinaryValue;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A server side entry attribute aware of schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class DefaultServerAttribute extends DefaultClientAttribute implements ServerAttribute
{
    public static final long serialVersionUID = 1L;
    
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultServerAttribute.class );
    
    /** The associated AttributeType */
    private AttributeType attributeType;
    
    
    //-----------------------------------------------------------------------
    // utility methods
    //-----------------------------------------------------------------------
    /**
     * Private helper method used to set an UpId from an attributeType
     * 
     * @param at The attributeType for which we want the upID
     * @return the ID of the given attributeType
     */
    private String getUpId( AttributeType at )
    {
        String atUpId = at.getName();
        
        if ( atUpId == null )
        {
            atUpId = at.getOid();
        }
        
        return atUpId;
    }
    
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------
    /**
     * 
     * Creates a new instance of DefaultServerAttribute, by copying
     * another attribute, which can be a ClientAttribute. If the other
     * attribute is a ServerAttribute, it will be copied.
     *
     * @param attributeType The attribute's type 
     * @param attribute The attribute to be copied
     */
    public DefaultServerAttribute( AttributeType attributeType, EntryAttribute attribute )
    {
        // Copy the common values. isHR is only available on a ServerAttribute 
        this.attributeType = attributeType;
        this.id = attribute.getId();
        this.upId = attribute.getUpId();

        if ( attribute instanceof ServerAttribute )
        {
            isHR = attribute.isHR();

            // Copy all the values
            for ( Value<?> value:attribute )
            {
                add( value.clone() );
            }
        }
        else
        {
            
            isHR = attributeType.getSyntax().isHumanReadable();

            // Copy all the values
            for ( Value<?> clientValue:attribute )
            {
                Value<?> serverValue = null; 

                // We have to convert the value first
                if ( clientValue instanceof ClientStringValue )
                {
                    if ( isHR )
                    {
                        serverValue = new ServerStringValue( attributeType, clientValue.getString() );
                    }
                    else
                    {
                        // We have to convert the value to a binary value first
                        serverValue = new ServerBinaryValue( attributeType, 
                            clientValue.getBytes() );
                    }
                }
                else if ( clientValue instanceof ClientBinaryValue )
                {
                    if ( isHR )
                    {
                        // We have to convert the value to a String value first
                        serverValue = new ServerStringValue( attributeType, 
                            clientValue.getString() );
                    }
                    else
                    {
                        serverValue = new ServerBinaryValue( attributeType, clientValue.getBytes() );
                    }
                }

                add( serverValue );
            }
        }
    }
    
    
    // maybe have some additional convenience constructors which take
    // an initial value as a string or a byte[]
    /**
     * Create a new instance of a EntryAttribute, without ID nor value.
     * 
     * @param attributeType the attributeType for the empty attribute added into the entry
     */
    public DefaultServerAttribute( AttributeType attributeType )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( "The AttributeType parameter should not be null" );
        }
        
        setAttributeType( attributeType );
    }


    /**
     * Create a new instance of a EntryAttribute, without value.
     * 
     * @param upId the ID for the added attributeType
     * @param attributeType the added AttributeType
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType )
    {
        if ( attributeType == null ) 
        {
            String message = "The AttributeType parameter should not be null";
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        setAttributeType( attributeType );
        setUpId( upId );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new Value which uses the specified
     * attributeType.
     *
     * @param attributeType the attribute type according to the schema
     * @param vals an initial set of values for this attribute
     */
    public DefaultServerAttribute( AttributeType attributeType, Value<?>... vals )
    {
        this( null, attributeType, vals );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new Value which uses the specified
     * attributeType.
     * 
     * Otherwise, the value is stored, but as a reference. It's not a copy.
     *
     * @param upId the ID of the added attribute
     * @param attributeType the attribute type according to the schema
     * @param vals an initial set of values for this attribute
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType, Value<?>... vals )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( "The AttributeType parameter should not be null" );
        }
        
        setAttributeType( attributeType );
        setUpId( upId, attributeType );
        add( vals );
    }


    /**
     * Create a new instance of a EntryAttribute, without ID but with some values.
     * 
     * @param attributeType The attributeType added on creation
     * @param vals The added value for this attribute
     */
    public DefaultServerAttribute( AttributeType attributeType, String... vals )
    {
        this( null, attributeType, vals );
    }


    /**
     * Create a new instance of a EntryAttribute.
     * 
     * @param upId the ID for the added attribute
     * @param attributeType The attributeType added on creation
     * @param vals the added values for this attribute
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType, String... vals )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( "The AttributeType parameter should not be null" );
        }

        setAttributeType( attributeType );
        add( vals );
        setUpId( upId, attributeType );
    }


    /**
     * Create a new instance of a EntryAttribute, with some byte[] values.
     * 
     * @param attributeType The attributeType added on creation
     * @param vals The value for the added attribute
     */
    public DefaultServerAttribute( AttributeType attributeType, byte[]... vals )
    {
        this( null, attributeType, vals );
    }


    /**
     * Create a new instance of a EntryAttribute, with some byte[] values.
     * 
     * @param upId the ID for the added attribute
     * @param attributeType the AttributeType to be added
     * @param vals the values for the added attribute
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType, byte[]... vals )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( "The AttributeType parameter should not be null" );
        }

        setAttributeType( attributeType );
        add( vals );
        setUpId( upId, attributeType );
    }
    
    
    //-------------------------------------------------------------------------
    // API
    //-------------------------------------------------------------------------
    /**
     * <p>
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * </p>
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     * <p>
     * If the value's type is different from the attribute's type,
     * the value is not added.
     * </p>
     * It's the responsibility of the caller to check if the stored
     * values are consistent with the attribute's type.
     * <p>
     *
     * @param vals some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    public int add( byte[]... vals )
    {
        if ( !isHR )
        {
            int nbAdded = 0;
            
            for ( byte[] val:vals )
            {
                Value<?> value = new ServerBinaryValue( attributeType, val );
                
                try
                {
                    value.normalize();
                }
                catch( NamingException ne )
                {
                    // The value can't be normalized : we don't add it.
                    LOG.error( "The value '" + val + "' can't be normalized, it hasn't been added" );
                    return 0;
                }
                
                if ( add( value ) != 0 )
                {
                    nbAdded++;
                }
                else
                {
                    LOG.error( "The value '" + val + "' is incorrect, it hasn't been added" );
                }
            }
            
            return nbAdded;
        }
        else
        {
            // We can't add Binary values into a String serverAttribute
            return 0;
        }
    }    


    /**
     * <p>
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * </p>
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     * If the value's type is different from the attribute's type,
     * the value is not added.
     *
     * @param vals some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    public int add( String... vals )
    {
        if ( isHR )
        {
            int nbAdded = 0;
            
            for ( String val:vals )
            {
                Value<String> newValue = new ServerStringValue( attributeType, val );
                
                if ( !contains( newValue ) )
                {
                    if ( add( newValue ) != 0 )
                    {
                        nbAdded++;
                    }
                    else
                    {
                        LOG.error( "The value '" + val + "' is incorrect, it hasn't been added" );
                    }
                }
            }
            
            return nbAdded;
        }
        else
        {
            // We can't add String values into a Binary serverAttribute
            return 0;
        }
    }    


    /**
     * @see EntryAttribute#add(org.apache.directory.shared.ldap.entry.Value...)
     * 
     * @return the number of added values into this attribute
     */
    public int add( Value<?>... vals )
    {
        int nbAdded = 0;
        
        for ( Value<?> val:vals )
        {
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                if ( ( val == null ) || val.isNull() )
                {
                    Value<String> nullSV = new ServerStringValue( attributeType, (String)null );
                    
                    if ( !values.contains( nullSV ) )
                    {
                        values.add( nullSV );
                        nbAdded++;
                    }
                }
                else if ( val instanceof ServerStringValue )
                {
                    if ( !values.contains( val ) )
                    {
                        if ( values.add( val ) )
                        {
                            nbAdded++;
                        }
                    }
                }
                else if ( val instanceof ClientStringValue )
                {
                    // If we get a Client value, convert it to a Server value first 
                    Value<String> serverStringValue = new ServerStringValue( attributeType, val.getString() ); 
                    
                    if ( !values.contains( serverStringValue ) )
                    {
                        if ( values.add( serverStringValue ) )
                        {
                            nbAdded++;
                        }
                    }
                }
                else
                {
                    String message = "The value must be a String, as its AttributeType is H/R";
                    LOG.error( message );
                }
            }
            else
            {
                if ( val == null )
                {
                    Value<byte[]> nullSV = new ServerBinaryValue( attributeType, (byte[])null );
                    
                    if ( !values.contains( nullSV ) )
                    {
                        values.add( nullSV );
                        nbAdded++;
                    }
                }
                else if ( ( val instanceof ClientBinaryValue ) )
                {
                    Value<byte[]> serverBinaryValue = new ServerBinaryValue( attributeType, val.getBytes() ); 
                    
                    if ( !values.contains( serverBinaryValue ) )
                    {
                        if ( values.add( serverBinaryValue ) )
                        {
                            nbAdded++;
                        }
                    }
                }
                else if ( val instanceof ServerBinaryValue )
                {
                    if ( !values.contains( val ) )
                    {
                        if ( values.add( val ) )
                        {
                            nbAdded++;
                        }
                    }
                }
                else
                {
                    String message = "The value must be a byte[], as its AttributeType is not H/R";
                    LOG.error( message );
                }
            }
        }
        
        return nbAdded;
    }


    /**
     * Remove all the values from this attribute type, including a 
     * null value. 
     */
    public void clear()
    {
        values.clear();
    }


    /**
     * <p>
     * Indicates whether all the specified values are attribute's values. If
     * at least one value is not an attribute's value, this method will return 
     * <code>false</code>
     * </p>
     * <p>
     * If the Attribute is HR, this method will returns <code>false</code>
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    public boolean contains( byte[]... vals )
    {
        if ( !isHR )
        {
            // Iterate through all the values, and quit if we 
            // don't find one in the values
            for ( byte[] val:vals )
            {
                ServerBinaryValue value = new ServerBinaryValue( attributeType, val );
                
                try
                {
                    value.normalize();
                }
                catch ( NamingException ne )
                {
                    return false;
                }
                
                if ( !values.contains( value ) )
                {
                    return false;
                }
            }
            
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * <p>
     * Indicates whether all the specified values are attribute's values. If
     * at least one value is not an attribute's value, this method will return 
     * <code>false</code>
     * </p>
     * <p>
     * If the Attribute is not HR, this method will returns <code>false</code>
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    public boolean contains( String... vals )
    {
        if ( isHR )
        {
            // Iterate through all the values, and quit if we 
            // don't find one in the values
            for ( String val:vals )
            {
                ServerStringValue value = new ServerStringValue( attributeType, val );
                
                if ( !values.contains( value ) )
                {
                    return false;
                }
            }
            
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * <p>
     * Indicates whether the specified values are some of the attribute's values.
     * </p>
     * <p>
     * If the Attribute is HR, te metho will only accept String Values. Otherwise, 
     * it will only accept Binary values.
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    public boolean contains( Value<?>... vals )
    {
        // Iterate through all the values, and quit if we 
        // don't find one in the values. We have to separate the check
        // depending on the isHR flag value.
        if ( isHR )
        {
            for ( Value<?> val:vals )
            {
                if ( val instanceof ServerStringValue )
                {
                    if ( !values.contains( val ) )
                    {
                        return false;
                    }
                }
                else if ( val instanceof ClientStringValue )
                {
                    ServerStringValue serverValue = new ServerStringValue( attributeType, val.isNull() ? (String)null : val.getString() );
                    
                    if ( !values.contains( serverValue ) )
                    {
                        return false;
                    }
                }
                else
                {
                    // Not a String value
                    return false;
                }
            }
        }
        else
        {
            for ( Value<?> val:vals )
            {
                if ( val instanceof ClientBinaryValue )
                {
                    if ( !values.contains( val ) )
                    {
                        return false;
                    }
                }
                else
                {
                    // Not a Binary value
                    return false;
                }
            }
        }
        
        return true;
    }


    /**
     * Get the attribute type associated with this ServerAttribute.
     *
     * @return the attributeType associated with this entry attribute
     */
    public AttributeType getAttributeType()
    {
        return attributeType;
    }
    
    
    /**
     * <p>
     * Check if the current attribute type is of the expected attributeType
     * </p>
     * <p>
     * This method won't tell if the current attribute is a descendant of 
     * the attributeType. For instance, the "CN" serverAttribute will return
     * false if we ask if it's an instance of "Name". 
     * </p> 
     *
     * @param attributeId The AttributeType ID to check
     * @return True if the current attribute is of the expected attributeType
     * @throws InvalidAttributeValueException If there is no AttributeType
     */
    public boolean instanceOf( String attributeId ) throws InvalidAttributeValueException
    {
        String trimmedId = StringTools.trim( attributeId );
        
        if ( StringTools.isEmpty( trimmedId ) )
        {
            return false;
        }
        
        String normId = StringTools.lowerCaseAscii( trimmedId );
        
        for ( String name:attributeType.getNames() )
        {
            if ( normId.equalsIgnoreCase( name ) )
            {
                return true;
            }
        }
        
        return normId.equalsIgnoreCase( attributeType.getOid() );
    }
    

    /**
     * <p>
     * Checks to see if this attribute is valid along with the values it contains.
     * </p>
     * <p>
     * An attribute is valid if :
     * <li>All of its values are valid with respect to the attributeType's syntax checker</li>
     * <li>If the attributeType is SINGLE-VALUE, then no more than a value should be present</li>
     *</p>
     * @return true if the attribute and it's values are valid, false otherwise
     * @throws NamingException if there is a failure to check syntaxes of values
     */
    public boolean isValid() throws NamingException
    {
        // First check if the attribute has more than one value
        // if the attribute is supposed to be SINGLE_VALUE
        if ( attributeType.isSingleValued() && ( values.size() > 1 ) )
        {
            return false;
        }

        // Check that we can have no value for this attributeType
        if ( values.size() == 0 )
        {
            return attributeType.getSyntax().getSyntaxChecker().isValidSyntax( null );
        }

        for ( Value<?> value : values )
        {
            if ( ! value.isValid() )
            {
                return false;
            }
        }
        
        return true;
    }


    /**
     * @see EntryAttribute#remove(byte[]...)
     * 
     * @return <code>true</code> if all the values shave been removed from this attribute
     */
    public boolean remove( byte[]... vals )
    {
        if ( isHR ) 
        {
            return false;
        }
        
        boolean removed = true;
        
        for ( byte[] val:vals )
        {
            ServerBinaryValue value = new ServerBinaryValue( attributeType, val );
            removed &= values.remove( value );
        }
        
        return removed;
    }


    /**
     * @see EntryAttribute#remove(String...)
     * 
     * @return <code>true</code> if all the values shave been removed from this attribute
     */
    public boolean remove( String... vals )
    {
        if ( !isHR )
        {
            return false;
        }
        
        boolean removed = true;
        
        for ( String val:vals )
        {
            ServerStringValue value = new ServerStringValue( attributeType, val );
            removed &= values.remove( value );
        }
        
        return removed;
    }


    /**
     * @see EntryAttribute#remove(org.apache.directory.shared.ldap.entry.Value...)
     * 
     * @return <code>true</code> if all the values shave been removed from this attribute
     */
    public boolean remove( Value<?>... vals )
    {
        boolean removed = true;
        
        // Loop through all the values to remove. If one of
        // them is not present, the method will return false.
        // As the attribute may be HR or not, we have two separated treatments
        if ( isHR )
        {
            for ( Value<?> val:vals )
            {
                if ( val instanceof ClientStringValue )
                {
                    ServerStringValue ssv = new ServerStringValue( attributeType, (String)val.get() );
                    removed &= values.remove( ssv );
                }
                else if ( val instanceof ServerStringValue )
                {
                    removed &= values.remove( val );
                }
                else
                {
                    removed = false;
                }
            }
        }
        else
        {
            for ( Value<?> val:vals )
            {
                if ( val instanceof ClientBinaryValue )
                {
                    ServerBinaryValue sbv = new ServerBinaryValue( attributeType, (byte[])val.get() );
                    removed &= values.remove( sbv );
                }
                else if ( val instanceof ServerBinaryValue )
                {
                    removed &= values.remove( val );
                }
                else
                {
                    removed = false;
                }
            }
        }
        
        return removed;
    }


    
    /**
     * <p>
     * Set the attribute type associated with this ServerAttribute.
     * </p>
     * <p>
     * The current attributeType will be replaced. It is the responsibility of
     * the caller to insure that the existing values are compatible with the new
     * AttributeType
     * </p>
     *
     * @param attributeType the attributeType associated with this entry attribute
     */
    public void setAttributeType( AttributeType attributeType )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( "The AttributeType parameter should not be null" );
        }

        this.attributeType = attributeType;
        setUpId( null, attributeType );
        
        if ( attributeType.getSyntax().isHumanReadable() )
        {
            isHR = true;
        }
        else
        {
            isHR = false;
        }
    }
    
    
    /**
     * <p>
     * Overload the ClientAttribte isHR method : we can't change this flag
     * for a ServerAttribute, as the HR is already set using the AttributeType.
     * Set the attribute to Human Readable or to Binary. 
     * </p>
     * 
     * @param isHR <code>true</code> for a Human Readable attribute, 
     * <code>false</code> for a Binary attribute.
     */
    public void setHR( boolean isHR )
    {
        // Do nothing...
    }

    
    /**
     * <p>
     * Overload the {@link DefaultClientAttribute#setId(String)} method.
     * </p>
     * <p>
     * As the attributeType has already been set, we have to be sure that the 
     * argument is compatible with the attributeType's name. 
     * </p>
     * <p>
     * If the given ID is not compatible with the attributeType's possible
     * names, the previously loaded ID will be kept.
     * </p>
     *
     * @param id The attribute ID
     */
    public void setId( String id )
    {
        if ( !StringTools.isEmpty( StringTools.trim( id  ) ) )
        {
            if ( attributeType.getName() == null )
            {
                // If the name is null, then we may have to store an OID
                if ( OID.isOID( id )  && attributeType.getOid().equals( id ) )
                {
                    // Everything is fine, store the upId.
                    // This should not happen...
                    super.setId( id );
                }
            }
            else
            {
                // We have at least one name. Check that the normalized upId
                // is one of those names. Otherwise, the upId may be an OID too.
                // In this case, it must be equals to the attributeType OID.
                String normId = StringTools.lowerCaseAscii( StringTools.trim( id ) );
                
                for ( String atName:attributeType.getNames() )
                {
                    if ( atName.equalsIgnoreCase( normId ) )
                    {
                        // Found ! We can store the upId and get out
                        super.setId( normId );
                        return;
                    }
                }
                
                // Last case, the UpId is an OID
                if ( OID.isOID( normId ) && attributeType.getOid().equals( normId ) )
                {
                    // We have an OID : stores it
                    super.setUpId( normId );
                }
                else
                {
                    // The id is incorrect : this is not allowed 
                    throw new IllegalArgumentException( "The ID '" + id + "'is incompatible with the AttributeType's id '" + 
                        attributeType.getName() + "'" );
                }
            }
        }
        else
        {
            throw new IllegalArgumentException( "An ID cannnot be null, empty, or resolved to an emtpy" +
            " value when trimmed" );
        }
    }
    
    
    /**
     * <p>
     * Overload the {@link DefaultClientAttribute#setUpId(String)} method.
     * </p>
     * <p>
     * As the attributeType has already been set, we have to be sure that the 
     * argument is compatible with the attributeType's name. 
     * </p>
     * <p>
     * If the given ID is not compatible with the attributeType's possible
     * names, the previously loaded ID will be kept.
     * </p>
     *
     * @param upId The attribute ID
     */
    public void setUpId( String upId )
    {
        if ( !StringTools.isEmpty( StringTools.trim( upId  ) ) )
        {
            if ( attributeType.getName() == null )
            {
                // If the name is null, then we may have to store an OID
                if ( OID.isOID( upId )  && attributeType.getOid().equals( upId ) )
                {
                    // Everything is fine, store the upId.
                    // This should not happen...
                    super.setUpId( upId );
                    
                }
            }
            else
            {
                // We have at least one name. Check that the normalized upId
                // is one of those names. Otherwise, the upId may be an OID too.
                // In this case, it must be equals to the attributeType OID.
                String normUpId = StringTools.lowerCaseAscii( StringTools.trim( upId ) );
                
                for ( String atId:attributeType.getNames() )
                {
                    if ( atId.equalsIgnoreCase( normUpId ) )
                    {
                        // Found ! We can store the upId and get out
                        super.setUpId( upId );
                        return;
                    }
                }
                
                // Last case, the UpId is an OID
                if ( OID.isOID( normUpId ) && attributeType.getOid().equals( normUpId ) )
                {
                    // We have an OID : stores it
                    super.setUpId( upId );
                }
            }
        }
    }
    
    
    /**
     * <p>
     * Set the user provided ID. If we have none, the upId is assigned
     * the attributetype's name. If it does not have any name, we will
     * use the OID.
     * </p>
     * <p>
     * If we have an upId and an AttributeType, they must be compatible. :
     *  - if the upId is an OID, it must be the AttributeType's OID
     *  - otherwise, its normalized form must be equals to ones of
     *  the attributeType's names.
     * </p>
     * <p>
     * In any case, the ATtributeType will be changed. The caller is responsible for
     * the present values to be compatoble with the new AttributeType.
     * </p>
     *
     * @param upId The attribute ID
     * @param attributeType The associated attributeType
     */
    public void setUpId( String upId, AttributeType attributeType )
    {
        if ( StringTools.isEmpty( StringTools.trim( upId  ) ) )
        {
            super.setUpId( getUpId( attributeType ) );
            this.attributeType = attributeType;
        }
        else
        {
            String name = attributeType.getName();
            
            if ( name == null )
            {
                // If the name is null, then we may have to store an OID
                if ( OID.isOID( upId )  && attributeType.getOid().equals( upId ) )
                {
                    //  Everything is fine, store the upId. 
                    super.setUpId( upId );
                    this.attributeType = attributeType;
                }
                else
                {
                    // We have a difference or the upId is not a valid OID :
                    // we will use the attributeTypeOID in this case.
                    LOG.warn( "The upID ({}) is not an OID or is different from the AttributeType OID({})",
                        upId, attributeType.getOid() );
                    super.setUpId( attributeType.getOid() );
                    this.attributeType = attributeType;
                }
            }
            else
            {
                // We have at least one name. Check that the normalized upId
                // is one of those names. Otherwise, the upId may be an OID too.
                // In this case, it must be equals to the attributeType OID.
                String normUpId = StringTools.lowerCaseAscii( StringTools.trim( upId ) );
                
                for ( String atId:attributeType.getNames() )
                {
                    if ( atId.equalsIgnoreCase( normUpId ) )
                    {
                        // Found ! We can store the upId and get out
                        super.setUpId( upId );
                        this.attributeType = attributeType;
                        return;
                    }
                }
    
                // UpId was not found in names. It should be an OID, or if not, we 
                // will use the AttributeType name.
                if ( OID.isOID( normUpId ) && attributeType.getOid().equals( normUpId ) )
                {
                    // We have an OID : stores it
                    super.setUpId( upId );
                    this.attributeType = attributeType;
                }
                else
                {
                    String message = "The upID (" + upId + ") is not an OID or is different from the AttributeType OID (" + 
                                        attributeType.getOid() + ")";
                    // Not a valid OID : use the AttributeTypes OID name instead
                    LOG.error( message );
                    throw new IllegalArgumentException( message );
                }
            }
        }
    }


    /**
     * Convert the ServerAttribute to a ClientAttribute
     *
     * @return An instance of ClientAttribute
     */
    public EntryAttribute toClientAttribute()
    {
        // Create the new EntryAttribute
        EntryAttribute clientAttribute = new DefaultClientAttribute( upId );
        
        // Copy the values
        for ( Value<?> value:this )
        {
            Value<?> clientValue = null;
            
            if ( value instanceof ServerStringValue )
            {
                clientValue = new ClientStringValue( value.getString() );
            }
            else
            {
                clientValue = new ClientBinaryValue( value.getBytes() );
            }
            
            clientAttribute.add( clientValue );
        }
        
        return clientAttribute;
    }


    //-------------------------------------------------------------------------
    // Serialization methods
    //-------------------------------------------------------------------------
    
    /**
     * @see java.io.Externalizable#writeExternal(ObjectOutput)
     * 
     * We can't use this method for a ServerAttribute, as we have to feed the value
     * with an AttributeType object
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        throw new IllegalStateException( "Cannot use standard serialization for a ServerAttribute" );
    }
    
    
    /**
     * @see Externalizable#writeExternal(ObjectOutput)
     * <p>
     * 
     * This is the place where we serialize attributes, and all theirs
     * elements. 
     * 
     * The inner structure is the same as the client attribute, but we can't call
     * it as we won't be able to serialize the serverValues
     * 
     */
    public void serialize( ObjectOutput out ) throws IOException
    {
        // Write the UPId (the id will be deduced from the upID)
        out.writeUTF( upId );
        
        // Write the HR flag, if not null
        if ( isHR != null )
        {
            out.writeBoolean( true );
            out.writeBoolean( isHR );
        }
        else
        {
            out.writeBoolean( false );
        }
        
        // Write the number of values
        out.writeInt( size() );
        
        if ( size() > 0 ) 
        {
            // Write each value
            for ( Value<?> value:values )
            {
                // Write the value, using the correct method
                if ( value instanceof ServerStringValue )
                {
                    ((ServerStringValue)value).serialize( out );
                }
                else
                {
                    ((ServerBinaryValue)value).serialize( out );
                }
            }
        }
    }

    
    /**
     * @see java.io.Externalizable#readExternal(ObjectInput)
     * 
     * We can't use this method for a ServerAttribute, as we have to feed the value
     * with an AttributeType object
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        throw new IllegalStateException( "Cannot use standard serialization for a ServerAttribute" );
    }
    
    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     */
    public void deserialize( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the ID and the UPId
        upId = in.readUTF();
        
        // Compute the id
        setUpId( upId );
        
        // Read the HR flag, if not null
        if ( in.readBoolean() )
        {
            isHR = in.readBoolean();
        }

        // Read the number of values
        int nbValues = in.readInt();

        if ( nbValues > 0 )
        {
            for ( int i = 0; i < nbValues; i++ )
            {
                Value<?> value = null;
                
                if ( isHR )
                {
                    value  = new ServerStringValue( attributeType );
                    ((ServerStringValue)value).deserialize( in );
                }
                else
                {
                    value  = new ServerBinaryValue( attributeType );
                    ((ServerBinaryValue)value).deserialize( in );
                }
                
                try
                {
                    value.normalize();
                }
                catch ( NamingException ne )
                {
                    // Do nothing...
                }
                    
                values.add( value );
            }
        }
    }
    
    
    //-------------------------------------------------------------------------
    // Overloaded Object class methods
    //-------------------------------------------------------------------------
    /**
     * Clone an attribute. All the element are duplicated, so a modification on
     * the original object won't affect the cloned object, as a modification
     * on the cloned object has no impact on the original object
     * 
     * @return a clone of the current attribute
     */
    public ServerAttribute clone()
    {
        // clone the structure by cloner the inherited class
        ServerAttribute clone = (ServerAttribute)super.clone();
        
        // We are done !
        return clone;
    }


    /**
     * @see Object#equals(Object)
     * 
     * @return <code>true</code> if the two objects are equal
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }
        
        if ( ! (obj instanceof ServerAttribute ) )
        {
            return false;
        }
        
        ServerAttribute other = (ServerAttribute)obj;
        
        if ( !attributeType.equals( other.getAttributeType() ) )
        {
            return false;
        }
        
        if ( values.size() != other.size() )
        {
            return false;
        }
        
        for ( Value<?> val:values )
        {
            if ( ! other.contains( val ) )
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * The hashCode is based on the id, the isHR flag and 
     * on the internal values.
     *  
     * @see Object#hashCode()
     * 
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int h = super.hashCode();
        
        if ( attributeType != null )
        {
            h = h*17 + attributeType.hashCode();
        }
        
        return h;
    }
    
    
    /**
     * @see Object#toString()
     * 
     * @return A String representation of this instance
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        if ( ( values != null ) && ( values.size() != 0 ) )
        {
            for ( Value<?> value:values )
            {
                sb.append( "    " ).append( upId ).append( ": " );
                
                if ( value.isNull() )
                {
                    sb.append( "''" );
                }
                else
                {
                    sb.append( value );
                }
                
                sb.append( '\n' );
            }
        }
        else
        {
            sb.append( "    " ).append( upId ).append( ": (null)\n" );
        }
        
        return sb.toString();
    }
}
