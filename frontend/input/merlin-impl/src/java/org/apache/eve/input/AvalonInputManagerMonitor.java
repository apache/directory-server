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

import org.apache.avalon.framework.logger.AbstractLogEnabled ;

import org.apache.eve.ResourceException ;
import org.apache.eve.buffer.BufferPool ;
import org.apache.eve.listener.ClientKey ;
import org.apache.eve.listener.KeyExpiryException ;


/**
 * A monitor that uses Avolon logging life-cycle an loggers to report events
 * in the InputManager.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev: 6373 $
 */
public class AvalonInputManagerMonitor extends AbstractLogEnabled
    implements InputManagerMonitor
{
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#bufferUnavailable(
     * org.apache.eve.buffer.BufferPool, org.apache.eve.ResourceException)
     */
    public void bufferUnavailable( BufferPool bp, ResourceException fault )
    {
        if ( getLogger().isErrorEnabled() )
        {    
            getLogger().error( 
                    "Failed to acquire buffer resource from buffer pool "
                    + bp, fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#channelCloseFailure(
     * java.nio.channels.SocketChannel, java.io.IOException)
     */
    public void channelCloseFailure( SocketChannel channel, 
                                     IOException fault )
    {
        if ( getLogger().isErrorEnabled() )
        {    
            getLogger().error( "Could not properly close socket channel " 
                    + channel, fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#channelRegistrationFailure(
     * java.nio.channels.Selector, java.nio.channels.SocketChannel, int, 
     * java.io.IOException)
     */
    public void channelRegistrationFailure( Selector selector,
											SocketChannel channel,
											int key,
											IOException fault )
    {
        if ( getLogger().isErrorEnabled() )
        {    
            getLogger().error( "Could not register socket channel " + channel 
                    + " for selector " + selector 
                    + " using selection key mode " + key, fault ) ;
        }
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#disconnectedClient(
     * org.apache.eve.listener.ClientKey)
     */
    public void disconnectedClient( ClientKey key )
    {
        if ( getLogger().isInfoEnabled() )
        {    
            getLogger().info( "Disconnected client with key: " + key ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#enteringSelect(
     * java.nio.channels.Selector)
     */
    public void enteringSelect( Selector selector )
    {
        if ( getLogger().isDebugEnabled() )
        {    
            getLogger().debug( "About to enter select() on selector " 
                    + selector ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#inputRecieved(
     * org.apache.eve.listener.ClientKey)
     */
    public void inputRecieved( ClientKey key )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Got some input from " + key ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#keyExpiryFailure(
     * org.apache.eve.listener.ClientKey, 
     * org.apache.eve.listener.KeyExpiryException)
     */
    public void keyExpiryFailure( ClientKey key, KeyExpiryException fault )
    {
        if ( getLogger().isInfoEnabled() )
        {
            getLogger().info( "While working with client key " + key 
                    + " it was prematurely expired!", fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#readFailed(
     * org.apache.eve.listener.ClientKey, java.io.IOException)
     */
    public void readFailed( ClientKey key, IOException fault )
    {
        if ( getLogger().isErrorEnabled() )
        {
            getLogger().error( "Encountered failure while reading from " 
                    + key, fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#registeredChannel(
     * org.apache.eve.listener.ClientKey, java.nio.channels.Selector)
     */
    public void registeredChannel( ClientKey key, Selector selector )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Succeeded in registering " + key 
                    + " with selector " + selector ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#selectFailure(
     * java.nio.channels.Selector, java.io.IOException)
     */
    public void selectFailure( Selector selector, IOException fault )
    {
        if ( getLogger().isErrorEnabled() )
        {
            getLogger().error( "Failed on select() of selector " + selector, 
                    fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#selectorReturned(
     * java.nio.channels.Selector)
     */
    public void selectorReturned( Selector selector )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Select on " + selector + " returned" ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#selectTimedOut(
     * java.nio.channels.Selector)
     */
    public void selectTimedOut( Selector selector )
    {
        if ( getLogger().isWarnEnabled() )
        {
            getLogger().warn( "Select on " + selector + " timed out" ) ;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#inputRecieved(
     * java.nio.Buffer, org.apache.eve.listener.ClientKey)
     */
    public void inputRecieved( ByteBuffer buffer, ClientKey key )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Recieved input [" +  toHexString( buffer ) 
                    + "] from client " + key ) ;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#cleanedStaleKey(
     * java.nio.channels.SelectionKey)
     */
    public void cleanedStaleKey( SelectionKey key )
    {
        if ( getLogger().isWarnEnabled() )
        {
            getLogger().warn( "Cleaning up stale connection key for client: " 
                    + key.attachment() ) ;
        }
    }
    
    
    /*
     * Generates a hex string for a buffer.
     */
    public String toHexString( ByteBuffer buf )
    {
        byte[] l_bites = new byte[buf.remaining()] ;
        buf.get( l_bites ) ;
        return new String ( l_bites ) ;
    }
}
