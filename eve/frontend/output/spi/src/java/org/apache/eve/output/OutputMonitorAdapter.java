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

import org.apache.commons.lang.exception.ExceptionUtils ;

import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.listener.ClientKey ;
import org.apache.eve.event.DisconnectEvent ;
import org.apache.eve.listener.KeyExpiryException ;


/**
 * A do nothing output monitor adapter.  For safety's sake error conditions are
 * printed to the console.  
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class OutputMonitorAdapter implements OutputMonitor
{

    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#
     * failedOnWrite(org.apache.eve.output.OutputManager, 
     * org.apache.eve.listener.ClientKey, java.lang.Throwable)
     */
    public void failedOnWrite( OutputManager manager, ClientKey key, 
                               Throwable t )
    {
        System.err.println( "Failed on write to client " + key + ":\n" 
                + ExceptionUtils.getFullStackTrace( t ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#writeOccurred(
     * org.apache.eve.output.OutputManager, org.apache.eve.listener.ClientKey)
     */
    public void writeOccurred( OutputManager manager, ClientKey key )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#writeLockAcquired(
     * org.apache.eve.output.OutputManager, org.apache.eve.listener.ClientKey)
     */
    public void writeLockAcquired( OutputManager manager, ClientKey key )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#channelMissing(
     * org.apache.eve.output.OutputManager, org.apache.eve.listener.ClientKey)
     */
    public void channelMissing( OutputManager manager, ClientKey key )
    {
        System.err.println( "Channel for client " + key + " missing." ) ; 
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#keyExpired(
     * org.apache.eve.output.OutputManager, 
     * org.apache.eve.listener.KeyExpiryException)
     */
    public void keyExpired( OutputManager manager, ClientKey key, 
                            KeyExpiryException e )
    {
        System.err.println( "Key for client " + key + ":\n" 
                + ExceptionUtils.getFullStackTrace( e ) ) ;
    }


    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#failedOnInform(
     * org.apache.eve.output.OutputManager, java.util.EventObject, 
     * java.lang.Throwable)
     */
    public void failedOnInform( OutputManager manager, EventObject event,
            Throwable fault )
    {
        System.err.println( "Failed to inform of " + event + ":\n" 
                + ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#addedClient(
     * org.apache.eve.output.OutputManager, org.apache.eve.event.ConnectEvent)
     */
    public void addedClient( OutputManager manager, ConnectEvent event )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#removedClient(
     * org.apache.eve.output.OutputManager, 
     * org.apache.eve.event.DisconnectEvent)
     */
    public void removedClient( OutputManager manager, DisconnectEvent event )
    {
    }
}
