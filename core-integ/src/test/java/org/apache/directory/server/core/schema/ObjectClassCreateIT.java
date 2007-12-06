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
package org.apache.directory.server.core.schema;


import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.SetupMode;
import org.apache.directory.server.core.integ.annotations.Mode;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import org.apache.directory.shared.ldap.name.LdapDN;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;


@RunWith ( CiRunner.class )
@Mode ( SetupMode.PRISTINE )
public class ObjectClassCreateIT
{
    private String testOID = "1.3.6.1.4.1.18060.0.4.0.3.1.555555.5555.5555555";


    public static DirectoryService service;


    /**
     * Gets relative DN to ou=schema.
     *
     * @param schemaName the name of the schema
     * @return the dn of the objectClass container
     * @throws NamingException on error
     */
    private LdapDN getObjectClassContainer( String schemaName ) throws NamingException
    {
        return new LdapDN( "ou=objectClasses,cn=" + schemaName );
    }


    /*
     * Test that I can create an ObjectClass entry with an invalid
     */
    @Test
    public void testCreateObjectClassWithInvalidNameAttribute() throws NamingException
    {
        Attributes attributes = new BasicAttributes();
        Attribute  objectClassAttribute = new BasicAttribute( "objectClass" );
        
        objectClassAttribute.add( "top" );
        objectClassAttribute.add( "metaTop" );
        objectClassAttribute.add( "metaObjectClass" );
        
        attributes.put( objectClassAttribute );
        
        attributes.put( "m-oid", "testOID" );
        
        // This name is invalid
        attributes.put( "m-name", "http://example.com/users/accounts/L0" );
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + testOID );
        
        try
        {
            getSchemaContext( service ).createSubcontext( dn, attributes );
            fail(); // Should not reach this point
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }

    /*
     * Test that I can create an ObjectClass entry with an invalid
     */
    @Test
    public void testCreateObjectClassWithNoObjectClass() throws NamingException
    {
        Attributes attributes = new BasicAttributes();
        Attribute  objectClassAttribute = new BasicAttribute( "objectClass" );
        
        objectClassAttribute.add( "top" );
        objectClassAttribute.add( "metaTop" );
        objectClassAttribute.add( "metaObjectClass" );
        
        // Don't put the objectclasses in the entry : this is in purpose !
        // attributes.put( objectClassAttribute );
        
        attributes.put( "m-oid", "testOID" );
        
        // This name is invalid
        attributes.put( "m-name", "no-objectClasses" );
        
        LdapDN dn = getObjectClassContainer( "apachemeta" );
        dn.add( MetaSchemaConstants.M_OID_AT + "=" + testOID );
        
        try
        {
            getSchemaContext( service ).createSubcontext( dn, attributes );
            fail(); // Should not reach this point
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }
}
