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


/**
 * Null adapter for the ListenerManagerMonitor.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Rev: 6456 $
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
    public void bindOccured( ServerListener a_listener )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#unbindOccured(
     * org.apache.eve.listener.ServerListener)
     */
    public void unbindOccured( ServerListener a_listener )
    {
    }

    
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#acceptOccured(
     * java.nio.channels.SelectionKey)
     */
    public void acceptOccured( SelectionKey a_key ) 
    {
    }
    
    
    /*
     *  (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#selectOccured(
     * java.nio.channels.Selector)
     */
    public void selectOccured( Selector a_selector ) 
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToBind(
     * org.apache.eve.listener.ServerListener, java.io.IOException)
     */
    public void failedToBind( ServerListener a_listener, IOException a_failure )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToUnbind(
     * org.apache.eve.listener.ServerListener, java.io.IOException)
     */
    public void failedToUnbind( ServerListener a_listener, 
                                IOException a_failure )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToExpire(
     * org.apache.eve.listener.ClientKey, java.io.IOException)
     */
    public void failedToExpire( ClientKey a_key, IOException a_failure )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToAccept(
     * java.nio.channels.SelectionKey, java.io.IOException)
     */
    public void failedToAccept( SelectionKey a_key, IOException a_failure )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#failedToSelect(
     * java.nio.channels.Selector, java.io.IOException)
     */
    public void failedToSelect( Selector a_selector, IOException a_failure )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#selectTimedOut(
     * java.nio.channels.Selector)
     */
    public void selectTimedOut(Selector a_a_selector)
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ListenerManagerMonitor#enteringSelect(
     * java.nio.channels.Selector)
     */
    public void enteringSelect( Selector a_selector )
    {
    }
}
