/*
 * $Id: CursorListener.java,v 1.2 2003/03/13 18:26:50 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.EventListener ;


public interface CursorListener
    extends EventListener
{
	void cursorAdvanced(CursorEvent an_event) ;
}
