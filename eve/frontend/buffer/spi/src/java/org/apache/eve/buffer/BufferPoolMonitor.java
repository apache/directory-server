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


/**
 * The BufferPool monitor interface.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface BufferPoolMonitor
{
    /**
     * Monitors the augmentation of a BufferPool.
     * 
     * @param a_bp the BufferPool that grew
     */
    void augmented( BufferPool a_bp ) ;
    
    /**
     * Monitors the release of an unclaimed buffer which cannot be released if
     * there is no claim and it is on the free list.
     * 
     * @param a_bp the BufferPool the buffer is released back to
     * @param a_buffer the buffer that is released
     * @param a_releaser the object doing the releasing
     */
    void releaseOfUnclaimed( BufferPool a_bp, ByteBuffer a_buffer, 
                             Object a_releaser ) ;
    
    /**
     * Monitors the giving of a buffer to a client.
     * 
     * @param a_bp the BufferPool the buffer is taken from
     * @param a_buffer the buffer that is taken
     * @param a_taker the object doing the taking
     */
    void bufferTaken( BufferPool a_bp, ByteBuffer a_buffer, Object a_taker ) ;
    
    /**
     * Monitors the release of a buffer to be reclaimed onto the free list.
     * 
     * @param a_bp the BufferPool the buffer is released back to
     * @param a_buffer the buffer that is released
     * @param a_releaser the object doing the releasing
     */
    void bufferReleased( BufferPool a_bp, ByteBuffer a_buffer, 
                         Object a_releaser ) ;
    
    /**
     * Monitors the claim of interest in a buffer.
     * 
     * @param a_bp the BufferPool the buffer of interest is from
     * @param a_buffer the buffer that is the interest
     * @param a_claimer the object doing the interest claiming
     */
    void interestClaimed( BufferPool a_bp,  ByteBuffer a_buffer, 
                          Object a_claimer ) ;
    
    /**
     * Monitors the release of a claim on a buffer.
     * 
     * @param a_bp the BufferPool the buffer of interest is from
     * @param a_buffer the buffer that has an interest claim released
     * @param a_releaser the object doing the interest claim releasing
     */
    void interestReleased( BufferPool a_bp,  ByteBuffer a_buffer, 
                          Object a_releaser ) ;

    /**
     * Monitors situations where the BufferPool is in full use at its maximum 
     * capacity and a request for a Buffer cannot be satisfied.
     * 
     * @param a_bp the BufferPool the where the buffer is unavailable
     * @param a_party the party trying to acquire the buffer resource
     */
    void resourceUnavailable( BufferPool a_bp, Object a_party ) ;
    
    /**
     * A party that never registered interest in a buffer is attempting to 
     * remove its interest on a buffer.
     * 
     * @param a_bp the buffer pool this fault is occuring in
     * @param a_buffer the buffer the party is trying to claim interest on
     * @param a_party the party trying to claim interest
     */
    void unregisteredParty( BufferPool a_bp, ByteBuffer a_buffer, 
                            Object a_party ) ;
    
    /**
     * Monitors attempts to deal with a buffer that is not a pooled resource.
     * 
     * @param a_bp the BufferPool that does not contain the buffer
     * @param a_buffer the buffer that is not pooled in the BufferPool
     * @param a_party the party that attempted the operation causing the fault
     */
    void nonPooledBuffer( BufferPool a_bp, ByteBuffer a_buffer, 
                          Object a_party ) ;
}
