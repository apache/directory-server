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
package org.apache.eve.decoder.impl ;


import java.nio.ByteBuffer ;
import java.util.EventObject ;

import org.apache.eve.buffer.BufferPool;
import org.apache.eve.buffer.BufferPoolConfig;
import org.apache.eve.buffer.DefaultBufferPool;
import org.apache.eve.buffer.DefaultBufferPoolConfig;
import org.apache.eve.event.AbstractSubscriber;
import org.apache.eve.event.ConnectEvent;
import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.DefaultEventRouter ;
import org.apache.eve.event.InputEvent;
import org.apache.eve.event.RequestEvent;
import org.apache.eve.event.RequestSubscriber;

import org.apache.eve.seda.DefaultStageConfig ;
import org.apache.eve.thread.ThreadPool;
import org.apache.ldap.common.message.AbandonRequest;
import org.apache.ldap.common.message.AbandonRequestImpl;
import org.apache.ldap.common.message.MessageEncoder;

import junit.framework.TestCase ;


/**
 * Tests the decoder manager pojo.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultDecoderManagerTest extends TestCase implements 
    RequestSubscriber
{
    private ThreadPool tpool = null ;
    private EventRouter router = null ;
    private BufferPoolConfig bpConfig = null ; 
    private DefaultStageConfig config = null ;
    private DecodeStageHandler handler = null ;
    private BufferPool bp = null ;
    private DefaultDecoderManager decodeMan = null ;
    private RequestEvent event = null ;
    

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( DefaultDecoderManagerTest.class ) ;
    }

    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp() ;
        
        bpConfig = new DefaultBufferPoolConfig( "default", 10, 100, 10, 1024 ) ;
        bp = new DefaultBufferPool( bpConfig ) ;
        router = new DefaultEventRouter() ;
        router.subscribe( RequestEvent.class, this ) ;
        tpool = new ThreadPool()
        {
            /* (non-Javadoc)
             * @see org.apache.eve.thread.ThreadPool#execute(java.lang.Runnable)
             */
            public void execute( Runnable runnable )
            {
                // fake it out
                runnable.run() ;
            }
        } ;
        
        config = new DefaultStageConfig( "default", tpool ) ;
        decodeMan = new DefaultDecoderManager( router, config ) ;
        config.setHandler( new DecodeStageHandler( decodeMan ) ) ;
        decodeMan.start() ;
    }

    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown() ;
        
        tpool = null ;
        router = null ;
        bpConfig = null ; 
        config = null ;
        handler = null ;
        bp = null ;
        decodeMan.stop() ;
        decodeMan = null ;
        event = null ;
    }

    
    /**
     * Constructor for DefaultDecoderManagerTest.
     * @param arg0
     */
    public DefaultDecoderManagerTest(String arg0)
    {
        super( arg0 ) ;
    }

    
    public void testDefaultDecoderManager() throws Exception
    {
        AbandonRequest request = new AbandonRequestImpl( 6 ) ;
        request.setAbandoned( 44 ) ;
        MessageEncoder encoder = new MessageEncoder() ;
        
        byte [] encoded = encoder.encode( request ) ;
        ByteBuffer buf = bp.getBuffer( this ) ;
        buf.put( encoded ) ;
        buf.flip() ;
        
        final BufferPool pool = bp ;
        InputEvent e = new InputEvent( this, null, buf ) 
        {
            public ByteBuffer claimInterest(Object party)
            {
                pool.claimInterest(getBuffer(), party) ;
                return getBuffer().asReadOnlyBuffer() ;
            }

            public void releaseInterest(Object party)
            {
                pool.releaseClaim(getBuffer(), party) ;
            }
        } ;
        
        bp.releaseClaim(buf, this) ;
        router.publish( new ConnectEvent(this, null) ) ;
        router.publish( e ) ;
        decodeMan.stop() ;
        assertNotNull( this.event ) ;
    }
    
    
    public void inform( RequestEvent event )
    {
        System.out.println( "\n\nRequestEvent Set!!!\n\n" ) ;
        this.event = event ;
    }

    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.Subscriber#inform(java.util.EventObject)
     */
    public void inform( EventObject event )
    {
        try
        {
            AbstractSubscriber.inform( this, event ) ;
        }
        catch ( Exception e )
        {
            fail( "we should be able to inform just fine" ) ;
        }
    }

    
    /*
     * Class to test for void inform(EventObject)
     */
    public void testInformEventObject()
    {
    }

    
    /*
     * Class to test for void inform(InputEvent)
     */
    public void testInformInputEvent()
    {
    }

    
    /*
     * Class to test for void inform(DisconnectEvent)
     */
    public void testInformDisconnectEvent()
    {
    }

    
    /*
     * Class to test for void inform(ConnectEvent)
     */
    public void testInformConnectEvent()
    {
    }

    
    public void testDecodeOccurred()
    {
    }

    
    public void testSetCallback()
    {
    }

    
    public void testSetDecoderMonitor()
    {
    }

    
    public void testDisable()
    {
    }

    
    /*
     * Class to test for void decode(ClientKey, ByteBuffer)
     */
    public void testDecodeClientKeyByteBuffer()
    {
    }

    
    /*
     * Class to test for Object decode(ByteBuffer)
     */
    public void testDecodeByteBuffer()
    {
    }

    
    public void testGetMonitor()
    {
    }

    
    public void testSetMonitor()
    {
    }

    
    public void testGetDecoder()
    {
    }
}
