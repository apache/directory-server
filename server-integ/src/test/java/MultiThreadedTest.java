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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;


//import com.telelogic.tds.common.TDSConstants;
//import com.telelogic.tds.common.TDSProperties;
//import com.telelogic.tds.engine.ldap.jndi.TDSDirObjectConstants;

public class MultiThreadedTest extends Thread
{

    private Logger _logger;

    private FileAppender _appender;


    public MultiThreadedTest( int i )
    {
        super();
        this.setName( "Worker-Thread" + i );
    }


    @Override
    public void run()
    {

        try
        {
            _logger = Logger.getLogger( this.getName() );
            _appender =
                new FileAppender( new SimpleLayout(), "C:\\threadlog\\"
                    + this.getName() + ".txt", false );
            _logger.addAppender( _appender );
            _logger.setLevel( ( Level ) Level.ERROR );

            LdapContext tdsContext = getContext();
            LdapContext cntx;

            // Create the initial context
            Map<String, Object> hMap = new HashMap<String, Object>();
            hMap.put( "data", "dsfdfd" );

            // authenticate user
            for ( int i = 0; i < 100000; i++ )
            {
                if ( i % 100 == 0 )
                {
                    System.out.println( "Thread[" + getName() + "]:" + i );
                }
                try
                {

                    //System.out.println(" Ops started " + getName());

                    cntx = tdsContext.newInstance( null );

                    setUserPreferences( cntx, "adsadminPref_" + getName(), hMap,
                        i );

                    /*System.out.println(" Preferences SET "
                        + getName()
                        + " "
                        + getId()
                        + " SIZE-- "
                        + ((hMap.get("data") != null) ? hMap.get("data")
                            .toString().length() : 0));*/

                    cntx.close();

                    //System.out.println(" Ops conducted successfully "
                    //    + getName());

                }
                catch ( NamingException e )
                {
                    //System.out.println(new Date() + " NAMING EXCETPION"
                    //    + getName() + " " + getId());
                    _logger.log( Level.ERROR, this.getName() + " : loop " + i );
                    break;
                    //e.printStackTrace();
                }
                catch ( Exception ex )
                {
                    //System.out.println(new Date() + " NAMING EXCETPION"
                    //    + getName() + " " + getId());
                    _logger.log( Level.ERROR, this.getName() + " : loop " + i );
                    break;
                    //ex.printStackTrace();
                }

                //Thread.sleep(delay);
            }
        }
        catch ( Throwable e )
        {
            e.printStackTrace();
        }

        System.out.println( "Thread[" + getName() + "] ended at " + ( System.currentTimeMillis() % 1000 ) );

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


    private static LdapContext getContext()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        LdapContext context = null;
        String adminName = "uid=admin,ou=system";
        String adminPassword = "secret";

        env.put( Context.INITIAL_CONTEXT_FACTORY,
            "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, adminName );
        env.put( Context.SECURITY_CREDENTIALS, adminPassword );
        //env.put(Context.PROVIDER_URL, "ldap://10.255.0.40:389");
        env.put( Context.PROVIDER_URL, "ldap://localhost:10389" );
        //env.put(TDSConstants.JNDI_LDAP_CONNECTION_TIMEOUT, TDSProperties
        //    .getProperty(TDSConstants.LDAP_CONNECTION_TIMEOUT, "2000"));

        env.put( "com.sun.jndi.ldap.connect.pool", "true" );

        try
        {
            context = new InitialLdapContext( env, null );
        }
        catch ( NamingException ne )
        {
            System.exit( 1 );
        }
        return context;
    }


    public void setUserPreferences( LdapContext context, String userName,
        Map<String, Object> attributes, int count ) throws NamingException
    {

        LdapContext derivedContext = context;

        String bindOp = "cn=" + userName + "," + "ou=system";

        try
        {

            try
            {
                // Step 1: Unbind the user preferences data
                //System.out.println("Unbind[" + count + "]" + bindOp);
                _logger.info( "Unbind[" + count + "]" + bindOp );
                derivedContext.unbind( bindOp );
            }
            catch ( CommunicationException ce )
            {
                System.out.println( "Trying to re-connect to RDS" );
                _logger.info( "Trying to re-connect to RDS" );
                //Impl reconnect logic

            }
            catch ( NameNotFoundException nnf )
            {
                System.out.println( "User: " + userName
                    + " cannot be found in the Ldap server" );
                _logger.info( "User: " + userName
                    + " cannot be found in the Ldap server" );
            }

            try
            {
                // Step 2: Bind the user preferences data
                //System.out.println("Bind[" + count + "]" + bindOp);
                derivedContext.bind( bindOp, attributes );
            }
            catch ( CommunicationException ce )
            {
                System.out.println( "Trying to re-connect to RDS" );
            }
            catch ( NameAlreadyBoundException nab )
            {
                System.out.println( "User: " + userName
                    + " already exists in the Ldap server" );
            }

            //derivedContext.rebind(bindOp, attributes);
        }
        catch ( NamingException ne )
        {
            //System.out.println("Could not set user profile for user: "
            //    + userName);
            //ne.printStackTrace();
            _logger.log( Level.ERROR, count );
            throw ne;
        }
    }


    /**
     * @param args
     */
    public static void main( String[] args )
    {
        Thread t1 = null;

        System.out.println( System.currentTimeMillis() % 1000 );
        for ( int i = 0; i < 20; ++i )
        {
            t1 = new MultiThreadedTest( i );
            t1.start();
        }
    }

}
