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
package org.apache.eve.buffer;

import java.nio.ByteBuffer;

/**
 * $todo$ doc me
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class BufferPoolConsoleLogger implements BufferPoolMonitor
{
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#
     * augmented(org.apache.eve.buffer.BufferPool)
     */
    public void augmented( BufferPool a_bp )
    {
        System.out.println( "Just augmented the buffer pool" ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#bufferReleased(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void bufferReleased(
        BufferPool a_bp,
        ByteBuffer a_buffer,
        Object a_releaser)
    {
        System.out.println( a_releaser + " released " + a_buffer 
                + " from pool " + a_bp ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#bufferTaken(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void bufferTaken(
        BufferPool a_bp,
        ByteBuffer a_buffer,
        Object a_taker)
    {
        System.out.println( a_taker + " took " + a_buffer 
                + " from pool " + a_bp ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#interestClaimed(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void interestClaimed(
        BufferPool a_bp,
        ByteBuffer a_buffer,
        Object a_claimer)
    {
        System.out.println( a_claimer + " claimed interest in " + a_buffer 
                + " from pool " + a_bp ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#interestReleased(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void interestReleased(
        BufferPool a_bp,
        ByteBuffer a_buffer,
        Object a_releaser)
    {
        System.out.println( a_releaser + " released interest in " + a_buffer 
                + " from pool " + a_bp ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#nonPooledBuffer(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void nonPooledBuffer(
        BufferPool a_bp,
        ByteBuffer a_buffer,
        Object a_party)
    {
        System.out.println( a_party + " tried to release interest in " 
                + a_buffer + " from pool " + a_bp 
                + " but resouce was not from this pool.") ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#resourceUnavailable(
     * org.apache.eve.buffer.BufferPool, java.lang.Object)
     */
    public void resourceUnavailable( BufferPool a_bp, Object a_party )
    {
        System.out.println( "BufferPool " + a_bp 
                + " is at capacity - cannot allocate buffer resouce to " 
                + a_party ) ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#unregisteredParty(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void unregisteredParty(
        BufferPool a_bp,
        ByteBuffer a_buffer,
        Object a_party)
    {
        System.out.println( a_party 
                + " has not registered as claiming interest on " + a_buffer 
                + " from pool " + a_bp ) ;
    }


    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#releaseOfUnclaimed(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void releaseOfUnclaimed( BufferPool a_bp, ByteBuffer a_buffer,
									Object a_releaser )
    {
        System.out.println( a_releaser + " attempted to release interest in " 
                + a_buffer + " from pool " 
                + a_bp + " when the buffer was not claimed." ) ;
    }
}
