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
package org.apache.eve.merlin.listener ;


import org.apache.eve.event.EventRouter ;
import org.apache.eve.listener.ListenerManager;

import org.apache.avalon.merlin.unit.AbstractMerlinTestCase ;


/**
 * Tests the listener out.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class MerlinListenerManagerTest extends AbstractMerlinTestCase
{
    EventRouter router = null ;
    ListenerManager listener = null ;

    /*
     * @see AbstractMerlinTestCase#setUp()
     */
    public void setUp() throws Exception
    {
        super.setUp() ;
        router = ( EventRouter ) resolve( "/server/event-router" ) ; 
        listener = ( ListenerManager ) resolve( "/server/listener-manager" ) ;
    }
    

    /*
     * @see AbstractMerlinTestCase#tearDown()
     */
    public void tearDown()
    {
        super.tearDown() ;
    }
    

    /**
     * Constructor for MerlinListenerManagerTest.
     * @param arg0
     */
    public MerlinListenerManagerTest(String arg0)
    {
        super( arg0 ) ;
    }

    
    public void testBind()
    {
    }


    public void testBind2()
    {
    }
}


