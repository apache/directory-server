/*
 * $Id: AttributeAdapter.java,v 1.3 2003/04/09 15:51:25 akarasulu Exp $
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
import org.apache.ldap.common.NotImplementedException ;
import javax.naming.NamingEnumeration ;
import javax.naming.directory.Attribute ;
import javax.naming.directory.DirContext ;


/**
 * Attribute adapter which is a simple wrapper around a Collection.
 */
public class AttributeAdapter
    implements Attribute
{
	Collection m_collection ;
	String m_id ;


	public AttributeAdapter(Collection a_collection, String a_id) {
		m_id = a_id ;
		m_collection = a_collection ;
	}


	public NamingEnumeration getAll() throws NamingException
	{
		final Iterator l_list = m_collection.iterator() ;
		return new NamingEnumeration() {
			public boolean hasMore() { return l_list.hasNext() ; }
			public boolean hasMoreElements() { return l_list.hasNext() ; }
			public Object nextElement() { return l_list.next() ; }
			public Object next() { return l_list.next() ; }
			public void close() {}
		} ;
	}


	public Object get() throws NamingException
	{
		if(m_collection instanceof ArrayList) {
			ArrayList l_al = (ArrayList) m_collection ;
			if(l_al.size() > 0) {
				return l_al.get(0) ;
			} else {
				return null ;
			}
		}

		throw new NotImplementedException() ;
	}


	public int size()
	{
		return m_collection.size() ;
	}


	public String getID()
	{
		return m_id ;
	}


	public boolean contains(Object attrVal)
	{
		return m_collection.contains(attrVal) ;
	}


	public boolean add(Object attrVal)
	{
		if(m_collection.contains(attrVal)) {
			return false ;
		} else {
			m_collection.add(attrVal) ;
			return true ;
		}
	}


	public boolean remove(Object attrval)
	{
		return m_collection.remove(attrval) ;
	}


	public void clear()
	{
		m_collection.clear() ;
	}


	public DirContext getAttributeSyntaxDefinition() throws NamingException
	{
		throw new UnsupportedOperationException(
			"AttributeAdapter does not support schema info right now.") ;
	}


	public DirContext getAttributeDefinition() throws NamingException
	{
		throw new UnsupportedOperationException(
			"AttributeAdapter does not support schema info right now.") ;
	}


	public Object clone() {
		throw new UnsupportedOperationException(
			"AttributeAdapter for event delivery does not support clone().") ;
	}

	//----------- Methods to support ordered multivalued attributes

	public boolean isOrdered()
	{
		if(m_collection instanceof Vector ||
		   m_collection instanceof ArrayList) {
			return true ;
		}

		return false ;
	}


	public Object get(int ix) throws NamingException
	{
		if(m_collection instanceof ArrayList) {
			return ((ArrayList) m_collection).get(ix) ;
		} else if(m_collection instanceof Vector) {
			return ((Vector) m_collection).get(ix) ;
		}

		throw new UnsupportedOperationException(
			"Collection of " + m_collection.getClass().getName()
			+ "does not support ordered access to attribute values") ;
	}


	public Object remove(int ix)
	{
		if(m_collection instanceof ArrayList) {
			return ((ArrayList) m_collection).remove(ix) ;
		} else if(m_collection instanceof Vector) {
			return ((Vector) m_collection).remove(ix) ;
		}

		throw new UnsupportedOperationException(
			"Collection of " + m_collection.getClass().getName()
			+ "does not support ordered removal of attribute values") ;
	}


	public void add(int ix, Object attrVal)
	{
		if(m_collection instanceof ArrayList) {
			((ArrayList) m_collection).add(ix, attrVal) ;
			return ;
		} else if(m_collection instanceof Vector) {
			((Vector) m_collection).add(ix, attrVal) ;
			return ;
		}

		throw new UnsupportedOperationException(
			"Collection of " + m_collection.getClass().getName()
			+ "does not support ordered addition to attribute values") ;
	}


	public Object set(int ix, Object attrVal)
	{
		if(m_collection instanceof ArrayList) {
			return ((ArrayList) m_collection).set(ix, attrVal) ;
		} else if(m_collection instanceof Vector) {
			return ((Vector) m_collection).set(ix, attrVal) ;
		}

		throw new UnsupportedOperationException(
			"Collection of " + m_collection.getClass().getName()
			+ "does not support ordered alteration of attribute values") ;
	}
}
