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
package org.apache.directory.server.core.operational;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import org.apache.directory.shared.ldap.constants.JndiPropertyConstants;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.util.StringTools;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;


/**
 * Tests the methods on JNDI contexts that are analogous to entry modify
 * operations in LDAP.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class OperationalAttributeServiceIT
{
    private static final String BINARY_KEY = "java.naming.ldap.attributes.binary";
    private static final String RDN_KATE_BUSH = "cn=Kate Bush";


    public static DirectoryService service;


    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" );
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );

        return attrs;
    }


    /**
     * @todo add this to an LDIF annotation
     *
     * @param sysRoot the system root context at ou=system as the admin
     * @throws NamingException on error
     */
    protected void createData( LdapContext sysRoot ) throws NamingException
    {
        // Create an entry for Kate Bush
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        DirContext ctx = sysRoot.createSubcontext( RDN_KATE_BUSH, attrs );
        assertNotNull( ctx );
    }


    @Test
    public void testBinaryAttributeFilterExtension() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attributes = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "objectClass", "top" );
        oc.add( "person" );
        oc.add( "organizationalPerson" );
        oc.add( "inetOrgPerson" );
        attributes.put( oc );

        attributes.put( "ou", "test" );
        attributes.put( "cn", "test" );
        attributes.put( "sn", "test" );

        sysRoot.createSubcontext( "ou=test", attributes );

        // test without turning on the property
        DirContext ctx = ( DirContext ) sysRoot.lookup( "ou=test" );
        Attribute ou = ctx.getAttributes( "" ).get( "ou" );
        Object value = ou.get();
        assertTrue( value instanceof String );

        // test with the property now making ou into a binary value
        sysRoot.addToEnvironment( BINARY_KEY, "ou" );
        ctx = ( DirContext ) sysRoot.lookup( "ou=test" );
        ou = ctx.getAttributes( "" ).get( "ou" );
        value = ou.get();
        assertEquals( "test", value );

        // try jpegPhoto which should be binary automatically - use ou as control
        byte[] keyValue = new byte[]
            { (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0x01, 0x02, 'J', 'F', 'I', 'F', 0x00, 0x45, 0x23, 0x7d, 0x7f };
        attributes.put( "jpegPhoto", keyValue );
        sysRoot.createSubcontext( "ou=anothertest", attributes );
        ctx = ( DirContext ) sysRoot.lookup( "ou=anothertest" );
        ou = ctx.getAttributes( "" ).get( "ou" );
        value = ou.get();
        assertEquals( "anothertest", value );
        Attribute jpegPhoto = ctx.getAttributes( "" ).get( "jpegPhoto" );
        value = jpegPhoto.get();
        assertTrue( value instanceof byte[] );
        assertEquals( "0xFF 0xD8 0xFF 0xE0 0x01 0x02 0x4A 0x46 0x49 0x46 0x00 0x45 0x23 0x7D 0x7F ", StringTools.dumpBytes( ( byte[] ) value ) );

        // try jpegPhoto which should be binary automatically but use String to
        // create so we should still get back a byte[] - use ou as control
        /*attributes.remove( "jpegPhoto" );
        attributes.put( "jpegPhoto", "testing a string" );
        sysRoot.createSubcontext( "ou=yetanothertest", attributes );
        ctx = ( DirContext ) sysRoot.lookup( "ou=yetanothertest" );
        ou = ctx.getAttributes( "" ).get( "ou" );
        value = ou.get();
        assertEquals( "yetanothertest", value );
        jpegPhoto = ctx.getAttributes( "" ).get( "jpegPhoto" );
        value = jpegPhoto.get();
        assertTrue( value instanceof byte[] );*/
    }


    @Test
    public void testModifyOperationalOpAttrs() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        /*
         * create ou=testing00,ou=system
         */
        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing00" );
        DirContext ctx = sysRoot.createSubcontext( "ou=testing00", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing00", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
        assertNull( attributes.get( "createTimestamp" ) );
        assertNull( attributes.get( "creatorsName" ) );

        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes( new String[]
            { "ou", "createTimestamp", "creatorsName" } );

        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES,
                AliasDerefMode.NEVER_DEREF_ALIASES.getJndiValue() );
        NamingEnumeration<SearchResult> list;
        list = sysRoot.search( "", "(ou=testing00)", ctls );
        SearchResult result = list.next();
        list.close();

        assertNotNull( result.getAttributes().get( "ou" ) );
        assertNotNull( result.getAttributes().get( "creatorsName" ) );
        assertNotNull( result.getAttributes().get( "createTimestamp" ) );
    }


    /**
     * Checks to confirm that the system context root ou=system has the
     * required operational attributes.  Since this is created automatically
     * on system database creation properties the create attributes must be
     * specified.  There are no interceptors in effect when this happens so
     * we must test explicitly.
     *
     *
     * @see <a href="http://nagoya.apache.org/jira/browse/DIREVE-57">DIREVE-57:
     * ou=system does not contain operational attributes</a>
     *
     * @throws NamingException on error
     */
    @Test
    public void testSystemContextRoot() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        NamingEnumeration<SearchResult> list;
        list = sysRoot.search( "", "(objectClass=*)", controls );
        SearchResult result = list.next();

        // test to make sure op attribute do not occur - this is the control
        Attributes attributes = result.getAttributes();
        assertNull( attributes.get( "creatorsName" ) );
        assertNull( attributes.get( "createTimestamp" ) );

        // now we ask for all the op attributes and check to get them
        String[] ids = new String[]
            { "creatorsName", "createTimestamp" };
        controls.setReturningAttributes( ids );
        list = sysRoot.search( "", "(objectClass=*)", controls );
        result = list.next();
        attributes = result.getAttributes();
        assertNotNull( attributes.get( "creatorsName" ) );
        assertNotNull( attributes.get( "createTimestamp" ) );
    }


    /**
     * Test which confirms that all new users created under the user's dn
     * (ou=users,ou=system) have the creatorsName set to the DN of the new
     * user even though the admin is creating the user.  This is the basis
     * for some authorization rules to protect passwords.
     *
     * NOTE THIS CHANGE WAS REVERTED SO WE ADAPTED THE TEST TO MAKE SURE THE
     * CHANGE DOES NOT PERSIST!
     *
     * @see <a href="http://nagoya.apache.org/jira/browse/DIREVE-67">JIRA Issue DIREVE-67</a>
     *
     * @throws NamingException on error
     */
    @Test
    public void testConfirmNonAdminUserDnIsCreatorsName() throws NamingException
    {
        Entry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );

        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        Attributes attributes = sysRoot.getAttributes( "uid=akarasulu,ou=users", new String[]
            { "creatorsName" } );

        assertFalse( "uid=akarasulu,ou=users,ou=system".equals( attributes.get( "creatorsName" ).get() ) );
    }

    
    /**
     * Modify an entry and check whether attributes modifiersName and modifyTimestamp are present.
     *
     * @throws NamingException on error
     */
    @Test
    public void testModifyShouldLeadToModifiersAttributes() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        ModificationItem modifyOp = new ModificationItem( DirContext.ADD_ATTRIBUTE, new BasicAttribute( "description",
            "Singer Songwriter" ) );

        sysRoot.modifyAttributes( RDN_KATE_BUSH, new ModificationItem[]
            { modifyOp } );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        String[] ids = new String[]
            { "modifiersName", "modifyTimestamp" };
        controls.setReturningAttributes( ids );

        NamingEnumeration<SearchResult> list = sysRoot.search( RDN_KATE_BUSH, "(objectClass=*)", controls );
        SearchResult result = list.next();
        Attributes attributes = result.getAttributes();
        assertNotNull( attributes.get( "modifiersName" ) );
        assertNotNull( attributes.get( "modifyTimestamp" ) );
    }
    
    
    /**
     * Modify an entry and check whether attribute modifyTimestamp changes.
     *
     * @throws NamingException on error
     * @throws InterruptedException on error
     */
    @Test
    public void testModifyShouldChangeModifyTimestamp() throws NamingException, InterruptedException
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        // Add attribute description to entry
        ModificationItem modifyAddOp = new ModificationItem( DirContext.ADD_ATTRIBUTE, new BasicAttribute(
            "description", "an English singer, songwriter, musician" ) );
        sysRoot.modifyAttributes( RDN_KATE_BUSH, new ModificationItem[]
            { modifyAddOp } );

        // Determine modifyTimestamp
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        String[] ids = new String[]
            { "modifyTimestamp" };
        controls.setReturningAttributes( ids );
        NamingEnumeration<SearchResult> list = sysRoot.search( RDN_KATE_BUSH, "(objectClass=*)", controls );
        SearchResult result = list.next();
        Attributes attributes = result.getAttributes();
        Attribute modifyTimestamp = attributes.get( "modifyTimestamp" );
        assertNotNull( modifyTimestamp );
        String oldTimestamp = modifyTimestamp.get().toString();
        
        // Wait two seconds
        Thread.sleep( 2000 );

        // Change value of attribute description
        ModificationItem modifyOp = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(
            "description", "one of England's most successful solo female performers" ) );
        sysRoot.modifyAttributes( RDN_KATE_BUSH, new ModificationItem[]
            { modifyOp } );

        // Determine modifyTimestamp after modification
        list = sysRoot.search( RDN_KATE_BUSH, "(objectClass=*)", controls );
        result = list.next();
        attributes = result.getAttributes();
        modifyTimestamp = attributes.get( "modifyTimestamp" );
        assertNotNull( modifyTimestamp );
        String newTimestamp = modifyTimestamp.get().toString();
        
        // assert the value has changed
        assertFalse( oldTimestamp.equals( newTimestamp ) );
    }


    /**
     * Try to add modifiersName attribute to an entry
     *
     * @throws NamingException on error
     */
    @Test
    public void testModifyOperationalAttributeAdd() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        ModificationItem modifyOp = new ModificationItem( DirContext.ADD_ATTRIBUTE, new BasicAttribute(
            "modifiersName", "cn=Tori Amos,dc=example,dc=com" ) );

        try
        {
            sysRoot.modifyAttributes( RDN_KATE_BUSH, new ModificationItem[]
                { modifyOp } );
            fail( "modification of entry should fail" );
        }
        catch ( InvalidAttributeValueException e )
        {
            // expected
        }
        catch ( NoPermissionException e )
        {
            // expected
        }
    }


    /**
     * Try to remove creatorsName attribute from an entry.
     *
     * @throws NamingException on error
     */
    @Test
    public void testModifyOperationalAttributeRemove() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        ModificationItem modifyOp = new ModificationItem( DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(
            "creatorsName" ) );

        try
        {
            sysRoot.modifyAttributes( RDN_KATE_BUSH, new ModificationItem[]
                { modifyOp } );
            fail( "modification of entry should fail" );
        }
        catch ( InvalidAttributeValueException e )
        {
            // expected
        }
        catch ( NoPermissionException e )
        {
            // expected
        }
    }


    /**
     * Try to replace creatorsName attribute on an entry.
     *
     * @throws NamingException on error
     */
    @Test
    public void testModifyOperationalAttributeReplace() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        createData( sysRoot );

        ModificationItem modifyOp = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl(
            "creatorsName", "cn=Tori Amos,dc=example,dc=com" ) );

        try
        {
            sysRoot.modifyAttributes( RDN_KATE_BUSH, new ModificationItem[]
                { modifyOp } );
            fail( "modification of entry should fail" );
        }
        catch ( InvalidAttributeValueException e )
        {
            // expected
        }
        catch ( NoPermissionException e )
        {
            // expected
        }
    }
}
