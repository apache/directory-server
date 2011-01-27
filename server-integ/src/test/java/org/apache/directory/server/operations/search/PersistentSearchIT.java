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
package org.apache.directory.server.operations.search;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.ObjectChangeListener;
import javax.naming.ldap.HasControls;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.event.EventService;
import org.apache.directory.server.core.event.RegistrationEntry;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.model.message.controls.EntryChange;
import org.apache.directory.shared.ldap.codec.search.controls.entryChange.EntryChangeDecorator;
import org.apache.directory.shared.ldap.codec.search.controls.entryChange.EntryChangeDecoder;
import org.apache.directory.shared.ldap.codec.search.controls.persistentSearch.PersistentSearch;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.apache.directory.shared.ldap.model.message.Control;
import org.apache.directory.shared.ldap.util.JndiUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test case which tests the correct operation of the persistent search decorator.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class ) 
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" )
    })
@ApplyLdifs( {
    // Entry # 2
    "dn: cn=Tori Amos,ou=system",
    "objectClass: person",
    "objectClass: top",
    "cn: Tori Amos",
    "description: an American singer-songwriter",
    "sn: Amos"
    }
)
public class PersistentSearchIT extends AbstractLdapTestUnit
{
    private static final Logger LOG = LoggerFactory.getLogger( PersistentSearchIT.class );
    
    private static final String BASE = "ou=system";
    private static final String PERSON_DESCRIPTION = "an American singer-songwriter";
    private static final String RDN = "cn=Tori Amos";


    /**
     * Creation of required attributes of a person entry.
     */
    private Attributes getPersonAttributes( String sn, String cn ) throws LdapException
    {
        Attributes attributes = LdifUtils.createAttributes( 
            "objectClass: top",
            "objectClass: person",
            "cn", cn,
            "sn", sn );

        return attributes;
    }

    
    EventDirContext ctx;
    EventService eventService; 
    PSearchListener listener;
    Thread t;
    

    private void setUpListenerReturnECs() throws Exception
    {
        setUpListener( true, new PersistentSearch(), false );
    }
    
    
    private void setUpListener( boolean returnECs, PersistentSearch persistentSearch, boolean ignoreEmptyRegistryCheck )
        throws Exception
    {
        ctx = ( EventDirContext ) getWiredContext( ldapServer).lookup( BASE );
        eventService = ldapServer.getDirectoryService().getEventService();
        List<RegistrationEntry> registrationEntryList = eventService.getRegistrationEntries();
        
        if ( ! ignoreEmptyRegistryCheck )
        {
            assertTrue( registrationEntryList.isEmpty() );
        }
        
        persistentSearch.setReturnECs( returnECs );
        listener = new PSearchListener( persistentSearch );
        t = new Thread( listener, "PSearchListener" );
        t.start();

        // let's wait until the listener thread started
        while ( eventService.getRegistrationEntries().isEmpty() )
        {
            Thread.sleep( 100 );
        }
        
        // Now we wait until the listener is registered (timing dependent crap)
        Thread.sleep( 250 );
    }
    
    
    private void setUpListener() throws Exception
    {
        ctx = ( EventDirContext ) getWiredContext( ldapServer).lookup( BASE );
        eventService = ldapServer.getDirectoryService().getEventService();
        List<RegistrationEntry> registrationEntryList = eventService.getRegistrationEntries();
        assertTrue( registrationEntryList.isEmpty() );
        
        listener = new PSearchListener();
        t = new Thread( listener, "PSearchListener" );
        t.start();

        // let's wait until the listener thread started
        while ( eventService.getRegistrationEntries().isEmpty() )
        {
            Thread.sleep( 100 );
        }
        
        // Now we wait until the listener is registered (timing dependent crap)
        Thread.sleep( 250 );
    }
    
    
    @After
    public void tearDownListener() throws Exception
    {
        listener.close();
        ctx.close();

        while ( ! eventService.getRegistrationEntries().isEmpty() )
        {
            Thread.sleep( 100 );
        }
    }

    
    private void waitForThreadToDie( Thread t ) throws Exception
    {
        long start = System.currentTimeMillis();
        
        while ( t.isAlive() )
        {
            Thread.sleep( 200 );
            if ( System.currentTimeMillis() - start > 1000 )
            {
                break;
            }
        }
    }

    
    /**
     * Shows correct notifications for modify(4) changes.
     */
    @Test
    public void testPsearchModify() throws Exception
    {
        setUpListener();
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, 
            new BasicAttributes( "description", PERSON_DESCRIPTION, true ) );
        waitForThreadToDie( t );
        assertNotNull( listener.result );
        assertEquals( RDN, listener.result.getName() );
    }


    /**
     * Shows correct notifications for moddn(8) changes.
     */
    @Test
    public void testPsearchModifyDn() throws Exception
    {
        setUpListener();
        ctx.rename( RDN, "cn=Jack Black" );
        waitForThreadToDie( t );
        assertNotNull( listener.result );
        assertEquals( "cn=Jack Black", listener.result.getName() );
    }


    /**
     * Shows correct notifications for delete(2) changes.
     */
    @Test
    public void testPsearchDelete() throws Exception
    {
        setUpListener();
        ctx.destroySubcontext( RDN );
        waitForThreadToDie( t );
        assertNotNull( listener.result );
        assertEquals( RDN, listener.result.getName() );
    }


    /**
     * Shows correct notifications for add(1) changes.
     */
    @Test
    public void testPsearchAdd() throws Exception
    {
        setUpListener();
        ctx.createSubcontext( "cn=Jack Black", getPersonAttributes( "Black", "Jack Black" ) );
        waitForThreadToDie( t );
        assertNotNull( listener.result );
        assertEquals( "cn=Jack Black", listener.result.getName() );
    }


    /**
     * Shows correct notifications for modify(4) changes with returned 
     * EntryChangeControl.
     */
    @Test
    public void testPsearchModifyWithEC() throws Exception
    {
        setUpListenerReturnECs();
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, new BasicAttributes( "description", PERSON_DESCRIPTION,
            true ) );
        waitForThreadToDie( t );
        assertNotNull( listener.result );
        assertEquals( RDN, listener.result.getName() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.MODIFY );
    }


    /**
     * Shows correct notifications for moddn(8) changes with returned 
     * EntryChangeControl.
     */
    @Test
    public void testPsearchModifyDnWithEC() throws Exception
    {
        setUpListenerReturnECs();
        ctx.rename( RDN, "cn=Jack Black" );
        waitForThreadToDie( t );
        assertNotNull( listener.result );
        assertEquals( "cn=Jack Black", listener.result.getName() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.MODDN );
        assertEquals( ( RDN + ",ou=system" ), listener.result.control.getPreviousDn().getName() );
    }


    /**
     * Shows correct notifications for delete(2) changes with returned 
     * EntryChangeControl.
     */
    @Test
    public void testPsearchDeleteWithEC() throws Exception
    {
        setUpListenerReturnECs();
        ctx.destroySubcontext( RDN );
        waitForThreadToDie( t );
        assertNotNull( listener.result );
        assertEquals( RDN, listener.result.getName() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.DELETE );
    }


    /**
     * Shows correct notifications for add(1) changes with returned 
     * EntryChangeControl.
     */
    @Test
    public void testPsearchAddWithEC() throws Exception
    {
        setUpListenerReturnECs();
        ctx.createSubcontext( "cn=Jack Black", getPersonAttributes( "Black", "Jack Black" ) );
        waitForThreadToDie( t );
        assertNotNull( listener.result );
        assertEquals( "cn=Jack Black", listener.result.getName() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.ADD );
    }


    /**
     * Shows correct notifications for only add(1) and modify(4) registered changes with returned 
     * EntryChangeControl but not deletes.
     */
    @Test
    public void testPsearchAddModifyEnabledWithEC() throws Exception
    {
        PersistentSearch ctrl = new PersistentSearch();
        ctrl.setReturnECs( true );
        ctrl.setChangeTypes( ChangeType.ADD.getValue() );
        ctrl.enableNotification( ChangeType.MODIFY );
        setUpListener( true, ctrl, false );
        ctx.createSubcontext( "cn=Jack Black", getPersonAttributes( "Black", "Jack Black" ) );
        waitForThreadToDie( t );

        assertNotNull( listener.result );
        assertEquals( "cn=Jack Black", listener.result.getName() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.ADD );
        tearDownListener();

        setUpListener( true, ctrl, true );
        ctx.destroySubcontext( "cn=Jack Black" );
        waitForThreadToDie( t );
        assertNull( listener.result );

        // thread is still waiting for notifications try a modify
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, new BasicAttributes( "description", PERSON_DESCRIPTION,
            true ) );
        waitForThreadToDie( t );
        
        assertNotNull( listener.result );
        assertEquals( RDN, listener.result.getName() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.MODIFY );
    }


    /**
     * Shows correct notifications for add(1) changes with returned 
     * EntryChangeControl and changesOnly set to false so we return
     * the first set of entries.
     * 
     * This test is commented out because it exhibits some producer
     * consumer lockups (server and client being in same process)
     * 
     * PLUS ALL THIS GARBAGE IS TIME DEPENDENT!!!!!
     */
    //    public void testPsearchAddWithECAndFalseChangesOnly() throws Exception
    //    {
    //        PersistentSearchDecorator decorator = new PersistentSearchDecorator();
    //        decorator.setReturnECs( true );
    //        decorator.setChangesOnly( false );
    //        PSearchListener listener = new PSearchListener( decorator );
    //        Thread t = new Thread( listener );
    //        t.start();
    //        
    //        Thread.sleep( 3000 );
    //
    //        assertEquals( 5, listener.count );
    //        ctx.createSubcontext( "cn=Jack Black", getPersonAttributes( "Black", "Jack Black" ) );
    //        
    //        long start = System.currentTimeMillis();
    //        while ( t.isAlive() )
    //        {
    //            Thread.sleep( 100 );
    //            if ( System.currentTimeMillis() - start > 3000 )
    //            {
    //                break;
    //            }
    //        }
    //        
    //        assertEquals( 6, listener.count );
    //        assertNotNull( listener.result );
    //        assertEquals( "cn=Jack Black", listener.result.getName() );
    //        assertEquals( listener.result.decorator.getChangeType(), ChangeType.ADD );
    //    }

    /**
     * Shows notifications functioning with the JNDI notification API of the SUN
     * provider.
     *
    @Test
    public void testPsearchAbandon() throws Exception
    {
        PersistentSearchDecorator decorator = new PersistentSearchDecorator();
        decorator.setReturnECs( true );
        PSearchListener listener = new PSearchListener( decorator );
        Thread t = new Thread( listener );
        t.start();

        while ( !listener.isReady )
        {
            Thread.sleep( 100 );
        }
        Thread.sleep( 250 );

        ctx.createSubcontext( "cn=Jack Black", getPersonAttributes( "Black", "Jack Black" ) );

        long start = System.currentTimeMillis();
        while ( t.isAlive() )
        {
            Thread.sleep( 100 );
            if ( System.currentTimeMillis() - start > 3000 )
            {
                break;
            }
        }

        assertNotNull( listener.result );
        assertEquals( "cn=Jack Black", listener.result.getName() );
        assertEquals( listener.result.decorator.getChangeType(), ChangeType.ADD );
        
        listener = new PSearchListener( decorator );

        t = new Thread( listener );
        t.start();

        ctx.destroySubcontext( "cn=Jack Black" );

        start = System.currentTimeMillis();
        while ( t.isAlive() )
        {
            Thread.sleep( 100 );
            if ( System.currentTimeMillis() - start > 3000 )
            {
                break;
            }
        }

        // there seems to be a race condition here
        // assertNull( listener.result );
        assertNotNull( listener.result );
        assertEquals( "cn=Jack Black", listener.result.getName() );
        assertEquals( ChangeType.DELETE, listener.result.decorator.getChangeType() );
        listener.result = null;

        // thread is still waiting for notifications try a modify
        ctx.modifyAttributes( Rdn, DirContext.REMOVE_ATTRIBUTE, new AttributesImpl( "description", PERSON_DESCRIPTION,
            true ) );
        start = System.currentTimeMillis();
        while ( t.isAlive() )
        {
            Thread.sleep( 200 );
            if ( System.currentTimeMillis() - start > 3000 )
            {
                break;
            }
        }

        assertNull( listener.result );
        //assertEquals( Rdn, listener.result.getName() );
        //assertEquals( listener.result.decorator.getChangeType(), ChangeType.MODIFY );
    }*/

    
    class JndiNotificationListener implements NamespaceChangeListener, ObjectChangeListener
    {
        boolean hasError = false;
        ArrayList<EventObject> list = new ArrayList<EventObject>();
        NamingExceptionEvent exceptionEvent = null;

        public void objectAdded( NamingEvent evt )
        {
            list.add( 0, evt );
        }


        public void objectRemoved( NamingEvent evt )
        {
            list.add( 0, evt );
        }


        public void objectRenamed( NamingEvent evt )
        {
            list.add( 0, evt );
        }


        public void namingExceptionThrown( NamingExceptionEvent evt )
        {
            hasError = true;
            exceptionEvent = evt;
            list.add( 0, evt );
        }


        public void objectChanged( NamingEvent evt )
        {
            list.add( 0, evt );
        }
    }

    
    class PSearchListener implements Runnable
    {
        boolean isReady = false;
        PSearchNotification result;
        final PersistentSearch persistentSearch;
        LdapContext ctx;
        NamingEnumeration<SearchResult> list;
        
        PSearchListener()
        {
            persistentSearch = new PersistentSearch();
        }


        PSearchListener( PersistentSearch persistentSearch )
        {
            this.persistentSearch = persistentSearch;
        }

        
        void close()
        {
            if ( list != null )
            {
                try
                {
                    list.close();
                    LOG.debug( "PSearchListener: search naming enumeration closed()" );
                }
                catch ( Exception e )
                {
                    LOG.error( "Error closing NamingEnumeration on PSearchListener", e );
                }
            }
            
            if ( ctx != null )
            {
                try
                {
                    ctx.close();
                    LOG.debug( "PSearchListener: search context closed()" );
                }
                catch ( Exception e )
                {
                    LOG.error( "Error closing connection on PSearchListener", e );
                }
            }
        }

        
        public void run()
        {
            LOG.debug( "PSearchListener.run() called." );
            persistentSearch.setCritical( true );
            
            persistentSearch.setValue( persistentSearch.getValue() );

            Control[] ctxCtls = new Control[] { persistentSearch };

            try
            {
                ctx = ( LdapContext ) getWiredContext( ldapServer).lookup( BASE );
                ctx.setRequestControls( JndiUtils.toJndiControls(ctxCtls) );
                isReady = true;
                LOG.debug( "PSearchListener is ready and about to issue persistent search request." );
                list = ctx.search( "", "objectClass=*", null );
                LOG.debug( "PSearchListener search request returned." );
                EntryChangeDecorator ecControl = null;

                while ( list.hasMore() )
                {
                    LOG.debug( "PSearchListener search request got an item." );
                    javax.naming.ldap.Control[] controls;
                    SearchResult sresult = list.next();
                    
                    if ( sresult instanceof HasControls )
                    {
                        controls = ( ( HasControls ) sresult ).getControls();
                        
                        if ( controls != null )
                        {
                            for ( javax.naming.ldap.Control control : controls )
                            {
                                if ( control.getID().equals(
                                    EntryChange.OID ) )
                                {
                                    EntryChangeDecoder decoder = new EntryChangeDecoder();
                                    ecControl = ( EntryChangeDecorator ) decoder.decode( control.getEncodedValue(), 
                                            new EntryChangeDecorator() );
                                }
                            }
                        }
                    }
                    
                    result = new PSearchNotification( sresult, (EntryChange) ecControl.getDecorated() );
                    break;
                }
                
                LOG.debug( "PSearchListener broke out of while loop." );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                LOG.error( "PSearchListener encountered error", e );
            }
            finally
            {
            }
        }
    }

    
    class PSearchNotification extends SearchResult
    {
        private static final long serialVersionUID = 1L;
        final EntryChange control;


        public PSearchNotification( SearchResult result, EntryChange control )
        {
            super( result.getName(), result.getClassName(), result.getObject(), result.getAttributes(), result
                .isRelative() );
            this.control = control;
        }


        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "Dn: " ).append( getName() ).append( "\n" );
            
            if ( control != null )
            {
                buf.append( "    EntryChangeControl =\n" );
                buf.append( "       changeType   : " ).append( control.getChangeType() ).append( "\n" );
                buf.append( "       previousDN   : " ).append( control.getPreviousDn() ).append( "\n" );
                buf.append( "       changeNumber : " ).append( control.getChangeNumber() ).append( "\n" );
            }
            
            return buf.toString();
        }
    }
}
