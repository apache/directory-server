/*
 * $Id: SingletonCursor.java,v 1.2 2003/03/13 18:27:01 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.NoSuchElementException ;


/**
 * An cursor that returns only one element! Used as annon inner class in so
 * many places I had to create this one as well.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class SingletonCursor
    extends Cursor
{
    private final Object m_singleton ;

    public SingletonCursor(Object a_singleton)
    {
        m_singleton = a_singleton ;
    }

    /**
     * Throws NoSuchElementException all the time.
     */
    public Object advance()
    {
        try { close() ; } catch(Exception e) {}
        return m_singleton ;
    }

    /**
     * Always returns false.
     */
    public boolean canAdvance()
    {
        return !isClosed() ;
    }

    /**
     * Does nothing.
     */
    public void freeResources() { }
}
