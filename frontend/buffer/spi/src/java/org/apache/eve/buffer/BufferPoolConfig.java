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

/**
 * BufferPool configuration parameters used regardless of implementation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface BufferPoolConfig
{
    /**
     * Gets the name of this BufferPool configuration.
     * 
     * @return the name 
     */
    String getName() ;
    
    /**
     * The increment by which the BufferPool should grow.
     * 
     * @return the increment amount for the BufferPool
     */
    int getIncrement() ;
    
    /**
     * The size of the buffers that are pooled.  Recommended settings are of 
     * multiples of 1024: 1k, 2k, 4k and 8k.
     * 
     * @return the size of the pooled buffers
     */
    int getBufferSize() ;
    
    /**
     * Gets the initial size of the pool.  This should be a multiple of the 
     * pool's growth increment.
     * 
     * @return the initial pool size.
     */
    int getInitialSize() ;
    
    /**
     * The maximum size a BufferPool can grow to.  This should be a multiple of
     * the pool's growth increment.
     * 
     * @return the maximum pool size.
     */
    int getMaximumSize() ;
}
