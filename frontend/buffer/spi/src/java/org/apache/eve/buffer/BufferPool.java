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


/**
 * Service interface for an NIO direct memory buffer pool.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface BufferPool
{
    /** for Avalon compatability */
    String ROLE = BufferPool.class.getName() ;
    
    /**
     * Acquires a dedicated buffer from the buffer pool and claims interest with
     * the buffer using an object representing the interested party.
     * 
     * @param party the object interested in the buffer
     * @return a claimed direct memory buffer
     */
    ByteBuffer getBuffer( Object party ) throws ResourceException ;
    
    /**
     * Allows a party to claim interest on a buffer pooled by this buffer.  The
     * buffer cannot be reclaimed until all the interested parties release their
     * interest claim on the buffer.
     * 
     * @param party the object interested in the buffer
     * @param buffer a claimed direct memory buffer pooled by this BufferPool
     * @throws IllegalArgumentException if the buffer is not direct or has not 
     * been recognized as a pooled resource of this pool. 
     */
    void claimInterest( ByteBuffer buffer, Object party ) ; 
    
    /**
     * Allows a party that claimed interest on a buffer to release the buffer.  
     * The buffer cannot be reclaimed until all the interested parties release 
     * their interest claim on the buffer.
     * 
     * @param buffer the buffer to release
     * @param owner the owner of the buffer
     * @throws IllegalArgumentException if the buffer is not direct or has not 
     * been recognized as a pooled resource of this pool. 
     */
    void releaseClaim( ByteBuffer buffer, Object party ) ;
    
    /**
     * Gets the configuration for this BufferPool.
     * 
     * @return the configuration for this BufferPool
     */
    BufferPoolConfig getConfig() ;
    
    /**
     * Gets the number of interested parties that have claimed interest on a
     * pooled buffer.  This number is like the link count.
     * 
     * @param buffer the buffer to get a interest count for
     * @return count of parties claiming interest on the buffer
     */
    int getInterestedCount( ByteBuffer buffer ) ; 
    
    /**
     * Gets a count of the number of free buffers in this BufferPool.
     * 
     * @return count of free buffers in this BufferPool
     */
    int getFreeCount() ;
    
    /**
     * Gets a count of the number of buffers currently being used in this 
     * BufferPool.
     * 
     * @return count of buffers currently being used in this BufferPool
     */
    int getInUseCount() ;
    
    /**
     * Gets the current size of this BufferPool.
     * 
     * @return the number of buffers total (free and in use) in this BufferPool
     */
    int size() ;

    /**
     * Gets the name of this BufferPool
     * 
     * @return the name of this BufferPool
     */
    String getName() ;
}
