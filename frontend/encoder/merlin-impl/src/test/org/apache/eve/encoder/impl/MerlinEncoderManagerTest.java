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
package org.apache.eve.encoder.impl ;


import java.util.EventObject;

import org.apache.avalon.merlin.unit.AbstractMerlinTestCase ;

import org.apache.eve.encoder.EncoderManager ;
import org.apache.eve.event.AbstractSubscriber;
import org.apache.eve.event.EventRouter;
import org.apache.eve.event.OutputEvent;
import org.apache.eve.event.OutputSubscriber;
import org.apache.eve.event.ResponseEvent;
import org.apache.ldap.common.message.AddResponse;
import org.apache.ldap.common.message.AddResponseImpl;
import org.apache.ldap.common.message.LdapResult;
import org.apache.ldap.common.message.LdapResultImpl;
import org.apache.ldap.common.message.ResultCodeEnum;


/**
 * Tests the Merlin component within Merlin!
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class MerlinEncoderManagerTest extends AbstractMerlinTestCase
    implements OutputSubscriber
{
    EncoderManager encman = null ;
    OutputEvent event = null ;
    EventRouter router = null ;

    
    public MerlinEncoderManagerTest( String a_name )
    {
        super( a_name ) ;
    }

    
    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( MerlinEncoderManagerTest.class ) ;
    }
    
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception
    {
        super.setUp() ;
        encman = ( EncoderManager ) 
            resolve( "/server/encoder-manager" ) ; 
        router = ( EventRouter )
            resolve( "/server/event-router" ) ; 
        router.subscribe( OutputEvent.class, this ) ;
    }

    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown()
    {
        super.tearDown() ;
        encman = null ;
        router = null ;
        event = null ;
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
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.event.OutputSubscriber#
     * inform(org.apache.eve.event.OutputEvent)
     */
    public void inform( OutputEvent event )
    {
        this.event = event ;
    }
    

    /**
     * Tests the encoding of a response to an add request.
     *
     */
    public void testAddResponse() throws Exception 
    {
        AddResponse response = new AddResponseImpl( 5 ) ;
        LdapResult result = new LdapResultImpl( response ) ;
        result.setErrorMessage( "Server is stubbed out" ) ;
        result.setMatchedDn( "uid=akarasulu,dc=example,dc=com" ) ;
        result.setResultCode( ResultCodeEnum.UNWILLINGTOPERFORM ) ;
        response.setLdapResult( result ) ;
        ResponseEvent event = new ResponseEvent( this, null, response ) ;
        router.publish( event ) ;
        
        Thread.sleep(1000) ;
        assertNotNull( this.event ) ;
    }
}
