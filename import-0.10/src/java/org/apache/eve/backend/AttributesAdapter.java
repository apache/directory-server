/*
 * $Id: AttributesAdapter.java,v 1.4 2003/04/09 15:51:25 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.Vector ;
import java.util.Iterator ;
import java.util.ArrayList ;
import java.util.Collection ;


import javax.naming.NamingException ;
import javax.naming.NamingEnumeration ;
import javax.naming.directory.Attribute ;
import javax.naming.directory.DirContext ;
import javax.naming.directory.Attributes ;

import org.apache.ldap.common.NotImplementedException ;

import org.apache.avalon.framework.CascadingRuntimeException ;


/**
 * An Attributes interface adapter around an LdapEntry.
 */
public class AttributesAdapter
    implements Attributes
{
	private final LdapEntry m_entry ;


    public AttributesAdapter(LdapEntry a_entry) {
        m_entry = a_entry ;
    }


    /**
      * Determines whether the attribute set ignores the case of
      * attribute identifiers when retrieving or adding attributes.
      * @return true if case is ignored; false otherwise.
      */
    public boolean isCaseIgnored()
    {
        return true ;
    }


    /**
      * Retrieves the number of attributes in the attribute set.
      *
      * @return The nonnegative number of attributes in this attribute set.
      */
    public int size()
    {
        return m_entry.attributes().size() ;
    }

    /**
      * Retrieves the attribute with the given attribute id from the
      * attribute set.
      *
      * @param attrID The non-null id of the attribute to retrieve.
      * 	  If this attribute set ignores the character
      *		  case of its attribute ids, the case of attrID
      *		  is ignored.
      * @return The attribute identified by attrID; null if not found.
      * @see #put
      * @see #remove
      */
    public Attribute get(String attrId)
    {
        if(m_entry.hasAttribute(attrId)) {
	        return new AttributeAdapter(m_entry.getMultiValue(attrId), attrId) ;
        }

        return null ;
    }


    /**
      * Retrieves an enumeration of the attributes in the attribute set.
      * The effects of updates to this attribute set on this enumeration
      * are undefined.
      *
      * @return A non-null enumeration of the attributes in this attribute set.
      *		Each element of the enumeration is of class <tt>Attribute</tt>.
      * 	If attribute set has zero attributes, an empty enumeration 
      * 	is returned.
      */
    public NamingEnumeration getAll()
    {
        final Iterator l_list = m_entry.attributes().iterator() ;
		return new NamingEnumeration() {
			public boolean hasMore() { return l_list.hasNext() ; }
			public boolean hasMoreElements() { return l_list.hasNext() ; }
			public Object nextElement()
            {
                String l_attrId = (String) l_list.next() ;
                Attribute l_attr = new AttributeAdapter(
                    m_entry.getMultiValue(l_attrId), l_attrId) ;
                return l_attr ;
            }
			public Object next()
            {
                String l_attrId = (String) l_list.next() ;
                Attribute l_attr = new AttributeAdapter(
                    m_entry.getMultiValue(l_attrId), l_attrId) ;
                return l_attr ;
            }
			public void close() {}
		} ;
    }


    /**
      * Retrieves an enumeration of the ids of the attributes in the
      * attribute set.
      * The effects of updates to this attribute set on this enumeration
      * are undefined.
      *
      * @return A non-null enumeration of the attributes' ids in
      * 	this attribute set. Each element of the enumeration is
      *		of class String.
      * 	If attribute set has zero attributes, an empty enumeration 
      * 	is returned.
      */
    public NamingEnumeration getIDs()
    {
        final Iterator l_list = m_entry.attributes().iterator() ;
		return new NamingEnumeration() {
			public boolean hasMore() { return l_list.hasNext() ; }
			public boolean hasMoreElements() { return l_list.hasNext() ; }
			public Object nextElement() { return l_list.next() ; }
			public Object next() { return l_list.next() ; }
			public void close() {}
		} ;
    }


    /**
      * Adds a new attribute to the attribute set.
      *
      * @param attrID 	non-null The id of the attribute to add.
      * 	  If the attribute set ignores the character
      *		  case of its attribute ids, the case of attrID
      *		  is ignored.
      * @param val	The possibly null value of the attribute to add.
      *			If null, the attribute does not have any values.
      * @return The Attribute with attrID that was previous in this attribute set;
      * 	null if no such attribute existed.
      * @see #remove
      */
    public Attribute put(String attrID, Object val)
    {
        try {
        	m_entry.addValue(attrID, val) ;
        } catch(NamingException e) {
            throw new CascadingRuntimeException("Naming exception thrown", e) ;
        }

        return new AttributeAdapter(m_entry.getMultiValue(attrID), attrID) ;
    }


    /**
      * Adds a new attribute to the attribute set.
      *
      * @param attr 	The non-null attribute to add.
      * 		If the attribute set ignores the character
      *		  	case of its attribute ids, the case of
      * 		attr's identifier is ignored.
      * @return The Attribute with the same ID as attr that was previous 
      * 	in this attribute set;
      * 	null if no such attribute existed.
      * @see #remove
      */
    public Attribute put(Attribute attr)
    {
        try {
			NamingEnumeration l_list = attr.getAll() ;
			while(l_list.hasMore()) {
				m_entry.addValue(attr.getID(), l_list.next()) ;
			}
	
			return new AttributeAdapter(m_entry.getMultiValue(attr.getID()),
				attr.getID());
        } catch(NamingException e) {
            throw new CascadingRuntimeException("Naming exception thrown", e) ;
        }
    }


    /**
      * Removes the attribute with the attribute id 'attrID' from
      * the attribute set. If the attribute does not exist, ignore.
      *
      * @param attrID 	The non-null id of the attribute to remove.
      * 		If the attribute set ignores the character
      *		  	case of its attribute ids, the case of 
      *               	attrID is ignored.
      * @return The Attribute with the same ID as attrID that was previous 
      * 	in the attribute set;
      * 	null if no such attribute existed.
      */
    public Attribute remove(String attrId)
    {
        try {
			Attribute l_attr =
				new AttributeAdapter(m_entry.getMultiValue(attrId), attrId) ;
			m_entry.removeValues(attrId) ;
			return l_attr ;
        } catch(NamingException e) {
            throw new CascadingRuntimeException("Naming exception thrown", e) ;
        }
    }


    /**
      * Makes a copy of the attribute set.
      * The new set contains the same attributes as the original set:
      * the attributes are not themselves cloned.
      *
      * @return A non-null copy of this attribute set.
      */
    public Object clone() {
        throw new UnsupportedOperationException(
            "AttributesAdapter for event delivery does not allow clone ops.") ;
    }
}
