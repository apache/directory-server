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
import java.nio.channels.Selector ; 
import java.nio.channels.SelectionKey ;
import java.nio.channels.SocketChannel ;

import org.apache.commons.lang.exception.ExceptionUtils ;

import org.apache.eve.ResourceException ;
import org.apache.eve.buffer.BufferPool ;
import org.apache.eve.listener.ClientKey ;
import org.apache.eve.listener.KeyExpiryException ;


/**
 * An adaptor for the InputManagerMonitor interface.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class InputManagerMonitorAdapter implements InputManagerMonitor
{
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * disconnectedClient(org.apache.eve.listener.ClientKey)
     */
    public void disconnectedClient( ClientKey key )
    {
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * registeredChannel(org.apache.eve.listener.ClientKey, 
     * java.nio.channels.Selector)
     */
    public void registeredChannel( ClientKey key, Selector selector )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * selectorReturned(java.nio.channels.Selector)
     */
    public void selectorReturned( Selector selector )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * inputRecieved(org.apache.eve.listener.ClientKey)
     */
    public void inputRecieved( ClientKey key )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * selectFailure(java.nio.channels.Selector, java.io.IOException)
     */
    public void selectFailure( Selector selector, IOException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#keyExpiryFailure(
     * org.apache.eve.listener.ClientKey, 
     * org.apache.eve.listener.KeyExpiryException)
     */
    public void keyExpiryFailure( ClientKey key, KeyExpiryException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * readFailed(org.apache.eve.listener.ClientKey, java.io.IOException)
     */
    public void readFailed( ClientKey key, IOException fault ) 
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * bufferUnavailable(org.apache.eve.buffer.BufferPool, 
     * org.apache.eve.ResourceException)
     */
    public void bufferUnavailable( BufferPool bp, ResourceException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * channelRegistrationFailure(java.nio.channels.Selector, 
     * java.nio.channels.SocketChannel, int, java.io.IOException)
     */
    public void channelRegistrationFailure( Selector selector,
        SocketChannel channel, int key, IOException fault)
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * channelCloseFailure(java.nio.channels.SocketChannel, java.io.IOException)
     */
    public void channelCloseFailure( SocketChannel channel, 
                                     IOException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#enteringSelect(
     * java.nio.channels.Selector)
     */
    public void enteringSelect( Selector selector )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#selectTimedOut(
     * java.nio.channels.Selector)
     */
    public void selectTimedOut( Selector selector )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#inputRecieved(
     * java.nio.ByteBuffer, org.apache.eve.listener.ClientKey)
     */
    public void inputRecieved( ByteBuffer buffer, ClientKey key )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#cleanedStaleKey(
     * java.nio.channels.SelectionKey)
     */
    public void cleanedStaleKey( SelectionKey key )
    {
    }
}
