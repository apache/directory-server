/*
 * $Id: NoDupsCursor.java,v 1.3 2003/03/13 18:27:33 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


import org.apache.eve.backend.Cursor ;
import org.apache.eve.backend.BackendException ;

import jdbm.helper.TupleBrowser ;
import jdbm.helper.Tuple;
import java.io.IOException ;
import javax.naming.NamingException ;

import org.apache.avalon.framework.ExceptionUtil ;


/**
 * A simple cursor over a TupleBrowser on a table that does not allow
 * duplicates.
 * 
 * @warning The Tuple returned by this Cursor is always the same instance object
 * returned every time. It is reused to for the sake of efficency rather than
 * creating a new tuple for each advance() call.
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.3 $
 */
public class NoDupsCursor
    extends Cursor
{
    private final Tuple m_returned = new Tuple() ;
    private final Tuple m_prefetched = new Tuple() ;
    private final TupleBrowser m_browser ;
    private final boolean doAscendingScan ;

    protected boolean canAdvance = true ;


    /**
     * Creates a cursor over a TupleBrowser where duplicates are not expected.
     */
    NoDupsCursor(TupleBrowser a_browser, boolean doAscendingScan)
        throws BackendException
    {
        m_browser = a_browser ;
        this.doAscendingScan = doAscendingScan ;
        prefetch() ;
    }


    boolean doAscendingScan()
    {
        return this.doAscendingScan ;
    }


    void prefetch()
        throws BackendException
    {
        // Prefetch into tuple!
        boolean isSuccess = false ;

        try {
            if(doAscendingScan) {
                isSuccess = m_browser.getNext(m_prefetched) ;
            } else {
                isSuccess = m_browser.getPrevious(m_prefetched) ;
            }
        } catch(IOException e) {
            throw new BackendException("Could not advance cursor due to "
                + " TupleBrowser failure:\n"
                + ExceptionUtil.printStackTrace(e)) ;
        }

        if(!isSuccess) {
            canAdvance = false ;
            try { close() ; } catch(NamingException e) { /* Never thrown */ }
        }
    }


    protected Object advance()
        throws BackendException
    {
        m_returned.setKey(m_prefetched.getKey()) ;
        m_returned.setValue(m_prefetched.getValue()) ;

        prefetch() ;

        return m_returned ;
    }


    protected boolean canAdvance()
    {
        return canAdvance ;
    }


    public void freeResources() { /* Does nothing! */ }
}
