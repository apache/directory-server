/*
 * $Id: SearchResultEnumeration.java,v 1.3 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.jndi ;


import java.util.HashMap ;

import javax.naming.NamingEnumeration ;

import org.apache.eve.backend.Cursor ;
import javax.naming.directory.SearchResult ;
import org.apache.eve.backend.LdapEntry ;
import java.util.Iterator;
import javax.naming.directory.BasicAttributes ;
import javax.naming.directory.DirContext ;
import javax.naming.Name ;
import org.apache.eve.backend.UnifiedBackend ;
import java.util.Collections ;
import javax.naming.NamingException ;
import org.apache.avalon.framework.ExceptionUtil ;
import javax.naming.directory.SearchControls ;



public class SearchResultEnumeration
    implements NamingEnumeration
{
    private final String [] m_attribsToReturn ;
    private final UnifiedBackend m_nexus ;
    private final DirContext m_context ;
    private final Cursor m_cursor ;
    private final SearchControls m_ctls ;


    SearchResultEnumeration(DirContext a_context,
        UnifiedBackend a_nexus, Cursor a_cursor, SearchControls a_ctls)
    {
        this(a_context, a_nexus, a_cursor, null, a_ctls) ;
    }


    SearchResultEnumeration(DirContext a_context, UnifiedBackend a_nexus,
        Cursor a_cursor, String [] a_attribsToReturn, SearchControls a_ctls)
    {
        m_ctls = a_ctls ;
        m_nexus = a_nexus ;
        m_cursor = a_cursor ;
        m_context = a_context ;

        if(a_attribsToReturn != null && a_attribsToReturn.length > 0) {
            m_attribsToReturn = a_attribsToReturn ;
        } else {
            m_attribsToReturn = m_ctls.getReturningAttributes() ;
        }
    }


    SearchResult getResult(LdapEntry a_entry)
        throws NamingException
    {
        Object l_obj = null ;
        BasicAttributes l_attribs = new BasicAttributes(true) ;

        if(null == m_attribsToReturn) {
            Iterator l_list = a_entry.attributes().iterator() ;
            while(l_list.hasNext()) {
                l_attribs.put(DirContextHelper.getAttribute(a_entry,
                    (String) l_list.next())) ;
            }
        } else {
            for(int ii = 0; ii < m_attribsToReturn.length; ii++) {
                l_attribs.put(DirContextHelper.getAttribute(a_entry,
                    m_attribsToReturn[ii])) ;
            }
        }

        if(a_entry.hasAttribute(ContextHelper.JCLASSNAME_ATTR)) {
            l_obj = ContextHelper.deserialize(a_entry) ;
        } else {
            l_obj = new UnifiedLdapContext( m_context.getEnvironment(),
                a_entry ) ;
        }

        return new SearchResult(a_entry.getEntryDN(), l_obj, l_attribs, false) ;
    }


    public boolean hasMore()
        throws NamingException
    {
        return m_cursor.hasMore() ;
    }


    public boolean hasMoreElements()
    {
        try {
            return hasMore() ;
        } catch(NamingException e) {
            throw new java.util.NoSuchElementException("Could not get next "
                + " element due to underlying exception:\n"
                + ExceptionUtil.printStackTrace(e)) ;
        }
    }


    public Object next()
        throws NamingException
    {
        return getResult((LdapEntry) m_cursor.next()) ;
    }


    public Object nextElement()
    {
        try {
            return next() ;
        } catch(NamingException e) {
            throw new java.util.NoSuchElementException("Could not get next "
                + " element due to underlying exception:\n"
                + ExceptionUtil.printStackTrace(e)) ;
        }
    }


    public void close()
        throws NamingException
    {
        m_cursor.close() ;
    }
}
