/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
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
