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
package org.apache.directory.shared.ldap.common;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.common.Value;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Attribute implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 499013 $
 */
public class ServerAttributeImpl implements ServerAttribute, Serializable, Cloneable
{
    private static final Logger log = LoggerFactory.getLogger( ServerAttributeImpl.class );

    /** For serialization */
    private static final long serialVersionUID = 2L;

    /** the name of the attribute, case sensitive */
    private final String upId;

    /** the OID of the attribute */
    private OID oid;

    /** In case we have only one value, just use this container */
    private Value value;
    
    /** the list of attribute values, if unordered */
    private List<Value> values;
    
    /** A size to handle the number of values, as one of them can be null */
    private int size;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a ServerAttribute with an id
     * 
     * @param id the id or name of this attribute.
     */
    public ServerAttributeImpl( String id ) throws NamingException
    {
        if ( StringTools.isEmpty( id ) )
        {
            log.error( "Attributes with an empty ID or OID are not allowed" );
            throw new NamingException( "Attributes with an empty ID or OID are not allowed" );
        }

        upId = id;
        value = null;
        values = null;
        oid = null;
        size = 0;
    }


    /**
     * Creates a ServerAttribute with an oid
     * 
     * @param oid the oid of this attribute.
     */
    public ServerAttributeImpl( OID oid ) throws NamingException
    {
        if ( oid == null )
        {
            log.error( "Attributes with an empty ID or OID are not allowed" );
            throw new NamingException( "Attributes with an empty ID or OID are not allowed" );
        }

        upId = oid.toString();
        value = null;
        values = null;
        this.oid = oid;
        size = 0;
    }


    /**
     * Creates a ServerAttribute with an id and a value
     * 
     * @param id the id or name of this attribute.
     * @param val the attribute's value
     */
    public ServerAttributeImpl( String id, Value val ) throws NamingException
    {
        if ( StringTools.isEmpty( id ) )
        {
            log.error( "Attributes with an empty ID or OID are not allowed" );
            throw new NamingException( "Attributes with an empty ID or OID are not allowed" );
        }

        upId = id;
        value = val;
        values = null;
        oid = null;
        size = 1;
    }


    /**
     * Creates a ServerAttribute with an oid and a value
     * 
     * @param oid the oid of this attribute.
     * @param val the attribute's value
     */
    public ServerAttributeImpl( OID oid, Value val ) throws NamingException
    {
        if ( oid == null )
        {
            log.error( "Attributes with an empty ID or OID are not allowed" );
            throw new NamingException( "Attributes with an empty ID or OID are not allowed" );
        }

        upId = oid.toString();
        value = val;
        values = null;
        this.oid = oid;
        size = 1;
    }

    
    /**
     * Creates a ServerAttribute with an id and a byte[] value
     * 
     * @param id the id or name of this attribute.
     * @param val a value for the attribute
     */
    public ServerAttributeImpl( String id, byte[] val ) throws NamingException
    {
        if ( StringTools.isEmpty( id ) )
        {
            log.error( "Attributes with an empty ID or OID are not allowed" );
            throw new NamingException( "Attributes with an empty ID or OID are not allowed" );
        }

        upId = id;
        values = null;
        value = new BinaryValue( val );
        oid = null;
        size = 1;
    }

    
    /**
     * Creates a ServerAttribute with an oid and a byte[] value
     * 
     * @param oid the oid of this attribute.
     * @param val a value for the attribute
     */
    public ServerAttributeImpl( OID oid, byte[] val ) throws NamingException
    {
        if ( oid == null )
        {
            log.error( "Attributes with an empty ID or OID are not allowed" );
            throw new NamingException( "Attributes with an empty ID or OID are not allowed" );
        }

        upId = oid.toString();
        values = null;
        value = new BinaryValue( val );
        this.oid = oid;
        size = 1;
    }

    
    /**
     * Creates a ServerAttribute with an id and a String value
     * 
     * @param id the id or name of this attribute.
     * @param val a value for the attribute
     */
    public ServerAttributeImpl( String id, String val ) throws NamingException
    {
        if ( StringTools.isEmpty( id ) )
        {
            log.error( "Attributes with an empty ID or OID are not allowed" );
            throw new NamingException( "Attributes with an empty ID or OID are not allowed" );
        }

        upId = id;
        values = null;
        value = new StringValue( val );
        oid = null;
        size = 1;
    }

    
    /**
     * Creates a ServerAttribute with an oid and a String value
     * 
     * @param oid the oid of this attribute.
     * @param val a value for the attribute
     */
    public ServerAttributeImpl( OID oid, String val ) throws NamingException
    {
        if ( oid == null )
        {
            log.error( "Attributes with an empty ID or OID are not allowed" );
            throw new NamingException( "Attributes with an empty ID or OID are not allowed" );
        }

        upId = oid.toString();
        values = null;
        value = new StringValue( val );
        this.oid = oid;
        size = 1;
    }

    
    /**
     * Create a copy of an Attribute, be it an ServerAttributeImpl
     * instance of a BasicAttribute instance
     * 
     * @param attribute the Attribute instace to copy
     * @throws NamingException if the attribute is not an instance of BasicAttribute
     * or ServerAttributeImpl or is null
     */
    public ServerAttributeImpl( Attribute attribute ) throws NamingException
    {
        if ( attribute == null )
        {
            log.error(  "Null attribute is not allowed" );
            throw new NamingException( "Null attribute is not allowed" );
        }
        else if ( attribute instanceof ServerAttributeImpl )
        {
            ServerAttributeImpl copy = (ServerAttributeImpl)attribute;
            
            upId  = copy.upId;
            oid = copy.oid;
            
            switch ( copy.size() )
            {
                case 0:
                    values = null;
                    value = null;
                    size = 0;
                    break;

                case 1:
                    value = getClonedValue( copy.get() );
                    values = null;
                    size = 1;
                    break;
                    
                default :
                    value = null;
                    values = new ArrayList<Value>( copy.size() );
                    
                    Iterator<Value> vals = copy.getAll();
                    
                    while ( vals.hasNext() )
                    {
                        Value val = vals.next();
                        values.add( val );
                    }
                    
                    size = copy.size();
                    
                    break;
            }

            oid = null;
        }
        else if ( attribute instanceof BasicAttribute )
        {
            upId = attribute.getID();
            oid = null;
            
            switch ( attribute.size() )
            {
                case 0 :
                    value = null;
                    values = null;
                    size = 0;
                    break;
                    
                case 1 :
                    Object val = attribute.get();
                    
                    if ( val instanceof String )
                    {
                        value = new StringValue( (String)val );
                    }
                    else if ( val instanceof byte[] )
                    {
                        value = new BinaryValue( (byte[])val );
                    }
                    else
                    {
                        log.error( "The value's type is not String or byte[]" );
                        throw new NamingException( "The value's type is not String or byte[]" );
                    }

                    values = null;
                    size = 1;
                    
                    break;
                    
                default :   
                    NamingEnumeration vals = attribute.getAll();
                    
                    while ( vals.hasMoreElements() )
                    {
                        val = vals.nextElement();
                        
                        if ( val instanceof String )
                        {
                            value = new StringValue( (String)val );
                        }
                        else if ( val instanceof byte[] )
                        {
                            value = new BinaryValue( (byte[])val );
                        }
                        else
                        {
                            log.error( "The value's type is not String or byte[]" );
                            throw new NamingException( "The value's type is not String or byte[]" );
                        }
                    }
                    
                    values = null;
                    size = attribute.size();
                    
                    break;
            }
        }
        else
        {
            log.error( "Attribute must be an instance of BasicAttribute or AttributeImpl" );
            throw new NamingException( "Attribute must be an instance of BasicAttribute or AttributeImpl" );
        }
    }

    
    /**
     * 
     * Clone a value. This private message is used to avoid adding try--catch
     * all over the code.
     */
    private Value getClonedValue( Value value )
    {
        try
        {
            return (Value)value.clone();
        }
        catch ( CloneNotSupportedException csne )
        {
            return null;
        }
    }
    
    // ------------------------------------------------------------------------
    // ServerAttribute Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets a Iterator wrapped around the iterator of the value list.
     * 
     * @return the Iterator wrapped as a NamingEnumberation.
     */
    public Iterator<Value> getAll()
    {
        if ( size < 2 )
        {
            return new Iterator<Value>()
            {
                private boolean more = (value != null);
                    
                public boolean hasNext() 
                {
                    return more;
                }
                
                public Value next() 
                {
                    more = false;
                    return value;
                }
                
                public void remove() 
                {
                    value = null;
                    more = true;
                }
            };
        }
        else
        {
            return values.iterator();
        }
    }


    /**
     * Gets the first value of the list or null if no values exist.
     * 
     * @return the first value or null.
     */
    public Value get()
    {
        switch ( size )
        {
            case 0 :
                return null;
                
            case 1 :
                return value;
                
            default :
                return values.get( 0 );
        }
    }


    /**
     * Gets the size of the value list.
     * 
     * @return size of the value list.
     */
    public int size()
    {
        return size;
    }


    /**
     * Gets the id or name of this Attribute.
     * 
     * @return the identifier for this Attribute.
     */
    public String getID()
    {
        return upId;
    }


    /**
     * Gets the OID of this Attribute.
     * 
     * @return the OID for this Attribute.
     */
    public OID getOid()
    {
        return oid;
    }


    /**
     * Checks to see if this Attribute contains val in the list.
     * 
     * @param val the value to test for
     * @return true if val is in the list backing store, false otherwise
     */
    public boolean contains( Value val )
    {
        switch ( size )
        {
            case 0 :
                return false;
                
            case 1 :
                return AttributeUtils.equals( value, val );
                
            default :
                for ( Value value:values )
                {
                    if ( AttributeUtils.equals( value, val ) )
                    {
                        return true;
                    }
                }
                
                return false;
        }
    }


    /**
     * Checks to see if this Attribute contains val in the list.
     * 
     * @param val the value to test for
     * @return true if val is in the list backing store, false otherwise
     */
    public boolean contains( String val )
    {
        return contains( new StringValue( val) );
    }
    
    
    /**
     * Checks to see if this Attribute contains val in the list.
     * 
     * @param val the value to test for
     * @return true if val is in the list backing store, false otherwise
     */
    public boolean contains( byte[] val )
    {
        return contains( new BinaryValue( val) );
    }

    
    /**
     * Adds val into the list of this Attribute's values at the end of the
     * list.
     * 
     * @param val the value to add to the end of the list.
     * @return true if val is added to the list backing store, false if it
     *         already existed there.
     */
    public boolean add( Value val )
    {
        boolean exists = false;
        
        if ( contains( val ) )
        {
            // Do not duplicate values
            return true;
        }
        
        // First copy the value
        val = getClonedValue( val );
        
        switch ( size() )
        {
            case 0 :
                value = val;
                size++;
                return true;
                
            case 1 :
                // We can't store different kind of Values in the attribute
                // The null value is an exception
                if ( ( value != null) && ( val != null ) && 
                     ( value.getClass() != val.getClass() ) )
                {
                    return false;
                }
                
                exists = value.equals( val );

                if ( exists )
                {
                    // Don't add two times the same value
                    return true;
                }
                else
                {
                    values = new ArrayList<Value>();
                    values.add( value );
                    values.add( val );
                    value = null;
                    size++;
                    return true;
                }
                
            default :
                Value firstValue = values.get( 0 );
            
                if ( firstValue.getValue() == null )
                {
                    firstValue = values.get( 1 );
                }
                
                // We can't store different kind of Values in the attribute
                // The null value is an exception
                if ( ( val != null ) && ( val.getValue() != null ) &&
                     ( firstValue.getValue().getClass() != val.getClass() ) )
                {
                    return false;
                }

                exists = values.contains( val ); 
            
                if ( exists )
                {
                    // Don't add two times the same value
                    return true;
                }
                
                values.add( val );
                size++;
                
                return exists;
        }
    }


    /**
     * Adds val into the list of this Attribute's values at the end of the
     * list.
     * 
     * @param val the value to add to the end of the list.
     * @return true if val is added to the list backing store, false if it
     *         already existed there.
     */
    public boolean add( String val )
    {
        return add( new StringValue( val) );
    }

    
    /**
     * Adds val into the list of this Attribute's values at the end of the
     * list.
     * 
     * @param val the value to add to the end of the list.
     * @return true if val is added to the list backing store, false if it
     *         already existed there.
     */
    public boolean add( byte[] val )
    {
        return add( new BinaryValue( val) );
    }
    
    
    /**
     * Removes val from the list of this Attribute's values.
     * 
     * @param val the value to remove
     * @return true if val is remove from the list backing store, false if
     *         never existed there.
     */
    public boolean remove( Value val )
    {
        if ( contains( val) )
        {
            switch ( size )
            {
                case 0 :
                    return false;
                    
                case 1 :
                    value = null;
                    size = 0;
                    return true;
                    
                case 2 : 
                    values.remove( val );
                    value = values.get(0);
                    values = null;
                    size--;
                    return true;
                    
                default :
                    values.remove( val );
                    size--;
                    return true;
            }
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Removes val from the list of this Attribute's values.
     * 
     * @param val the value to remove
     * @return true if val is remove from the list backing store, false if
     *         never existed there.
     */
    public boolean remove( String val )
    {
        return remove( new StringValue( val ) );
    }
    
    
    /**
     * Removes val from the list of this Attribute's values.
     * 
     * @param val the value to remove
     * @return true if val is remove from the list backing store, false if
     *         never existed there.
     */
    public boolean remove( byte[] val )
    {
        return remove( new BinaryValue( val ) );
    }


    /**
     * Removes all the values of this Attribute from the list backing store.
     */
    public void clear()
    {
        switch ( size )
        {
            case 0 :
                return;
                
            case 1 :
                value = null;
                size = 0;
                return;
                
            default :
                values = null;
                size = 0;
                return;
        }
    }


    /**
     * Not a deep clone.
     * 
     * @return a copy of this attribute using the same parent lock and id
     *         containing references to all the values of the original.
     */
    public Object clone()
    {
        try
        {
            ServerAttributeImpl clone = (ServerAttributeImpl)super.clone();
            
            // Simply copy the OID.
            clone.oid = oid;
            
            if ( size < 2 )
            {
                clone.value = (Value)value.clone();
            }
            else
            {
                clone.values = new ArrayList<Value>( values.size() );
                
                for ( Value value:values )
                {
                    Value newValue = (Value)value.clone();
                    clone.values.add( newValue );
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
     * Checks for equality between this Attribute instance and another. The 
     * Attribute ID's are compared with regard to case.
     * 
     * The values are supposed to have been normalized first
     *       
     * @param obj the Attribute to test for equality
     * @return true if the obj is an Attribute and equals this Attribute false
     *         otherwise
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( ( obj == null ) || !( obj instanceof ServerAttributeImpl ) )
        {
            return false;
        }

        ServerAttributeImpl attr = ( ServerAttributeImpl ) obj;
        
        if ( attr.oid != oid )
        {
            return false;
        }
        
        if ( !upId.equalsIgnoreCase( attr.getID() ) )
        {
            return false;
        }
        
        if ( attr.size != size )
        {
            return false;
        }

        switch ( size )
        {
            case 0 :
                return true;
                
            case 1 :
                return ( value.equals( attr.get() ) );
            
            default :
                Iterator<Value> vals = getAll();
                
                while ( vals.hasNext() )
                {
                    Value val = vals.next();
                    
                    if ( !attr.contains( val ) )
                    {
                        return false;
                    }
                }
                
                return true;
        }
    }
    
    
    /**
     * Normalize the attribute, setting the OID and normalizing the values 
     *
     * @param oid The attribute OID
     * @param normalizer The normalizer
     */
    @SuppressWarnings(value="unchecked")
    public void normalize( OID oid, Normalizer<?> normalizer ) throws NamingException
    {
        this.oid = oid;
        
        switch ( size )
        {
            case 0 :
                return;
                
            case 1 :
                value.normalize( normalizer );
                return;
                
            default :
                for ( Value value:values)
                {
                    value.normalize( normalizer );
                }
        }
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "ServerAttribute id : '" ).append( upId ).append( "', " );
        
        if ( oid != null )
        {
            sb.append( " OID : " ).append( oid ).append( '\n' );
        }
        
        sb.append( " Values : [");
        
        switch ( size )
        {
            case 0 :
                sb.append( "]\n" );
                break;
                
            case 1 :
                sb.append( '\'' ).append( value ).append( '\'' );
                sb.append( "]\n" );
                break;
                
            default :
                boolean isFirst = true;
            
                for ( Value value:values )
                {
                    if ( isFirst == false )
                    {
                        sb.append( ", " );
                    }
                    else
                    {
                        isFirst = false;
                    }
                    
                    sb.append( '\'' ).append( value ).append( '\'' );
                }
                
                sb.append( "]\n" );
                break;
        }
        
        return sb.toString();
    }
}
