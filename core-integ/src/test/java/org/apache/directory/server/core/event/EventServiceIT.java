/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.event;


import static org.apache.directory.server.core.integ.IntegrationUtils.getConnectionAs;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.ObjectChangeListener;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test cases for the event service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "EventServiceIT")
public class EventServiceIT extends AbstractLdapTestUnit
{
    /**
     * Test to make sure NamingListener's are no longer registered
     * after they are removed via the EventContex.removeNamingListener method.
     *
     * @throws NamingException on failures
     */
    @Test
    public void testRemoveNamingListener() throws Exception
    {
        String userDn = "uid=admin,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "secret" );
        TestListener listener = new TestListener();
        EventDirContext ctx = ( EventDirContext ) getSystemContext( getService() ).lookup( "" );
        ctx.addNamingListener( "", SearchControls.SUBTREE_SCOPE, listener );

        Entry testEntry = new DefaultEntry( "ou=testentry,ou=system",
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou", "testentry" );

        connection.add(testEntry);

        //  Wait 1 second, as the process is asynchronous
        Thread.sleep( 1000 );

        assertEquals( 1, listener.getEventRecords().size() );
        EventRecord rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
        assertEquals( ctx, rec.event.getSource() );

        ctx.removeNamingListener( listener );
        connection.delete(testEntry.getDn());

        //  Wait 1 second, as the process is asynchronous
        Thread.sleep( 1000 );

        assertEquals( 1, listener.getEventRecords().size() );
        rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
        assertEquals( ctx, rec.event.getSource() );

        // read the entry once again just to make sure
        connection.add(testEntry);

        //  Wait 1 second, as the process is asynchronous
        Thread.sleep( 1000 );

        assertEquals( 1, listener.getEventRecords().size() );
        rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
        assertEquals( ctx, rec.event.getSource() );
    }


    /**
     * Test to make sure NamingListener's are no longer registered
     * after the context used for registration is closed.
     *
     * @throws NamingException on failures
     */
    @Test
    public void testContextClose() throws Exception
    {
        String userDn = "uid=admin,ou=system";
        TestListener listener = new TestListener();
        LdapConnection connection = getConnectionAs( getService(), userDn, "secret" );
        EventDirContext ctx = ( EventDirContext ) getSystemContext( getService() ).lookup( "" );
        ctx.addNamingListener( "", SearchControls.SUBTREE_SCOPE, listener );

        Entry testEntry = new DefaultEntry( "ou=testEntry,ou=system",
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou", "testentry" );

        connection.add( testEntry );

        //  Wait 1 second, as the process is asynchronous
        Thread.sleep( 1000 );

        assertEquals( 1, listener.getEventRecords().size() );
        EventRecord rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
        assertEquals( ctx, rec.event.getSource() );

        ctx.close();
        connection.delete( "ou=testentry,ou=system" );

        //  Wait 1 second, as the process is asynchronous
        Thread.sleep( 1000 );

        assertEquals( 1, listener.getEventRecords().size() );
        rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );

        // readd the entry once again just to make sure
        connection.add( testEntry );

        //  Wait 1 second, as the process is asynchronous
        Thread.sleep( 1000 );

        assertEquals( 1, listener.getEventRecords().size() );
        rec = ( EventRecord ) listener.getEventRecords().get( 0 );
        assertEquals( "objectAdded", rec.method );
    }
    

    public class TestListener implements ObjectChangeListener, NamespaceChangeListener
    {
        List<EventRecord> events = new ArrayList<EventRecord>();


        public List<EventRecord> getEventRecords()
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


        EventRecord( String method, EventObject event )
        {
            this.method = method;
            this.event = event;
        }
    }
}
