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


import java.io.IOException ;

import java.nio.ByteBuffer ;
import java.nio.channels.SocketChannel ;

import java.util.Map ;
import java.util.HashMap ;
import java.util.EventObject ;

import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.OutputEvent ;
import org.apache.eve.seda.DefaultStage ;
import org.apache.eve.seda.StageHandler ;
import org.apache.eve.listener.ClientKey ;
import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.output.OutputManager ;
import org.apache.eve.output.OutputMonitor ;
import org.apache.eve.event.DisconnectEvent ;
import org.apache.eve.event.OutputSubscriber ;
import org.apache.eve.event.ConnectSubscriber ;
import org.apache.eve.seda.DefaultStageConfig ;
import org.apache.eve.event.AbstractSubscriber ;
import org.apache.eve.seda.LoggingStageMonitor ;
import org.apache.eve.event.DisconnectSubscriber ;
import org.apache.eve.output.LoggingOutputMonitor ;
import org.apache.eve.listener.KeyExpiryException ;


/**
 * The default OutputManager implementation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultOutputManager extends DefaultStage 
    implements 
    OutputManager, 
    OutputSubscriber, 
    ConnectSubscriber, 
    DisconnectSubscriber
{
    /** the router we subscribe for OutputEvents on */
    private final EventRouter router ;
    /** the monitor used to track notable events in this OutputManager */
    private OutputMonitor monitor ;
    /** a map of channels by ClientKey */
    private Map channels = new HashMap() ;
    
    
    // ------------------------------------------------------------------------
    // constructors
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates a defualt OutputManager.
     * 
     * @param router the router we subscribe for OutputEvents on
     * @param config the configuration for this Stage
     */
    public DefaultOutputManager( EventRouter router, DefaultStageConfig config )
    {
        super( config ) ;
        this.router = router ;
        this.router.subscribe( OutputEvent.class, this ) ;
        this.router.subscribe( ConnectEvent.class, this ) ;
        this.router.subscribe( DisconnectEvent.class, this ) ;
        config.setHandler( new OutputStageHandler() ) ;
        this.setMonitor( new LoggingStageMonitor() ) ;
        this.setOutputMonitor( new LoggingOutputMonitor() ) ;
    }


    // ------------------------------------------------------------------------
    // subscriber inform methods 
    // ------------------------------------------------------------------------
    
    
    /* 
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject event )
    {
        try
        {
            AbstractSubscriber.inform( this, event ) ;
        }
        catch( Throwable t )
        {
            monitor.failedOnInform( this, event, t ) ;
        }
    }
    
    
    /*
     * @see org.apache.eve.event.OutputSubscriber#inform(
     * org.apache.eve.event.OutputEvent)
     */
    public void inform( OutputEvent event )
    {
        enqueue( event ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.ConnectSubscriber#inform(
     * org.apache.eve.event.ConnectEvent)
     */
    public void inform( ConnectEvent event )
    {
        ClientKey key = event.getClientKey() ;
        
        try
        {
            channels.put( key, key.getSocket().getChannel() ) ;
        }
        catch( KeyExpiryException e )
        {
            monitor.keyExpired( this, key, e ) ;
        }
        
        monitor.addedClient( this, event ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.DisconnectSubscriber#inform(
     * org.apache.eve.event.DisconnectEvent)
     */
    public void inform( DisconnectEvent event )
    {
        channels.remove( event.getClientKey() ) ;
        monitor.removedClient( this, event ) ;
    }
    
    
    // ------------------------------------------------------------------------
    // OutputManager method
    // ------------------------------------------------------------------------
    
    
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.output.OutputManager#write(
     * org.apache.eve.listener.ClientKey, java.nio.ByteBuffer)
     */
    public void write( ClientKey key, ByteBuffer buf )
        throws IOException
    {
        Object lock = null ;
        SocketChannel channel = ( SocketChannel ) channels.get( key ) ;
        
        if ( null == channel ) 
        {
            monitor.channelMissing( this, key ) ;
            return ;
        }

        // Obtain output lock for write to client.
        try 
        {
            lock = key.getOutputLock() ;
        } 
        catch ( KeyExpiryException e )  
        {
            monitor.keyExpired( this, key, e ) ;
            return ;
        }

        // synchronize on client output stream lock object.
        synchronized( lock ) 
        {
            monitor.writeLockAcquired( this, key )  ;
            channel.write( buf ) ;
            lock.notifyAll() ;
        }
        
        monitor.writeOccurred( this, key ) ;
    }
    
    
    /**
     * Sets the output manager's monitor.
     * 
     * @param monitor the monitor used by this output manager
     */
    public void setOutputMonitor( OutputMonitor monitor )
    {
        this.monitor = monitor ;
    }

    
    /**
     * EventHandler designed for processing output events.
     */
    class OutputStageHandler implements StageHandler
    {
        public void handleEvent( EventObject generic )
        {
            if ( generic instanceof OutputEvent )
            {    
                OutputEvent event = ( OutputEvent ) generic ;
                
                try
                {
                    write( event.getClientKey(), event.getBuffer() ) ;
                }
                catch ( IOException e )
                {
                    monitor.failedOnWrite( DefaultOutputManager.this, 
                            event.getClientKey(), e ) ;
                }
            }
        }
    }
}
