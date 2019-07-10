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
package org.apache.directory.server.schema;


import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSchemaContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.ObjectClass;
import org.apache.directory.api.ldap.model.schema.parsers.AttributeTypeDescriptionSchemaParser;
import org.apache.directory.api.ldap.model.schema.parsers.ObjectClassDescriptionSchemaParser;
import org.apache.directory.api.ldap.util.JndiUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotIndex;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotPartition;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * An integration test class for testing the addition of schema elements
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "mavibot-class",
    partitions =
        {
            @CreatePartition(
                type = MavibotPartition.class,
                cacheSize = 12000,
                name = "test",
                suffix = "ou=test",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: ou=test\n" +
                        "ou: territoryOwners\n" +
                        "objectClass: top\n" +
                        "objectClass: organizationalUnit\n\n"),
                indexes =
                    {
                        @CreateIndex(type = MavibotIndex.class, attribute = "objectClass", cacheSize = 2000),
                        @CreateIndex(type = MavibotIndex.class, attribute = "sn", cacheSize = 2000),
                        @CreateIndex(type = MavibotIndex.class, attribute = "cn", cacheSize = 2000),
                        @CreateIndex(type = MavibotIndex.class, attribute = "displayName", cacheSize = 2000)
                })
    },
    enableChangeLog = false)
@CreateLdapServer(transports =
    { @CreateTransport(port = -1, protocol = "LDAP") })
public class MavibotSchemaIT extends AbstractLdapTestUnit
{
    private static final String SUBSCHEMA_SUBENTRY = "subschemaSubentry";
    private static final AttributeTypeDescriptionSchemaParser ATTRIBUTE_TYPE_DESCRIPTION_SCHEMA_PARSER = new AttributeTypeDescriptionSchemaParser();
    private static final ObjectClassDescriptionSchemaParser OBJECT_CLASS_DESCRIPTION_SCHEMA_PARSER = new ObjectClassDescriptionSchemaParser();


    /**
     * Tests to see if an attributeType is persisted when added, then server
     * is shutdown, then restarted again.
     *
     * @throws Exception on error
     */
    @Test
    @CreateDS(name = "SchemaAddAT-test")
    @ApplyLdifs(
        {
            // Inject an AT
            "dn: m-oid=1.3.6.1.4.1.18060.0.4.1.2.999,ou=attributeTypes,cn=other,ou=schema",
            "m-usage: USER_APPLICATIONS",
            "m-equality: integerOrderingMatch",
            "objectClass: metaAttributeType",
            "objectClass: metaTop",
            "objectClass: top",
            "m-name: numberOfGuns",
            "m-oid: 1.3.6.1.4.1.18060.0.4.1.2.999",
            "m-singleValue: TRUE",
            "m-description: Number of guns of a ship",
            "m-collective: FALSE",
            "m-obsolete: FALSE",
            "m-noUserModification: FALSE",
            "m-syntax: 1.3.6.1.4.1.1466.115.121.1.27",

            // Inject an OC
            "dn: m-oid=1.3.6.1.4.1.18060.0.4.1.1.999,ou=objectClasses,cn=other,ou=schema",
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaObjectclass",
            "m-supObjectClass: top",
            "m-oid: 1.3.6.1.4.1.18060.0.4.1.2.999",
            "m-name: ship",
            "m-must: cn",
            "m-may: numberOfGuns",
            "m-may: description",
            "m-typeObjectClass: STRUCTURAL",
            "m-obsolete: FALSE",
            "m-description: A ship"
    }
        )
        public void testAddAttributeTypeObjectClass() throws Exception
    {
        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.999", "other", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.1.999", "other", true );

        // sync operation happens anyway on shutdowns but just to make sure we can do it again
        getService().sync();

        getService().shutdown();
        getService().startup();

        checkAttributeTypePresent( "1.3.6.1.4.1.18060.0.4.1.2.999", "other", true );
        checkObjectClassPresent( "1.3.6.1.4.1.18060.0.4.1.1.999", "other", true );
    }


    @Test
    @CreateDS(name = "SchemaAddAT-test")
    @ApplyLdifs(
        {
            // Inject an AT
            "dn: cn=schema",
            "changetype: modify",
            "add: attributeTypes",
            "attributeTypes: ( 1.3.6.1.4.1.65536.0.4.3.2.1 NAME 'templateData' DESC 'template data' SYNTAX 1.3.6.1.4.1.1466.115.121.1.5 SINGLE-VALUE X-SCHEMA 'other' )",
            "-",

            // Inject an OC
            "dn: cn=schema",
            "changetype: modify",
            "add: objectClasses",
            "objectClasses: ( 1.3.6.1.4.1.65536.0.4.3.2.2 NAME 'templateObject' DESC 'test OC' SUP top STRUCTURAL MUST ( templateData $ cn ) X-SCHEMA 'other' )",
            "-"
    }
        )
        public void testAddAttributeTypeObjectClassSubSchemaSubEntry() throws Exception
    {
        checkAttributeTypePresent( "1.3.6.1.4.1.65536.0.4.3.2.1", "other", true );
        checkObjectClassPresent( "1.3.6.1.4.1.65536.0.4.3.2.2", "other", true );

        // sync operation happens anyway on shutdowns but just to make sure we can do it again
        getService().sync();

        getService().shutdown();
        getService().startup();

        checkAttributeTypePresent( "1.3.6.1.4.1.65536.0.4.3.2.1", "other", true );
        checkObjectClassPresent( "1.3.6.1.4.1.65536.0.4.3.2.2", "other", true );
    }


    @Test
    public void testAddBinaryAttributeType() throws Exception
    {
        List<String> descriptions = new ArrayList<String>();

        // -------------------------------------------------------------------
        // test successful add with everything
        // -------------------------------------------------------------------

        descriptions.add(
            "( 1.3.6.1.4.1.65536.0.4.3.2.1" +
                " NAME 'templateData'" +
                " DESC 'template data'" +
                " SYNTAX 1.3.6.1.4.1.1466.115.121.1.5" +
                " SINGLE-VALUE" +
                " X-SCHEMA 'other' )" );

        modify( DirContext.ADD_ATTRIBUTE, descriptions, "attributeTypes" );

        descriptions.clear();
        descriptions.add(
            "( 1.3.6.1.4.1.65536.0.4.3.2.2 " +
                " NAME 'templateObject' " +
                " DESC 'test OC' " +
                " SUP top " +
                " STRUCTURAL " +
                " MUST ( templateData $ cn ) " +
                " X-SCHEMA 'other' )" );

        modify( DirContext.ADD_ATTRIBUTE, descriptions, "objectClasses" );

        checkAttributeTypePresent( "1.3.6.1.4.1.65536.0.4.3.2.1", "other", true );
        checkObjectClassPresent( "1.3.6.1.4.1.65536.0.4.3.2.2", "other", true );

        // sync operation happens anyway on shutdowns but just to make sure we can do it again
        getService().sync();

        getService().shutdown();
        getService().startup();

        checkAttributeTypePresent( "1.3.6.1.4.1.65536.0.4.3.2.1", "other", true );
        checkObjectClassPresent( "1.3.6.1.4.1.65536.0.4.3.2.2", "other", true );

        Attributes attrs = new BasicAttributes();
        BasicAttribute ocattr = new BasicAttribute( "objectclass" );
        ocattr.add( "top" );
        ocattr.add( "templateObject" );
        attrs.put( ocattr );
        byte[] templateData = new byte[4096];
        attrs.put( "templateData", templateData );
        attrs.put( "cn", "atemplate" );
        getRootContext( getService() ).bind( "cn=atemplate,ou=system", null, attrs );

        Attributes data = getRootContext( getService() ).getAttributes( "cn=atemplate,ou=system", new String[]
            { "templateData", "cn" } );

        assertTrue( Arrays.equals( templateData, ( byte[] ) data.get( "templateData" ).get() ) );
    }


    // -----------------------------------------------------------------------
    // Private Utility Methods
    // -----------------------------------------------------------------------

    private void modify( int op, List<String> descriptions, String opAttr ) throws Exception
    {
        Dn dn = new Dn( getSubschemaSubentryDN() );
        Attribute attr = new BasicAttribute( opAttr );

        for ( String description : descriptions )
        {
            attr.add( description );
        }

        Attributes mods = new BasicAttributes( true );
        mods.put( attr );

        getRootContext( getService() ).modifyAttributes( JndiUtils.toName( dn ), op, mods );
    }


    /**
     * Get's the subschemaSubentry attribute value from the rootDSE.
     *
     * @return the subschemaSubentry distinguished name
     * @throws NamingException if there are problems accessing the RootDSE
     */
    private String getSubschemaSubentryDN() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { SUBSCHEMA_SUBENTRY } );

        NamingEnumeration<SearchResult> results = getRootContext( getService() ).search( "", "(objectClass=*)",
            controls );
        SearchResult result = results.next();
        results.close();
        Attribute subschemaSubentry = result.getAttributes().get( SUBSCHEMA_SUBENTRY );
        return ( String ) subschemaSubentry.get();
    }


    /**
     * Gets the subschemaSubentry attributes for the global schema.
     *
     * @return all operational attributes of the subschemaSubentry
     * @throws NamingException if there are problems accessing this entry
     */
    private Attributes getSubschemaSubentryAttributes() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );

        NamingEnumeration<SearchResult> results = getRootContext( getService() ).search( getSubschemaSubentryDN(),
            "(objectClass=*)", controls );
        SearchResult result = results.next();
        results.close();
        return result.getAttributes();
    }


    private void checkAttributeTypePresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------

        Attributes attrs = getSubschemaSubentryAttributes();
        Attribute attrTypes = attrs.get( "attributeTypes" );
        AttributeType attributeType = null;

        for ( int i = 0; i < attrTypes.size(); i++ )
        {
            String desc = ( String ) attrTypes.get( i );

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

        attrs = null;

        if ( isPresent )
        {
            attrs = getSchemaContext( getService() ).getAttributes(
                "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
            assertNotNull( attrs );
        }
        else
        {
            //noinspection EmptyCatchBlock
            try
            {
                attrs = getSchemaContext( getService() ).getAttributes(
                    "m-oid=" + oid + ",ou=attributeTypes,cn=" + schemaName );
                fail( "should never get here" );
            }
            catch ( NamingException e )
            {
            }
            assertNull( attrs );
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


    private void checkObjectClassPresent( String oid, String schemaName, boolean isPresent ) throws Exception
    {
        // -------------------------------------------------------------------
        // check first to see if it is present in the subschemaSubentry
        // -------------------------------------------------------------------

        Attributes attrs = getSubschemaSubentryAttributes();
        Attribute attrTypes = attrs.get( "objectClasses" );
        ObjectClass objectClass = null;

        for ( int i = 0; i < attrTypes.size(); i++ )
        {
            String desc = ( String ) attrTypes.get( i );

            if ( desc.indexOf( oid ) != -1 )
            {
                objectClass = OBJECT_CLASS_DESCRIPTION_SCHEMA_PARSER.parse( desc );
                break;
            }
        }

        if ( isPresent )
        {
            assertNotNull( objectClass );
            assertEquals( oid, objectClass.getOid() );
        }
        else
        {
            assertNull( objectClass );
        }

        // -------------------------------------------------------------------
        // check next to see if it is present in the schema partition
        // -------------------------------------------------------------------

        attrs = null;

        if ( isPresent )
        {
            attrs = getSchemaContext( getService() ).getAttributes(
                "m-oid=" + oid + ",ou=objectClasses,cn=" + schemaName );
            assertNotNull( attrs );
        }
        else
        {
            //noinspection EmptyCatchBlock
            try
            {
                attrs = getSchemaContext( getService() ).getAttributes(
                    "m-oid=" + oid + ",ou=objectClasses,cn=" + schemaName );
                fail( "should never get here" );
            }
            catch ( NamingException e )
            {
            }
            assertNull( attrs );
        }

        // -------------------------------------------------------------------
        // check to see if it is present in the objectClassRegistry
        // -------------------------------------------------------------------

        if ( isPresent )
        {
            assertTrue( getService().getSchemaManager().getObjectClassRegistry().contains( oid ) );
        }
        else
        {
            assertFalse( getService().getSchemaManager().getObjectClassRegistry().contains( oid ) );
        }
    }
}
