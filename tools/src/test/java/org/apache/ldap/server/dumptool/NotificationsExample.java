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
package org.apache.ldap.server.dumptool;


import java.util.Hashtable;

import javax.naming.directory.SearchControls;
import javax.naming.event.EventContext;
import javax.naming.event.NamingExceptionEvent;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.UnsolicitedNotification;
import javax.naming.ldap.UnsolicitedNotificationListener;
import javax.naming.ldap.UnsolicitedNotificationEvent;

import org.apache.ldap.common.message.extended.GracefulDisconnect;
import org.apache.ldap.common.message.extended.NoticeOfDisconnect;


/**
 * A simple program which demonstrates how to get unsolicited notifications
 * especially GracefulDisconnect events.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NotificationsExample implements UnsolicitedNotificationListener
{
    UnsolicitedNotification notification;
    boolean canceled = false;

    
    public static void main( String[] args ) throws Exception
    {
        Hashtable env = new Hashtable();
        env.put("java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("java.naming.provider.url", "ldap://"+args[0]+":10389/ou=system" ); 
        env.put("java.naming.security.principal", "uid=admin,ou=system" ); 
        env.put("java.naming.security.credentials", "secret" );
        env.put("java.naming.security.authentication", "simple");
        
        LdapContext ctx = new InitialLdapContext( env, null );
        ctx = ctx.newInstance( null );
        UnsolicitedNotificationListener listener = new NotificationsExample();
        ( ( EventContext ) ctx ).addNamingListener( "", SearchControls.SUBTREE_SCOPE, listener );
        
        System.out.println( "Listening for notifications." );
        System.out.println( "Press any key to terminate." );
        System.in.read();
        ctx.close();
        System.out.println( "Process terminated!!!" );
    }


    public void notificationReceived( UnsolicitedNotificationEvent evt )
    {
        notification = evt.getNotification();
        
        if ( notification.getID().equals( NoticeOfDisconnect.EXTENSION_OID ) )
        {
            System.out.println( "\nRecieved NoticeOfDisconnect: " + NoticeOfDisconnect.EXTENSION_OID );
            System.out.println( "Expect to loose this connection without further information." );
            canceled = true;
        }
        else if ( notification.getID().equals( GracefulDisconnect.EXTENSION_OID ) )
        {
            System.out.println( "Recieved GracefulDisconnect: " + GracefulDisconnect.EXTENSION_OID );
            GracefulDisconnect gd = new GracefulDisconnect( notification.getEncodedValue() );
            System.out.println( "LDAP server will shutdown in " + gd.getDelay() + " seconds." );
            System.out.println( "LDAP server will be back online in " + gd.getTimeOffline() + " minutes." );
            
            if ( gd.getDelay() > 0 )
            {
                Thread t = new Thread( new Counter( gd.getDelay() ) );
                t.start();
            }
        }
        else 
        {
            System.out.println( "Unknown event recieved with OID: " + evt.getNotification().getID() );
        }
    }


    public void namingExceptionThrown( NamingExceptionEvent evt )
    {
        canceled = true;
        System.out.println( "Got an excption event: " + evt.getException().getMessage() );
        System.out.println( "Process shutting down abruptly." );
        System.exit( 1 );
    }
    
    
    class Counter implements Runnable
    {
        int delay;
        
        Counter( int delay )
        {
            this.delay = delay;
        }
        
        public void run()
        {
            System.out.println( "Starting countdown until server shutdown:" );
            System.out.print( "[" ); 
            long delayMillis = delay * 1000 - 1000; // 1000 is for setup costs
            long startTime = System.currentTimeMillis();
            while ( System.currentTimeMillis() - startTime < delayMillis && !canceled )
            {
                try{ Thread.sleep( 1000 ); }catch ( InterruptedException e ){}
                System.out.print( "." );
            }
            
            if ( canceled )
            {
                System.out.println( " -- countdown canceled -- " );
            }
            else
            {
                System.out.println( "]" );
                System.out.println( "Client shutting down gracefully." );
                System.exit( 0 );
            }
        }
    }
}
