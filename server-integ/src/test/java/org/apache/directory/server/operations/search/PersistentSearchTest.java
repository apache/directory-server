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


import java.util.ArrayList;
import java.util.EventObject;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.ObjectChangeListener;
import javax.naming.ldap.Control;
import javax.naming.ldap.HasControls;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.integ.SiRunner;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.codec.search.controls.EntryChangeControlCodec;
import org.apache.directory.shared.ldap.codec.search.controls.EntryChangeControlDecoder;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.PersistentSearchControl;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * TODO - enable me - not executed (not in suite yet)
 * Test case which tests the correct operation of the persistent search control.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.SUITE )
@ApplyLdifs( {
    // Entry # 2
    "dn: cn=Tori Amos,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "cn: Tori Amos\n" +
    "description: an American singer-songwriter\n" +
    "sn: Amos\n\n"
    }
)
public class PersistentSearchTest
{
    private static final String BASE = "ou=system";
    private static final String PERSON_DESCRIPTION = "an American singer-songwriter";
    private static final String RDN = "cn=Tori Amos";

    public static LdapServer ldapServer;
    

    /**
     * Creation of required attributes of a person entry.
     */
    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attributes = new AttributesImpl();
        Attribute attribute = new AttributeImpl( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );

        return attributes;
    }

    
    /**
     * Shows correct notifications for modify(4) changes.
     */
    @Test
    public void testPsearchModify() throws Exception
    {
        EventDirContext ctx = ( EventDirContext ) getWiredContext( ldapServer).lookup( BASE );
        PSearchListener listener = new PSearchListener();
        Thread t = new Thread( listener, "PSearchListener" );
        t.start();

        // let's wait until the listener thread started
        while ( !listener.isReady )
        {
            Thread.sleep( 100 );
        }
        // Now we wait until the listener is registered (timing dependent crap)
        Thread.sleep( 250 );

        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, 
            new AttributesImpl( "description", PERSON_DESCRIPTION, true ) );
        long start = System.currentTimeMillis();
        while ( t.isAlive() )
        {
            Thread.sleep( 200 );
            if ( System.currentTimeMillis() - start > 3000 )
            {
                break;
            }
        }

        assertNotNull( listener.result );
        assertEquals( RDN, listener.result.getName() );
    }


    /**
     * Shows correct notifications for moddn(8) changes.
     */
    @Test
    public void testPsearchModifyDn() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer).lookup( BASE );
        PSearchListener listener = new PSearchListener();
        Thread t = new Thread( listener );
        t.start();

        while ( !listener.isReady )
        {
            Thread.sleep( 100 );
        }
        Thread.sleep( 250 );

        ctx.rename( RDN, "cn=Jack Black" );

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
    }


    /**
     * Shows correct notifications for delete(2) changes.
     */
    @Test
    public void testPsearchDelete() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer).lookup( BASE );
        PSearchListener listener = new PSearchListener();
        Thread t = new Thread( listener );
        t.start();

        while ( !listener.isReady )
        {
            Thread.sleep( 100 );
        }
        Thread.sleep( 250 );

        ctx.destroySubcontext( RDN );

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
        assertEquals( RDN, listener.result.getName() );
    }


    /**
     * Shows correct notifications for add(1) changes.
     */
    @Test
    public void testPsearchAdd() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer).lookup( BASE );
        PSearchListener listener = new PSearchListener();
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
    }


    /**
     * Shows correct notifications for modify(4) changes with returned 
     * EntryChangeControl.
     */
    @Test
    public void testPsearchModifyWithEC() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer).lookup( BASE );
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
        Thread t = new Thread( listener, "PSearchListener" );
        t.start();

        while ( !listener.isReady )
        {
            Thread.sleep( 100 );
        }
        Thread.sleep( 250 );

        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, new AttributesImpl( "description", PERSON_DESCRIPTION,
            true ) );
        long start = System.currentTimeMillis();
        while ( t.isAlive() )
        {
            Thread.sleep( 200 );
            if ( System.currentTimeMillis() - start > 3000 )
            {
                break;
            }
        }

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
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer).lookup( BASE );
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
        Thread t = new Thread( listener );
        t.start();

        while ( !listener.isReady )
        {
            Thread.sleep( 100 );
        }
        Thread.sleep( 250 );

        ctx.rename( RDN, "cn=Jack Black" );

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
        assertEquals( listener.result.control.getChangeType(), ChangeType.MODDN );
        assertEquals( ( RDN + ",ou=system" ), listener.result.control.getPreviousDn().getUpName() );
    }


    /**
     * Shows correct notifications for delete(2) changes with returned 
     * EntryChangeControl.
     */
    @Test
    public void testPsearchDeleteWithEC() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer).lookup( BASE );
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
        Thread t = new Thread( listener );
        t.start();

        while ( !listener.isReady )
        {
            Thread.sleep( 100 );
        }
        Thread.sleep( 250 );

        ctx.destroySubcontext( RDN );

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
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer).lookup( BASE );
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
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
        assertEquals( listener.result.control.getChangeType(), ChangeType.ADD );
    }


    /**
     * Shows correct notifications for only add(1) and modify(4) registered changes with returned 
     * EntryChangeControl.
     */
    @Test
    public void testPsearchAddModifyEnabledWithEC() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer).lookup( BASE );
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        control.setChangeTypes( ChangeType.ADD_VALUE );
        control.enableNotification( ChangeType.MODIFY );
        PSearchListener listener = new PSearchListener( control );
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
        assertEquals( listener.result.control.getChangeType(), ChangeType.ADD );
        listener.result = null;
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

        assertNull( listener.result );

        // thread is still waiting for notifications try a modify
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, new AttributesImpl( "description", PERSON_DESCRIPTION,
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
    //        PersistentSearchControl control = new PersistentSearchControl();
    //        control.setReturnECs( true );
    //        control.setChangesOnly( false );
    //        PSearchListener listener = new PSearchListener( control );
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
    //        assertEquals( listener.result.control.getChangeType(), ChangeType.ADD );
    //    }

    /**
     * Shows notifications functioning with the JNDI notification API of the SUN
     * provider.
     */
    @Test
    public void testPsearchUsingJndiNotifications() throws Exception
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapServer).lookup( BASE );
        JndiNotificationListener listener = new JndiNotificationListener();
        EventDirContext edc = ( EventDirContext ) getWiredContext( ldapServer).lookup( BASE );
        edc.addNamingListener( "", EventContext.ONELEVEL_SCOPE, listener );

        while ( listener.list.isEmpty() )
        {
            Thread.sleep( 250 );
            String rdn = "cn=Jack Black";
            ctx.createSubcontext( rdn, getPersonAttributes( "Black", "Jack Black" ) );
            ctx.destroySubcontext( rdn );
        }

        if ( ! listener.hasError )
        {
            EventObject event = listener.list.get( 0 );
            assertEquals( edc, event.getSource() );
        }
        else
        {
            throw new RuntimeException( "got naming exception while processing events", 
                listener.exceptionEvent.getException() );
        }
    }


    /**
     * Shows notifications functioning with the JNDI notification API of the SUN
     * provider.
     *
    @Test
    public void testPsearchAbandon() throws Exception
    {
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
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
        assertEquals( listener.result.control.getChangeType(), ChangeType.ADD );
        
        listener = new PSearchListener( control );

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
        assertEquals( ChangeType.DELETE, listener.result.control.getChangeType() );
        listener.result = null;

        // thread is still waiting for notifications try a modify
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, new AttributesImpl( "description", PERSON_DESCRIPTION,
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
        //assertEquals( RDN, listener.result.getName() );
        //assertEquals( listener.result.control.getChangeType(), ChangeType.MODIFY );
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
        int count = 0;
        final PersistentSearchControl control;


        PSearchListener()
        {
            control = new PersistentSearchControl();
        }


        PSearchListener(PersistentSearchControl control)
        {
            this.control = control;
        }


        public void run()
        {
            NamingEnumeration<SearchResult> list = null;
            control.setCritical( true );
            Control[] ctxCtls = new Control[]
                { control };

            try
            {
                LdapContext ctx = ( LdapContext ) getWiredContext( ldapServer).lookup( BASE );
                ctx.setRequestControls( ctxCtls );
                isReady = true;
                list = ctx.search( "", "objectClass=*", null );
                EntryChangeControlCodec ecControl = null;

                while ( list.hasMore() )
                {
                    Control[] controls = null;
                    SearchResult sresult = list.next();
                    count++;
                    if ( sresult instanceof HasControls )
                    {
                        controls = ( ( HasControls ) sresult ).getControls();
                        if ( controls != null )
                        {
                            for ( int ii = 0; ii < controls.length; ii++ )
                            {
                                if ( controls[ii].getID().equals(
                                    org.apache.directory.shared.ldap.message.EntryChangeControl.CONTROL_OID ) )
                                {
                                    EntryChangeControlDecoder decoder = new EntryChangeControlDecoder();
                                    ecControl = ( EntryChangeControlCodec ) decoder.decode( controls[ii].getEncodedValue() );
                                }
                            }
                        }
                    }
                    result = new PSearchNotification( sresult, ecControl );
                    break;
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            finally
            {
                if ( list != null )
                {
                    try
                    {
                        list.close();
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class PSearchNotification extends SearchResult
    {
        private static final long serialVersionUID = 1L;
        final EntryChangeControlCodec control;


        public PSearchNotification(SearchResult result, EntryChangeControlCodec control)
        {
            super( result.getName(), result.getClassName(), result.getObject(), result.getAttributes(), result
                .isRelative() );
            this.control = control;
        }


        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "DN: " ).append( getName() ).append( "\n" );
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
