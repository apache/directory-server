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


import java.nio.ByteBuffer;

import org.apache.eve.ResourceException;

import junit.framework.TestCase ;


/**
 * Tests the default buffer pool implementation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultBufferPoolTest extends TestCase
{

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( DefaultBufferPoolTest.class ) ;
    }

    
    /**
     * Constructor for DefaultBufferPoolTest.
     * @param arg0
     */
    public DefaultBufferPoolTest( String arg0 )
    {
        super( arg0 ) ;
    }


    DefaultBufferPool m_bp = null ;
    
    
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception
    {
        super.setUp() ;
        DefaultBufferPoolConfig l_config = new DefaultBufferPoolConfig(
                "default", 4, 10, 2, 4096 ) ;
        m_bp = new DefaultBufferPool( l_config ) ;
        m_bp.setMonitor( new LoggingBufferMonitor() ) ;
    }

    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() throws Exception
    {
        m_bp = null ;
        super.tearDown() ;
    }
    
    
    public void testGetConfig() throws Exception
    {
        BufferPoolConfig l_config = m_bp.getConfig() ;
        assertNotNull( "Configuration was null", l_config ) ;
    }
    

    public void testGetBuffer() throws Exception
    {
        final int l_max = m_bp.getConfig().getMaximumSize() ;
        ByteBuffer[] l_buffers = new ByteBuffer[ l_max ] ;
        
        for ( int ii = 0; ii < m_bp.getConfig().getMaximumSize(); ii++ )
        {    
            l_buffers[ii] = m_bp.getBuffer( this ) ;
            assertNotNull( "Got null ByteBuffer", l_buffers[ii] ) ;
        }
        
        ByteBuffer l_buf = null ;
        try
        {
            l_buf = m_bp.getBuffer( this ) ;
        }
        catch( ResourceException e ) 
        {
            assertNull( l_buf ) ;
            assertNotNull( e ) ;
        }
    }


    public void testGetFreeCount() throws Exception
    {
        final int l_max = m_bp.getConfig().getMaximumSize() ;
        ByteBuffer[] l_buffers = new ByteBuffer[ l_max ] ;
        
        for ( int ii = 0; ii < m_bp.getConfig().getMaximumSize(); ii++ )
        {    
            l_buffers[ii] = m_bp.getBuffer( this ) ;
            assertNotNull( "Got null ByteBuffer", l_buffers[ii] ) ;
            assertEquals( "Free count was off", m_bp.size() - ii - 1,
                    m_bp.getFreeCount() ) ;
        }
        
        ByteBuffer l_buf = null ;
        try
        {
            l_buf = m_bp.getBuffer( this ) ;
        }
        catch( ResourceException e ) 
        {
            assertNull( l_buf ) ;
            assertNotNull( e ) ;
        }
    }

    
    public void testGetInUseCount() throws Exception
    {
        final int l_max = m_bp.getConfig().getMaximumSize() ;
        ByteBuffer[] l_buffers = new ByteBuffer[ l_max ] ;
        
        for ( int ii = 0; ii < m_bp.getConfig().getMaximumSize(); ii++ )
        {    
            l_buffers[ii] = m_bp.getBuffer( this ) ;
            assertNotNull( "Got null ByteBuffer", l_buffers[ii] ) ;
            assertEquals( "In use count was off", ii+1,
                    m_bp.getInUseCount() ) ;
        }
        
        ByteBuffer l_buf = null ;
        try
        {
            l_buf = m_bp.getBuffer( this ) ;
        }
        catch( ResourceException e ) 
        {
            assertNull( l_buf ) ;
            assertNotNull( e ) ;
        }
    }
    
    
    public void testGetInterestedCount()
        throws Exception
    {
        final int l_max = m_bp.getConfig().getMaximumSize() ;
        ByteBuffer[] l_buffers = new ByteBuffer[ l_max ] ;
        
        for ( int ii = 0; ii < m_bp.getConfig().getMaximumSize(); ii++ )
        {    
            l_buffers[ii] = m_bp.getBuffer( this ) ;
            assertNotNull( "Got null ByteBuffer", l_buffers[ii] ) ;
            assertEquals( "In use count was off", ii+1,
                    m_bp.getInUseCount() ) ;
            assertEquals( "Interest count was off", 
                    1, m_bp.getInterestedCount( l_buffers[ii] ) ) ;
        }
        
        ByteBuffer l_buf = null ;
        try
        {
            l_buf = m_bp.getBuffer( this ) ;
        }
        catch( ResourceException e ) 
        {
            assertNull( l_buf ) ;
            assertNotNull( e ) ;
        }
    }

    
    public void testReleaseClaim() 
        throws Exception
    {
    }

    
    public void testSize() 
        throws Exception
    {
        final int l_max = m_bp.getConfig().getMaximumSize() ;
        ByteBuffer[] l_buffers = new ByteBuffer[ l_max ] ;
        
        for ( int ii = 0; ii < m_bp.getConfig().getMaximumSize(); ii++ )
        {    
            l_buffers[ii] = m_bp.getBuffer( this ) ;
            assertNotNull( "Got null ByteBuffer", l_buffers[ii] ) ;
            
            if ( ii < 2 )
            {    
                assertEquals( "size was off", 2, m_bp.size() ) ;
            }
            else if ( ii >= 2 && ii < 6 )
            {
                assertEquals( "size was off", 6, m_bp.size() ) ;
            }
            else if ( ii >= 6 && ii < 10 )
            {
                assertEquals( "size was off", 10, m_bp.size() ) ;
            }
            else
            {
                throw new IllegalStateException( "should never get here!" ) ;
            }
        }
        
        ByteBuffer l_buf = null ;
        try
        {
            l_buf = m_bp.getBuffer( this ) ;
        }
        catch( ResourceException e ) 
        {
            assertNull( l_buf ) ;
            assertNotNull( e ) ;
        }
    }
}
