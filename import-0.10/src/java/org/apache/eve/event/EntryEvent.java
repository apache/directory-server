/*
 * $Id: EntryEvent.java,v 1.2 2003/03/13 18:27:19 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.util.EventObject ;


public class EntryEvent
    extends EventObject
{
    public EntryEvent(Object a_source)
    {
        super(a_source) ;
    }
}
