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
package org.apache.eve.event ;


import java.nio.ByteBuffer ;
import java.util.EventObject ;

import org.apache.eve.listener.ClientKey ;

import junit.framework.TestCase ;


/**
 * Tests the AbstractSubscriber class.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class AbstractSubscriberTest extends TestCase
{
    TestSubscriber subscriber = null ;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp() ;
        subscriber = new TestSubscriber() ;
    }

    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown() ;
        subscriber = null ;
    }

    
    /*
     * Class to test for void AbstractSubscriber()
     */
    public void testAbstractSubscriber()
    {
    }

    
    /*
     * Class to test for void AbstractSubscriber(SubscriberMonitor)
     */
    public void testAbstractSubscriberSubscriberMonitor()
    {
    }
    

    /*
     * Class to test for void inform(EventObject)
     */
    public void testInformEventObject()
    {
        EventObject e = new EventSubclass( null, null ) ;
        subscriber.inform( e ) ;
        subscriber.assertDelivery() ;
        subscriber.inform( e ) ;
        subscriber.assertDelivery() ;
    }

    
    /*
     * Class to test for void inform(Subscriber, EventObject, SubscriberMonitor)
     */
    public void testInformSubscriberEventObjectSubscriberMonitor()
    {
    }

    
    class EventSubclass extends InputEvent  
    {
        public EventSubclass( ClientKey key, ByteBuffer buf )
        {
            super( AbstractSubscriberTest.this, key, buf ) ;
        }
        
        /* (non-Javadoc)
         * @see org.apache.eve.event.InputEvent#claimInterest(java.lang.Object)
         */
        public ByteBuffer claimInterest( Object party )
        {
            return null ;
        }

        /* (non-Javadoc)
         * @see org.apache.eve.event.InputEvent#releaseInterest(java.lang.Object)
         */
        public void releaseInterest( Object party )
        {
        }
    } ;


    class TestSubscriber extends AbstractSubscriber
    {
        InputEvent e = null ;
            
        public void assertDelivery() 
        {
            assertNotNull( e ) ;
            e = null ;
        }
            
        public void inform( InputEvent event )
        {
            assertNotNull( event ) ;
            e = event ;
        }
    } ;

}
