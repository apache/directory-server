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
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
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
    /** the monitor for the BufferPool */
    private AvalonLoggingMonitor m_monitor = new AvalonLoggingMonitor() ;
    
    
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
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#size()
     */
    public int size()
    {
        return m_bp.size() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getName()
     */
    public String getName()
    {
        return m_bp.getName() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getInUseCount()
     */
    public int getInUseCount()
    {
        return m_bp.getInUseCount() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getFreeCount()
     */
    public int getFreeCount()
    {
        return m_bp.getFreeCount() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getInterestedCount(java.nio.ByteBuffer)
     */
    public int getInterestedCount(ByteBuffer a_buffer)
    {
        return m_bp.getInterestedCount( a_buffer ) ;
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
        m_monitor.enableLogging( getLogger() ) ;
        m_bp.setMonitor( m_monitor ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.avalon.framework.configuration.Configurable#configure(
     * org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure( Configuration a_config ) 
        throws ConfigurationException
    {
        String l_name = a_config.getChild( "name" ).getValue() ;
        int l_max = Integer.parseInt( a_config
                .getChild( "maximum" ).getValue() ) ;
        int l_ini = Integer.parseInt( a_config
                .getChild( "initial" ).getValue() ) ;
        int l_inc = Integer.parseInt( a_config
                .getChild( "increment" ).getValue() ) ;
        int l_size = Integer.parseInt( a_config
                .getChild( "bufferSize" ).getValue() ) ;
        
        m_config = new DefaultBufferPoolConfig( l_name, l_inc, l_max, 
                l_ini, l_size ) ;
        
        if ( getLogger().isInfoEnabled() )
        {   
            StringBuffer l_buf = new StringBuffer() ;
            l_buf.append( m_config.getName() ) ;
            l_buf.append( " direct byte buffer pool created initially with " ) ;
            l_buf.append( m_config.getInitialSize() ) ;
            l_buf.append( " byte buffers of " ) ;
            l_buf.append( m_config.getBufferSize() ) ;
            l_buf.append( " bytes each.  " ) ;
            l_buf.append( "The pool will grow to a maximum size of " ) ;
            l_buf.append( m_config.getMaximumSize() ) ;
            l_buf.append( " with size increments of " ) ;
            l_buf.append( m_config.getIncrement() ) ;
            l_buf.append( " as demand increases." ) ;
            
            getLogger().info( l_buf.toString() ) ;
        }
    }
}
