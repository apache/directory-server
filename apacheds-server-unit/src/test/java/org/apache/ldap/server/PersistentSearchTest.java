/*
 * Copyright (c) 2004 Solarsis Group LLC.
 *
 * Licensed under the Open Software License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://opensource.org/licenses/osl-2.1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ldap.server;


import java.util.ArrayList;
import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext;
import javax.naming.event.EventDirContext;
import javax.naming.event.NamespaceChangeListener;
import javax.naming.event.NamingEvent;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.event.ObjectChangeListener;
import javax.naming.ldap.Control;
import javax.naming.ldap.HasControls;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.codec.search.controls.ChangeType;
import org.apache.ldap.common.codec.search.controls.EntryChangeControl;
import org.apache.ldap.common.codec.search.controls.EntryChangeControlDecoder;
import org.apache.ldap.common.message.PersistentSearchControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Testcase which tests the correct operation of the persistent search control.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PersistentSearchTest extends AbstractServerTest
{
    public static final Logger log = LoggerFactory.getLogger( PersistentSearchTest.class );
    public static final String PERSON_DESCRIPTION = "an American singer-songwriter";
    public static final String RDN = "cn=Tori Amos";
    private LdapContext ctx = null;



    /**
     * Creation of required attributes of a person entry.
     */
    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attributes = new BasicAttributes();
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );

        return attributes;
    }


    /**
     * Create context and a person entry.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );

        ctx = new InitialLdapContext( env, null );
        assertNotNull( ctx );

        // Create a person with description
        Attributes attributes = this.getPersonAttributes( "Amos", "Tori Amos" );
        attributes.put( "description", PERSON_DESCRIPTION );
        ctx.createSubcontext( RDN, attributes );
    }


    /**
     * Remove person entry and close context.
     */
    public void tearDown() throws Exception
    {
        ctx.unbind( RDN );
        ctx.close();

        ctx.close();
        ctx = null;

        super.tearDown();
    }


    /**
     * Shows correct notifications for modify(4) changes.
     */
    public void testPsearchModify() throws Exception
    {
        PSearchListener listener = new PSearchListener();
        Thread t = new Thread( listener, "PSearchListener" );
        t.start();
        
        while( ! listener.isReady )
        {
            Thread.sleep( 100 );
        }
        Thread.sleep( 250 );

        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, 
            new BasicAttributes( "description", PERSON_DESCRIPTION, true ) );
        long start = System.currentTimeMillis();
        while ( t.isAlive() )
        {
            Thread.sleep( 200 );
            if ( System.currentTimeMillis() - start > 3000 )
            {
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( RDN.toLowerCase(), listener.result.getName().toLowerCase() );
    }


    /**
     * Shows correct notifications for moddn(8) changes.
     */
    public void testPsearchModifyDn() throws Exception
    {
        PSearchListener listener = new PSearchListener();
        Thread t = new Thread( listener );
        t.start();
        
        while( ! listener.isReady )
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( "cn=Jack Black".toLowerCase(), listener.result.getName().toLowerCase() );
    }

    
    /**
     * Shows correct notifications for delete(2) changes.
     */
    public void testPsearchDelete() throws Exception
    {
        PSearchListener listener = new PSearchListener();
        Thread t = new Thread( listener );
        t.start();
        
        while( ! listener.isReady )
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( RDN.toLowerCase(), listener.result.getName().toLowerCase() );
    }

    
    /**
     * Shows correct notifications for add(1) changes.
     */
    public void testPsearchAdd() throws Exception
    {
        PSearchListener listener = new PSearchListener();
        Thread t = new Thread( listener );
        t.start();
        
        while( ! listener.isReady )
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( "cn=Jack Black".toLowerCase(), listener.result.getName().toLowerCase() );
    }


    /**
     * Shows correct notifications for modify(4) changes with returned 
     * EntryChangeControl.
     */
    public void testPsearchModifyWithEC() throws Exception
    {
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
        Thread t = new Thread( listener, "PSearchListener" );
        t.start();
        
        while( ! listener.isReady )
        {
            Thread.sleep( 100 );
        }
        Thread.sleep( 250 );

        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, 
            new BasicAttributes( "description", PERSON_DESCRIPTION, true ) );
        long start = System.currentTimeMillis();
        while ( t.isAlive() )
        {
            Thread.sleep( 200 );
            if ( System.currentTimeMillis() - start > 3000 )
            {
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( RDN.toLowerCase(), listener.result.getName().toLowerCase() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.MODIFY );
    }


    /**
     * Shows correct notifications for moddn(8) changes with returned 
     * EntryChangeControl.
     */
    public void testPsearchModifyDnWithEC() throws Exception
    {
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
        Thread t = new Thread( listener );
        t.start();
        
        while( ! listener.isReady )
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( "cn=Jack Black".toLowerCase(), listener.result.getName().toLowerCase() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.MODDN );
        assertEquals( ( RDN + ",ou=system" ).toLowerCase(), listener.result.control.getPreviousDn().toLowerCase() );
    }

    
    /**
     * Shows correct notifications for delete(2) changes with returned 
     * EntryChangeControl.
     */
    public void testPsearchDeleteWithEC() throws Exception
    {
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
        Thread t = new Thread( listener );
        t.start();
        
        while( ! listener.isReady )
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( RDN.toLowerCase(), listener.result.getName().toLowerCase() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.DELETE );
    }

    
    /**
     * Shows correct notifications for add(1) changes with returned 
     * EntryChangeControl.
     */
    public void testPsearchAddWithEC() throws Exception
    {
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
        Thread t = new Thread( listener );
        t.start();
        
        while( ! listener.isReady )
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( "cn=Jack Black".toLowerCase(), listener.result.getName().toLowerCase() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.ADD );
    }

    
    /**
     * Shows correct notifications for only add(1) and modify(4) registered changes with returned 
     * EntryChangeControl.
     */
    public void testPsearchAddModifyEnabledWithEC() throws Exception
    {
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        control.setChangeTypes( ChangeType.ADD_VALUE );
        control.enableNotification( ChangeType.MODIFY );
        PSearchListener listener = new PSearchListener( control );
        Thread t = new Thread( listener );
        t.start();
        
        while( ! listener.isReady )
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( "cn=Jack Black".toLowerCase(), listener.result.getName().toLowerCase() );
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }

        assertNull( listener.result );

        // thread is still waiting for notifications try a modify
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, 
            new BasicAttributes( "description", PERSON_DESCRIPTION, true ) );
        start = System.currentTimeMillis();
        while ( t.isAlive() )
        {
            Thread.sleep( 200 );
            if ( System.currentTimeMillis() - start > 3000 )
            {
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( RDN.toLowerCase(), listener.result.getName().toLowerCase() );
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
//                System.out.println( "PSearchListener thread not dead yet" );
//                break;
//            }
//        }
//        
//        assertEquals( 6, listener.count );
//        assertNotNull( listener.result );
//        // darn it getting normalized name back
//        assertEquals( "cn=Jack Black".toLowerCase(), listener.result.getName().toLowerCase() );
//        assertEquals( listener.result.control.getChangeType(), ChangeType.ADD );
//    }


    /**
     * Shows notifications functioning with the JNDI notification API of the SUN
     * provider.
     */
    public void testPsearchUsingJndiNotifications() throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );

        JndiNotificationListener listener = new JndiNotificationListener();
        InitialDirContext idc = new InitialDirContext( env );
        EventDirContext edc = ( EventDirContext ) ( idc.lookup( "" ) );
        edc.addNamingListener( "", EventContext.ONELEVEL_SCOPE, listener );
        
        while ( listener.list.isEmpty() )
        {
            Thread.sleep( 250 );
            String rdn = "cn=Jack Black";
            ctx.createSubcontext( rdn, getPersonAttributes( "Black", "Jack Black" ) );
            ctx.destroySubcontext( rdn );
        }
        
        NamingEvent event = ( NamingEvent ) listener.list.get( 0 );
        assertEquals( edc, event.getSource() );
    }

    
    /**
     * Shows notifications functioning with the JNDI notification API of the SUN
     * provider.
     */
    public void testPsearchAbandon() throws Exception
    {
        PersistentSearchControl control = new PersistentSearchControl();
        control.setReturnECs( true );
        PSearchListener listener = new PSearchListener( control );
        Thread t = new Thread( listener );
        t.start();
        
        while( ! listener.isReady )
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( "cn=Jack Black".toLowerCase(), listener.result.getName().toLowerCase() );
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
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }

        assertNull( listener.result );

        // thread is still waiting for notifications try a modify
        ctx.modifyAttributes( RDN, DirContext.REMOVE_ATTRIBUTE, 
            new BasicAttributes( "description", PERSON_DESCRIPTION, true ) );
        start = System.currentTimeMillis();
        while ( t.isAlive() )
        {
            Thread.sleep( 200 );
            if ( System.currentTimeMillis() - start > 3000 )
            {
                System.out.println( "PSearchListener thread not dead yet" );
                break;
            }
        }
        
        assertNotNull( listener.result );
        // darn it getting normalized name back
        assertEquals( RDN.toLowerCase(), listener.result.getName().toLowerCase() );
        assertEquals( listener.result.control.getChangeType(), ChangeType.MODIFY );
    }

    
    class JndiNotificationListener implements NamespaceChangeListener, ObjectChangeListener
    {
        ArrayList list = new ArrayList();
        
        public void objectAdded(NamingEvent evt)
        {
            System.out.println( "added: " + evt.getNewBinding() );
            list.add( 0, evt );
        }

        public void objectRemoved(NamingEvent evt)
        {
            System.out.println( "removed: " + evt.getOldBinding() );
            list.add( 0, evt );
        }

        public void objectRenamed(NamingEvent evt)
        {
            System.out.println( "renamed: " + evt.getNewBinding() + " from " + evt.getOldBinding() );
            list.add( 0, evt );
        }

        public void namingExceptionThrown(NamingExceptionEvent evt)
        {
            System.out.println( "listener got an exceptioin" );
            evt.getException().printStackTrace();
            list.add( 0, evt );
        }

        public void objectChanged(NamingEvent evt)
        {
            System.out.println( "changed: " + evt.getNewBinding() + " from " + evt.getOldBinding() );
            list.add( 0, evt );
        }
    }
    
    
    class PSearchListener implements Runnable
    {
        boolean isReady = false;
        PSearchNotification result;
        int count = 0;
        final PersistentSearchControl control;

        PSearchListener() { control = new PersistentSearchControl(); }
        PSearchListener( PersistentSearchControl control ) { this.control = control; }
        
        public void run()
        {
            NamingEnumeration list = null;
            control.setCritical( true );
            Control[] ctxCtls = new Control[] { control };
            
            try
            {
                ctx.setRequestControls( ctxCtls );
                isReady = true;
                list = ctx.search( "", "objectClass=*", null );
                EntryChangeControl ecControl = null;
                
                while( list.hasMore() )
                {
                    Control[] controls = null;
                    SearchResult sresult = ( SearchResult ) list.next();
                    count++;
                    if ( sresult instanceof HasControls )
                    {
                        controls = ( ( HasControls ) sresult ).getControls();
                        if ( controls != null )
                        {
                            for ( int ii = 0; ii < controls.length; ii ++ )
                            {
                                if ( controls[ii].getID().equals( 
                                    org.apache.ldap.common.message.EntryChangeControl.CONTROL_OID ) )
                                {
                                    EntryChangeControlDecoder decoder = new EntryChangeControlDecoder();
                                    ecControl = ( EntryChangeControl ) decoder.decode( controls[ii].getEncodedValue() );
                                }
                            }
                        }
                    }
                    result = new PSearchNotification( sresult, ecControl );
                    System.out.println( "got notifiaction: " + result );
                    break;
                }
            }
            catch( Exception e ) 
            {
                e.printStackTrace();
            }
            finally
            {
                if ( list != null )
                {
                    try { list.close(); } catch ( Exception e ) { e.printStackTrace(); };
                }
            }
        }
    }


    class PSearchNotification extends SearchResult
    {
        private static final long serialVersionUID = 1L;
        final EntryChangeControl control;
        
        public PSearchNotification( SearchResult result, EntryChangeControl control )
        {
            super( result.getName(), result.getClassName(), result.getObject(), result.getAttributes(), result.isRelative() );
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
