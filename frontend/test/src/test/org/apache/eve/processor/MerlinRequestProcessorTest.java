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
package org.apache.eve.processor ;


import java.util.EventObject;

import org.apache.avalon.merlin.unit.AbstractMerlinTestCase ;

import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.RequestEvent ;
import org.apache.eve.event.ResponseEvent ;
import org.apache.eve.event.ResponseSubscriber ;
import org.apache.eve.event.AbstractSubscriber ;
import org.apache.eve.processor.RequestProcessor;
import org.apache.ldap.common.message.AbandonRequest ;
import org.apache.ldap.common.message.AbandonRequestImpl ;
import org.apache.ldap.common.message.AddRequest;
import org.apache.ldap.common.message.AddRequestImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.message.MessageTypeEnum;


/**
 * Tests the Merlin component within Merlin!
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class MerlinRequestProcessorTest extends AbstractMerlinTestCase
    implements ResponseSubscriber
{
    ResponseEvent event = null ;
    EventRouter router = null ;
    RequestProcessor processor = null ;

    
    public MerlinRequestProcessorTest( String a_name )
    {
        super( a_name ) ;
    }

    
    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( MerlinRequestProcessorTest.class ) ;
    }
    
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception
    {
        super.setUp() ;
        router = ( EventRouter ) resolve( "/server/event-router" ) ; 
        router.subscribe( ResponseEvent.class, this ) ;
        processor = ( RequestProcessor ) resolve( "/server/request-processor" ); 
    }

    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown()
    {
        super.tearDown() ;
        event = null ;
        router = null ;
        processor = null ;
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
        catch( Throwable t )
        {
            t.printStackTrace() ;
            fail( "failed to deliver event " + event ) ;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.ResponseSubscriber#
     * inform(org.apache.eve.event.ResponseEvent)
     */
    public void inform( ResponseEvent event )
    {
        assertNotNull( event ) ;
        this.event = event ;
    }


    // ------------------------------------------------------------------------
    // T E S T C A S E S
    // ------------------------------------------------------------------------


    /**
     * Tests the handling of an Abandon request.
     * 
     * @throws Exception on failures
     */
    public void testAbandon() throws Exception
    {
        AbandonRequest req = new AbandonRequestImpl( 5 ) ;
        req.setAbandoned( 3 ) ;
        RequestEvent e = new RequestEvent( this, null, req ) ;
        router.publish( e ) ;

        // stop the kernel to get event delivery
        super.tearDown();

        // this message does not produce a response
        assertNull( this.event ) ;
    }


    /**
     * Tests the handling of an Add request.
     * 
     * @throws Exception on failures
     */
    public void testAdd() throws Exception
    {
        AddRequest req = new AddRequestImpl( 5 ) ;
        LockableAttributesImpl attrs = new LockableAttributesImpl( req ) ;
        attrs.put( "testAttrId", "testAttrValue" ) ;
        req.setEntry( attrs ) ;
        req.setName( "uid=akarasulu,dc=example,dc=com" ) ;
        RequestEvent e = new RequestEvent( this, null, req ) ;
        router.publish( e ) ;
        
        // stop the kernel to get event delivery
        super.tearDown();

        // this message does not produce a response
        assertNotNull( this.event ) ;
        assertEquals( 5, this.event.getResponse().getMessageId() ) ;
        assertEquals( MessageTypeEnum.ADDRESPONSE, 
                this.event.getResponse().getType() ) ;
    }
}
