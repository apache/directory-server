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
package org.apache.eve.output ;


import java.util.EventObject ;

import org.apache.eve.listener.ClientKey ; 
import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.event.DisconnectEvent ;
import org.apache.eve.listener.KeyExpiryException ;


/**
 * A monitor for the OutputManager.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface OutputMonitor
{
    /**
     * Monitors failed send events to a client.
     * 
     * @param manager the OutputManager sending the data
     * @param key the key of the client data is sent to
     * @param t the fault causing the failure
     */
    void failedOnWrite( OutputManager manager, ClientKey key, Throwable t ) ;
    
    /**
     * Monitors send events to a client.
     * 
     * @param manager the OutputManager sending the data
     * @param key the key of the client data is sent to
     */
    void writeOccurred( OutputManager manager, ClientKey key ) ;
    
    /**
     * Monitors locks acquired to write to a client's output channel.
     * 
     * @param manager the OutputManager write locking
     * @param key the key of the client lock is for
     */
    void writeLockAcquired( OutputManager manager, ClientKey key ) ;
    
    /**
     * Monitors situations where a channel is no longer present for a client.
     * 
     * @param manager the OutputManager detecting the missing channel
     * @param key the key of the client the channel was missing from
     */
    void channelMissing( OutputManager manager, ClientKey key ) ;
    
    /**
     * Monitors failures resulting from accessing a client key that has been 
     * expired.
     * 
     * @param manager the OutputManager accessing the key
     * @param key the key of the client that expired
     * @param e the key expiration exception
     */
    void keyExpired( OutputManager manager, ClientKey key, 
                     KeyExpiryException e ) ;
    
    /**
     * Monitors failures on inform events to subscriber methods.
     * 
     * @param manager the OutputManager that failed to inform
     * @param event the event that failed to be routed
     * @param fault the faulting exception associated with the failure
     */
    void failedOnInform( OutputManager manager, EventObject event, 
                         Throwable fault ) ;
    
    /**
     * Called when connections are established and clients are registered with
     * the OutputManager.
     * 
     * @param manager the output manager with which a client was registered
     * @param event the ConnectEvent that caused registration
     */
    void addedClient( OutputManager manager, ConnectEvent event ) ;
    
    /**
     * Called when connections are dropped and clients are unregistered with
     * the OutputManager.
     * 
     * @param manager the output manager with which a client was unregistered
     * @param event the DisconnectEvent that caused unregistration
     */
    void removedClient( OutputManager manager, DisconnectEvent event ) ;
}
