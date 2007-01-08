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


import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;

import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A case-insensitive Lockable JNDI Attributes implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributesImpl implements Attributes
{
    static transient final long serialVersionUID = 1L;
    
    /** This flag is used if the attribute name is not caseSensitive */
    private boolean ignoreCase;

    /**
     * An holder to store <Id, Attribute> couples
     */
    private class Holder implements Serializable, Cloneable
    {
        static transient final long serialVersionUID = 1L;
        
        // The user provided attribute ID
        private String upId;
        
        // The attribute. Should be an instance of LockableAttribute
        private Attribute attribute;
        
        /**
         * Create a new holder for the given ID
         * @param upId The attribute UP id
         * @param attribute The attribute
         */
        private Holder( String upId, Attribute attribute )
        {
            this.upId = upId;
            this.attribute = attribute;
        }
        
        /**
         * @see Object#clone()
         */
        public Object clone() throws CloneNotSupportedException
        {
            Holder clone = (Holder)super.clone();
            
            clone.upId = upId;
            
            if ( attribute instanceof BasicAttribute )
            {
                // The BasicAttribute clone() method does not
                // copy the values.
                clone.attribute = new AttributeImpl( attribute.getID() );
                
                try
                {
                    NamingEnumeration values = attribute.getAll();
                    
                    while ( values.hasMoreElements() )
                    {
                        clone.attribute.add( AttributeUtils.cloneValue( values.nextElement() ) );
                    }
                }
                catch( NamingException ne )
                {
                    
                }
            }
            clone.attribute = (Attribute)attribute.clone();
            
            return clone;
        }
        
        /**
         * @see Object#toString()
         */
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            
            sb.append( upId ).append( ": " );
            
            sb.append( attribute ).append( '\n' );
            
            return sb.toString();
        }
    }
    
    /**
     * An iterator which returns Attributes.  
     */
    public class AttributeIterator<T> implements Iterator<Attribute>
    {
        /** The internal iterator */
        private Iterator<Holder> iterator; 
        
        /** Create an attribute's iterator */
        private AttributeIterator( AttributesImpl attributes )
        {
            iterator = attributes.keyMap.values().iterator();
        }
        
        /**
         * Returns <tt>true</tt> if the iteration has more elements. (In other
         * words, returns <tt>true</tt> if <tt>next</tt> would return an element
         * rather than throwing an exception.)
         *
         * @return <tt>true</tt> if the iterator has more elements.
         */
        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        /**
         * Returns the next element in the iteration.  Calling this method
         * repeatedly until the {@link #hasNext()} method returns false will
         * return each element in the underlying collection exactly once.
         *
         * @return the next element in the iteration.
         * @exception NoSuchElementException iteration has no more elements.
         */
        public Attribute next()
        {
            return iterator.next().attribute;
        }

        /**
         * 
         * Removes from the underlying collection the last element returned by the
         * iterator (optional operation).  This method can be called only once per
         * call to <tt>next</tt>.  The behavior of an iterator is unspecified if
         * the underlying collection is modified while the iteration is in
         * progress in any way other than by calling this method.
         *
         * @exception UnsupportedOperationException if the <tt>remove</tt>
         *        operation is not supported by this Iterator.
         
         * @exception IllegalStateException if the <tt>next</tt> method has not
         *        yet been called, or the <tt>remove</tt> method has already
         *        been called after the last call to the <tt>next</tt>
         *        method.
         */
        public void remove()
        {
            iterator.remove();
        }
    }
    
    /** Cache of lowercase id Strings to mixed cased user provided String ids */
    private Map<String, Holder> keyMap;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates an Attributes
     */
    public AttributesImpl()
    {
        keyMap = new HashMap<String, Holder>();
        ignoreCase = true;
    }

    /**
     * Creates an Attributes
     */
    public AttributesImpl( boolean ignoreCase )
    {
        keyMap = new HashMap<String, Holder>();
        this.ignoreCase = ignoreCase;
    }

    /**
     * Creates an Attributes with one Attribute
     */
    public AttributesImpl( String id, Object value )
    {
        keyMap = new HashMap<String, Holder>();
        put( id, value );
        ignoreCase = true;
    }

    /**
     * Creates an Attributes with one attribute
     */
    public AttributesImpl(  String id, Object value, boolean ignoreCase )
    {
        keyMap = new HashMap<String, Holder>();
        put( id, value );
        this.ignoreCase = ignoreCase;
    }

    // ------------------------------------------------------------------------
    // Serialization methods
    //
    // We will try to minimize the cost of reading and writing objects to the 
    // disk.
    //
    // We need to save all the attributes stored into the 'map' object, and 
    // their associated User Provided value.
    // Attributes are stored following this pattern :
    //  ( attributeType = (attribute value )* )*
    //
    // The streamed value will looks like :
    // [nbAttrs:int]
    //   (
    //      [length attributeType(i):int] [attributeType(i):String] 
    //      [length attributeTypeUP(i):int] [attributeTypeUP(i):String]
    //      [attributeValues(i)]
    //   )*
    //
    // The attribute value is streamed by the LockableAttributeImpl class.
    // ------------------------------------------------------------------------
    /*public void readObject( ObjectInputStream oi ) throws IOException, ClassNotFoundException
    {
        oi.defaultReadObject();
        
        // Read the map size
        int size = oi.readInt();
        
        keyMap = new HashMap( size );
        
        for ( int i = 0; i < size(); i++ )
        {
            int keySize = oi.readInt();
            char[] keyChars = new char[keySize];
            
            for ( int j = 0; j < keySize; j++)
            {
                keyChars[j] = oi.readChar();
            }
            
            String upId = new String( keyChars );
            String key = upId.toLowerCase();
            
            Attribute attribute = (LockableAttributeImpl)oi.readObject();
            
            Holder holder = new Holder( upId, attribute);
            
            keyMap.put( key, holder );
        }
    }*/

    /**
     * Write the Attribute to a stream
     */
    /*private void writeObject( ObjectOutputStream oo ) throws IOException
    {
        oo.defaultWriteObject();
        
        // Write the map size
        oo.write( keyMap.size() );
        
        Iterator keys = keyMap.keySet().iterator(); 
        
        while ( keys.hasNext() )
        {
            String key = (String)keys.next();
            Holder holder = (Holder)keyMap.get( key );

            // Write the userProvided key
            // No need to write the key, it will be
            // rebuilt by the read operation
            oo.write( holder.upId.length() );
            oo.writeChars( holder.upId );
            
            // Recursively call the writeExternal metho
            // of the attribute object
            oo.writeObject( holder.attribute );
        }
        
        // That's it !
    }*/

    // ------------------------------------------------------------------------
    // javax.naming.directory.Attributes Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Determines whether the attribute set ignores the case of attribute
     * identifiers when retrieving or adding attributes.
     * 
     * @return true always.
     */
    public boolean isCaseIgnored()
    {
        return ignoreCase;
    }


    /**
     * Retrieves the number of attributes in the attribute set.
     * 
     * @return The nonnegative number of attributes in this attribute set.
     */
    public int size()
    {
        return keyMap.size();
    }


    /**
     * Retrieves the attribute with the given attribute id from the attribute
     * set.
     * 
     * @param attrId
     *            The non-null id of the attribute to retrieve. If this
     *            attribute set ignores the character case of its attribute ids,
     *            the case of attrID is ignored.
     * @return The attribute identified by attrID; null if not found.
     * @see #put
     * @see #remove
     */
    public Attribute get( String attrId )
    {
        if ( attrId != null )
        {
            Holder holder = keyMap.get( StringTools.toLowerCase( attrId ) );
            return holder != null ? holder.attribute : null;
        }
        else
        {
            return null;
        }
    }


    /**
     * Retrieves an enumeration of the attributes in the attribute set. The
     * effects of updates to this attribute set on this enumeration are
     * undefined.
     * 
     * @return A non-null enumeration of the attributes in this attribute set.
     *         Each element of the enumeration is of class <tt>Attribute</tt>.
     *         If attribute set has zero attributes, an empty enumeration is
     *         returned.
     */
    public NamingEnumeration<Attribute> getAll()
    {
        return new IteratorNamingEnumeration<Attribute>( new AttributeIterator<Attribute>( this ) );
    }


    /**
     * Retrieves an enumeration of the ids of the attributes in the attribute
     * set. The effects of updates to this attribute set on this enumeration are
     * undefined.
     * 
     * @return A non-null enumeration of the attributes' ids in this attribute
     *         set. Each element of the enumeration is of class String. If
     *         attribute set has zero attributes, an empty enumeration is
     *         returned.
     */
    public NamingEnumeration<String> getIDs()
    {
        String[] ids = new String[keyMap.size()];
        
        Iterator values = keyMap.values().iterator();
        int i = 0;
        
        while ( values.hasNext() )
        {
            ids[i++] = ((Holder)values.next()).upId;
        }
        
        return new ArrayNamingEnumeration<String>( ids );
    }


    /**
     * Adds a new attribute to the attribute set.
     * 
     * @param attrId
     *            non-null The id of the attribute to add. If the attribute set
     *            ignores the character case of its attribute ids, the case of
     *            attrID is ignored.
     * @param val
     *            The possibly null value of the attribute to add. If null, the
     *            attribute does not have any values.
     * @return The Attribute with attrID that was previous in this attribute set
     *         null if no such attribute existed.
     * @see #remove
     */
    public Attribute put( String attrId, Object val )
    {
        Attribute attr = new AttributeImpl( attrId );
        attr.add( val );
        
        String key = StringTools.toLowerCase( attrId );
        keyMap.put( key, new Holder( attrId, attr) );
        return attr;
    }


    /**
     * Adds a new attribute to the attribute set.
     * 
     * @param attr
     *            The non-null attribute to add. If the attribute set ignores
     *            the character case of its attribute ids, the case of attr's
     *            identifier is ignored.
     *            The store attribute is a clone of the given attribute.
     * @return The Attribute with the same ID as attr that was previous in this
     *         attribute set; The new attr if no such attribute existed.
     * @see #remove
     */
    public Attribute put( Attribute attr )
    {
        String key = StringTools.toLowerCase( attr.getID() );
        Attribute old = null;
        Attribute newAttr = attr;
        
        if ( keyMap.containsKey( key ) )
        {
            old = keyMap.remove( key ).attribute;
        }
        else
        {
            old = attr;
        }

        if ( attr instanceof AttributeImpl )
        {
            newAttr = attr;
        }
        else if ( attr instanceof BasicAttribute )
        {
            newAttr = new AttributeImpl( attr.getID() );
             
            try
            {
                NamingEnumeration values = attr.getAll();
                 
                while ( values.hasMore() )
                {
                    newAttr.add( AttributeUtils.cloneValue( values.next() ) );
                }
            }
            catch ( NamingException ne )
            {
                // do nothing
            }
        }
        
        keyMap.put( key, new Holder( attr.getID(), newAttr ) );
        return old;
    }


    /**
     * Removes the attribute with the attribute id 'attrID' from the attribute
     * set. If the attribute does not exist, ignore.
     * 
     * @param attrId
     *            The non-null id of the attribute to remove. If the attribute
     *            set ignores the character case of its attribute ids, the case
     *            of attrID is ignored.
     * @return The Attribute with the same ID as attrID that was previous in the
     *         attribute set; null if no such attribute existed.
     */
    public Attribute remove( String attrId )
    {
        String key = StringTools.toLowerCase( attrId );
        
        if ( keyMap.containsKey( key ) )
        {
            Holder holder = keyMap.remove( key );
            
            if ( holder != null ) 
            {
                return holder.attribute;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }


    /**
     * Makes a shallow copy of the attribute set. The new set contains the same
     * attributes as the original set.
     * 
     * @return A non-null copy of this attribute set.
     */
    public Object clone()
    {
        try
        {
            AttributesImpl clone = (AttributesImpl)super.clone();
    
            clone.keyMap = new HashMap<String, Holder>( keyMap.size() );
            
            Iterator keys = keyMap.keySet().iterator();
    
            while ( keys.hasNext() )
            {
                String key = (String)keys.next();
                Holder holder = keyMap.get( key );
                clone.keyMap.put( key, (Holder)holder.clone() );
            }
            
            return clone;
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
    }


    /**
     * Returns a string representation of the object.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();

        Iterator attrs = keyMap.values().iterator();
        
        while ( attrs.hasNext() )
        {
            Holder holder = (Holder)attrs.next();
            Attribute attr = holder.attribute;

            buf.append( holder.upId );
            buf.append( ": " );
            buf.append( attr );
        }

        return buf.toString();
    }


    /**
     * Checks to see if this Attributes implemenation is equivalent to another.
     * The comparision does not take into account the implementation or any
     * Lockable interface properties. Case independent lookups by Attribute ID
     * is considered to be significant.
     * 
     * @param obj
     *            the Attributes object to test for equality to this
     * @return true if the Attributes are equal false otherwise
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( ( obj == null ) || !( obj instanceof AttributesImpl ) )
        {
            return false;
        }

        Attributes attrs = ( Attributes ) obj;

        if ( attrs.size() != size() )
        {
            return false;
        }

        if ( attrs.isCaseIgnored() != isCaseIgnored() )
        {
            return false;
        }

        NamingEnumeration list = attrs.getAll();

        while ( list.hasMoreElements() )
        {
            Attribute attr = ( Attribute )list.nextElement();
            Attribute myAttr = get( attr.getID() );

            if ( myAttr == null )
            {
                return false;
            }

            if ( !myAttr.equals( attr ) )
            {
                return false;
            }
        }

        return true;
    }
}
