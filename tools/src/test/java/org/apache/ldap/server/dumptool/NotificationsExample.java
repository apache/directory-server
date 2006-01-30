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
        
        if ( notification.getID().equals( NoticeOfDisconnect.OID ) )
        {
            System.out.println( "Recieved NoticeOfDisconnect: " + NoticeOfDisconnect.OID );
            System.out.println( "Expect to loose this connection without further information." );
        }
        else if ( notification.getID().equals( GracefulDisconnect.OID ) )
        {
            System.out.println( "Recieved GracefulDisconnect: " + GracefulDisconnect.OID );
            GracefulDisconnect gd = new GracefulDisconnect( notification.getEncodedValue() );
            System.out.println( "LDAP server will shutdown in " + gd.getDelay() + " seconds." );
            System.out.println( "LDAP server will be back online in " + gd.getTimeOffline() + " minutes." );
            
            if ( gd.getDelay() > 0 )
            {
                System.out.println( "Starting countdown until server shutdown:" );
                System.out.print( "[" ); 
                long delayMillis = gd.getDelay() * 1000;
                long startTime = System.currentTimeMillis();
                while ( System.currentTimeMillis() - startTime < delayMillis )
                {
                    try{ Thread.sleep( 990 ); }catch ( InterruptedException e ){}
                    System.out.print( "." );
                }
                System.out.println( "]" );
            }
        }
        else 
        {
            System.out.println( "Unknown event recieved with OID: " + evt.getNotification().getID() );
        }
    }


    public void namingExceptionThrown( NamingExceptionEvent evt )
    {
        System.out.println( "Got an excption event: " + evt.getException().getMessage() );
    }
}
