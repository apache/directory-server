/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.listener ;


import java.io.IOException ;
import java.nio.channels.Selector ;
import java.nio.channels.SelectionKey ;


/**
 * Used to monitor the activities of a ListenerManager.
 * 
 * @todo why the heck does this interface references to an implementation object
 * like a Selector?
 * 
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface ListenerManagerMonitor
{
    /**
     * Monitors when the ListnenerManager starts.
     */
    void started() ;
    
    /**
     * Monitors when the ListnenerManager stops.
     */
    void stopped() ;

    /**
     * Monitors bind occurences.
     * 
     * @param a_listener the listener just bound to a port
     */
    void bindOccured( ServerListener a_listener ) ;
    
    /**
     * Monitors unbind occurences.
     * 
     * @param a_listener the listener just unbound from a port
     */
    void unbindOccured( ServerListener a_listener ) ;
    
    /**
     * Monitors the occurrence of successful socket accept attempts
     * 
     * @param a_key
     */
    void acceptOccured( SelectionKey a_key ) ;
    
    /**
     * Monitors the occurrence of successful select calls on a selector
     * 
     * @param a_selector
     */
    void selectOccured( Selector a_selector ) ;
    
    /**
     * Monitors the occurrence of successful select timeouts on a selector
     * 
     * @param a_selector
     */
    void selectTimedOut( Selector a_selector ) ;
    
    /**
     * Monitors bind failures.
     * 
     * @param a_listener the listener whose bind attempt failed
     * @param a_failure the exception resulting from the failure
     */
    void failedToBind( ServerListener a_listener, IOException a_failure ) ;
    
    /**
     * Monitors unbind failures.
     * 
     * @param a_listener the listener whose unbind attempt failed
     * @param a_failure the exception resulting from the failure
     */
    void failedToUnbind( ServerListener a_listener, IOException a_failure ) ;
    
    /**
     * Monitors expiration failures on client keys.
     * 
     * @param a_key the client key that caused the failure
     * @param a_failure the exception resulting from the failure
     */
    void failedToExpire( ClientKey a_key, IOException a_failure ) ;
    
    /**
     * Monitors accept failures on socket channels.
     * 
     * @param a_key the selector key associated with the channel
     * @param a_failure the exception resulting from the failure
     */
    void failedToAccept( SelectionKey a_key, IOException a_failure ) ;
    
    /**
     * Monitors select failures on a selector.
     * 
     * @param a_selector the selector on which the select failed
     * @param a_failure the exception resulting from the failure
     */
    void failedToSelect( Selector a_selector, IOException a_failure ) ;
    
    /**
     * A select call is about to be made.
     *
     * @param a_selector the selector on which the select is called
     */
    void enteringSelect( Selector a_selector ) ;
}
