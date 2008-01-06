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


import org.apache.directory.shared.ldap.entry.AbstractBinaryValue;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.AbstractStringValue;
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
public final class DefaultClientAttribute extends AbstractClientAttribute
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultClientAttribute.class );
    
    
    // maybe have some additional convenience constructors which take
    // an initial value as a string or a byte[]
    /**
     * Create a new instance of a EntryAttribute, without ID nor value.
     */
    public DefaultClientAttribute()
    {
    }


    /**
     * Create a new instance of a EntryAttribute, without value.
     */
    public DefaultClientAttribute( String upId )
    {
        setUpId( upId );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ClientValue which uses the specified
     * attributeType.
     *
     * @param vals an initial set of values for this attribute
     * @throws NamingException if there are problems creating the new attribute
     */
    public DefaultClientAttribute( ClientValue<?>... vals ) throws NamingException
    {
        this( null, vals );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ClientValue which uses the specified
     * attributeType.
     * 
     * Otherwise, the value is stored, but as a reference. It's not a copy.
     *
     * @param upId
     * @param attributeType the attribute type according to the schema
     * @param vals an initial set of values for this attribute
     * @throws NamingException if there are problems creating the new attribute
     */
    public DefaultClientAttribute( String upId, ClientValue<?>... vals ) throws NamingException
    {
        // The value can be null, this is a valid value.
        if ( vals[0] == null )
        {
             add( new ClientStringValue() );
        }
        else
        {
            for ( ClientValue<?> val:vals )
            {
                if ( val instanceof ClientStringValue )
                {
                    ClientStringValue serverString = ( ClientStringValue ) val;
                    add( new ClientStringValue( serverString.get() ) );
                }
                else if ( val instanceof ClientBinaryValue )
                {
                    ClientBinaryValue serverBinary = ( ClientBinaryValue ) val;
                    add( new ClientBinaryValue( serverBinary.getCopy() ) );
                }
                else
                {
                    String message = "Unknown value type: " + val.getClass().getName();
                    LOG.error( message );
                    throw new IllegalStateException( message );
                }
            }
        }
        
        setUpId( upId );
    }


    /**
     * Create a new instance of a EntryAttribute, without ID but with some values.
     */
    public DefaultClientAttribute( String... vals ) throws NamingException
    {
        this( null, vals );
    }


    /**
     * Create a new instance of a EntryAttribute.
     */
    public DefaultClientAttribute( String upId, String... vals ) throws NamingException
    {
        add( vals );
        setUpId( upId );
    }


    /**
     * Create a new instance of a EntryAttribute, with some byte[] values.
     */
    public DefaultClientAttribute( byte[]... vals ) throws NamingException
    {
        this( null, vals );
    }


    /**
     * Create a new instance of a EntryAttribute, with some byte[] values.
     */
    public DefaultClientAttribute( String upId, byte[]... vals ) throws NamingException
    {
        add( vals );
        setUpId( upId );
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
     * @see EntryAttribute#add(org.apache.directory.shared.ldap.entry.Value)
     */
    public boolean add( ClientValue<?> val ) throws InvalidAttributeValueException, NamingException
    {
        if ( val == null )
        {
            ClientValue<String> nullSV = new ClientStringValue( (String)null );
            
            if ( values.contains( nullSV ) )
            {
                return false;
            }
            else
            {
                values.add( nullSV );
                return true;
            }
        }

        if ( values.contains( val ) )
        {
            return false;
        }
        
        return values.add( val );
    }


    /**
     * @see EntryAttribute#add(org.apache.directory.shared.ldap.entry.Value...)
     */
    public int add( ClientValue<?>... vals ) throws InvalidAttributeValueException, NamingException
    {
        int nbAdded = 0;
        
        for ( ClientValue<?> val:vals )
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
        return add( new ClientStringValue( val ) );
    }


    /**
     * @see EntryAttribute#add(String...)
     */
    public int add( String... vals ) throws NamingException
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
        return add( new ClientBinaryValue( val ) );
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
    
    /**
     * @see Object#toString() 
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        if ( ( values != null ) && ( values.size() != 0 ) )
        {
            for ( ClientValue<?> value:values )
            {
                sb.append( "    " ).append( upId ).append( ": " ).append( value ).append( '\n' );
            }
        }
        else
        {
            sb.append( "    " ).append( upId ).append( ": (null)\n" );
        }
        
        return sb.toString();
    }

}
