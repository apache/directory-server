/*
 * $Id: IteratorCursor.java,v 1.2 2003/03/13 18:26:54 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.Iterator ;


/**
 * A cursor using an underlying Iterator.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class IteratorCursor
    extends Cursor
{
    private final Iterator m_iterator ;

    /**
     * Creates a cursor over an Iterator.
     * 
     * @param a_iterator the underlying iterator this cursor uses.
     */
    public IteratorCursor(Iterator a_iterator)
    {
        m_iterator = a_iterator ;
    }


    /** Returns iterator.next() */
    public Object advance()
    {
        return m_iterator.next() ;
    }


    /** Returns iterator.hasNext() */
    public boolean canAdvance()
    {
        return m_iterator.hasNext() ;
    }


    /** Does nothing */
    public void freeResources() { }
}
