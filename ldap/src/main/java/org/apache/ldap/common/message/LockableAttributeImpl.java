/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.ldap.common.message ;


import java.util.ArrayList;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;

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
    /** the name of the attribute */
    private final String id ;
    /** the list of attribute values */
    private final ArrayList list ;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a permanently LockableAttribute on id whose locking behavoir
     * is dicatated by parent.
     *
     * @param id the id or name of this attribute.
     */
    public LockableAttributeImpl( final String id )
    {
        this.id = id ;
        list = new ArrayList() ;
    }


    /**
     * Creates a permanently LockableAttribute on id with a single value.
     *
     * @param id the id or name of this attribute.
     * @param value a value for the attribute
     */
    public LockableAttributeImpl( final String id, final Object value )
    {
        this.id = id ;
        list = new ArrayList() ;
        list.add( value );
    }




    /**
     * Creates a permanently LockableAttribute on id with a single value.
     *
     * @param id the id or name of this attribute.
     * @param value a value for the attribute
     */
    public LockableAttributeImpl( final String id, final byte[] value )
    {
        this.id = id ;
        list = new ArrayList() ;
        list.add( value );
    }


    /**
     * Creates a permanently LockableAttribute on id whose locking behavoir
     * is dicatated by parent.  Used for the clone method.
     *
     * @param id the id or name of this attribute
     * @param list the list of values to start with
     */
    private LockableAttributeImpl( final String id, final ArrayList list )
    {
        this.id = id ;
        this.list = list ;
    }

    
    // ------------------------------------------------------------------------
    // javax.naming.directory.Attribute Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets a NamingEnumberation wrapped around the iterator of the value list.
     *
     * @return the Iterator wrapped as a NamingEnumberation.
     */
    public NamingEnumeration getAll()
    {
        return new IteratorNamingEnumeration( list.iterator() ) ;
    }


    /**
     * Gets the first value of the list or null if no values exist.
     *
     * @return the first value or null.
     */
    public Object get()
    {
        if ( list.isEmpty() )
        {
            return null ;
        }

        return list.get( 0 ) ;
    }


    /**
     * Gets the size of the value list.
     *
     * @return size of the value list.
     */
    public int size()
    {
        return list.size() ;
    }


    /**
     * Gets the id or name of this Attribute.
     *
     * @return the identifier for this Attribute.
     */
    public String getID()
    {
        return id ;
    }


    /**
     * Checks to see if this Attribute contains attrVal in the list.
     *
     * @param attrVal the value to test for
     * @return true if attrVal is in the list backing store, false otherwise
     */
    public boolean contains( Object attrVal )
    {
        return list.contains( attrVal ) ;
    }


    /**
     * Adds attrVal into the list of this Attribute's values at the end of the
     * list.
     *
     * @param attrVal the value to add to the end of the list.
     * @return true if attrVal is added to the list backing store, false if it
     * already existed there.
     */
    public boolean add( Object attrVal )
    {
        return list.add( attrVal ) ;
    }


    /**
     * Removes attrVal from the list of this Attribute's values.
     *
     * @param attrVal the value to remove
     * @return true if attrVal is remove from the list backing store, false if
     * never existed there.
     */
    public boolean remove( Object attrVal )
    {
        return list.remove( attrVal ) ;
    }


    /**
     * Removes all the values of this Attribute from the list backing store.
     */
    public void clear()
    {
        list.clear() ;
    }


    /**
     * NOT SUPPORTED - throws OperationNotSupportedException
     * @see javax.naming.directory.Attribute#getAttributeSyntaxDefinition()
     */
    public DirContext getAttributeSyntaxDefinition()
        throws NamingException
    {
        throw new OperationNotSupportedException(
            "Extending subclasses may override this if they like!" ) ;
    }


    /**
     * NOT SUPPORTED - throws OperationNotSupportedException
     * @see javax.naming.directory.Attribute#getAttributeDefinition()
     */
    public DirContext getAttributeDefinition()
        throws NamingException
    {
        throw new OperationNotSupportedException(
            "Extending subclasses may override this if they like!" ) ;
    }


    /**
     * Not a deep clone.
     *
     * @return a copy of this attribute using the same parent lock and id 
     * containing references to all the values of the original.
     */
    public Object clone()
    {
        ArrayList l_list = ( ArrayList ) list.clone() ;
        return new LockableAttributeImpl( id, l_list ) ;
    }


    /**
     * Always returns true since list is used to preserve value addition order.
     *
     * @return true.
     */
    public boolean isOrdered()
    {
        return true ;
    }


    /**
     * Gets the value at an index.
     *
     * @param index the index of the value in the ordered list of attribute
     * values. 0 <= ix < size().
     * @return this Attribute's value at the index null if no values exist.
     */
    public Object get( int index )
    {
        return list.get( index ) ;
    }


    /**
     * Removes the value at an index.
     *
     * @param index the index of the value in the ordered list of attribute
     * values. 0 <= ix < size().
     * @return this Attribute's value removed at the index
     */
    public Object remove( int index )
    {
        return list.remove( index ) ;
    }


    /**
     * Inserts attrVal into the list of this Attribute's values at the
     * specified index in the list.
     *
     * @param index the index to add the value at.
     * @param attrVal the value to add to the end of the list.
     */
    public void add( int index, Object attrVal )
    {
        list.add( index, attrVal ) ;
    }


    /**
     * Sets an attribute value in the ordered list of attribute values.
     *
     * @param index the index to set the value to.
     * @param attrVal the value to set at the index.
     * @return the old value at the specified index.
     */
    public Object set( int index, Object attrVal )
    {
        return list.set( index, attrVal ) ;
    }


    /**
     * Checks for equality between this Attribute instance and another.  The
     * lockable properties are not factored into the equality semantics and
     * neither is the Attribute implementation.  The Attribute ID's are
     * compared with regard to case and value order is only considered if
     * the Attribute arguement is ordered itself.
     *
     * @todo start looking at comparing syntaxes to determine if attributes are
     * really equal
     * @param obj the Attribute to test for equality
     * @return true if the obj is an Attribute and equals this Attribute false
     *  otherwise
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( ! ( obj instanceof Attribute ) )
        {
            return false;
        }

        Attribute attr = ( Attribute ) obj;
        if ( ! id.equals( attr.getID() ) )
        {
            return false;
        }

        if ( attr.size() != list.size() )
        {
            return false;
        }

//        if ( attr.isOrdered() )
//        {
//            for ( int ii = 0; ii < attr.size(); ii++ )
//            {
//                try
//                {
//                    if ( ! list.get( ii).equals( attr.get( ii ) ) )
//                    {
//                        return false;
//                    }
//                }
//                catch ( NamingException e )
//                {
//                    log.warn( "Failed to get an attribute from the specifid attribute: " + attr, e );
//                    return false;
//                }
//            }
//        }
//        else
//        {
            for ( int ii = 0; ii < attr.size(); ii++ )
            {
                try
                {
                    if ( ! list.contains( attr.get( ii ) ) )
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
//        }

        return true;
    }
}

