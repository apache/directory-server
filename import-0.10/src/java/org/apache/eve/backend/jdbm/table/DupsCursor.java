/*
 * $Id: DupsCursor.java,v 1.3 2003/03/13 18:27:30 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


import java.util.TreeSet ;
import java.util.Iterator ;
import java.util.ArrayList ;
import java.util.Collections ;

import javax.naming.NamingException ;

import org.apache.eve.backend.Cursor ;
import org.apache.eve.backend.BackendException ;

import jdbm.helper.Tuple ;


/**
 * Cursor that iterates over duplicate values nested into a value using a
 * TreeSet.
 *
 * @warning The Tuple returned by this Cursor is always the same instance object
 * returned every time. It is reused to for the sake of efficency rather than
 * creating a new tuple for each advance() call.
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.3 $
 */
public class DupsCursor
    extends Cursor
{
    private final Tuple m_returned = new Tuple() ;
    private final Tuple m_prefetched = new Tuple() ;
    private final NoDupsCursor m_cursor ;

    private Iterator m_iterator ;
    private Tuple m_duplicates ;


    DupsCursor(NoDupsCursor a_cursor)
        throws BackendException
    {
        m_cursor = a_cursor ;

        try {
            // Protect against closed cursors
            if(m_cursor.isClosed()) {
                close() ;
                return ;
            }
    
            prefetch() ;
        } catch(NamingException e) { /* NEVER THROWN */ }
    }


    private void prefetch()
        throws BackendException, NamingException
    {
        while(null == m_iterator || !m_iterator.hasNext()) {
            if(m_cursor.hasMoreElements()) {
                m_duplicates = (Tuple) m_cursor.next() ;
                TreeSet l_set = (TreeSet) m_duplicates.getValue() ;

                if(m_cursor.doAscendingScan()) {
                    m_iterator = l_set.iterator() ;
                } else {
                    ArrayList l_list = new ArrayList(l_set.size()) ;
                    l_list.addAll(l_set) ;
                    Collections.reverse(l_list) ;
                    m_iterator = l_list.iterator() ;
                }
            } else {
                close() ;
                return ;
            }
        }

        m_prefetched.setKey(m_duplicates.getKey()) ;
        m_prefetched.setValue(m_iterator.next()) ;
    }


    public Object advance()
        throws BackendException, NamingException
    {
        m_returned.setKey(m_prefetched.getKey()) ;
        m_returned.setValue(m_prefetched.getValue()) ;

        prefetch() ;

        return m_returned ;
    }


    public boolean canAdvance()
    {
        return !isClosed() ;
    }


    public void freeResources() { /* Does nothing! */ }
}
