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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.naming.NamingException;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.parsers.AttributeTypeDescriptionSchemaParser;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * An integration test class for testing persistence for various operations
 * on the subschemaSubentry with server restarts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "SchemaPersistenceIT-class")
public class SchemaPersistenceIT extends AbstractLdapTestUnit
{
    private static final String SUBSCHEMA_SUBENTRY = "subschemaSubentry";
    private static final AttributeTypeDescriptionSchemaParser ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER = new AttributeTypeDescriptionSchemaParser();
    public static SchemaManager schemaManager;
    ///private static LdapConnection connection;


    @BeforeEach
    public void setup() throws Exception
    {
        //connection = IntegrationUtils.getAdminConnection( getService() );
        schemaManager = getService().getSchemaManager();
    }


    /**
     * Tests to see if an attributeType is persisted when added, then server
     * is shutdown, then restarted again.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddAttributeTypePersistence() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            enableSchema( connection, "nis" );

            // -------------------------------------------------------------------
            // test successful add with everything
            // -------------------------------------------------------------------

            String[] descriptions = new String[] {
                "( 1.3.6.1.4.1.18060.0.4.1.2.10000 " +
                    "  NAME 'type0' " +
                    "  OBSOLETE SUP 2.5.4.41 " +
                    "  EQUALITY caseExactIA5Match " +
                    "  ORDERING octetStringOrderingMatch " +
                    "  SUBSTR caseExactIA5SubstringsMatch " +
                    "  COLLECTIVE " +
                    "  SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 " +
                    "  USAGE userApplications " +
                    "  X-SCHEMA 'nis' )",

                "( 1.3.6.1.4.1.18060.0.4.1.2.10001 " +
                    "  NAME ( 'type1' 'altName' ) " +
                    "  SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 " +
                    "  SUP 2.5.4.41 " +
                    "  USAGE userApplications " +
                    "  X-SCHEMA 'nis' )"
            };

            connection.modify( getSubschemaSubentryDN( connection ), new DefaultModification( ModificationOperation.ADD_ATTRIBUTE, "attributeTypes", descriptions ) );

            checkAttributeTypePresent( connection, "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", true );
            checkAttributeTypePresent( connection, "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", true );

            // sync operation happens anyway on shutdowns but just to make sure we can do it again
            getService().sync();
        }
        
        getService().shutdown();
        getService().startup();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            connection.add( 
                new DefaultEntry( 
                    "cn=blah,ou=schema", 
                    "objectClass", "metaSchema",
                    "cn", "blah" ) );

            checkAttributeTypePresent( connection, "1.3.6.1.4.1.18060.0.4.1.2.10000", "nis", true );
            checkAttributeTypePresent( connection, "1.3.6.1.4.1.18060.0.4.1.2.10001", "nis", true );
        }
        catch ( Exception e )
        {
            throw e;
        }
    }


    /**
     * Tests to see if we can create a schema with a mixed case name (see DIRSERVER-1718)
     *
     * @throws Exception on error
     */
    @Test
    public void testAddSchemaMixedCase() throws Exception
    {
        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            Dn dn = new Dn( "cn=DuMMy,ou=schema" );

            Entry dummySchema = new DefaultEntry(
                dn,
                "objectClass: top",
                "objectClass: metaSchema",
                "cn: DuMMy" );

            connection.add( dummySchema );

            assertNotNull( connection.lookup( "cn=dummy,ou=schema" ) );

            // sync operation happens anyway on shutdowns but just to make sure we can do it again
            getService().sync();
        }

        getService().shutdown();
        getService().startup();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            // Check that the schema still exists
            assertNotNull( connection.lookup( "cn=dummy,ou=schema" ) );

            // Now, delete the schema
            connection.delete( "cn=dummy,ou=schema" );

            assertNull( connection.lookup( "cn=dummy,ou=schema" ) );

            // sync operation happens anyway on shutdowns but just to make sure we can do it again
            getService().sync();
        }

        getService().shutdown();
        getService().startup();

        try ( LdapConnection connection = IntegrationUtils.getAdminConnection( getService() ) )
        {
            // Check that the schema does not exists
            assertNull( connection.lookup( "cn=dummy,ou=schema" ) );
        }
    }


    // -----------------------------------------------------------------------
    // Private Utility Methods
    // -----------------------------------------------------------------------
    private void enableSchema( LdapConnection connection, String schemaName ) throws Exception
    {
        // now enable the test schema
        connection.modify( "cn=" + schemaName + ",ou=schema", 
            new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "m-disabled", "FALSE" ) );
    }


    /**
     * Get's the subschemaSubentry attribute value from the rootDSE.
     *
     * @return the subschemaSubentry distinguished name
     * @throws NamingException if there are problems accessing the RootDSE
     */
    private String getSubschemaSubentryDN( LdapConnection conn ) throws Exception
    {
        Entry entry = conn.getRootDse( SUBSCHEMA_SUBENTRY );

        return entry.get( SUBSCHEMA_SUBENTRY ).getString();
    }


    /**
     * Gets the subschemaSubentry attributes for the global schema.
     *
     * @return all operational attributes of the subschemaSubentry
     * @throws NamingException if there are problems accessing this entry
     */
    private Entry getSubschemaSubentryAttributes( LdapConnection conn ) throws Exception
    {
        return conn.lookup( "cn=schema", "*", "+" );
    }


    private void checkAttributeTypePresent( LdapConnection conn, String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------

        Entry entry = getSubschemaSubentryAttributes( conn );
        Attribute attributeTypes = entry.get( "attributeTypes" );
        AttributeType attributeType = null;

        for ( Value value : attributeTypes )
        {
            String desc = value.getString();

            if ( desc.indexOf( oid ) != -1 )
            {
                attributeType = ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER.parse( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( attributeType );
            assertEquals( oid, attributeType.getOid() );
        }
        else
        {
            assertNull( attributeType );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            entry = conn.lookup( "ou=schema", "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
            assertNotNull( entry );
        }
        else
        {
            entry = conn.lookup( "ou=schema", "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
            
            assertNull( entry );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the attributeTypeRegistry
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getAttributeTypeRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getAttributeTypeRegistry().contains( oid ) );
        }
    }
}
