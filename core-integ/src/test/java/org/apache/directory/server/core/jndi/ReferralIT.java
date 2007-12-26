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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.util.HashMap;
import java.util.Map;


/**
 * Tests the referral handling functionality within the server's core.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class ReferralIT
{
    public static DirectoryService service;
    private TestData td = new TestData();


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

    private class TestData
    {
        LdapContext rootCtx;
        Name ctxDn;
        LdapContext refCtx;
    }


    private void addReferralEntry() throws NamingException
    {
        String ref0 = "ldap://fermi:10389/ou=users,ou=system";
        String ref1 = "ldap://hertz:10389/ou=users,dc=example,dc=com";
        String ref2 = "ldap://maxwell:10389/ou=users,ou=system";
        td.rootCtx = getSystemContext( service );

        Entry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );

        // -------------------------------------------------------------------
        // Adds a referral entry regardless of referral handling settings
        // -------------------------------------------------------------------

        // Add a referral entry ( should be fine with or without the control )
        Attributes referral = new AttributesImpl( "objectClass", "top", true );
        referral.get( "objectClass" ).add( "referral" );
        referral.get( "objectClass" ).add( "extensibleObject" );
        referral.put( "ref", ref0 );
        referral.get( "ref" ).add( ref1 );
        referral.get( "ref" ).add( ref2 );
        referral.put( "ou", "users" );

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
            td.refCtx = ( LdapContext ) td.rootCtx.createSubcontext( "ou=users", referral );
        }
        catch ( NameAlreadyBoundException e )
        {
            td.refCtx = ( LdapContext ) td.rootCtx.lookup( "ou=users" );
        }
        referral = td.refCtx.getAttributes( "" );
        assertTrue( referral.get( "ou" ).contains( "users" ) );
        assertTrue( referral.get( "objectClass" ).contains( "referral" ) );
    }


    private void checkAncestorReferrals( ReferralException e ) throws Exception
    {
        assertEquals( "ldap://fermi:10389", e.getReferralInfo() );
        assertTrue( e.skipReferral() );
        assertEquals( "ldap://hertz:10389/cn=alex%20karasulu,ou=apache,ou=users,dc=example,dc=com", e.getReferralInfo() );
        assertTrue( e.skipReferral() );
        assertEquals( "ldap://maxwell:10389", e.getReferralInfo() );
        assertFalse( e.skipReferral() );
    }


    private void checkParentReferrals( ReferralException e ) throws Exception
    {
        assertEquals( "ldap://fermi:10389", e.getReferralInfo() );
        assertTrue( e.skipReferral() );
        assertEquals( "ldap://hertz:10389/cn=alex%20karasulu,ou=users,dc=example,dc=com", e.getReferralInfo() );
        assertTrue( e.skipReferral() );
        assertEquals( "ldap://maxwell:10389", e.getReferralInfo() );
        assertFalse( e.skipReferral() );
    }


    /**
     * Checks for correct core behavior when Context.REFERRAL is set to <b>throw</b>
     * for an add operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testAddWithReferralParent() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to add a normal entry below the referral parent. We should
        // encounter referral errors with referral setting set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        Attributes userEntry = new AttributesImpl( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "karasulu" );
        userEntry.put( "cn", "alex karasulu" );

        try
        {
            td.refCtx.createSubcontext( "cn=alex karasulu", userEntry );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an add operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testAddWithReferralAncestor() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to add a normal entry below the referral ancestor. We should
        // encounter referral errors with referral setting set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        Attributes userEntry = new AttributesImpl( "objectClass", "top", true );
        userEntry.get( "objectClass" ).add( "person" );
        userEntry.put( "sn", "karasulu" );
        userEntry.put( "cn", "alex karasulu" );

        try
        {
            td.refCtx.createSubcontext( "cn=alex karasulu,ou=apache", userEntry );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an delete operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testDeleteWithReferralParent() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to delete a non-existent entry below the referral parent. 
        // We should encounter referral errors with referral setting set to 
        // throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.destroySubcontext( "cn=alex karasulu" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a delete operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testDeleteWithReferralAncestor() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to delete a non-existent entry below the referral ancestor. 
        // We should encounter referral errors when referral setting is set to 
        // throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.destroySubcontext( "cn=alex karasulu,ou=apache" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for an delete operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testCompareWithReferralParent() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to compare attributes in an entry below the referral parent. 
        // We should encounter referral errors with referral setting set to 
        // throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            if ( td.refCtx instanceof ServerLdapContext )
            {
                LdapDN dn = new LdapDN( "cn=alex karasulu,ou=users,ou=system" );
                ( ( ServerLdapContext ) td.refCtx ).compare( dn, "sn", "karasulu" );
            }
            else
            {
                // abort the test because we're using the sun jdni provider
                return;
            }
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a compare operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testCompareWithReferralAncestor() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to compare attributes in an entry below the referral ancestor. 
        // We should encounter referral errors when referral setting is set to 
        // throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            if ( td.refCtx instanceof ServerLdapContext )
            {
                LdapDN dn = new LdapDN( "cn=alex karasulu,ou=apache,ou=users,ou=system" );
                ( ( ServerLdapContext ) td.refCtx ).compare( dn, "sn", "karasulu" );
            }
            else
            {
                // abort the test because we're using the sun jdni provider
                return;
            }
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testModifyWithReferralParent() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // parent.  We should encounter referral errors with referral setting 
        // set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.modifyAttributes( "cn=alex karasulu", DirContext.ADD_ATTRIBUTE, new AttributesImpl(
                "description", "just some text", true ) );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testModifyWithReferralAncestor() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // ancestor. We should encounter referral errors when referral setting 
        // is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.modifyAttributes( "cn=alex karasulu,ou=apache", DirContext.ADD_ATTRIBUTE, new AttributesImpl(
                "description", "just some text", true ) );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify operation with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testModifyWithReferralParent2() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // parent.  We should encounter referral errors with referral setting 
        // set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            ModificationItemImpl[] mods = new ModificationItemImpl[]
                { new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, new AttributeImpl( "description", "just some text" ) ) };
            td.refCtx.modifyAttributes( "cn=alex karasulu", mods );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify operation with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testModifyWithReferralAncestor2() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the attributes of an entry below the referral 
        // ancestor. We should encounter referral errors when referral setting 
        // is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            ModificationItemImpl[] mods = new ModificationItemImpl[]
                { new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, new AttributeImpl( "description", "just some text" ) ) };
            td.refCtx.modifyAttributes( "cn=alex karasulu,ou=apache", mods );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify rdn interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testModifyRdnWithReferralParent() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.rename( "cn=alex karasulu", "cn=aok" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a modify rdn interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testModifyRdnWithReferralAncestor() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.rename( "cn=alex karasulu,ou=apache", "cn=aok,ou=apache" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testMoveWithReferralParent() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.rename( "cn=alex karasulu", "cn=alex karasulu,ou=groups" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testMoveWithReferralAncestor() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.rename( "cn=alex karasulu,ou=apache", "cn=alex karasulu,ou=groups" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testMoveWithReferralParent2() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.rename( "cn=alex karasulu", "cn=aok,ou=groups" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkParentReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testMoveWithReferralAncestor2() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.rename( "cn=alex karasulu,ou=apache", "cn=aok,ou=groups" );
            fail( "Should fail here throwing a ReferralException" );
        }
        catch ( ReferralException e )
        {
            checkAncestorReferrals( e );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testMoveWithReferralParentDest() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        createLocalUser();
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.rootCtx.rename( "cn=akarasulu", "cn=akarasulu,ou=users" );
            fail( "Should fail here throwing a LdapNamingException with ResultCodeEnum = AFFECTSMULTIPLEDSAS" );
        }
        catch ( LdapNamingException e )
        {
            assertTrue( e.getResultCode() == ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testMoveWithReferralAncestorDest() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        createDeepLocalUser();
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.rootCtx.rename( "cn=akarasulu,ou=deep", "cn=akarasulu,ou=users" );
            fail( "Should fail here throwing a LdapNamingException with ResultCodeEnum = AFFECTSMULTIPLEDSAS" );
        }
        catch ( LdapNamingException e )
        {
            assertTrue( e.getResultCode() == ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with the parent context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testMoveWithReferralParent2Dest() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an parent which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        createLocalUser();
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.rootCtx.rename( "cn=akarasulu", "cn=aok,ou=users" );
            fail( "Should fail here throwing a LdapNamingException with ResultCodeEnum = AFFECTSMULTIPLEDSAS" );
        }
        catch ( LdapNamingException e )
        {
            assertTrue( e.getResultCode() == ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
        }
    }


    /**
     * Checks for correct core behavoir when Context.REFERRAL is set to <b>throw</b>
     * for a move interceptor operation (corresponds to a subset of the modify 
     * dn operation) with an ancestor context being a referral.
     * 
     * @throws Exception if something goes wrong.
     */
    @Test
    public void testMoveWithReferralAncestor2Dest() throws Exception
    {
        addReferralEntry();

        // -------------------------------------------------------------------
        // Attempt to modify the last component of the entry's name which 
        // resides below an ancestor which is a referral. We should encounter 
        // referral errors when referral setting is set to throw.
        // -------------------------------------------------------------------

        createDeepLocalUser();
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.rootCtx.rename( "cn=akarasulu,ou=deep", "cn=aok,ou=users" );
            fail( "Should fail here throwing a LdapNamingException with ResultCodeEnum = AFFECTSMULTIPLEDSAS" );
        }
        catch ( LdapNamingException e )
        {
            assertTrue( e.getResultCode() == ResultCodeEnum.AFFECTS_MULTIPLE_DSAS );
        }
    }


    private void createLocalUser() throws Exception
    {
        addReferralEntry();

        LdapContext userCtx;
        Attributes referral = new AttributesImpl( "objectClass", "top", true );
        referral.get( "objectClass" ).add( "person" );
        referral.put( "cn", "akarasulu" );
        referral.put( "sn", "karasulu" );

        //noinspection EmptyCatchBlock
        try
        {
            td.rootCtx.destroySubcontext( "uid=akarasulu" );
        }
        catch ( NameNotFoundException e )
        {
        }
        
        try
        {
            userCtx = ( LdapContext ) td.rootCtx.createSubcontext( "cn=akarasulu", referral );
        }
        catch ( NameAlreadyBoundException e )
        {
            userCtx = ( LdapContext ) td.rootCtx.lookup( "cn=akarasulu" );
        }
        
        referral = userCtx.getAttributes( "" );
        assertTrue( referral.get( "cn" ).contains( "akarasulu" ) );
        assertTrue( referral.get( "sn" ).contains( "karasulu" ) );
    }


    private void createDeepLocalUser() throws Exception
    {
        addReferralEntry();

        LdapContext userCtx = null;
        Attributes referral = new AttributesImpl( "objectClass", "top", true );
        referral.get( "objectClass" ).add( "person" );
        referral.get( "objectClass" ).add( SchemaConstants.EXTENSIBLE_OBJECT_OC );
        referral.put( "cn", "akarasulu" );
        referral.put( "sn", "karasulu" );
        referral.put( "ou", "deep" );

        //noinspection EmptyCatchBlock
        try
        {
            td.rootCtx.destroySubcontext( "uid=akarasulu,ou=deep" );
        }
        catch ( NameNotFoundException e )
        {
        }
        //noinspection EmptyCatchBlock
        try
        {
            td.rootCtx.destroySubcontext( "ou=deep" );
        }
        catch ( NameNotFoundException e )
        {
        }
        try
        {
            Attributes attrs = new AttributesImpl( "ou", "deep" );
            Attribute oc = new AttributeImpl( "ObjectClass" );
            oc.add( "top" );
            oc.add( "organizationalUnit" );
            attrs.put( oc );

            td.rootCtx.createSubcontext( "ou=deep", attrs );
            userCtx = ( LdapContext ) td.rootCtx.createSubcontext( "cn=akarasulu,ou=deep", referral );
        }
        catch ( NameAlreadyBoundException e )
        {
            td.refCtx = ( LdapContext ) td.rootCtx.lookup( "cn=akarasulu,ou=deep" );
        }
        referral = userCtx.getAttributes( "" );
        assertTrue( referral.get( "cn" ).contains( "akarasulu" ) );
        assertTrue( referral.get( "sn" ).contains( "karasulu" ) );
    }


    @Test
    public void testSearchBaseIsReferral() throws Exception
    {
        addReferralEntry();

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.rootCtx.search( "ou=users", "(objectClass=*)", controls );
            fail( "should never get here" );
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
    }


    @Test
    public void testSearchBaseParentIsReferral() throws Exception
    {
        addReferralEntry();

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.search( "cn=alex karasulu", "(objectClass=*)", controls );
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/cn=alex%20karasulu,ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/cn=alex%20karasulu,ou=users,dc=example,dc=com??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/cn=alex%20karasulu,ou=users,ou=system??base", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
    }


    @Test
    public void testSearchBaseAncestorIsReferral() throws Exception
    {
        addReferralEntry();

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        td.refCtx.addToEnvironment( Context.REFERRAL, "throw" );
        try
        {
            td.refCtx.search( "cn=alex karasulu,ou=apache", "(objectClass=*)", controls );
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/cn=alex%20karasulu,ou=apache,ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/cn=alex%20karasulu,ou=apache,ou=users,dc=example,dc=com??base", e
                .getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/cn=alex%20karasulu,ou=apache,ou=users,ou=system??base", e
                .getReferralInfo() );
            assertFalse( e.skipReferral() );
        }
    }


    @Test
    public void testSearchContinuations() throws Exception
    {
        addReferralEntry();

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration<SearchResult> list = td.rootCtx.search( "", "(objectClass=*)", controls );
        Map<String, SearchResult> results = new HashMap<String, SearchResult>();
        
        while ( list.hasMore() )
        {
            SearchResult result = list.next();
            results.put( result.getName(), result );
        }

        assertNotNull( results.get( "ou=users,ou=system" ) );

        // -------------------------------------------------------------------
        // Now we will throw exceptions when searching for referrals 
        // -------------------------------------------------------------------

        td.rootCtx.addToEnvironment( Context.REFERRAL, "throw" );
        list = td.rootCtx.search( "", "(objectClass=*)", controls );
        results = new HashMap<String, SearchResult>();

        try
        {
            while ( list.hasMore() )
            {
                SearchResult result = ( SearchResult ) list.next();
                results.put( result.getName(), result );
            }
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??sub", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??sub", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }

        assertNull( results.get( "ou=users" ) );

        // try again but this time with single level scope

        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        list = td.rootCtx.search( "", "(objectClass=*)", controls );
        results = new HashMap<String, SearchResult>();

        try
        {
            while ( list.hasMore() )
            {
                SearchResult result = ( SearchResult ) list.next();
                results.put( result.getName(), result );
            }
        }
        catch ( ReferralException e )
        {
            assertEquals( "ldap://fermi:10389/ou=users,ou=system??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://hertz:10389/ou=users,dc=example,dc=com??base", e.getReferralInfo() );
            assertTrue( e.skipReferral() );
            assertEquals( "ldap://maxwell:10389/ou=users,ou=system??base", e.getReferralInfo() );
            assertFalse( e.skipReferral() );
        }

        assertNull( results.get( "ou=users" ) );
    }


    /**
     * Checks that when injecting LDAP ref with an empty DN we get an exception
     *
     * @throws Exception on error
     */
    @Test
    public void testAddWReferralWithEmptyDN() throws Exception
    {
        addReferralEntry();

        Attributes attrs = new AttributesImpl( true );

        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "extensibleObject" );
        oc.add( "referral" );
        attrs.put( oc );
        
        Attribute ref = new AttributeImpl( "ref", "ldap://" );
        attrs.put( ref );

        attrs.put( "cn", "refWithEmptyDN" );

        String base = "cn=refWithEmptyDN";

        //create subcontext
        try
        {
            getSystemContext( service ).createSubcontext( base, attrs );
            fail( "Should not reach this state" );
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Checks that when injecting LDAP ref with attribuutes we get an exception
     *
     * @throws Exception on error
     */
    @Test
    public void testAddWReferralWithAttrs() throws Exception
    {
        addReferralEntry();

        Attributes attrs = new AttributesImpl( true );

        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "extensibleObject" );
        oc.add( "referral" );
        attrs.put( oc );
        
        Attribute ref = new AttributeImpl( "ref", "ldap://localhost/cn=RefWithAttributes?cn" );
        attrs.put( ref );

        attrs.put( "cn", "RefWithAttributes" );

        String base = "cn=RefWithAttributes";

        //create subcontext
        try
        {
            getSystemContext( service ).createSubcontext( base, attrs );
            fail( "Should not reach this state" );
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Checks that when injecting LDAP ref with a scope we get an exception
     *
     * @throws Exception on error
     */
    @Test
    public void testAddWReferralWithScope() throws Exception
    {
        addReferralEntry();

        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "extensibleObject" );
        oc.add( "referral" );
        attrs.put( oc );
        
        Attribute ref = new AttributeImpl( "ref", "ldap://localhost/cn=RefWithScope??sub" );
        attrs.put( ref );

        attrs.put( "cn", "RefWithScope" );

        String base = "cn=RefWithScope";

        //create subcontext
        try
        {
            getSystemContext( service ).createSubcontext( base, attrs );
            fail( "Should not reach this state" );
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Checks that when injecting LDAP ref with a filter we get an exception
     *
     * @throws Exception on error
     */
    @Test
    public void testAddWReferralWithFilters() throws Exception
    {
        addReferralEntry();

        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "extensibleObject" );
        oc.add( "referral" );
        attrs.put( oc );
        
        Attribute ref = new AttributeImpl( "ref", "ldap://localhost/cn=RefWithFilter???(cn=*)" );
        attrs.put( ref );

        attrs.put( "cn", "RefWithFilter" );

        String base = "cn=RefWithFilter";

        //create subcontext
        try
        {
            getSystemContext( service ).createSubcontext( base, attrs );
            fail( "Should not reach this state" );
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Checks that when injecting LDAP ref with an extension we get an exception
     *
     * @throws Exception on error
     */
    @Test
    public void testAddWReferralWithExtension() throws Exception
    {
        addReferralEntry();

        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "extensibleObject" );
        oc.add( "referral" );
        attrs.put( oc );
        
        Attribute ref = new AttributeImpl( "ref", "ldap://localhost/cn=RefWithExtension????x-extension=1.2.3.4" );
        attrs.put( ref );

        attrs.put( "cn", "RefWithExtension" );

        String base = "cn=RefWithExtension";

        //create subcontext
        try
        {
            getSystemContext( service ).createSubcontext( base, attrs );
            fail( "Should not reach this state" );
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * Checks that when injecting LDAP ref with a critical extension we get an exception
     *
     * @throws Exception on error
     */
    @Test
    public void testAddWReferralWithCriticalExtension() throws Exception
    {
        addReferralEntry();

        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "extensibleObject" );
        oc.add( "referral" );
        attrs.put( oc );
        
        Attribute ref = new AttributeImpl( "ref", "ldap://localhost/cn=RefWithCriticalExtension????!x-extension=1.2.3.4" );
        attrs.put( ref );

        attrs.put( "cn", "RefWithCriticalExtension" );

        String base = "cn=RefWithCriticalExtension";

        //create subcontext
        try
        {
            getSystemContext( service ).createSubcontext( base, attrs );
            fail( "Should not reach this state" );
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }
}
