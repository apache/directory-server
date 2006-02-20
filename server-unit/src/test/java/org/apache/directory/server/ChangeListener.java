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
package org.apache.directory.server;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.HasControls;
import javax.naming.ldap.InitialLdapContext;

import org.apache.directory.shared.ldap.codec.search.controls.EntryChangeControl;
import org.apache.directory.shared.ldap.codec.search.controls.EntryChangeControlDecoder;
import org.apache.directory.shared.ldap.message.PersistentSearchControl;


/**
 * A simple change listener application that prints out changes returned using
 * the psearch control.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ChangeListener
{
    public static void main( String[] args ) throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:10389/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        InitialLdapContext ctx = new InitialLdapContext( env, null );
        Runtime.getRuntime().addShutdownHook( new Thread( new ShutdownHook( ctx ) ) );
        PersistentSearchControl control = new PersistentSearchControl();
        control.setChangesOnly( false );
        control.setReturnECs( true );
        control.setCritical( true );
        control.setChangeTypes( PersistentSearchControl.ALL_CHANGES );
        Control[] ctxCtls = new Control[]
            { control };

        try
        {
            Control[] respCtls;
            ctx.setRequestControls( ctxCtls );
            EntryChangeControl ecCtl = null;
            NamingEnumeration list = ctx.search( "", "objectClass=*", null );
            while ( list.hasMore() )
            {
                SearchResult result = ( SearchResult ) list.next();
                if ( result instanceof HasControls )
                {
                    respCtls = ( ( HasControls ) result ).getControls();
                    if ( respCtls != null )
                    {
                        for ( int ii = 0; ii < respCtls.length; ii++ )
                        {
                            if ( respCtls[ii].getID().equals(
                                org.apache.directory.shared.ldap.message.EntryChangeControl.CONTROL_OID ) )
                            {
                                EntryChangeControlDecoder decoder = new EntryChangeControlDecoder();
                                ecCtl = ( EntryChangeControl ) decoder.decode( respCtls[ii].getEncodedValue() );
                            }
                        }
                    }
                }

                StringBuffer buf = new StringBuffer();
                buf.append( "DN: " ).append( result.getName() ).append( "\n" );
                if ( ecCtl != null )
                {
                    System.out.println( "================ NOTIFICATION ================" );
                    buf.append( "    EntryChangeControl =\n" );
                    buf.append( "        changeType   : " ).append( ecCtl.getChangeType() ).append( "\n" );
                    buf.append( "        previousDN   : " ).append( ecCtl.getPreviousDn() ).append( "\n" );
                    buf.append( "        changeNumber : " ).append( ecCtl.getChangeNumber() ).append( "\n" );
                }

                System.out.println( buf.toString() );

                if ( ecCtl != null )
                {
                    System.out.println( "==============================================" );
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    static class ShutdownHook implements Runnable
    {
        final Context ctx;


        ShutdownHook(Context ctx)
        {
            this.ctx = ctx;
        }


        public void run()
        {
            if ( ctx != null )
            {
                try
                {
                    ctx.close();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                };
            }
        }
    }
}
