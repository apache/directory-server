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


/**
 * A BufferPoolConfig implementation bean.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
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
     * @param a_name the name
     * @param a_inc the growth increment
     * @param a_max the maximum pool size
     * @param a_ini the initial pool size 
     * @param a_size the size of the buffers pooled
     */
    public DefaultBufferPoolConfig( String a_name, int a_inc, int a_max, 
                                    int a_ini, int a_size )
    {
        m_name = a_name ;
        m_inc = a_inc ;
        m_max = a_max ;
        m_ini = a_ini ;
        m_size = a_size ;
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
