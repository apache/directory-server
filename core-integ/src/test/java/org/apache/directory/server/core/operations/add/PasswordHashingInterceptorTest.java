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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.config.beans.HashInterceptorBean;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.hash.ConfigurableHashingInterceptor;
import org.apache.directory.server.core.hash.CryptPasswordHashingInterceptor;
import org.apache.directory.server.core.hash.Md5PasswordHashingInterceptor;
import org.apache.directory.server.core.hash.Pkcs5s2PasswordHashingInterceptor;
import org.apache.directory.server.core.hash.Sha256PasswordHashingInterceptor;
import org.apache.directory.server.core.hash.Sha384PasswordHashingInterceptor;
import org.apache.directory.server.core.hash.Sha512PasswordHashingInterceptor;
import org.apache.directory.server.core.hash.ShaPasswordHashingInterceptor;
import org.apache.directory.server.core.hash.Smd5PasswordHashingInterceptor;
import org.apache.directory.server.core.hash.Ssha256PasswordHashingInterceptor;
import org.apache.directory.server.core.hash.Ssha384PasswordHashingInterceptor;
import org.apache.directory.server.core.hash.Ssha512PasswordHashingInterceptor;
import org.apache.directory.server.core.hash.SshaPasswordHashingInterceptor;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test case for checking PasswordHashingInterceptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "PasswordHashingInterceptorTest-DS")
public class PasswordHashingInterceptorTest extends AbstractLdapTestUnit
{

    @Test
    public void testAllMechanisms() throws Exception
    {
        List<Class<?>> allMechanism = new ArrayList<Class<?>>();
        allMechanism.add( CryptPasswordHashingInterceptor.class );
        allMechanism.add( Md5PasswordHashingInterceptor.class );
        allMechanism.add( Sha256PasswordHashingInterceptor.class );
        allMechanism.add( Sha384PasswordHashingInterceptor.class );
        allMechanism.add( Sha512PasswordHashingInterceptor.class );
        allMechanism.add( ShaPasswordHashingInterceptor.class );
        allMechanism.add( Smd5PasswordHashingInterceptor.class );
        allMechanism.add( Ssha256PasswordHashingInterceptor.class );
        allMechanism.add( Ssha384PasswordHashingInterceptor.class );
        allMechanism.add( Ssha512PasswordHashingInterceptor.class );
        allMechanism.add( SshaPasswordHashingInterceptor.class );
        allMechanism.add( Pkcs5s2PasswordHashingInterceptor.class );
        allMechanism.add( ConfigurableHashingInterceptor.class );

        Entry entry = new DefaultEntry( service.getSchemaManager(), "cn=test,ou=system",
            "objectClass: person",
            "cn: test",
            "sn: sn_test",
            "userPassword: secret" );

        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        List<Interceptor> interceptors = service.getInterceptors();

        for ( int i = 0; i < allMechanism.size(); i++ )
        {
            Class<?> clazz = allMechanism.get( i );
            Interceptor hashMech = null;
            if ( clazz == ConfigurableHashingInterceptor.class ) 
            { 
                HashInterceptorBean config = new HashInterceptorBean();
                config.setHashAlgorithm( "SSHA-256" );
                config.addHashAttributes( new String[] { "2.5.4.35" } );
                hashMech = new ConfigurableHashingInterceptor( config );
            }
            else 
            {
                hashMech = ( Interceptor ) clazz.newInstance();
            }
            hashMech.init( service );

            // make sure to remove the last added mechanism
            if ( i > 0 )
            {
                interceptors.remove( interceptors.size() - 1 );
            }

            interceptors.add( hashMech );

            service.setInterceptors( interceptors );

            connection.add( entry );

            // System.out.println( "using hash mechanism " + hashMech.getName() );
            
            testAddWithPlainPassword();
            testModifyWithPlainPassword();
            testModifyWithEmptyPassword();
            testAddWithHashedPassword();
            testModifyWithHashedPassword();

            connection.delete( entry.getDn() );
        }
    }


    public void testAddWithPlainPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        byte[] plainPwd = "secret".getBytes( StandardCharsets.UTF_8 );
        Dn dn = new Dn( "cn=test,ou=system" );

        Entry entry = connection.lookup( dn );
        Attribute pwdAt = entry.get( SchemaConstants.USER_PASSWORD_AT );

        assertFalse( Arrays.equals( plainPwd, pwdAt.getBytes() ) );
        assertTrue( PasswordUtil.compareCredentials( plainPwd, pwdAt.getBytes() ) );
    }


    public void testModifyWithPlainPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        byte[] plainPwd = "newsecret".getBytes( StandardCharsets.UTF_8 );
        Dn dn = new Dn( "cn=test,ou=system" );

        AttributeType pwdAtType = getService().getSchemaManager().lookupAttributeTypeRegistry(
            SchemaConstants.USER_PASSWORD_AT );

        Attribute pwdAt = new DefaultAttribute( pwdAtType );
        pwdAt.add( plainPwd );

        Modification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, pwdAt );
        connection.modify( dn, mod );

        Entry entry = connection.lookup( dn );
        pwdAt = entry.get( pwdAtType );

        assertFalse( Arrays.equals( plainPwd, pwdAt.getBytes() ) );
        assertTrue( PasswordUtil.compareCredentials( plainPwd, pwdAt.getBytes() ) );
    }


    public void testModifyWithEmptyPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        Dn dn = new Dn( "cn=test,ou=system" );

        AttributeType pwdAtType = getService().getSchemaManager().lookupAttributeTypeRegistry(
            SchemaConstants.USER_PASSWORD_AT );

        Attribute pwdAt = new DefaultAttribute( pwdAtType );
        pwdAt.add( ( byte[] ) null );

        Modification mod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, pwdAt );
        connection.modify( dn, mod );

        Entry entry = connection.lookup( dn );
        pwdAt = entry.get( pwdAtType );

        assertNull( pwdAt );
    }


    public void testAddWithHashedPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        byte[] plainPwd = "secret".getBytes( StandardCharsets.UTF_8 );
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

        connection.delete( dn );
    }


    public void testModifyWithHashedPassword() throws Exception
    {
        LdapConnection connection = IntegrationUtils.getAdminConnection( getService() );

        byte[] plainPwd = "xyzsecret".getBytes( StandardCharsets.UTF_8 );
        byte[] hashedPwd = PasswordUtil.createStoragePassword( plainPwd, LdapSecurityConstants.HASH_METHOD_SSHA256 );

        Dn dn = new Dn( "cn=test,ou=system" );

        AttributeType pwdAtType = getService().getSchemaManager().lookupAttributeTypeRegistry(
            SchemaConstants.USER_PASSWORD_AT );

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
