/*
 * $Id: InputListener.java,v 1.2 2003/03/13 18:27:21 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event ;
import org.apache.avalon.framework.CascadingException;
import java.util.EventListener;


public interface InputListener
    extends EventListener
{
    void inputReceived(InputEvent an_event) ;
}
