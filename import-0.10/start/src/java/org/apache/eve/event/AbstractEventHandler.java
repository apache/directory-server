/*
 * $Id: AbstractEventHandler.java,v 1.2 2003/03/13 18:27:14 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;

import org.apache.avalon.framework.logger.AbstractLogEnabled ;

public abstract class AbstractEventHandler
    extends AbstractLogEnabled
    implements EventHandler
{
}

