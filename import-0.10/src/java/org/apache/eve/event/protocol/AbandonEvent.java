/*
 * $Id: AbandonEvent.java,v 1.2 2003/03/13 18:27:25 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


import java.util.EventObject ;


/**
 * Event which logically represents a protocol abandon operation.
 */
public class AbandonEvent
    extends ProtocolEvent
{
    public AbandonEvent(Object a_src, boolean a_isPduDelivered)
    {
       super(a_src, ABANDONREQUEST_MASK, a_isPduDelivered) ;
    }
}
