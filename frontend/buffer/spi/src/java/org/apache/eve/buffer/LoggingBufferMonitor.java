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

import org.apache.commons.logging.Log ;
import org.apache.commons.logging.LogFactory ;


/**
 * Logging monitor for a BufferPool.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class LoggingBufferMonitor implements BufferPoolMonitor
{
    /** the default name for the logging channel */
    private final String DEFAULT = "BufferPool" ;
    /** the log - can be any implementation */
    private final Log log ; 
    
    
    /**
     * Creates a buffer pool logging monitor.
     */
    public BufferPoolConsoleLogger()
    {
        log = LogFactory.getLog( DEFAULT ) ;
    }
    
    
    /**
     * Creates a buffer pool logging monitor.
     * 
     * @param name the logging channel name
     */
    public BufferPoolConsoleLogger( String name )
    {
        log = LogFactory.getLog( name ) ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#
     * augmented(org.apache.eve.buffer.BufferPool)
     */
    public void augmented( BufferPool bp )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Just augmented the buffer pool" ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#bufferReleased(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void bufferReleased( BufferPool bp, ByteBuffer buffer,
								Object releaser)
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( releaser + " released " + buffer 
                    + " from pool " + bp ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#bufferTaken(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void bufferTaken( BufferPool bp, ByteBuffer buffer,
							 Object taker )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( taker + " took " + buffer + " from pool " + bp ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#interestClaimed(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void interestClaimed( BufferPool bp, ByteBuffer buffer, 
                                 Object claimer )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( claimer + " claimed interest in " + buffer 
                        + " from pool " + bp ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#interestReleased(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void interestReleased( BufferPool bp, ByteBuffer buffer,
								  Object releaser )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( releaser + " released interest in " + buffer 
                    + " from pool " + bp ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#nonPooledBuffer(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void nonPooledBuffer( BufferPool bp, ByteBuffer buffer,
								 Object party )
    {
        if ( log.isErrorEnabled() )
        {
            log.error( party + " tried to release interest in " 
                        + buffer + " from pool " + bp 
                        + " but resouce was not from this pool.") ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#resourceUnavailable(
     * org.apache.eve.buffer.BufferPool, java.lang.Object)
     */
    public void resourceUnavailable( BufferPool bp, Object party )
    {
        if ( log.isErrorEnabled() )
        {
            log.error( "BufferPool " + bp 
                + " is at capacity - cannot allocate buffer resouce to " 
                + party ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#unregisteredParty(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void unregisteredParty( BufferPool bp, ByteBuffer buffer, 
                                   Object party )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( party 
                + " has not registered as claiming interest on " + buffer 
                + " from pool " + bp ) ;
        }
    }


    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#releaseOfUnclaimed(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void releaseOfUnclaimed( BufferPool bp, ByteBuffer buffer,
									Object releaser )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( releaser + " attempted to release interest in " 
                    + buffer + " from pool " 
                    + bp + " when the buffer was not claimed." ) ;
        }
    }
}
