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
    public void bufferUnavailable( BufferPool a_bp, ResourceException a_fault )
    {
        if ( getLogger().isErrorEnabled() )
        {    
            getLogger().error( 
                    "Failed to acquire buffer resource from buffer pool "
                    + a_bp, a_fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#channelCloseFailure(
     * java.nio.channels.SocketChannel, java.io.IOException)
     */
    public void channelCloseFailure( SocketChannel a_channel, 
                                     IOException a_fault )
    {
        if ( getLogger().isErrorEnabled() )
        {    
            getLogger().error( "Could not properly close socket channel " 
                    + a_channel, a_fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#channelRegistrationFailure(
     * java.nio.channels.Selector, java.nio.channels.SocketChannel, int, 
     * java.io.IOException)
     */
    public void channelRegistrationFailure( Selector a_selector,
											SocketChannel a_channel,
											int a_key,
											IOException a_fault )
    {
        if ( getLogger().isErrorEnabled() )
        {    
            getLogger().error( "Could not register socket channel " + a_channel 
                    + " for selector " + a_selector 
                    + " using selection key mode " + a_key, a_fault ) ;
        }
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#disconnectedClient(
     * org.apache.eve.listener.ClientKey)
     */
    public void disconnectedClient( ClientKey a_key )
    {
        if ( getLogger().isInfoEnabled() )
        {    
            getLogger().info( "Disconnected client with key: " + a_key ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#enteringSelect(
     * java.nio.channels.Selector)
     */
    public void enteringSelect( Selector a_selector )
    {
        if ( getLogger().isDebugEnabled() )
        {    
            getLogger().debug( "About to enter select() on selector " 
                    + a_selector ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#inputRecieved(
     * org.apache.eve.listener.ClientKey)
     */
    public void inputRecieved( ClientKey a_key )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Got some input from " + a_key ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#keyExpiryFailure(
     * org.apache.eve.listener.ClientKey, 
     * org.apache.eve.listener.KeyExpiryException)
     */
    public void keyExpiryFailure( ClientKey a_key, KeyExpiryException a_fault )
    {
        if ( getLogger().isInfoEnabled() )
        {
            getLogger().info( "While working with client key " + a_key 
                    + " it was prematurely expired!", a_fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#readFailed(
     * org.apache.eve.listener.ClientKey, java.io.IOException)
     */
    public void readFailed( ClientKey a_key, IOException a_fault )
    {
        if ( getLogger().isErrorEnabled() )
        {
            getLogger().error( "Encountered failure while reading from " 
                    + a_key, a_fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#registeredChannel(
     * org.apache.eve.listener.ClientKey, java.nio.channels.Selector)
     */
    public void registeredChannel( ClientKey a_key, Selector a_selector )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Succeeded in registering " + a_key 
                    + " with selector " + a_selector ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#selectFailure(
     * java.nio.channels.Selector, java.io.IOException)
     */
    public void selectFailure( Selector a_selector, IOException a_fault )
    {
        if ( getLogger().isErrorEnabled() )
        {
            getLogger().error( "Failed on select() of selector " + a_selector, 
                    a_fault ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#selectorReturned(
     * java.nio.channels.Selector)
     */
    public void selectorReturned( Selector a_selector )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Select on " + a_selector + " returned" ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#selectTimedOut(
     * java.nio.channels.Selector)
     */
    public void selectTimedOut( Selector a_selector )
    {
        if ( getLogger().isWarnEnabled() )
        {
            getLogger().warn( "Select on " + a_selector + " timed out" ) ;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#inputRecieved(
     * java.nio.Buffer, org.apache.eve.listener.ClientKey)
     */
    public void inputRecieved( ByteBuffer a_buffer, ClientKey a_key )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Recieved input [" +  toHexString( a_buffer ) 
                    + "] from client " + a_key ) ;
        }
    }
    
    
    /*
     * Generates a hex string for a buffer.
     */
    public String toHexString( ByteBuffer a_buf )
    {
        byte[] l_bites = new byte[a_buf.remaining()] ;
        a_buf.get( l_bites ) ;
        return new String ( l_bites ) ;
    }
}
