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


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.message.DerefAliasesEnum;


/**
 * Tests the methods on JNDI contexts that are analogous to entry modify
 * operations in LDAP.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OperationalAttributeServiceITest extends AbstractAdminTestCase
{
    private static final String CREATORS_NAME = "creatorsName";
    private static final String CREATE_TIMESTAMP = "createTimestamp";


    public void testModifyOperationalOpAttrs() throws NamingException
    {
        /*
         * create ou=testing00,ou=system
         */
        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
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
        assertNull( attributes.get( CREATE_TIMESTAMP ) );
        assertNull( attributes.get( CREATORS_NAME ) );

        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes( new String[]
            { "ou", "createTimestamp", "creatorsName" } );

        sysRoot.addToEnvironment( DerefAliasesEnum.JNDI_PROP, DerefAliasesEnum.NEVERDEREFALIASES_NAME );
        NamingEnumeration list;
        list = sysRoot.search( "", "(ou=testing00)", ctls );
        SearchResult result = ( SearchResult ) list.next();
        list.close();

        assertNotNull( result.getAttributes().get( "ou" ) );
        assertNotNull( result.getAttributes().get( CREATORS_NAME ) );
        assertNotNull( result.getAttributes().get( CREATE_TIMESTAMP ) );
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
     */
    public void testSystemContextRoot() throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        NamingEnumeration list;
        list = sysRoot.search( "", "(objectClass=*)", controls );
        SearchResult result = ( SearchResult ) list.next();

        // test to make sure op attribute do not occur - this is the control
        Attributes attributes = result.getAttributes();
        assertNull( attributes.get( "creatorsName" ) );
        assertNull( attributes.get( "createTimestamp" ) );

        // now we ask for all the op attributes and check to get them
        String[] ids = new String[]
            { "creatorsName", "createTimestamp" };
        controls.setReturningAttributes( ids );
        list = sysRoot.search( "", "(objectClass=*)", controls );
        result = ( SearchResult ) list.next();
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
     */
    public void testConfirmNonAdminUserDnIsCreatorsName() throws NamingException
    {
        Attributes attributes = sysRoot.getAttributes( "uid=akarasulu,ou=users", new String[]
            { "creatorsName" } );

        assertFalse( "uid=akarasulu,ou=users,ou=system".equals( attributes.get( "creatorsName" ).get() ) );
    }
}
