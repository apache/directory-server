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

import org.apache.commons.logging.Log ;
import org.apache.commons.logging.LogFactory ;
import org.apache.commons.lang.exception.ExceptionUtils ;

import org.apache.eve.event.ConnectEvent ;
import org.apache.eve.listener.ClientKey ;
import org.apache.eve.event.DisconnectEvent ;
import org.apache.eve.listener.KeyExpiryException ;


/**
 * A logging monitor for any OutputManager.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class LoggingOutputMonitor implements OutputMonitor
{
    private final Log log = LogFactory.getLog( "OutputManager" ) ;

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#failedOnWrite(
     * org.apache.eve.output.OutputManager, org.apache.eve.listener.ClientKey, 
     * java.lang.Throwable)
     */
    public void failedOnWrite( OutputManager manager, ClientKey key, 
                               Throwable t )
    {
        if ( log.isErrorEnabled() )
        {
            log.error( manager + " failed while trying to write to client " 
                    + key + ":\n"
                    + ExceptionUtils.getFullStackTrace( t ) ) ;
        }
    }


    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#writeOccurred(
     * org.apache.eve.output.OutputManager, org.apache.eve.listener.ClientKey)
     */
    public void writeOccurred( OutputManager manager, ClientKey key )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( manager + " wrote to client " + key ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#writeLockAcquired(
     * org.apache.eve.output.OutputManager, org.apache.eve.listener.ClientKey)
     */
    public void writeLockAcquired( OutputManager manager, ClientKey key )
    {
        if ( log.isTraceEnabled() )
        {
            log.trace( manager + " locked channel for client " + key ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#channelMissing(
     * org.apache.eve.output.OutputManager, org.apache.eve.listener.ClientKey)
     */
    public void channelMissing( OutputManager manager, ClientKey key )
    {
        if ( log.isWarnEnabled() )
        {
            log.warn( manager + " could not find channel for client " + key ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#keyExpired(
     * org.apache.eve.output.OutputManager, org.apache.eve.listener.ClientKey, 
     * org.apache.eve.listener.KeyExpiryException)
     */
    public void keyExpired( OutputManager manager, ClientKey key,
							KeyExpiryException e )
    {
        if ( log.isWarnEnabled() )
        {
            log.warn( manager + " cannot use expired key for client " + key 
                    + ":\n" + ExceptionUtils.getFullStackTrace( e ) ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#failedOnInform(
     * org.apache.eve.output.OutputManager, java.util.EventObject, 
     * java.lang.Throwable)
     */
    public void failedOnInform( OutputManager manager, EventObject event,
								Throwable fault )
    {
        if ( log.isErrorEnabled() )
        {
            log.error( manager + " failed to be informed of " + event ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#addedClient(
     * org.apache.eve.output.OutputManager, org.apache.eve.event.ConnectEvent)
     */
    public void addedClient( OutputManager manager, ConnectEvent event )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( manager + " added client " + event.getClientKey() ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.output.OutputMonitor#removedClient(
     * org.apache.eve.output.OutputManager, 
     * org.apache.eve.event.DisconnectEvent)
     */
    public void removedClient( OutputManager manager, DisconnectEvent event )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( manager + " removed client " + event.getClientKey() ) ;
        }
    }
}
