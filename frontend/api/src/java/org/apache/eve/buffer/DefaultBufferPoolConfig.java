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
 * A BufferPoolConfig implementation bean.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultBufferPoolConfig implements BufferPoolConfig
{
    /** the name */
    private final String m_name ;
    /** the growth increment */
    private int m_inc = 0 ;
    /** the maximum pool size */
    private int m_max = 0 ;
    /** the initial pool size */
    private int m_ini = 0 ;
    /** the size of the buffers pooled */
    private int m_size = 0 ;
    
    
    /**
     * Creates a BufferPool configuration bean using the supplied values.
     * 
     * @param name the name
     * @param inc the growth increment
     * @param max the maximum pool size
     * @param ini the initial pool size 
     * @param size the size of the buffers pooled
     */
    public DefaultBufferPoolConfig( String name, int inc, int max, 
                                    int ini, int size )
    {
        m_name = name ;
        m_inc = inc ;
        m_max = max ;
        m_ini = ini ;
        m_size = size ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolConfig#getIncrement()
     */
    public int getIncrement()
    {
        return m_inc ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolConfig#getBufferSize()
     */
    public int getBufferSize()
    {
        return m_size ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolConfig#getInitialSize()
     */
    public int getInitialSize()
    {
        return m_ini ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolConfig#getMaximumSize()
     */
    public int getMaximumSize()
    {
        return m_max ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.buffer.BufferPoolConfig#getName()
     */
    public String getName()
    {
        return m_name ;
    }
}
