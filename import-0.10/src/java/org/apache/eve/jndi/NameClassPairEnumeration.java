/*
 * $Id: NameClassPairEnumeration.java,v 1.3 2003/03/13 18:27:33 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.jndi ;
import javax.naming.NamingEnumeration;
import org.apache.eve.backend.Cursor;
import javax.naming.NamingException;
import org.apache.eve.backend.LdapEntry;
import javax.naming.NameClassPair;
import org.apache.eve.backend.UnifiedBackend;
import java.util.Hashtable;
import org.apache.avalon.framework.ExceptionUtil;


public class NameClassPairEnumeration
    implements NamingEnumeration
{
    private final Cursor m_cursor ;
    private final UnifiedBackend m_nexus ;


    NameClassPairEnumeration(UnifiedBackend a_nexus, Cursor a_cursor)
    {
        m_nexus = a_nexus ;
        m_cursor = a_cursor ;
    }


    public void close()
        throws NamingException
    {
        m_cursor.close() ;
    }


    public boolean hasMore()
        throws NamingException
    {
        return m_cursor.hasMore() ;
    }


    public Object next()
        throws NamingException
    {
        String l_rdn = null ;
        String l_class = null ;

        LdapEntry l_entry = (LdapEntry) m_cursor.next() ;
        if(l_entry.hasAttribute(ContextHelper.JCLASSNAME_ATTR)) {
            l_class = (String)
                l_entry.getSingleValue(ContextHelper.JCLASSNAME_ATTR) ;
        } else {
            l_class = UnifiedLdapContext.class.getName() ;
        }

        l_rdn = m_nexus.getNormalizedName(l_entry.getEntryDN()).get(0) ;
        return new NameClassPair(l_rdn, l_class) ;
    }


    public boolean hasMoreElements()
    {
        return m_cursor.hasMoreElements() ;
    }


    public Object nextElement()
    {
        String l_rdn = null ;
        String l_class = null ;

        LdapEntry l_entry = (LdapEntry) m_cursor.nextElement() ;
        if(l_entry.hasAttribute(ContextHelper.JCLASSNAME_ATTR)) {
            l_class = (String)
                l_entry.getSingleValue(ContextHelper.JCLASSNAME_ATTR) ;
        } else {
            l_class = UnifiedLdapContext.class.getName() ;
        }

        try {
            l_rdn = m_nexus.getNormalizedName(l_entry.getEntryDN()).get(0) ;
        } catch(NamingException e) {
            try {
                m_cursor.close() ;
            } catch(Exception e2) { }
            throw new java.util.NoSuchElementException("Premature close of "
                + "backend cursor:\n" + ExceptionUtil.printStackTrace(e)) ;
        }

        return new NameClassPair(l_rdn, l_class) ;
    }
}
