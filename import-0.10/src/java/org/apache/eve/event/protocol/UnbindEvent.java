/*
 * $Id: UnbindEvent.java,v 1.2 2003/03/13 18:27:30 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


/**
 * Event which logically represents a protocol unbind operation.
 */
public class UnbindEvent
    extends ProtocolEvent
{
    public UnbindEvent(Object a_src, boolean pduDelivered)
    {
       super(a_src, UNBINDREQUEST_MASK, pduDelivered) ;
    }
}
