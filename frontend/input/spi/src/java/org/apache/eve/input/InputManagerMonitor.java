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
package org.apache.eve.input ;


import java.io.IOException ;

import java.nio.ByteBuffer ;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector ;
import java.nio.channels.SocketChannel ;

import org.apache.eve.ResourceException ;
import org.apache.eve.buffer.BufferPool ;
import org.apache.eve.listener.ClientKey ;
import org.apache.eve.listener.KeyExpiryException ;


/**
 * Monitors input activity managed by the the InputManager.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface InputManagerMonitor
{
    /**
     * Monitors client disconnections.
     * 
     * @param key the key of the client that was disconnected
     */
    void disconnectedClient( ClientKey key ) ;
    
    /**
     * Monitors channel registrations which occur on client sockets.
     * 
     * @param key the key of the client whose channel got registered
     * @param selector the selector used to register the client's channel
     */
    void registeredChannel( ClientKey key, Selector selector ) ;
    
    /**
     * Monitors returns from the selector denoting a timeout or a wakeup due to
     * input availability.
     *
     * @param selector the selector that has returned
     */
    void selectorReturned( Selector selector ) ;
    
    /**
     * Monitors input read from a client socket channel.
     * 
     * @param key the key of the client sending the request
     */
    void inputRecieved( ClientKey key ) ;

    /**
     * Monitors input read from a client socket channel.
     * 
     * @param buffer the input recieved
     * @param key the key of the client sending the request
     */
    void inputRecieved( ByteBuffer buffer, ClientKey key ) ;

    
    // ------------------------------------------------------------------------
    // failure monitoring methods
    // ------------------------------------------------------------------------


    /**
     * Monitor method for handling selector select call failures.
     * 
     * @param selector the selector the select was called on
     * @param fault the faulting exception
     */
    void selectFailure( Selector selector, IOException fault ) ;
    
    /**
     * Monitors attempts to use client key's that have expired.
     *  
     * @param key the client key that expired
     * @param fault the faulting exception
     */
    void keyExpiryFailure( ClientKey key, KeyExpiryException fault ) ;
    
    /**
     * Monitors failed read attempts from client socket channel.
     * 
     * @param key the key of the client the read failed on
     * @param fault the faulting exception
     */
    void readFailed( ClientKey key, IOException fault ) ;

    /**
     * Monitors failed attempts to acquire a buffer from the BufferPool.
     * 
     * @param bp the buffer pool a buffer was requested from
     * @param fault the faulting exception
     */
    void bufferUnavailable( BufferPool bp, ResourceException fault ) ;
    
    /**
     * Monitors failures to register channels with a selector.
     * 
     * @param selector the selector the register method was called on
     * @param channel the channel that failed registeration
     * @param selectionKey the selection key used to register
     * @param fault the faulting exception
     */
    void channelRegistrationFailure( Selector selector, SocketChannel channel,
                                     int selectionKey, IOException fault ) ;
    
    /**
     * Monitors failures to close a client's socket channel.
     * 
     * @param channel the channel that failed to close
     * @param fault the faulting exception
     */
    void channelCloseFailure( SocketChannel channel, IOException fault ) ;
    
    /**
     * Monitors the occurrence of successful select timeouts on a selector
     * 
     * @param selector
     */
    void selectTimedOut( Selector selector ) ;
    
    /**
     * A select call is about to be made.
     *
     * @param selector the selector on which the select is called
     */
    void enteringSelect( Selector selector ) ;
    
    /**
     * Monitors the removal of stale keys from the selection set.  This occurs
     * when connections are abrubtly dropped by clients and are left 
     * inconsistant.  These keys wakeup threads in select calls yet have 
     * no incomming IO on them.  They are removed to cleanup and this method
     * is called when one is cleaned up.
     * 
     * @param key the selection key of the client that was cleaned up
     */
    void cleanedStaleKey( SelectionKey key ) ;
}
