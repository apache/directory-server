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
package org.apache.eve.listener ;


import org.apache.eve.event.EventRouter ;
import org.apache.eve.event.DefaultEventRouter ;

import junit.framework.TestCase ;


/**
 * Tests the default ListenerManager's operations.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultListenerManagerTest extends TestCase
{
    /** An event router to use for testing */
    private EventRouter router = null ;
    /** the defualt ListenerManager to test */
    private DefaultListenerManager listener = null ;
    
    

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp() ;
        
        router = new DefaultEventRouter() ;
        listener = new DefaultListenerManager( router ) ;
        listener.start() ;
    }

    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown() ;
        
        router = null ;
        listener.stop() ;
        listener = null ;
    }
    

    /**
     * Constructor for DefaultListenerManagerTest.
     * @param arg0
     */
    public DefaultListenerManagerTest( String arg0 )
    {
        super( arg0 ) ;
    }
    

    public void testBind() throws Exception
    {
        listener.bind( new 
                LdapServerListener( "localhost", 10389, 100, false ) ) ;
    }

    
    public void testUnbind() throws Exception
    {
        ServerListener sl = new 
            LdapServerListener( "localhost", 10389, 100, false ) ;
        listener.bind( sl ) ;
        listener.unbind( sl ) ;
    }
}
