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
package org.apache.eve.input ;


import java.io.IOException ;
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
     * @param a_key the key of the client that was disconnected
     */
    void disconnectedClient( ClientKey a_key ) ;
    
    /**
     * Monitors channel registrations which occur on client sockets.
     * 
     * @param a_key the key of the client whose channel got registered
     * @param a_selector the selector used to register the client's channel
     */
    void registeredChannel( ClientKey a_key, Selector a_selector ) ;
    
    /**
     * Monitors returns from the selector denoting a timeout or a wakeup due to
     * input availability.
     *
     * @param a_selector the selector that has returned
     */
    void selectorReturned( Selector a_selector ) ;
    
    /**
     * Monitors input read from a client socket channel.
     * 
     * @param a_key the key of the client sending the request
     */
    void inputRecieved( ClientKey a_key ) ;

    
    // ------------------------------------------------------------------------
    // failure monitoring methods
    // ------------------------------------------------------------------------


    /**
     * Monitor method for handling selector select call failures.
     * 
     * @param a_selector the selector the select was called on
     * @param a_fault the faulting exception
     */
    void selectFailure( Selector a_selector, IOException a_fault ) ;
    
    /**
     * Monitors attempts to use client key's that have expired.
     *  
     * @param a_key the client key that expired
     * @param a_fault the faulting exception
     */
    void keyExpiryFailure( ClientKey a_key, KeyExpiryException a_fault ) ;
    
    /**
     * Monitors failed read attempts from client socket channel.
     * 
     * @param a_key the key of the client the read failed on
     * @param a_fault the faulting exception
     */
    void readFailed( ClientKey a_key, IOException a_fault ) ;

    /**
     * Monitors failed attempts to acquire a buffer from the BufferPool.
     * 
     * @param a_bp the buffer pool a buffer was requested from
     * @param a_fault the faulting exception
     */
    void bufferUnavailable( BufferPool a_bp, ResourceException a_fault ) ;
    
    /**
     * Monitors failures to register channels with a selector.
     * 
     * @param a_selector the selector the register method was called on
     * @param a_channel the channel that failed registeration
     * @param a_selectionKey the selection key used to register
     * @param a_fault the faulting exception
     */
    void channelRegistrationFailure( Selector a_selector, 
                                     SocketChannel a_channel,
                                     int a_selectionKey,
                                     IOException a_fault ) ;
    
    /**
     * Monitors failures to close a client's socket channel.
     * 
     * @param a_channel the channel that failed to close
     * @param a_fault the faulting exception
     */
    void channelCloseFailure( SocketChannel a_channel, IOException a_fault ) ;
    
    /**
     * Monitors the occurrence of successful select timeouts on a selector
     * 
     * @param a_selector
     */
    void selectTimedOut( Selector a_selector ) ;
    
    /**
     * A select call is about to be made.
     *
     * @param a_selector the selector on which the select is called
     */
    void enteringSelect( Selector a_selector ) ;
}
