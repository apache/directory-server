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

import java.util.ArrayList ;

import org.apache.commons.lang.Validate ;
import org.apache.eve.ResourceException ;


/**
 * The default BufferPool implementation.
 * 
 * @see <a 
 * href="http://nagoya.apache.org/jira/secure/ViewIssue.jspa?key=DIR-12">
 * JIRA Issue</a>
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultBufferPool implements BufferPool
{
    /** a configuration bean */
    private final BufferPoolConfig m_config ;
    /** list of all buffers */
    private final ArrayList m_allList ;
    /** list of currently free buffers */
    private final ArrayList m_freeList ;
    /** list of currently in use buffers */
    private final ArrayList m_inUseList ;
    /** the monitor for this DefaultBufferPool */
    private BufferPoolMonitor m_monitor = new BufferPoolMonitorAdapter() ;
    
    
    /**
     * Creates a BufferPool using a pool configuration bean.
     * 
     * @param config the pool configuration bean
     */
    public DefaultBufferPool( BufferPoolConfig config )
    {
        super() ;
        
        m_config = config ;
        m_freeList = new ArrayList( config.getMaximumSize() ) ;
        m_inUseList = new ArrayList( config.getMaximumSize() ) ;
        m_allList = new ArrayList( config.getMaximumSize() ) ;

        for( int ii = 0; ii < m_config.getInitialSize(); ii++ )
        {
            BufferListPair l_list = new BufferListPair() ;
            m_freeList.add( l_list ) ;
            m_allList.add( l_list ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getBuffer(java.lang.Object)
     */
    public synchronized ByteBuffer getBuffer( Object party ) 
        throws ResourceException
    {
        BufferListPair l_list = null ;
        
        if ( m_freeList.size() == 0 )
        {
            if ( m_config.getIncrement() <= m_config.getMaximumSize() )
            {
                for ( int ii = 0; ii < m_config.getIncrement(); ii++ )
                {
                    l_list = new BufferListPair() ;
                    m_freeList.add( l_list ) ;
                    m_allList.add( l_list ) ;
                }
            }
            else
            {    
                m_monitor.resourceUnavailable( this, party ) ;
                throw new ResourceException( "Free Buffers unavailable" ) ;
            }
        }
        
        // remove from free list and add to in use list then report to monitir
        l_list = ( BufferListPair ) m_freeList.remove( 0 ) ;
        m_inUseList.add( l_list ) ;
        m_monitor.bufferTaken( this, l_list.getBuffer(), party ) ;

        // claim interest on the buffer automatically then report to monitor
        l_list.add( party ) ;
        m_monitor.interestClaimed( this, l_list.getBuffer(), party ) ;
        return l_list.getBuffer() ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#claimInterest(java.nio.ByteBuffer, 
     * java.lang.Object)
     */
    public synchronized void claimInterest( ByteBuffer buffer, 
                                            Object party )
    {
        BufferListPair l_list = getBufferListPair( buffer ) ;
        
        if ( null == l_list )
        {
            m_monitor.nonPooledBuffer( this, buffer, party ) ;
            throw new IllegalStateException( "Not a BufferPool resource" ) ;
        }
        
        l_list.add( party ) ;
        m_monitor.interestClaimed( this, buffer, party ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#releaseClaim(java.nio.ByteBuffer, 
     * java.lang.Object)
     */
    public synchronized void releaseClaim( ByteBuffer buffer, Object party )
    {
        BufferListPair l_list = getBufferListPair( buffer ) ;
        
        if ( null == l_list )
        {
            m_monitor.releaseOfUnclaimed( this, buffer, party ) ;
            throw new IllegalArgumentException( "Not a pooled resource" ) ;
        }
        
        if ( ! l_list.contains( party ) )
        {
            m_monitor.unregisteredParty( this, buffer, party ) ;
            throw new IllegalStateException( 
                    "Party never registered interest with buffer" ) ;
        }
        
        l_list.remove( party ) ;
        m_monitor.interestReleased( this, buffer, party ) ;
        
        // if the list of interested parties hits zero then we release buf
        if ( l_list.size() == 0 )
        {
            m_inUseList.remove( l_list ) ;
            m_freeList.add( l_list ) ;
            m_monitor.bufferReleased( this, buffer, party ) ;
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
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#size()
     */
    public synchronized int size()
    {
        return m_allList.size() ;
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getName()
     */
    public String getName()
    {
        return m_config.getName() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getFreeCount()
     */
    public synchronized int getFreeCount()
    {
        return m_freeList.size() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getInUseCount()
     */
    public synchronized int getInUseCount()
    {
        return m_inUseList.size() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPool#getInterestedCount(
     * java.nio.ByteBuffer)
     */
    public int getInterestedCount( ByteBuffer buffer )
    {
        BufferListPair l_list = getBufferListPair( buffer ) ;
        Validate.notNull( l_list ) ;
        return l_list.size() ;
    }
    
    
    /**
     * Gets the monitor.
     * 
     * @return returns the monitor
     */
    public BufferPoolMonitor getMonitor()
    {
        return m_monitor ;
    }
    

    /**
     * Sets the monitor.
     * 
     * @param monitor the monitor to set
     */
    public void setMonitor( BufferPoolMonitor monitor )
    {
        m_monitor = monitor ;
    }
    
    
    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer l_buf = new StringBuffer( m_config.getName() ) ;
        l_buf.append( " buffer pool" ) ;
        return l_buf.toString() ;
    }
    
    
    // ------------------------------------------------------------------------
    // code dealing with pairs of buffers and an interested party list 
    // ------------------------------------------------------------------------
    
    
    /**
     * Finds and returns a BufferListPair by scanning the complete list of 
     * pairs looking for the pair that contains the same buffer.
     * 
     * @param buffer the buffer to search for in the list of pairs
     * @return null if the buffer does not exist or the pair containing it
     */
    BufferListPair getBufferListPair( ByteBuffer buffer )
    {
        BufferListPair l_list = null ;
        
        for ( int ii = 0; ii < m_allList.size(); ii++ )
        {
            l_list = ( BufferListPair ) m_allList.get( ii ) ;
            if ( buffer == l_list.getBuffer() )
            {
                return l_list ;
            }
        }
        
        return null ;
    }
    
    
    /**
     * Class used to pair up a buffer with a list to track the parties 
     * interested in the buffer. 
     */
    class BufferListPair
    {
        final ArrayList m_list ;
        final ByteBuffer m_buffer ;
        
        
        BufferListPair()
        {
            this( new ArrayList( 3 ), 
                    ByteBuffer.allocateDirect( m_config.getBufferSize() ) ) ;
        }

        
        BufferListPair( ByteBuffer buffer )
        {
            this( new ArrayList( 3 ), buffer ) ;
        }

        
        BufferListPair( ArrayList list, ByteBuffer buffer )
        {
            m_list = list ;
            m_buffer = buffer ;
        }
        
        
        ByteBuffer getBuffer()
        {
            return m_buffer ;
        }
        
        
        ArrayList getList()
        {
            return m_list ;
        }
        
        
        void add( Object party )
        {
            m_list.add( party ) ;
        }
        
        
        boolean contains( Object party )
        {
            return m_list.contains( party ) ;
        }
        
        
        boolean remove( Object party )
        {
            return m_list.remove( party ) ;
        }
        
        
        int size()
        {
            return m_list.size() ;
        }
    }
}
