/*
 * $Id: EventHandler.java,v 1.3 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;


import java.util.EventObject ;
import java.util.EventListener ;

import org.apache.avalon.framework.logger.LogEnabled ;
import org.apache.avalon.framework.service.Serviceable ;
import org.apache.avalon.framework.context.Contextualizable ;
import org.apache.avalon.framework.CascadingRuntimeException ;


/**
 * Event handler used by Stages.
 *
 * @todo Should this even be LogEnabled? Not a good idea I think. Let's think
 * about refactoring this later down the road.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.3 $
 */
public interface EventHandler
    extends EventListener, LogEnabled
{
    /**
     * Exception is explicitly made to be a runtime exception so it can tunnel
     * up through the run() call of handler Runnable.
     *
     * @param a_event the event to process or handle.
     */
    void handleEvent( EventObject a_event ) ;
}
