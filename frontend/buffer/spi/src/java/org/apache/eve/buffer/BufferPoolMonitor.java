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


/**
 * The BufferPool monitor interface.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface BufferPoolMonitor
{
    /**
     * Monitors the augmentation of a BufferPool.
     * 
     * @param bp the BufferPool that grew
     */
    void augmented( BufferPool bp ) ;
    
    /**
     * Monitors the release of an unclaimed buffer which cannot be released if
     * there is no claim and it is on the free list.
     * 
     * @param bp the BufferPool the buffer is released back to
     * @param buffer the buffer that is released
     * @param releaser the object doing the releasing
     */
    void releaseOfUnclaimed( BufferPool bp, ByteBuffer buffer, 
                             Object releaser ) ;
    
    /**
     * Monitors the giving of a buffer to a client.
     * 
     * @param bp the BufferPool the buffer is taken from
     * @param buffer the buffer that is taken
     * @param taker the object doing the taking
     */
    void bufferTaken( BufferPool bp, ByteBuffer buffer, Object taker ) ;
    
    /**
     * Monitors the release of a buffer to be reclaimed onto the free list.
     * 
     * @param bp the BufferPool the buffer is released back to
     * @param buffer the buffer that is released
     * @param releaser the object doing the releasing
     */
    void bufferReleased( BufferPool bp, ByteBuffer buffer, Object releaser ) ;
    
    /**
     * Monitors the claim of interest in a buffer.
     * 
     * @param bp the BufferPool the buffer of interest is from
     * @param buffer the buffer that is the interest
     * @param claimer the object doing the interest claiming
     */
    void interestClaimed( BufferPool bp,  ByteBuffer buffer, Object claimer ) ;
    
    /**
     * Monitors the release of a claim on a buffer.
     * 
     * @param bp the BufferPool the buffer of interest is from
     * @param buffer the buffer that has an interest claim released
     * @param releaser the object doing the interest claim releasing
     */
    void interestReleased( BufferPool bp,  ByteBuffer buffer, 
                           Object releaser ) ;

    /**
     * Monitors situations where the BufferPool is in full use at its maximum 
     * capacity and a request for a Buffer cannot be satisfied.
     * 
     * @param bp the BufferPool the where the buffer is unavailable
     * @param party the party trying to acquire the buffer resource
     */
    void resourceUnavailable( BufferPool bp, Object party ) ;
    
    /**
     * A party that never registered interest in a buffer is attempting to 
     * remove its interest on a buffer.
     * 
     * @param bp the buffer pool this fault is occuring in
     * @param buffer the buffer the party is trying to claim interest on
     * @param party the party trying to claim interest
     */
    void unregisteredParty( BufferPool bp, ByteBuffer buffer, Object party ) ;
    
    /**
     * Monitors attempts to deal with a buffer that is not a pooled resource.
     * 
     * @param bp the BufferPool that does not contain the buffer
     * @param buffer the buffer that is not pooled in the BufferPool
     * @param party the party that attempted the operation causing the fault
     */
    void nonPooledBuffer( BufferPool bp, ByteBuffer buffer, Object party ) ;
}
