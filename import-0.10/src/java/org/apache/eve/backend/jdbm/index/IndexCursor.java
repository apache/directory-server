/*
 * $Id: IndexCursor.java,v 1.3 2003/03/13 18:27:26 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.index ;


import org.apache.eve.backend.Cursor ;
import org.apache.eve.backend.BackendException;
import javax.naming.NamingException;
import jdbm.helper.Tuple;
import org.apache.regexp.RE;


public class IndexCursor
    extends Cursor
{
    private final RE m_re ;
    private final IndexRecord m_tmp = new IndexRecord() ;
    private final IndexRecord m_returned = new IndexRecord() ;
    private final IndexRecord m_prefetched = new IndexRecord() ;
    private final boolean swapKeyVal ;
    private final Cursor m_cursor ;


    IndexCursor(Cursor a_cursor)
        throws BackendException, NamingException
    {
        this(a_cursor, false, null) ;
    }


    IndexCursor(Cursor a_cursor, boolean swapKeyVal)
        throws BackendException, NamingException
    {
        this(a_cursor, swapKeyVal, null) ;
    }


    IndexCursor(Cursor a_cursor, boolean swapKeyVal, RE a_regex)
        throws BackendException, NamingException
    {
        m_re = a_regex ;
        m_cursor = a_cursor ;
        this.swapKeyVal = swapKeyVal ;

        if(a_cursor.isClosed()) {
            super.close() ;
            return ;
        }

        prefetch() ;
    }


    private void prefetch()
        throws BackendException, NamingException
    {
        while(m_cursor.hasMore()) {
            Tuple l_tuple = (Tuple) m_cursor.next() ;

            if(swapKeyVal) {
                m_tmp.setSwapped(l_tuple) ;
            } else {
                m_tmp.setTuple(l_tuple) ;
            }

            // If regex is null just transfer into prefetched from tmp record
            // but if it is not then use it to match.  Successful match shorts
            // while loop.
            if(null == m_re || m_re.match((String) m_tmp.getIndexKey())) {
                m_prefetched.setIndexKey(m_tmp.getIndexKey()) ;
                m_prefetched.setEntryId(m_tmp.getEntryId()) ;
                return ;
            }
        }

        // If we got down here then m_cursor has been consumed without a match!
        close() ;
    }


    protected Object advance()
        throws BackendException, NamingException
    {
        m_returned.setEntryId(m_prefetched.getEntryId()) ;
        m_returned.setIndexKey(m_prefetched.getIndexKey()) ;
        prefetch() ;
        return m_returned ;
    }


    protected boolean canAdvance()
        throws BackendException, NamingException
    {
        return !isClosed() ;
    }


    protected void freeResources() {/*Does nothing*/}
}
