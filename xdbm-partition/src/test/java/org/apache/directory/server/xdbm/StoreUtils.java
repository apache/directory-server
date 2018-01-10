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
package org.apache.directory.server.xdbm;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.MutableAttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.partition.Partition;


/**
 * A utility class for loading example LDIF data.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreUtils
{
    /** CSN factory instance */
    private static final CsnFactory CSN_FACTORY = new CsnFactory( 0 );

    public static final String TEST_INT_OID = "1.1.1.1.1.1";
    public static final String TEST_INT_DESCENDANT_OID = "1.1.1.1.1.1.1";
    public static final String TEST_INT_NO_INDEX_OID = "1.1.1.1.1.2";
    public static final String TEST_INT_DESCENDANT_NO_INDEX_OID = "1.1.1.1.1.2.1";

    /**
     * Create 4 attributeTypes that have an ORDERING MatchingRule. 2 of them
     * will be indexed :
     * 
     * <pre>
     * testInt : indexed
     *   ^
     *   |
     *   +-- testIntDescendant : indexed, with testInt being the SUPERIOR
     *   
     * testIntNoIndex : not indexed
     *   ^
     *   |
     *   +-- testIntDescendantNoIndex : not indexed, with testIntNoIndex being the SUPERIOR
     * </pre>
     * 
     * @param schemaManager
     * @throws LdapException
     */
    public static void createdExtraAttributes( SchemaManager schemaManager ) throws LdapException
    {
        createTestInt( schemaManager );
        createTestIntDescendant( schemaManager );
        createTestIntNoIndex( schemaManager );
        createTestIntDescendantNoIndex( schemaManager );
    }

    private static void createTestInt( SchemaManager schemaManager ) throws LdapException
    {
        MutableAttributeType attributeType = new MutableAttributeType( TEST_INT_OID );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.27" );
        attributeType.setNames( "testInt" );
        attributeType.setEqualityOid( "2.5.13.14" );
        attributeType.setOrderingOid( "2.5.13.15" );
        attributeType.setSubstringOid( null );
        attributeType.setEnabled( true );

        // Add the AttributeType
        schemaManager.add( attributeType );
    }
    

    private static void createTestIntDescendant( SchemaManager schemaManager ) throws LdapException
    {
        MutableAttributeType attributeType = new MutableAttributeType( TEST_INT_DESCENDANT_OID );
        attributeType.setNames( "testIntDescendant" );
        attributeType.setSuperior( schemaManager.getAttributeType( TEST_INT_OID ) );
        attributeType.setEnabled( true );

        // Add the AttributeType
        schemaManager.add( attributeType );
    }


    private static void createTestIntNoIndex( SchemaManager schemaManager ) throws LdapException
    {
        MutableAttributeType attributeType = new MutableAttributeType( TEST_INT_NO_INDEX_OID );
        attributeType.setSyntaxOid( "1.3.6.1.4.1.1466.115.121.1.27" );
        attributeType.setNames( "testIntNoIndex" );
        attributeType.setEqualityOid( "2.5.13.14" );
        attributeType.setOrderingOid( "2.5.13.15" );
        attributeType.setSubstringOid( null );
        attributeType.setEnabled( true );

        // Add the AttributeType
        schemaManager.add( attributeType );
    }
    

    private static void createTestIntDescendantNoIndex( SchemaManager schemaManager ) throws LdapException
    {
        MutableAttributeType attributeType = new MutableAttributeType( TEST_INT_DESCENDANT_NO_INDEX_OID );
        attributeType.setNames( "testIntDescendantNoIndex" );
        attributeType.setSuperior( schemaManager.getAttributeType( TEST_INT_NO_INDEX_OID ) );
        attributeType.setEnabled( true );

        // Add the AttributeType
        schemaManager.add( attributeType );
    }

    
    /**
     * Initializes and loads a store with the example data shown in
     * <a href="http://cwiki.apache.org/confluence/display/DIRxSRVx11/Structure+and+Organization">
     * Structure and Organization</a>
     *
     * TODO might want to make this load an LDIF instead in the future
     * TODO correct size of spaces in user provided Dn
     * 
     * @param store the store object to be initialized
     * @param registries oid registries
     * @throws Exception on access exceptions
     */
    public static void loadExampleData( Store store, SchemaManager schemaManager ) throws Exception
    {
        Dn suffixDn = new Dn( schemaManager, "o=Good Times Co." );
        long index = 1L;

        // Entry #1
        Entry entry = new DefaultEntry( schemaManager, suffixDn,
            "objectClass: organization",
            "o: Good Times Co.",
            "postalCode: 1",
            "testInt: 1",
            "testIntNoIndex: 1" );
        injectEntryInStore( store, entry, index++ );

        // Entry #2
        Dn dn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: Sales",
            "postalCode: 1",
            "testInt: 1",
            "testIntNoIndex: 1" );
        injectEntryInStore( store, entry, index++ );

        // Entry #3
        dn = new Dn( schemaManager, "ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: Board of Directors",
            "postalCode: 1",
            "testInt: 1",
            "testIntNoIndex: 1" );
        injectEntryInStore( store, entry, index++ );

        // Entry #4
        dn = new Dn( schemaManager, "ou=Engineering,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: Engineering",
            "postalCode: 2",
            "testInt: 2",
            "testIntNoIndex: 2" );
        injectEntryInStore( store, entry, index++ );

        // Entry #5
        dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "ou: Sales",
            "cn: JOhnny WAlkeR",
            "sn: WAlkeR",
            "postalCode: 3",
            "testInt: 3",
            "testIntNoIndex: 3" );
        injectEntryInStore( store, entry, index++ );

        // Entry #6
        dn = new Dn( schemaManager, "cn=JIM BEAN,ou=Sales,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "ou: Sales",
            "cn: JIM BEAN",
            "surName: BEAN",
            "postalCode: 4",
            "testInt: 4",
            "testIntNoIndex: 4" );
        injectEntryInStore( store, entry, index++ );

        // Entry #7
        dn = new Dn( schemaManager, "ou=Apache,ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: Apache",
            "postalCode: 5",
            "testInt: 5",
            "testIntNoIndex: 5" );
        injectEntryInStore( store, entry, index++ );

        // Entry #8
        dn = new Dn( schemaManager, "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "ou: Engineering",
            "cn: Jack Daniels",
            "SN: Daniels",
            "postalCode: 6",
            "testInt: 6",
            "testIntNoIndex: 6" );
        injectEntryInStore( store, entry, index++ );

        // aliases -------------

        // Entry #9
        dn = new Dn( schemaManager, "commonName=Jim Bean,ou=Apache,ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: alias",
            "objectClass: extensibleObject",
            "ou: Apache",
            "commonName: Jim Bean",
            "aliasedObjectName: cn=Jim Bean,ou=Sales,o=Good Times Co." );
        injectEntryInStore( store, entry, index++ );

        // Entry #10
        dn = new Dn( schemaManager, "commonName=Jim Bean,ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: alias",
            "objectClass: extensibleObject",
            "commonName: Jim Bean",
            "aliasedObjectName: cn=Jim Bean,ou=Sales,o=Good Times Co." );
        injectEntryInStore( store, entry, index++ );

        // Entry #11
        dn = new Dn( schemaManager, "2.5.4.3=Johnny Walker,ou=Engineering,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: alias",
            "objectClass: extensibleObject",
            "ou: Engineering",
            "2.5.4.3: Johnny Walker",
            "aliasedObjectName: cn=Johnny Walker,ou=Sales,o=Good Times Co." );
        injectEntryInStore( store, entry, index++ );
    }


    /**
     * 
     * adds a given <i>ServerEntry</i> to the store after injecting entryCSN and entryUUID operational
     * attributes
     *
     * @param store the store
     * @param dn the normalized Dn
     * @param entry the server entry
     * @param index the UUID number
     * @throws Exception in case of any problems in adding the entry to the store
     */
    public static void injectEntryInStore( Store store, Entry entry, long index ) throws Exception
    {
        entry.add( SchemaConstants.ENTRY_CSN_AT, CSN_FACTORY.newInstance().toString() );
        entry.add( SchemaConstants.ENTRY_UUID_AT, Strings.getUUID( index ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        ( ( Partition ) store ).add( addContext );
    }
}
