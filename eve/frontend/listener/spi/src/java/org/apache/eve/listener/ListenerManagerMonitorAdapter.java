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

import org.apache.commons.lang.exception.ExceptionUtils ;


/**
 * Null adapter for the ListenerManagerMonitor.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class ListenerManagerMonitorAdapter implements ListenerManagerMonitor
{
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#started()
     */
    public void started() 
    {
    }

    
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#stopped()
     */
    public void stopped() 
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#bindOccured(
     * org.apache.eve.listener.ServerListener)
     */
    public void bindOccured( ServerListener listener )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#unbindOccured(
     * org.apache.eve.listener.ServerListener)
     */
    public void unbindOccured( ServerListener listener )
    {
    }

    
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#acceptOccured(
     * java.nio.channels.SelectionKey)
     */
    public void acceptOccured( SelectionKey key ) 
    {
    }
    
    
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#selectOccured(
     * java.nio.channels.Selector)
     */
    public void selectOccured( Selector selector ) 
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToBind(
     * org.apache.eve.listener.ServerListener, java.io.IOException)
     */
    public void failedToBind( ServerListener listener, IOException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToUnbind(
     * org.apache.eve.listener.ServerListener, java.io.IOException)
     */
    public void failedToUnbind( ServerListener listener, 
                                IOException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToExpire(
     * org.apache.eve.listener.ClientKey, java.io.IOException)
     */
    public void failedToExpire( ClientKey key, IOException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToAccept(
     * java.nio.channels.SelectionKey, java.io.IOException)
     */
    public void failedToAccept( SelectionKey key, IOException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToSelect(
     * java.nio.channels.Selector, java.io.IOException)
     */
    public void failedToSelect( Selector selector, IOException fault )
    {
        System.err.println( ExceptionUtils.getFullStackTrace( fault ) ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#selectTimedOut(
     * java.nio.channels.Selector)
     */
    public void selectTimedOut( Selector selector )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#enteringSelect(
     * java.nio.channels.Selector)
     */
    public void enteringSelect( Selector selector )
    {
    }
}
