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
 * An adaptor for the InputManagerMonitor interface.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class InputManagerMonitorAdapter implements InputManagerMonitor
{
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * disconnectedClient(org.apache.eve.listener.ClientKey)
     */
    public void disconnectedClient( ClientKey a_key )
    {
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * registeredChannel(org.apache.eve.listener.ClientKey, 
     * java.nio.channels.Selector)
     */
    public void registeredChannel( ClientKey a_key, Selector a_selector )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * selectorReturned(java.nio.channels.Selector)
     */
    public void selectorReturned( Selector a_selector )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * inputRecieved(org.apache.eve.listener.ClientKey)
     */
    public void inputRecieved( ClientKey a_key )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * selectFailure(java.nio.channels.Selector, java.io.IOException)
     */
    public void selectFailure( Selector a_selector, IOException a_fault )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#keyExpiryFailure(
     * org.apache.eve.listener.ClientKey, 
     * org.apache.eve.listener.KeyExpiryException)
     */
    public void keyExpiryFailure( ClientKey a_key, KeyExpiryException a_fault )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * readFailed(org.apache.eve.listener.ClientKey, java.io.IOException)
     */
    public void readFailed( ClientKey a_key, IOException a_fault ) 
    {
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * bufferUnavailable(org.apache.eve.buffer.BufferPool, 
     * org.apache.eve.ResourceException)
     */
    public void bufferUnavailable( BufferPool a_bp, ResourceException a_fault )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * channelRegistrationFailure(java.nio.channels.Selector, 
     * java.nio.channels.SocketChannel, int, java.io.IOException)
     */
    public void channelRegistrationFailure( Selector a_selector,
        SocketChannel a_channel, int a_key, IOException a_fault)
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#
     * channelCloseFailure(java.nio.channels.SocketChannel, java.io.IOException)
     */
    public void channelCloseFailure( SocketChannel a_channel, 
                                     IOException a_fault )
    {
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#enteringSelect(
     * java.nio.channels.Selector)
     */
    public void enteringSelect( Selector a_selector )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.input.InputManagerMonitor#selectTimedOut(
     * java.nio.channels.Selector)
     */
    public void selectTimedOut( Selector a_selector )
    {
    }
}
