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


/**
 * Service interface for an NIO direct memory buffer pool.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
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
     * @param a_party the object interested in the buffer
     * @return a claimed direct memory buffer
     */
    ByteBuffer getBuffer( Object a_party ) throws ResourceException ;
    
    /**
     * Allows a party to claim interest on a buffer pooled by this buffer.  The
     * buffer cannot be reclaimed until all the interested parties release their
     * interest claim on the buffer.
     * 
     * @param a_party the object interested in the buffer
     * @param a_buffer a claimed direct memory buffer pooled by this BufferPool
     * @throws IllegalArgumentException if the buffer is not direct or has not 
     * been recognized as a pooled resource of this pool. 
     */
    void claimInterest( ByteBuffer a_buffer, Object a_party ) ; 
    
    /**
     * Allows a party that claimed interest on a buffer to release the buffer.  
     * The buffer cannot be reclaimed until all the interested parties release 
     * their interest claim on the buffer.
     * 
     * @param a_buffer the buffer to release
     * @param a_owner the owner of the buffer
     * @throws IllegalArgumentException if the buffer is not direct or has not 
     * been recognized as a pooled resource of this pool. 
     */
    void releaseClaim( ByteBuffer a_buffer, Object a_party ) ;
    
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
     * @param a_buffer the buffer to get a interest count for
     * @return count of parties claiming interest on the buffer
     */
    int getInterestedCount( ByteBuffer a_buffer ) ; 
    
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
