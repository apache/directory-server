/*
 * $Id: ResponseListener.java,v 1.4 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.util.EventListener ;

import org.apache.avalon.framework.CascadingRuntimeException ;


/**
 * Components which listen for the composition of a response.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.4 $
 */
public interface ResponseListener
    extends EventListener
{
    /**
     * Listener's handler method to take action upon the reciept of a
     * ResponseEvent.
     *
     * @param a_event the ResponseEvent to handle.
     */
    void responseComposed( ResponseEvent a_event ) ;
}
