/*
 * $Id: EntryCursor.java,v 1.3 2003/03/13 18:27:14 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm ;


import org.apache.eve.backend.Cursor ;
import org.apache.eve.backend.BackendException ;
import org.apache.eve.backend.jdbm.index.IndexRecord ;

import javax.naming.NamingException ;

/**
 * @testcase org.apache.eve.backend.jdbm.TestEntryCursor 
 */
public class EntryCursor
    extends Cursor
{
	protected final JdbmModule m_backend ;
    protected final Cursor m_cursor ;

    /**
     * Creates a cursor over all the elements in the table.  The key will change
     * to represent the value of key pointed to by this cursor.
     *
     * @param a_table Db to iterate over.
     * @throws DbException if something goes drastically wrong.
     */
    public EntryCursor(JdbmModule a_backend, Cursor a_cursor)
    {
		m_cursor = a_cursor ;
        m_backend = a_backend ;
    }


    protected Object advance()
        throws BackendException, NamingException
    {
		IndexRecord l_rec = (IndexRecord) m_cursor.next() ;
        return m_backend.read(l_rec.getEntryId()) ;
    }


    protected boolean canAdvance()
        throws BackendException, NamingException
    {
		return m_cursor.hasMore() ;
    }


    protected void freeResources()
    {
        try {
            m_cursor.close() ;
        } catch (Exception e) {
            e.printStackTrace() ;
            getLogger().error("While closing underlying IndexCursor: ", e) ;
        }
    }
}
