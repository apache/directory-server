/*
 * $Id: EnumerationCursor.java,v 1.2 2003/03/13 18:26:52 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.Enumeration ;


/**
 * A cursor using an underlying Enumeration.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class EnumerationCursor
    extends Cursor
{
    private final Enumeration m_enumeration ;

    /**
     * Creates a cursor over an Enumeration.
     * 
     * @param a_enumeration the underlying Enumeration this cursor uses.
     */
    public EnumerationCursor(Enumeration a_enumeration)
    {
        m_enumeration = a_enumeration ;
    }


    /**
     * Returns enumeration.nextElement()
     * @return enumeration.nextElement()
     */
    public Object advance()
    {
        return m_enumeration.nextElement() ;
    }


    /**
     * Returns enumeration.hasMoreElements()
     * @return Returns enumeration.hasMoreElements()
     */
    public boolean canAdvance()
    {
        return m_enumeration.hasMoreElements() ;
    }


    /** Does nothing */
    public void freeResources() { }
}
