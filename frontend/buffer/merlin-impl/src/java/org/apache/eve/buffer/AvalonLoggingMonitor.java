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

import org.apache.avalon.framework.logger.AbstractLogEnabled ;
import org.apache.commons.lang.ClassUtils;


/**
 * A monitor that is a Avalon LogEnabled and reports events on behalf of the
 * BufferPool component. 
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Rev: 6444 $
 */
public class AvalonLoggingMonitor
    extends AbstractLogEnabled
    implements BufferPoolMonitor
{
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#augmented(
     * org.apache.eve.buffer.BufferPool)
     */
    public void augmented( BufferPool a_bp )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( a_bp + " grew by and increment of "
                    + a_bp.getConfig().getIncrement() + " to a size of " 
                    + a_bp.size() + " buffers total!" ) ;
            getLogger().debug( a_bp + " currently has " + a_bp.getFreeCount()
                    + " buffers free with " + a_bp.getInUseCount() 
                    + " buffers in use." ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#bufferTaken(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void bufferTaken( BufferPool a_bp, ByteBuffer a_buffer,
        Object a_taker )
    {
        if ( getLogger().isDebugEnabled() )
        {    
            getLogger().debug( a_bp + " had a buffer taken by " 
                    + getName( a_taker ) ) ;
            getLogger().debug( a_bp + " currently has " + a_bp.getFreeCount()
                    + " buffers free with " + a_bp.getInUseCount() 
                    + " buffers in use." ) ;
            getLogger().debug( "taken buffer has an interested party count of " 
                    + a_bp.getInterestedCount( a_buffer ) ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#bufferReleased(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void bufferReleased( BufferPool a_bp, ByteBuffer a_buffer,
        Object a_releaser )
    {
        if ( getLogger().isDebugEnabled() )
        {    
            getLogger().debug( a_bp + " had buffer released by " 
                    + getName( a_releaser ) ) ;
            getLogger().debug( a_bp + " currently has " + a_bp.getFreeCount()
                    + " buffers free with " + a_bp.getInUseCount() 
                    + " buffers in use." ) ;
            getLogger().debug( "taken buffer has an interested party count of " 
                    + a_bp.getInterestedCount( a_buffer ) ) ;
        }
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#interestClaimed(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void interestClaimed( BufferPool a_bp, ByteBuffer a_buffer,
        Object a_claimer )
    {
        if ( getLogger().isDebugEnabled() )
        {    
            getLogger().debug( getName( a_claimer ) 
                    + " claimed interest on a buffer from " + a_bp ) ;
            getLogger().debug( a_bp + " currently has " + a_bp.getFreeCount()
                    + " buffers free with " + a_bp.getInUseCount() 
                    + " buffers in use." ) ;
            getLogger().debug( "taken buffer has an interested party count of " 
                    + a_bp.getInterestedCount( a_buffer ) ) ;
        }
    }
    

    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#interestReleased(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void interestReleased( BufferPool a_bp, ByteBuffer a_buffer,
                                  Object a_releaser )
    {
        if ( getLogger().isDebugEnabled() )
        {    
            getLogger().debug( getName( a_releaser ) 
                    + " released interest on a buffer from " + a_bp ) ;
            getLogger().debug( a_bp + " currently has " + a_bp.getFreeCount()
                    + " buffers free with " + a_bp.getInUseCount() 
                    + " buffers in use." ) ;
            getLogger().debug( "taken buffer has an interested party count of " 
                    + a_bp.getInterestedCount( a_buffer ) ) ;
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#resourceUnavailable(
     * org.apache.eve.buffer.BufferPool, java.lang.Object)
     */
    public void resourceUnavailable( BufferPool a_bp, Object a_party ) 
    {
        if ( getLogger().isErrorEnabled() )
        {    
            getLogger().error( getName( a_party ) 
                    + " tried to get a buffer from " 
                    + a_bp + " but no free resourses were available." ) ;
        }
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#unregisteredParty(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void unregisteredParty( BufferPool a_bp, ByteBuffer a_buffer, 
                                   Object a_party ) 
    {
        if ( getLogger().isDebugEnabled() )
        {    
            getLogger().debug( getName( a_party ) 
                    + " never claimed interest on a buffer from " + a_bp ) ;
        }
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#nonPooledBuffer(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void nonPooledBuffer( BufferPool a_bp, ByteBuffer a_buffer, 
                                 Object a_party )
    {
        if ( getLogger().isDebugEnabled() )
        {    
            getLogger().debug( getName( a_party ) 
                    + " tried to claim or release interest for a buffer from " 
                    + a_bp ) ;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolMonitor#releaseOfUnclaimed(
     * org.apache.eve.buffer.BufferPool, java.nio.ByteBuffer, java.lang.Object)
     */
    public void releaseOfUnclaimed( BufferPool a_bp, ByteBuffer a_buffer, 
                                    Object a_releaser )
    {
        if ( getLogger().isErrorEnabled() )
        {    
            getLogger().error( getName( a_releaser ) 
                    + " that never claimed interest on "
                    + "a buffer tried to release claim to it from " + a_bp ) ;
        }
    }
    
    
    public String getName( Object a_obj )
    {
        return ClassUtils.getShortClassName( a_obj.getClass() ) ;
    }
}
