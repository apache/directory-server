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
package org.apache.directory.shared.ldap.entry.client;


import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.StringTools;
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
public abstract class AbstractClientAttribute implements ClientAttribute
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractClientAttribute.class );

    /** The set of contained values */
    protected List<ClientValue<?>> values = new ArrayList<ClientValue<?>>();
    
    /** The User provided ID */
    protected String upId;

    
    // -----------------------------------------------------------------------
    // utility methods
    // -----------------------------------------------------------------------
    /**
     *  Check the attributeType member. It should not be null, 
     *  and it should contains a syntax.
     */
    protected String getErrorMessage( AttributeType attributeType )
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
     * Private helper method used to set an UpId from an attributeType
     */
    private String getUpId( AttributeType attributeType )
    {
        String upId = attributeType.getName();
        
        if ( upId == null )
        {
            upId = attributeType.getOid();
        }
        
        return upId;
    }
    
    
    /**
     * Set the user provided ID. If we have none, the upId is assigned
     * the attributetype's name. If it does not have any name, we will
     * use the OID.
     * <p>
     * If we have an upId and an AttributeType, they must be compatible. :
     *  - if the upId is an OID, it must be the AttributeType's OID
     *  - otherwise, its normalized form must be equals to ones of
     *  the attributeType's names.
     *
     * @param upId The attribute ID
     * @param attributeType The associated attributeType
     */
    protected void setUpId( String upId )
    {
        this.upId = upId;
    }


    // -----------------------------------------------------------------------
    // API
    // -----------------------------------------------------------------------
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
        for ( ClientValue<?> value : values )
        {
            if ( ! value.isValid() )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * @see EntryAttribute#add(org.apache.directory.shared.ldap.entry.Value...)
     */
    public int add( ClientValue<?>... vals ) throws InvalidAttributeValueException, NamingException
    {
        int nbAdded = 0;
        
        for ( ClientValue<?> val:vals )
        {
            values.add( val );
            nbAdded ++;
        }
        
        return nbAdded;
    }


    /**
     * @see EntryAttribute#put(org.apache.directory.shared.ldap.entry.Value...)
     */
    public int put( ClientValue<?>... vals ) throws InvalidAttributeValueException, NamingException
    {
        values.clear();
        return add( vals );
    }
    
    /**
     * @see EntryAttribute#add(String...)
     */
    public int add( String... vals ) throws InvalidAttributeValueException, NamingException
    {
        int nbAdded = 0;
        
        for ( String val:vals )
        {
            values.add( new ClientStringValue( val ) );

            nbAdded ++;
        }
        
        return nbAdded;
    }    
    
    
    /**
     * @see EntryAttribute#put(String...)
     */
    public int put( String... vals ) throws InvalidAttributeValueException, NamingException
    {
        values.clear();
        return add( vals );
    }
    
    
    /**
     * @see EntryAttribute#add(byte[]...)
     */
    public int add( byte[]... vals ) throws InvalidAttributeValueException, NamingException
    {
        int nbAdded = 0;
        
        for ( byte[] val:vals )
        {
            values.add( new ClientBinaryValue( val ) );
            
            nbAdded ++;
        }
        
        return nbAdded;
    }    


    /**
     * @see EntryAttribute#put(byte[]...)
     */
    public int put( byte[]... vals ) throws InvalidAttributeValueException, NamingException
    {
        values.clear();
        return add( vals );
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
     * @return A copy of the current attribute
     */
    public ClientAttribute clone()
    {
        try
        {
            AbstractClientAttribute clone = (AbstractClientAttribute)super.clone();

            // Copy the values. The attributeType is immutable.
            if ( ( values != null ) && ( values.size() != 0 ) )
            {
                clone.values = new ArrayList<ClientValue<?>>( values.size() );
                
                for ( ClientValue<?> value:values )
                {
                    clone.values.add( value.clone() );
                }
            }
        
            return clone;
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
    }

    

    /**
     * @see EntryAttribute#contains(org.apache.directory.shared.ldap.entry.Value)
     */
    public boolean contains( ClientValue<?> val )
    {
        return values.contains( val );
    }


    /**
     * @see EntryAttribute#contains(org.apache.directory.shared.ldap.entry.Value...)
     */
    public boolean contains( ClientValue<?>... vals )
    {
        // Iterate through all the values, and quit if we 
        // don't find one in the values
        for ( ClientValue<?> val:vals )
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
        ClientStringValue value = new ClientStringValue( val );
        
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
        ClientBinaryValue sbv = new ClientBinaryValue( val );
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
    public ClientValue<?> get()
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
    public Iterator<ClientValue<?>> getAll()
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
    public boolean remove( ClientValue<?> val )
    {
        return values.remove( val );
    }


    /**
     * @see EntryAttribute#remove(org.apache.directory.shared.ldap.entry.Value...)
     */
    public boolean remove( ClientValue<?>... vals )
    {
        boolean removed = false;
        
        // Loop through all the values to remove. If one of
        // them is not present, the method will return false.
        for ( ClientValue<?> val:vals )
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
        ClientBinaryValue sbv = new ClientBinaryValue( val );
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
            ClientBinaryValue value = new ClientBinaryValue( val );
            removed &= values.remove( value );
        }
        
        return removed;
    }


    /**
     * @see EntryAttribute#remove(String)
     */
    public boolean remove( String val )
    {
        ClientStringValue ssv = new ClientStringValue( val );
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
            ClientStringValue value = new ClientStringValue( val );
            removed &= values.remove( value );
        }
        
        return removed;
    }


    /**
     * An iterator on top of the stored values.
     * 
     * @return an iterator over the stored values.
     */
    public Iterator<ClientValue<?>> iterator()
    {
        return values.iterator();
    }
}
