/*
 * $Id: TupleIteratorCursor.java,v 1.2 2003/03/13 18:27:35 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


import java.util.Iterator ;
import org.apache.eve.backend.Cursor;
import jdbm.helper.Tuple;


/**
 * A cursor using an underlying Iterator.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class TupleIteratorCursor
    extends Cursor
{
    private final Object m_key ;
    private final Iterator m_iterator ;
    private final Tuple m_tuple = new Tuple() ;


    /**
     * Creates a cursor over an Iterator.
     * 
     * @param a_iterator the underlying iterator this cursor uses.
     */
    public TupleIteratorCursor(Object a_key, Iterator a_iterator)
    {
        m_key = a_key ;
        m_tuple.setKey(a_key) ;
        m_iterator = a_iterator ;
    }


    /** Returns iterator.next() */
    public Object advance()
    {
        m_tuple.setKey(m_key) ;
        m_tuple.setValue(m_iterator.next()) ;
        return m_tuple ;
    }


    /** Returns iterator.hasNext() */
    public boolean canAdvance()
    {
        return m_iterator.hasNext() ;
    }


    /** Does nothing */
    public void freeResources() { }
}
