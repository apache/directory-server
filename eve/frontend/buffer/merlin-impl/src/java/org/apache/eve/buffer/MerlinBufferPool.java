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
package org.apache.eve.buffer ;


import java.nio.ByteBuffer ;

import org.apache.eve.ResourceException ;

import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;
import org.apache.avalon.framework.configuration.Configurable ;
import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;


/**
 * A Merlin BufferPool service. 
 * 
 * @avalon.component name="buffer-pool" lifestyle="singleton"
 * @avalon.service type="org.apache.eve.buffer.BufferPool" version="1.0"
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Rev: 6444 $
 */
public class MerlinBufferPool
    extends AbstractLogEnabled
    implements BufferPool, Initializable, Configurable
{
    /** the underlying BufferPool implementation wrapped by this service */
    private DefaultBufferPool m_bp = null ;
    /** the configuration bean for this BufferPool */
    private BufferPoolConfig m_config = null ;
    
    
    // ------------------------------------------------------------------------
    // BufferPool Interface Methods
    // ------------------------------------------------------------------------

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getBuffer(java.lang.Object)
     */
    public ByteBuffer getBuffer( Object a_party ) throws ResourceException
    {
        return m_bp.getBuffer( a_party ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#claimInterest(java.nio.ByteBuffer,
     * java.lang.Object)
     */
    public void claimInterest( ByteBuffer a_buffer, Object a_party )
    {
        m_bp.claimInterest( a_buffer, a_party ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#releaseClaim(
     * java.nio.ByteBuffer, java.lang.Object)
     */
    public void releaseClaim( ByteBuffer a_buffer, Object a_party )
    {
        m_bp.releaseClaim( a_buffer, a_party ) ;
    }


    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getConfig()
     */
    public BufferPoolConfig getConfig()
    {
        return m_config ;
    }
    
    
    // ------------------------------------------------------------------------
    // Avalon Life Cycle Methods
    // ------------------------------------------------------------------------

    
    /* (non-Javadoc)
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception
    {
        m_bp = new DefaultBufferPool( m_config ) ;
        MerlinBufferPoolMonitor l_monitor = new MerlinBufferPoolMonitor() ;
        l_monitor.enableLogging( getLogger() ) ;
        m_bp.setMonitor( l_monitor ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.avalon.framework.configuration.Configurable#configure(
     * org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure( Configuration a_config ) 
        throws ConfigurationException
    {
        int l_max = Integer.parseInt( a_config
                .getChild( "maximum" ).getValue() ) ;
        int l_ini = Integer.parseInt( a_config
                .getChild( "initial" ).getValue() ) ;
        int l_inc = Integer.parseInt( a_config
                .getChild( "increment" ).getValue() ) ;
        int l_size = Integer.parseInt( a_config
                .getChild( "bufferSize" ).getValue() ) ;
        
        m_config = new DefaultBufferPoolConfig( l_inc, l_max, l_ini, l_size ) ;
    }
}
