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

import java.util.HashMap ;
import java.util.ArrayList ;

import org.apache.eve.ResourceException ;


/**
 * The default BufferPool implementation.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class DefaultBufferPool implements BufferPool
{
    /** a configuration bean */
    private final BufferPoolConfig m_config ;
    /** list of currently free buffers */
    private final ArrayList m_freeList ;
    /** list of currently in use buffers */
    private final ArrayList m_inUseList ;
    /** map of buffers to their interest lists */
    private final HashMap m_interestLists ;

    /** the monitor for this DefaultBufferPool */
    private BufferPoolMonitor m_monitor = new BufferPoolMonitorAdapter() ;
    
    
    /**
     * Creates a BufferPool using a pool configuration bean.
     * 
     * @param a_config the pool configuration bean
     */
    public DefaultBufferPool( BufferPoolConfig a_config )
    {
        super() ;
        
        m_config = a_config ;
        m_freeList = new ArrayList( a_config.getIncrement() ) ;
        m_inUseList = new ArrayList( a_config.getIncrement() ) ;
        m_interestLists = new HashMap( a_config.getIncrement() ) ;

        for( int ii = 0; ii < m_config.getInitialSize(); ii++ )
        {
            ByteBuffer l_buf = ByteBuffer.allocateDirect( m_config
                    .getBufferSize() ) ;
            m_freeList.add( l_buf ) ;
            
            // create interest lists in advance for the buffers
            m_interestLists.put( l_buf, new ArrayList( 3 ) ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getBuffer(java.lang.Object)
     */
    public synchronized ByteBuffer getBuffer( Object a_party ) 
        throws ResourceException
    {
        ByteBuffer l_buf = null ;
        
        if ( m_freeList.size() == 0 )
        {
            if ( ( m_freeList.size() + m_config.getIncrement() ) <= 
                    m_config.getMaximumSize() )
            {
                for ( int ii = 0; ii < m_config.getIncrement(); ii++ )
                {
                    l_buf = ByteBuffer.allocateDirect( m_config
                            .getBufferSize() ) ; 
                    m_interestLists.put( l_buf, new ArrayList( 3 ) ) ;
                }
            }
            else
            {    
                m_monitor.resourceUnavailable( this, a_party ) ;
                throw new ResourceException( "Free Buffers unavailable" ) ;
            }
        }
        
        // remove from free list and add to in use list then report to monitir
        l_buf = ( ByteBuffer ) m_freeList.remove( 0 ) ;
        m_inUseList.add( l_buf ) ;
        m_monitor.bufferTaken( this, l_buf, a_party ) ;

        // claim interest on the buffer automatically then report to monitor
        ArrayList l_list = ( ArrayList ) m_interestLists.get( l_buf ) ;
        l_list.add( a_party ) ;
        m_monitor.interestClaimed( this, l_buf, a_party ) ;
        return l_buf ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#claimInterest(java.nio.ByteBuffer, 
     * java.lang.Object)
     */
    public synchronized void claimInterest( ByteBuffer a_buffer, 
                                            Object a_party )
    {
        if ( ! m_interestLists.containsKey( a_buffer ) )
        {
            m_monitor.nonPooledBuffer( this, a_buffer, a_party ) ;
            throw new IllegalStateException( "Not a BufferPool resource" ) ;
        }
        
        ArrayList l_list = ( ArrayList ) m_interestLists.get( a_buffer ) ;
        l_list.add( a_party ) ;
        m_monitor.interestClaimed( this, a_buffer, a_party ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#releaseClaim(java.nio.ByteBuffer, 
     * java.lang.Object)
     */
    public synchronized void releaseClaim( ByteBuffer a_buffer, Object a_party )
    {
        if ( ! m_interestLists.containsKey( a_buffer ) )
        {
            m_monitor.nonPooledBuffer( this, a_buffer, a_party ) ;
            throw new IllegalStateException( "Not a BufferPool resource" ) ;
        }
        
        ArrayList l_list = ( ArrayList ) m_interestLists.get( a_buffer ) ;
        
        if ( ! l_list.contains( a_party ) )
        {
            m_monitor.unregisteredParty( this, a_buffer, a_party ) ;
            throw new IllegalStateException( 
                    "Party never registered interest with buffer" ) ;
        }
        
        l_list.remove( a_party ) ;
        m_monitor.interestReleased( this, a_buffer, a_party ) ;
        
        // if the list of interested parties hits zero then we release buf
        if ( l_list.size() == 0 )
        {
            m_inUseList.remove( a_buffer ) ;
            m_freeList.add( a_buffer ) ;
            m_monitor.bufferReleased( this, a_buffer, a_party ) ;
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getConfig()
     */
    public BufferPoolConfig getConfig()
    {
        return m_config ;
    }
    
    
    /**
     * Gets the monitor.
     * 
     * @return returns the monitor
     */
    BufferPoolMonitor getMonitor()
    {
        return m_monitor ;
    }
    

    /**
     * Sets the monitor.
     * 
     * @param a_monitor the monitor to set
     */
    void setMonitor( BufferPoolMonitor a_monitor )
    {
        m_monitor = a_monitor ;
    }
}
