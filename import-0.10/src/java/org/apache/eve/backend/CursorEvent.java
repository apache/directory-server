/*
 * $Id: CursorEvent.java,v 1.2 2003/03/13 18:26:47 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.EventObject;


public class CursorEvent
	extends EventObject
{
	public final Object element ;

    CursorEvent(final Cursor a_cursor, final Object a_element)
    {
        super(a_cursor) ;

        element = a_element ;
    }


    Cursor getCursorSource()
    {
        return (Cursor) this.source ;
    }


    Object getElement()
    {
        return element ;
    }
}
