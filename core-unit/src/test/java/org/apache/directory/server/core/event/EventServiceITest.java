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
package org.apache.directory.server.core.event;


import org.apache.directory.server.core.unit.AbstractAdminTestCase;

import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.EventObject;


/**
 * Test cases for the event service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EventServiceITest extends AbstractAdminTestCase
{
    /**
     * Test to make sure NamingListener's are no longer registered
     * after they are removed via the EventContex.removeNamingListener method.
     */
    public void testRemoveNamingListener() throws NamingException
    {
        TestListener listener = new TestListener();
        EventDirContext ctx = ( EventDirContext ) super.sysRoot.lookup( "" );
        ctx.addNamingListener( "", SearchControls.SUBTREE_SCOPE, listener );
        Attributes testEntry = new BasicAttributes( "ou", "testentry", true );
        Attribute objectClass = new BasicAttribute( "objectClass", "top" );
        objectClass.add( "organizationalUnit" );
        testEntry.put( objectClass );
        ctx.createSubcontext( "ou=testentry", testEntry );

        assertEquals( 1, listener.getEventRecords().size() );
        EventRecord rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
        assertEquals( ctx, rec.event.getSource() );

        ctx.removeNamingListener( listener );
        ctx.destroySubcontext( "ou=testentry" );

        assertEquals( 1, listener.getEventRecords().size() );
        rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
        assertEquals( ctx, rec.event.getSource() );

        // readd the entry once again just to make sure
        ctx.createSubcontext( "ou=testentry", testEntry );
        assertEquals( 1, listener.getEventRecords().size() );
        rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
        assertEquals( ctx, rec.event.getSource() );
    }


    /**
     * Test to make sure NamingListener's are no longer registered
     * after the context used for registration is closed.
     */
    public void testContextClose() throws NamingException
    {
        TestListener listener = new TestListener();
        EventDirContext ctx = ( EventDirContext ) super.sysRoot.lookup( "" );
        ctx.addNamingListener( "", SearchControls.SUBTREE_SCOPE, listener );
        Attributes testEntry = new BasicAttributes( "ou", "testentry", true );
        Attribute objectClass = new BasicAttribute( "objectClass", "top" );
        objectClass.add( "organizationalUnit" );
        testEntry.put( objectClass );
        ctx.createSubcontext( "ou=testentry", testEntry );

        assertEquals( 1, listener.getEventRecords().size() );
        EventRecord rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
        assertEquals( ctx, rec.event.getSource() );

        ctx.close();
        ctx = ( EventDirContext ) super.sysRoot.lookup( "" );
        ctx.destroySubcontext( "ou=testentry" );

        assertEquals( 1, listener.getEventRecords().size() );
        rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );

        // readd the entry once again just to make sure
        ctx.createSubcontext( "ou=testentry", testEntry );
        assertEquals( 1, listener.getEventRecords().size() );
        rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
    }

    public class TestListener implements ObjectChangeListener, NamespaceChangeListener
    {
        List events = new ArrayList();


        public List getEventRecords()
        {
            return events;
        }


        public void objectChanged( NamingEvent event )
        {
            events.add( new EventRecord( "objectChanged", event ) );
        }


        public void namingExceptionThrown( NamingExceptionEvent event )
        {
            events.add( new EventRecord( "namingExceptionThrown", event ) );
        }


        public void objectAdded( NamingEvent event )
        {
            events.add( new EventRecord( "objectAdded", event ) );
        }


        public void objectRemoved( NamingEvent event )
        {
            events.add( new EventRecord( "objectRemoved", event ) );
        }


        public void objectRenamed( NamingEvent event )
        {
            events.add( new EventRecord( "objectRenamed", event ) );
        }
    }

    public class EventRecord
    {
        String method;
        EventObject event;


        EventRecord(String method, EventObject event)
        {
            this.method = method;
            this.event = event;
        }
    }
}
