/*
 * $Id: Stage.java,v 1.3 2003/03/13 18:28:15 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.seda ;


import java.util.EventObject ;
import org.apache.avalon.framework.CascadingRuntimeException ;


public interface Stage
{
    void enqueue(EventObject an_event) throws CascadingRuntimeException ;
    void addPredicate(EnqueuePredicate a_predicate) ;
}

