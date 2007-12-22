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


import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import java.util.Iterator;


/**
 * A server side entry attribute aware of schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class DefaultServerAttribute extends AbstractServerAttribute
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultServerAttribute.class );

    // maybe have some additional convenience constructors which take
    // an initial value as a string or a byte[]
    /**
     * Create a new instance of a EntryAttribute, without ID nor value.
     */
    public DefaultServerAttribute( AttributeType attributeType )
    {
        assert checkAttributeType( attributeType) == null : logAssert( checkAttributeType( attributeType ) );
        
        this.attributeType = attributeType;
        setUpId( null, attributeType );
    }


    /**
     * Create a new instance of a EntryAttribute, without value.
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType )
    {
        assert checkAttributeType( attributeType) == null : logAssert( checkAttributeType( attributeType ) );

        this.attributeType = attributeType;
        setUpId( upId, attributeType );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ServerValue which uses the specified
     * attributeType.
     *
     * @param attributeType the attribute type according to the schema
     * @param val an initial value for this attribute
     * @throws NamingException if there are problems creating the new attribute
     */
    public DefaultServerAttribute( AttributeType attributeType, ServerValue<?> val ) throws NamingException
    {
        this( null, attributeType, val );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ServerValue which uses the specified
     * attributeType.
     * 
     * Otherwise, the value is stored, but as a reference. It's not a copy.
     *
     * @param upId
     * @param attributeType the attribute type according to the schema
     * @param val an initial value for this attribute
     * @throws NamingException if there are problems creating the new attribute
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType, ServerValue<?> val ) throws NamingException
    {
        assert checkAttributeType( attributeType) == null : logAssert( checkAttributeType( attributeType ) );
        
        this.attributeType = attributeType;
        
        // The value can be null, this is a valid value.
        if ( val == null )
        {
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                add( new ServerStringValue( attributeType ) );
            }
            else
            {
                add( new ServerBinaryValue( attributeType ) );
            }
        }
        else
        {
            if ( attributeType.equals( val.getAttributeType() ) )
            {
                add( val );
            }
            else if ( val instanceof ServerStringValue )
            {
                ServerStringValue serverString = ( ServerStringValue ) val;
                add( new ServerStringValue( attributeType, serverString.get() ) );
            }
            else if ( val instanceof ServerBinaryValue )
            {
                ServerBinaryValue serverBinary = ( ServerBinaryValue ) val;
                add( new ServerBinaryValue( attributeType, serverBinary.getCopy() ) );
            }
            else
            {
                String message = "Unknown value type: " + val.getClass().getName();
                LOG.error( message );
                throw new IllegalStateException( message );
            }
        }
        
        setUpId( upId, attributeType );
    }


    /**
     * Create a new instance of a EntryAttribute, withoiut ID but with a value.
     */
    public DefaultServerAttribute( AttributeType attributeType, String val ) throws NamingException
    {
        this( null, attributeType, val );
    }


    /**
     * Create a new instance of a EntryAttribute.
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType, String val ) throws NamingException
    {
        assert checkAttributeType( attributeType) == null : logAssert( checkAttributeType( attributeType ) );

        this.attributeType = attributeType;
        add( val );
        setUpId( upId, attributeType );
    }


    /**
     * Create a new instance of a EntryAttribute, with a byte[] value.
     */
    public DefaultServerAttribute( AttributeType attributeType, byte[] val ) throws NamingException
    {
        this( null, attributeType, val );
    }


    /**
     * Create a new instance of a EntryAttribute, with a String value.
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType, byte[] val ) throws NamingException
    {
        assert checkAttributeType( attributeType) == null : logAssert( checkAttributeType( attributeType ) );

        this.attributeType = attributeType;
        add( val );
        setUpId( upId, attributeType );
    }


    /**
     * Gets the attribute type associated with this ServerAttribute.
     *
     * @return the attributeType associated with this entry attribute
     */
    public AttributeType getType()
    {
        return attributeType;
    }


    /**
     * Get's the user provided identifier for this entry.  This is the value
     * that will be used as the identifier for the attribute within the
     * entry.  If this is a commonName attribute for example and the user
     * provides "COMMONname" instead when adding the entry then this is
     * the format the user will have that entry returned by the directory
     * server.  To do so we store this value as it was given and track it
     * in the attribute using this property.
     *
     * @return the user provided identifier for this attribute
     */
    public String getUpId()
    {
        return upId;
    }


    /**
     * Checks to see if this attribute is valid along with the values it contains.
     *
     * @return true if the attribute and it's values are valid, false otherwise
     * @throws NamingException if there is a failure to check syntaxes of values
     */
    public boolean isValid() throws NamingException
    {
        if ( attributeType.isSingleValue() && values.size() > 1 )
        {
            return false;
        }

        for ( ServerValue<?> value : values )
        {
            if ( ! value.isValid() )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * @see EntryAttribute#add(org.apache.directory.shared.ldap.entry.Value)
     */
    public boolean add( ServerValue<?> val ) throws InvalidAttributeValueException, NamingException
    {
        if ( attributeType.getSyntax().isHumanReadable() )
        {
            if ( !( val instanceof StringValue ) )
            {
                String message = "The value must be a String, as its AttributeType is H/R";
                LOG.error( message );
                throw new InvalidAttributeValueException( message );
            }
        }
        else
        {
            if ( !( val instanceof BinaryValue ) )
            {
                String message = "The value must be a byte[], as its AttributeType is not H/R";
                LOG.error( message );
                throw new InvalidAttributeValueException( message );
            }
        }
        
        return values.add( val );
    }


    /**
     * @see EntryAttribute#add(org.apache.directory.shared.ldap.entry.Value...)
     */
    public int add( ServerValue<?>... vals ) throws InvalidAttributeValueException, NamingException
    {
        int nbAdded = 0;
        
        for ( ServerValue<?> val:vals )
        {
            if ( add( val ) )
            {
                nbAdded ++;
            }
        }
        
        return nbAdded;
    }


    /**
     * @see EntryAttribute#add(String)
     */
    public boolean add( String val ) throws InvalidAttributeValueException, NamingException
    {
        if ( attributeType.getSyntax().isHumanReadable() )
        {
            return add( new ServerStringValue( attributeType, val ) );
        }
        else
        {
            String message = "The value must be a String, as its AttributeType is H/R";
            LOG.warn( message );
            return add( new ServerBinaryValue( attributeType, StringTools.getBytesUtf8( val ) ) );
        }
    }


    /**
     * @see EntryAttribute#add(String...)
     */
    public int add( String... vals ) throws InvalidAttributeValueException, NamingException
    {
        int nbAdded = 0;
        
        for ( String val:vals )
        {
            if ( add( val ) )
            {
                nbAdded ++;
            }
        }
        
        return nbAdded;
    }    
    
    
    /**
     * @see EntryAttribute#add(byte[])
     */
    public boolean add( byte[] val ) throws InvalidAttributeValueException, NamingException
    {
        if ( ! attributeType.getSyntax().isHumanReadable() )
        {
            return add( new ServerBinaryValue( attributeType, val ) );
        }
        else
        {
            String message = "The value must be a String, as its AttributeType is H/R";
            LOG.error( message );
            throw new InvalidAttributeValueException( message );
        }
    }

    
    /**
     * @see EntryAttribute#add(byte[]...)
     */
    public int add( byte[]... vals ) throws InvalidAttributeValueException, NamingException
    {
        int nbAdded = 0;
        
        for ( byte[] val:vals )
        {
            if ( add( val ) )
            {
                nbAdded ++;
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
     * @see EntryAttribute#contains(org.apache.directory.shared.ldap.entry.Value)
     */
    public boolean contains( ServerValue<?> val )
    {
        return values.contains( val );
    }


    /**
     * @see EntryAttribute#contains(org.apache.directory.shared.ldap.entry.Value...)
     */
    public boolean contains( ServerValue<?>... vals )
    {
        // Iterate through all the values, and quit if we 
        // don't find one in the values
        for ( ServerValue<?> val:vals )
        {
            if ( !values.contains( val ) )
            {
                return false;
            }
        }
        
        return true;
    }


    /**
     * @see EntryAttribute#contains(String)
     */
    public boolean contains( String val )
    {
        try
        {
            if ( !attributeType.getSyntax().isHumanReadable() )
            {
                return false;
            }
        }
        catch ( NamingException ne )
        {
            // We have had a pb while getting the syntax...
            return false;
        }
        
        ServerStringValue value = new ServerStringValue( attributeType, val );
        
        return values.contains( value );
    }


    /**
     * @see EntryAttribute#contains(String...)
     */
    public boolean contains( String... vals )
    {
        // Iterate through all the values, and quit if we 
        // don't find one in the values
        for ( String val:vals )
        {
            if ( !contains( val ) )
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * @see EntryAttribute#contains(byte[])
     */
    public boolean contains( byte[] val )
    {
        try
        {
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                return false;
            }
        }
        catch ( NamingException ne )
        {
            // We have had a pb while getting the syntax...
            return false;
        }
        
        ServerBinaryValue sbv = new ServerBinaryValue( attributeType, val );
        return values.contains( sbv );
    }


    /**
     * @see EntryAttribute#contains(byte[]...)
     */
    public boolean contains( byte[]... vals )
    {
        // Iterate through all the values, and quit if we 
        // don't find one in the values
        for ( byte[] val:vals )
        {
            if ( !contains( val ) )
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * Get the first value of this attribute. If there is none, 
     * null is returned.
     * 
     * Note : as we are storing values into a Set, one can't assume
     * the values to be ordered in any way. This method is meant to
     * be used if the attribute hold only one value.
     * 
     *  @return The first value for this attribute.
     */
    public ServerValue<?> get()
    {
        if ( values.isEmpty() )
        {
            return null;
        }
        
        return values.iterator().next();
    }


    /**
     * Get all the stored values.
     * 
     * @return An iterator over the values stored into the attribute
     */
    public Iterator<ServerValue<?>> getAll()
    {
        return iterator();
    }


    /**
     * Get the number or values stored in the attribute
     * 
     * @return the number of stored values. As 'null' can be a valid
     * value, it is counted as one result, not 0.
     */
    public int size()
    {
        return values.size();
    }


    /**
     * @see EntryAttribute#remove(org.apache.directory.shared.ldap.entry.Value)
     */
    public boolean remove( ServerValue<?> val )
    {
        return values.remove( val );
    }


    /**
     * @see EntryAttribute#remove(org.apache.directory.shared.ldap.entry.Value...)
     */
    public boolean remove( ServerValue<?>... vals )
    {
        boolean removed = false;
        
        // Loop through all the values to remove. If one of
        // them is not present, the method will return false.
        for ( ServerValue<?> val:vals )
        {
            removed &= values.remove( val );
        }
        
        return removed;
    }


    /**
     * @see EntryAttribute#remove(byte[])
     */
    public boolean remove( byte[] val )
    {
        ServerBinaryValue sbv = new ServerBinaryValue( attributeType, val );
        return values.remove( sbv );
    }


    /**
     * @see EntryAttribute#remove(byte[]...)
     */
    public boolean remove( byte[]... vals )
    {
        boolean removed = true;
        
        for ( byte[] val:vals )
        {
            ServerBinaryValue value = new ServerBinaryValue( attributeType, val );
            removed &= values.remove( value );
        }
        
        return removed;
    }


    /**
     * @see EntryAttribute#remove(String)
     */
    public boolean remove( String val )
    {
        ServerStringValue ssv = new ServerStringValue( attributeType, val );
        return values.remove( ssv );
    }


    /**
     * @see EntryAttribute#remove(String...)
     */
    public boolean remove( String... vals )
    {
        boolean removed = true;
        
        for ( String val:vals )
        {
            ServerStringValue value = new ServerStringValue( attributeType, val );
            removed &= values.remove( value );
        }
        
        return removed;
    }


    /**
     * An iterator on top of the stored values.
     * 
     * @return an iterator over the stored values.
     */
    public Iterator<ServerValue<?>> iterator()
    {
        return values.iterator();
    }
}
