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
package org.apache.eve.listener ;


import java.io.IOException ;
import java.nio.channels.Selector ;
import java.nio.channels.SelectionKey ;


/**
 * Null adapter for the ListenerManagerMonitor.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
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
}
