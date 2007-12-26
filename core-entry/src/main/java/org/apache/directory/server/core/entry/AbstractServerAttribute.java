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


import org.apache.directory.shared.ldap.entry.AbstractBinaryValue;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.AbstractStringValue;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * An abstract class to collect common methods and common members for the
 * BasicServerAttribute and ObjectClassAttribute classes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractServerAttribute implements ServerAttribute
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractServerAttribute.class );

    /** The set of contained values */
    protected List<ServerValue<?>> values = new ArrayList<ServerValue<?>>();
    
    /** The associated AttributeType */
    protected AttributeType attributeType;
    
    /** The User provided ID */
    protected String upId;

    
    // -----------------------------------------------------------------------
    // utility methods
    // -----------------------------------------------------------------------
    /**
     * Utility method to get some logs if an assert fails
     */
    protected String logAssert( String message )
    {
        LOG.error(  message );
        return message;
    }
    
    /**
     *  Check the attributeType member. It should not be null, 
     *  and it should contains a syntax.
     */
    protected String checkAttributeType( AttributeType attributeType )
    {
        try
        {
            if ( attributeType == null )
            {
                return "The AttributeType parameter should not be null";
            }
            
            if ( attributeType.getSyntax() == null )
            {
                return "There is no Syntax associated with this attributeType";
            }

            return null;
        }
        catch ( NamingException ne )
        {
            return "This AttributeType is incorrect";
        }
    }
    
    /**
     * Set the user provided ID. If we have none, the upId is assigned
     * the attributetype's name. If it does not have any name, we will
     * use the OID.
     *
     * @param upId The attribute ID
     * @param attributeType The associated attributeType
     */
    protected void setUpId( String upId, AttributeType attributeType )
    {
        if ( upId == null )
        {
            String name = attributeType.getName();
            
            if ( name == null )
            {
                this.upId = attributeType.getOid();
            }
            else
            {
                this.upId = name;
            }
        }
        else
        {
            this.upId = upId;
        }
    }


    // -----------------------------------------------------------------------
    // API
    // -----------------------------------------------------------------------
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
            if ( !( val instanceof AbstractStringValue ) )
            {
                String message = "The value must be a String, as its AttributeType is H/R";
                LOG.error( message );
                throw new InvalidAttributeValueException( message );
            }
        }
        else
        {
            if ( !( val instanceof AbstractBinaryValue ) )
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
            LOG.error( message );
            throw new InvalidAttributeValueException( message );
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
            String message = "The value must be a byte[], as its AttributeType is not H/R";
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
