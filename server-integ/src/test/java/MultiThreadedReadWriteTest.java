/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

/**
 * A class to test DIRSERVER-1992.
 *
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MultiThreadedReadWriteTest
{

    private volatile boolean stop = false;

    private Runnable reader = new Runnable()
    {
        
        @Override
        public void run()
        {
            try
            {
                LdapNetworkConnection connection = getConnection();
                while ( !stop )
                {
                    read( connection );
                }
                
                connection.close();
            }
            catch( Exception e )
            {
                System.out.println( "Reader " + Thread.currentThread().getId() + " failed" );
                e.printStackTrace();
                System.exit( 0 );
            }
        }
    };

    private Runnable writer = new Runnable()
    {
        
        @Override
        public void run()
        {
            try
            {
                sync();
            }
            catch( Exception e )
            {
                System.out.println( "Sync failed" );
                e.printStackTrace();
                System.exit( 0 );
            }
        }
    };

    private void read( LdapNetworkConnection connection ) throws Exception
    {
        EntryCursor cursor = connection.search( "ou=users,ou=system", "(objectClass=*)", SearchScope.ONELEVEL, "*" );
        int count = 0;
        
        while( cursor.next() )
        {
            cursor.get();
            count++;
        }
        
        System.out.println( "Read " + count + " entries" );
    }
    
    private void sync() throws Exception
    {
        LdapNetworkConnection connection = getConnection();
        String dn = "uid={uid},ou=users,ou=system";
        
        String personTemplate = "objectClass: top\n" +
            "objectClass: person\n" +
            "objectClass: organizationalPerson\n" +
            "objectClass: inetOrgPerson\n" +
            "givenName: {uid}_{uid}\n" +
            "sn: {uid}_sn\n" +
            "cn: {uid}_cn\n" +
            "uid: {uid}\n\n";

        int total = 2500;
        
        System.out.println( "writing " );
        for( int i =0; i< total; i++ )
        {
            String uid = "user"+ i;
            String userDn = dn.replace( "{uid}", uid );
            String user = personTemplate.replace( "{uid}", uid );
            
            DefaultEntry entry = new DefaultEntry( userDn, user );
            
            connection.add( entry );
        }

        System.out.println( "deleting " );
        for( int i =0; i< total; i++ )
        {
            String uid = "user"+ i;
            String userDn = dn.replace( "{uid}", uid );
            
            connection.delete( userDn );
        }
        
        connection.close();
    }
    
    
    public static String getStackTrace( Throwable t )
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw, true );
        t.printStackTrace( pw );
        pw.flush();
        sw.flush();
        return sw.toString();
    }


    private LdapNetworkConnection getConnection() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, 10389 );
        connection.bind( "uid=admin,ou=system", "secret" );
        connection.setTimeOut( Long.MAX_VALUE );
        
        return connection;
    }


    /**
     * @param args
     */
    public static void main( String[] args ) throws Exception
    {
        MultiThreadedReadWriteTest mtrwt = new MultiThreadedReadWriteTest();
        
        Thread writer = new Thread( mtrwt.writer );
        writer.start();
        
        for ( int i = 0; i < 5; ++i )
        {
            Thread reader = new Thread( mtrwt.reader );
            reader.start();
        }

        System.out.println( "waiting for writer to stop" );
        writer.join();
        
        mtrwt.stop = true;
    }

}
