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

import org.apache.avalon.framework.logger.AbstractLogEnabled ;
import org.apache.commons.lang.ClassUtils;


/**
 * A monitor that is a Avalon LogEnabled and reports events on behalf of the
 * BufferPool component. 
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
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
