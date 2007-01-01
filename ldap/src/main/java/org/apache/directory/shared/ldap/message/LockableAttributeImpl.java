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
package org.apache.directory.shared.ldap.message;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;

import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Permanently Lockable ordered JNDI Attribute implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$
 */
public class LockableAttributeImpl implements Attribute
{
    private static final Logger log = LoggerFactory.getLogger( LockableAttributeImpl.class );

    private static final long serialVersionUID = -5158233254341746514L;

    /** the name of the attribute, case sensitive */
    private final String upId;

    /** In case we have only one value, just use this container */
    Object value;
    
    /** the list of attribute values */
    private List<Object> list;
    
    /** The number of values stored */
    int size = 0;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a permanently LockableAttribute on id whose locking behavoir is
     * dicatated by parent.
     * 
     * @param id
     *            the id or name of this attribute.
     */
    public LockableAttributeImpl(final String id)
    {
        upId = id;
        value = null;
        list = null; //new ArrayList();
        size = 0;
    }


    /**
     * Creates a permanently LockableAttribute on id with a single value.
     * 
     * @param id
     *            the id or name of this attribute.
     * @param value
     *            a value for the attribute
     */
    public LockableAttributeImpl(final String id, final Object value)
    {
        upId = id;
        list = null; // new ArrayList();
        this.value = value; //list.add( value );
        size = 1;
    }


    /**
     * Creates a permanently LockableAttribute on id with a single value.
     * 
     * @param id
     *            the id or name of this attribute.
     * @param value
     *            a value for the attribute
     */
    public LockableAttributeImpl(final String id, final byte[] value)
    {
        upId = id;
        list = null; //new ArrayList();
        this.value = value;
        //list.add( value );
        size = 1;
    }


    /**
     * Creates a permanently LockableAttribute on id whose locking behavoir is
     * dicatated by parent. Used for the clone method.
     * 
     * @param id
     *            the id or name of this attribute
     * @param list
     *            the list of values to start with
     */
    private LockableAttributeImpl(final String id, final List<Object> list)
    {
        upId = id;
        this.list = list;
        value = null;
        size = (list != null ? list.size() : 0);
    }


    // ------------------------------------------------------------------------
    // javax.naming.directory.Attribute Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets a NamingEnumberation wrapped around the iterator of the value list.
     * 
     * @return the Iterator wrapped as a NamingEnumberation.
     */
    public NamingEnumeration<Object> getAll()
    {
    	if ( size < 2 )
    	{
    		return new IteratorNamingEnumeration( new Iterator<Object>()
    		{
    			private boolean done = (size != 0);
    				
    			public boolean hasNext() 
    			{
    				return done;
    			}
    			
    			public Object next() throws NoSuchElementException
    			{
    				done = false;
    				return value;
    			}
    			
    			public void remove() 
    			{
    				value = null;
    				done = false;
    				size = 0;
    			}
    		});
    	}
    	else
    	{
    		return new IteratorNamingEnumeration( list.iterator() );
    	}
    }


    /**
     * Gets the first value of the list or null if no values exist.
     * 
     * @return the first value or null.
     */
    public Object get()
    {
    	if ( list == null )
    	{
    		return value;
    	}
    	else if ( list.isEmpty() )
        {
            return null;
        }
    	else
    	{
    		return list.get( 0 );
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
     * Checks to see if this Attribute contains attrVal in the list.
     * 
     * @param attrVal
     *            the value to test for
     * @return true if attrVal is in the list backing store, false otherwise
     */
    public boolean contains( Object attrVal )
    {
    	switch (size)
    	{
    		case 0 :
    			return false;
    			
    		case 1 :
    			return value == null ? attrVal == null : value.equals( attrVal );
    			
    		default :
    			return list.contains( attrVal );
    	}
    }


    /**
     * Adds attrVal into the list of this Attribute's values at the end of the
     * list.
     * 
     * @param attrVal
     *            the value to add to the end of the list.
     * @return true if attrVal is added to the list backing store, false if it
     *         already existed there.
     */
    public boolean add( Object attrVal )
    {
    	boolean exists = false;
    	
    	switch ( size )
    	{
    		case 0 :
    			value = attrVal;
    			size++;
    			return true;
    			
    		case 1 :
    			exists = value.equals( attrVal );

    			list = new ArrayList<Object>();
    			list.add( value );
    			list.add( attrVal );
    			size++;
    			value = null;
    			return exists;
    			
    		default :
    			exists = list.contains( attrVal ); 
    		
    			list.add( attrVal );
    			size++;
    			return exists;
    	}
    }


    /**
     * Removes attrVal from the list of this Attribute's values.
     * 
     * @param attrVal
     *            the value to remove
     * @return true if attrVal is remove from the list backing store, false if
     *         never existed there.
     */
    public boolean remove( Object attrVal )
    {
    	switch ( size )
    	{
    		case 0 :
    			return false;
    			
    		case 1 :
    			value = null;
    			size--;
    			return true;
    			
    		case 2 :
    			list.remove( attrVal );
    			value = list.get(0);
    			size = 1;
    			list = null;
    			return true;
    			
    		default :
    			list.remove( attrVal );
    			size--;
    			return true;
    	}
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
    			list = null;
    			size = 0;
    			return;
    	}
    }


    /**
     * NOT SUPPORTED - throws OperationNotSupportedException
     * 
     * @see javax.naming.directory.Attribute#getAttributeSyntaxDefinition()
     */
    public DirContext getAttributeSyntaxDefinition() throws NamingException
    {
        throw new OperationNotSupportedException( "Extending subclasses may override this if they like!" );
    }


    /**
     * NOT SUPPORTED - throws OperationNotSupportedException
     * 
     * @see javax.naming.directory.Attribute#getAttributeDefinition()
     */
    public DirContext getAttributeDefinition() throws NamingException
    {
        throw new OperationNotSupportedException( "Extending subclasses may override this if they like!" );
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
            super.clone();
        }
        catch ( CloneNotSupportedException cnse )
        {
            // Just do nothing... Object is Cloneable
        }
        
    	switch ( size )
    	{
    		case 0 :
    			return new LockableAttributeImpl( upId );
    			
    		case 1 :
    			return new LockableAttributeImpl( upId, value );
    			
    		default :
    			return new LockableAttributeImpl( upId, (List<Object>)((ArrayList<Object>)list).clone() );
    	}
    }


    /**
     * Always returns true since list is used to preserve value addition order.
     * 
     * @return true.
     */
    public boolean isOrdered()
    {
        return true;
    }


    /**
     * Gets the value at an index.
     * 
     * @param index
     *            the index of the value in the ordered list of attribute
     *            values. 0 <= ix < size().
     * @return this Attribute's value at the index null if no values exist.
     */
    public Object get( int index )
    {
    	switch ( size )
    	{
    		case 0 :
    			return null;
    			
    		case 1 :
    			return value;
    			
    		default :
    			return list.get( index );
    	}
    }


    /**
     * Removes the value at an index.
     * 
     * @param index
     *            the index of the value in the ordered list of attribute
     *            values. 0 <= ix < size().
     * @return this Attribute's value removed at the index
     */
    public Object remove( int index )
    {
    	switch ( size )
    	{
    		case 0 :
    			return null;
    			
    		case 1 :
    			Object result = value;
    			value = null;
    			size = 0;
    			return result;
    			
    		default :
    			size--;
    			return list.remove( index );
    	}
    }


    /**
     * Inserts attrVal into the list of this Attribute's values at the specified
     * index in the list.
     * 
     * @param index
     *            the index to add the value at.
     * @param attrVal
     *            the value to add to the end of the list.
     */
    public void add( int index, Object attrVal )
    {
    	switch ( size )
    	{
    		case 0 :
    			size++;
    			value = attrVal;
    			return;
    			
    		case 1 :
    			list = new ArrayList<Object>();
    			
    			if ( index == 0 )
    			{
	    			list.add( attrVal );
	    			list.add( value );
    			}
    			else
    			{
	    			list.add( value );
	    			list.add( attrVal );
    			}

    			size++;
    			value = null;
    			return;
    			
    		default :
    			list.add( index, attrVal );
    			size++;
    			return;
    	}
    }


    /**
     * Sets an attribute value in the ordered list of attribute values.
     * 
     * @param index
     *            the index to set the value to.
     * @param attrVal
     *            the value to set at the index.
     * @return the old value at the specified index.
     */
    public Object set( int index, Object attrVal )
    {
    	switch ( size )
    	{
    		case 0 :
    			size++;
    			value = attrVal;
    			return null;
    			
    		case 1 :
    			if ( index == 0 )
    			{
	    			Object result = value;
	    			value = attrVal;
	    			return result;
    			}
    			else
    			{
    				list = new ArrayList<Object>();
    				list.add( value );
    				list.add( attrVal );
    				size = 2;
    				value = null;
    				return null;
    			}
    			
    		default :
    			Object oldValue = list.get( index );
    			list.set( index, attrVal );
    			return oldValue;
    	}
    }


    /**
     * Checks for equality between this Attribute instance and another. The
     * lockable properties are not factored into the equality semantics and
     * neither is the Attribute implementation. The Attribute ID's are compared
     * with regard to case and value order is only considered if the Attribute
     * arguement is ordered itself.
     * 
     * TODO start looking at comparing syntaxes to determine if attributes are
     *       really equal
     * @param obj
     *            the Attribute to test for equality
     * @return true if the obj is an Attribute and equals this Attribute false
     *         otherwise
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( ( obj == null ) || !( obj instanceof Attribute ) )
        {
            return false;
        }

        Attribute attr = ( Attribute ) obj;
        
        if ( !upId.equals( attr.getID() ) )
        {
            return false;
        }

        if ( attr.size() != size )
        {
            return false;
        }

        switch ( size )
        {
        	case 0 :
        		return true;
        		
        	case 1 :
                try
                {
                	return value.equals( attr.get( 0 ) );
                }
                catch ( NamingException e )
                {
                    log.warn( "Failed to get an attribute from the specifid attribute: " + attr, e );
                    return false;
                }
        		
        	default :
                for ( int i = 0; i < size; i++ )
                {
                    try
                    {
                        if ( !list.contains( attr.get( i ) ) )
                        {
                            return false;
                        }
                    }
                    catch ( NamingException e )
                    {
                        log.warn( "Failed to get an attribute from the specifid attribute: " + attr, e );
                        return false;
                    }
                }
        		
        		return true;
        }
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        int hash = 7;
        hash = hash*31 + size;
        hash = hash*31 + ( upId == null ? 0 : upId.hashCode() );
        
        switch (size)
        {
            case 0 :
                return hash;
                
            case 1 :
                return hash*31 + ( value == null ? 0 : value.hashCode() );
                
            default :
                for ( Object value:list )
                {
                    hash = hash*31 + ( value == null ? 0 : value.hashCode() );
                }
            
                return hash;
        }
    }

    /**
     * @see Object#toString()
     */
    public String toString()
    {
    	StringBuffer sb = new StringBuffer();
    	
    	sb.append( "Attribute id : '" ).append( upId ).append( "', " );
    	sb.append( " Values : [");
    	
    	switch (size)
    	{
    		case 0 :
    			sb.append( "]\n" );
    			break;
    			
    		case 1 :
    			if ( value instanceof String ) 
    			{
    				sb.append( '\'' ).append( value ).append( '\'' );
				}
    			else
    			{
    				sb.append( StringTools.dumpBytes( (byte[])value ) );
    			}
    			
    			sb.append( "]\n" );
    			break;
    			
    		default :
    			boolean isFirst = true;
    		
	    		Iterator values = list.iterator();
	    		
	    		while ( values.hasNext() )
	    		{
	    			Object v = values.next();
	    			
	    			if ( isFirst == false )
	    			{
	    				sb.append( ", " );
	    			}
	    			else
	    			{
	    				isFirst = false;
	    			}
	    			
	    			if ( v instanceof String ) 
	    			{
	    				sb.append( '\'' ).append( v ).append( '\'' );
					}
	    			else
	    			{
	    				sb.append( StringTools.dumpBytes( (byte[])v ) );
	    			}
	    		}
	    		
	    		sb.append( "]\n" );
	    		break;
    	}
    	
    	return sb.toString();
    }
}
