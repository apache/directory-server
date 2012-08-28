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

package org.apache.directory.server.core.operations.add;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.authn.PasswordUtil;
import org.apache.directory.server.core.hash.Md5PasswordHashingInterceptor;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for checking PasswordHashingInterceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "PasswordHashingInterceptorTest-DS", additionalInterceptors=Md5PasswordHashingInterceptor.class)
@ApplyLdifs( {
    "dn: cn=test,ou=system",
    "objectClass: person",
    "cn: test",
    "sn: sn_test",
    "userPassword: secret"
})
public class PasswordHashingInterceptorIT extends AbstractLdapTestUnit
{

    @Test
    public void testAddWithPlainPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        byte[] plainPwd = "secret".getBytes();
        Dn dn = new Dn( "cn=test,ou=system" );

        Entry entry = connection.lookup( dn );
        Attribute pwdAt = entry.get( SchemaConstants.USER_PASSWORD_AT );
        
        assertFalse( Arrays.equals( plainPwd, pwdAt.getBytes() ) );
        assertTrue( PasswordUtil.compareCredentials( plainPwd, pwdAt.getBytes() ) );
    }
    
    
    @Test
    public void testModifyWithPlainPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        byte[] plainPwd = "newsecret".getBytes();
        Dn dn = new Dn( "cn=test,ou=system" );

        AttributeType pwdAtType = getService().getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.USER_PASSWORD_AT );
        
        Attribute pwdAt = new DefaultAttribute( pwdAtType );
        pwdAt.add( plainPwd );
        
        Modification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, pwdAt );
        connection.modify( dn, mod );
        
        Entry entry = connection.lookup( dn );
        pwdAt = entry.get( pwdAtType );
        
        assertFalse( Arrays.equals( plainPwd, pwdAt.getBytes() ) );
        assertTrue( PasswordUtil.compareCredentials( plainPwd, pwdAt.getBytes() ) );
    }

    
    @Test
    public void testAddWithHashedPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        byte[] plainPwd = "secret".getBytes();
        byte[] hashedPwd = PasswordUtil.createStoragePassword( plainPwd, LdapSecurityConstants.HASH_METHOD_SSHA );
        
        Dn dn = new Dn( "cn=testHash,ou=system" );
        Entry entry = new DefaultEntry( getService().getSchemaManager(), dn );
        entry.add( "ObjectClass", "top", "person" );
        entry.add( "sn", "TEST" );
        entry.add( "cn", "testHash" );
        entry.add( SchemaConstants.USER_PASSWORD_AT, hashedPwd );

        connection.add( entry );

        entry = connection.lookup( dn );
        Attribute pwdAt = entry.get( SchemaConstants.USER_PASSWORD_AT );
        assertTrue( Arrays.equals( hashedPwd, pwdAt.getBytes() ) );
        assertTrue( PasswordUtil.compareCredentials( plainPwd, pwdAt.getBytes() ) );
    }
    
    
    @Test
    public void testModifyWithHashedPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        byte[] plainPwd = "xyzsecret".getBytes();
        byte[] hashedPwd = PasswordUtil.createStoragePassword( plainPwd, LdapSecurityConstants.HASH_METHOD_SSHA256 );

        Dn dn = new Dn( "cn=test,ou=system" );

        AttributeType pwdAtType = getService().getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.USER_PASSWORD_AT );
        
        Attribute pwdAt = new DefaultAttribute( pwdAtType );
        pwdAt.add( hashedPwd );
        
        Modification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, pwdAt );
        connection.modify( dn, mod );
        
        Entry entry = connection.lookup( dn );
        pwdAt = entry.get( pwdAtType );
        
        assertTrue( Arrays.equals( hashedPwd, pwdAt.getBytes() ) );
        assertTrue( PasswordUtil.compareCredentials( plainPwd, pwdAt.getBytes() ) );
    }
}
