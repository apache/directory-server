/*
 * $Id: EntryEventSource.java,v 1.2 2003/03/13 18:27:19 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;

public interface EntryEventSource
{
    void addEntryListener(EntryListener a_listener) ;
    void removeEntryListener(EntryListener a_listener) ;
}

