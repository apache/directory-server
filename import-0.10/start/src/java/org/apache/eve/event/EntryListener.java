/*
 * $Id: EntryListener.java,v 1.2 2003/03/13 18:27:20 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;

import java.util.EventListener ;

public interface EntryListener
    extends EventListener
{
    void entryAdded(EntryEvent an_event) ;
    void entryChanged(EntryEvent an_event) ;
    void entryRemoved(EntryEvent an_event) ;
}

