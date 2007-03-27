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
package org.apache.directory.server.core.jndi;


import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;


/**
 * Test DIRSERVER-806 issue. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 515058 $
 */
public class Referral806Test extends AbstractAdminTestCase
{
    private static final boolean SUNJNDI = false;
    private boolean sunjndi = System.getProperty( "sunjndi" ) != null || SUNJNDI;
    TestData td = new TestData();

    private static final String REF_CN = "theReferral";

    private static final String REF_RDN = "cn=" + REF_CN;

    /**
     * Create attributes ror a referral entry
     */
    protected Attributes getReferralEntryAttributes( String cn, String ref ) 
    {
        Attributes refEntry = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute("objectClass");
        ocls.add("top");
        ocls.add("referral");
        ocls.add("extensibleObject");
        refEntry.put(ocls);
        refEntry.put("cn", cn);
        refEntry.put("ref", ref);

        return refEntry;
    }

    public void setUp() throws Exception
    {
        if ( !sunjndi )
        {
            super.setUp();
        }

        addReferralEntry();
    }


    public void tearDown() throws Exception
    {
        td.refCtx.addToEnvironment(Context.REFERRAL, "ignore");
        td.refCtx.destroySubcontext( "" );

        
        if ( td.refCtx != null )
        {
            td.refCtx.close();
        }

        if ( !sunjndi )
        {
            super.tearDown();
        }
        else if ( td.rootCtx != null )
        {
            td.rootCtx.close();
        }
    }


    /*
     * NOTE: We may encounter conflicting circumstances where the ManageDsaIT control
     * is included in the request controls yet the Context.REFERRAL is set to 
     * something other than ignore: throw or follow.  We have to figure out what to
     * do in these cases.
     * 
     * Simply throw an illegal state exception when this happens?
     * 
     * NOTE: Need to figure out the behavoir of referral handling during search
     * when aliases are being dereferenced.
     */

    /**
     * Get's the root "ou=system" in either the embedded instance or an
     * external instance.  We do this to mold test cases to make sure the 
     * ApacheDS JNDI LDAP provider behaves just like the SUN JNDI LDAP
     * provider does.
     */
    private LdapContext getSystemRoot() throws NamingException
    {
        if ( !sunjndi )
        {
            return sysRoot;
        }

        Hashtable env = new Hashtable();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://hertz.karasulu.homeip.net:10390/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "longsecret" );
        env.put( "java.naming.security.authentication", "simple" );
        return new InitialLdapContext( env, null );
    }

    class TestData
    {
        LdapContext rootCtx;
        Name ctxDn;
        LdapContext refCtx;
        List refs;
    }


    public void addReferralEntry() throws NamingException
    {
        Attributes refEntry = getReferralEntryAttributes( REF_CN, "ldap://someHost:389/cn=somewhere" );

        td.rootCtx = getSystemRoot();

        // -------------------------------------------------------------------
        // Adds a referral entry regardless of referral handling settings
        // -------------------------------------------------------------------

        // Just in case if server is a remote server destroy remaing referral
        td.rootCtx.addToEnvironment( Context.REFERRAL, "ignore" );

        try
        {
            td.rootCtx.destroySubcontext( "uid=akarasulu,ou=users" );
        }
        catch ( NameNotFoundException e )
        {
            e.printStackTrace();
        }
        
        try
        {
            td.rootCtx.destroySubcontext( "ou=users" );
        }
        catch ( NameNotFoundException e )
        {
            e.printStackTrace();
        }
        
        try
        {
            td.refCtx = ( LdapContext ) td.rootCtx.createSubcontext( REF_RDN, refEntry );
        }
        catch ( NameAlreadyBoundException e )
        {
            td.refCtx = ( LdapContext ) td.rootCtx.lookup( "ou=users" );
        }
        
        Attributes referral = td.refCtx.getAttributes( "" );
        assertTrue( referral.get( "cn" ).contains( "theReferral" ) );
        assertTrue( referral.get( "objectClass" ).contains( "referral" ) );
    }

    /**
     * Search with default filter (objectClass=*) and check for continuation
     * reference
     */
    public void testSimpleContinuationWithDefaultFilter() throws NamingException {

        SearchControls ctls = new SearchControls();
        ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String filter = "(objectClass=referral)";
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );

        try {
            NamingEnumeration enm = td.rootCtx.search("", filter, ctls);
            
            while (enm.hasMore()) 
            {
                Object attrs = enm.next();
                
                System.out.println(attrs);
            }
            
            fail("No referral exception");
        } catch (ReferralException e) {
            assertNotNull(e.getReferralInfo());
            String referralInfo = e.getReferralInfo().toString();
            assertTrue(referralInfo.startsWith("ldap://"));
        }
    }
}
