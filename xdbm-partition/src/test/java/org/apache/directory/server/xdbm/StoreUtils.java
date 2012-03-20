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


import java.util.UUID;

import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.csn.CsnFactory;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * A utility class for loading example LDIF data.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreUtils
{
    /** CSN factory instance */
    private static final CsnFactory CSN_FACTORY = new CsnFactory( 0 );


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
    //This will suppress PMD.AvoidUsingHardCodedIP warnings in this class
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static void loadExampleData( Store<Entry, Long> store, SchemaManager schemaManager ) throws Exception
    {
        Dn suffixDn = new Dn( schemaManager, "o=Good Times Co." );

        // Entry #1
        Entry entry = new DefaultEntry( schemaManager, suffixDn,
            "objectClass: organization",
            "o: Good Times Co.",
            "postalCode: 1",
            "postOfficeBox: 1" );
        injectEntryInStore( store, entry );

        // Entry #2
        Dn dn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: Sales",
            "postalCode: 1",
            "postOfficeBox: 1" );
        injectEntryInStore( store, entry );

        // Entry #3
        dn = new Dn( schemaManager, "ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: Board of Directors",
            "postalCode: 1",
            "postOfficeBox: 1" );
        injectEntryInStore( store, entry );

        // Entry #4
        dn = new Dn( schemaManager, "ou=Engineering,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: Engineering",
            "postalCode: 2",
            "postOfficeBox: 2" );
        injectEntryInStore( store, entry );

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
            "postOfficeBox: 3" );
        injectEntryInStore( store, entry );

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
            "postOfficeBox: 4" );
        injectEntryInStore( store, entry );

        // Entry #7
        dn = new Dn( schemaManager, "ou=Apache,ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: organizationalUnit",
            "ou: Apache",
            "postalCode: 5",
            "postOfficeBox: 5" );
        injectEntryInStore( store, entry );

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
            "postOfficeBox: 6" );
        injectEntryInStore( store, entry );

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
        injectEntryInStore( store, entry );

        // Entry #10
        dn = new Dn( schemaManager, "commonName=Jim Bean,ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: alias",
            "objectClass: extensibleObject",
            "commonName: Jim Bean",
            "aliasedObjectName: cn=Jim Bean,ou=Sales,o=Good Times Co." );
        injectEntryInStore( store, entry );

        // Entry #11
        dn = new Dn( schemaManager, "2.5.4.3=Johnny Walker,ou=Engineering,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn,
            "objectClass: top",
            "objectClass: alias",
            "objectClass: extensibleObject",
            "ou: Engineering",
            "2.5.4.3: Johnny Walker",
            "aliasedObjectName: cn=Johnny Walker,ou=Sales,o=Good Times Co." );
        injectEntryInStore( store, entry );
    }


    /**
     * 
     * adds a given <i>ServerEntry</i> to the store after injecting entryCSN and entryUUID operational
     * attributes
     *
     * @param store the store
     * @param dn the normalized Dn
     * @param entry the server entry
     * @throws Exception in case of any problems in adding the entry to the store
     */
    public static void injectEntryInStore( Store<Entry, Long> store, Entry entry ) throws Exception
    {
        entry.add( SchemaConstants.ENTRY_CSN_AT, CSN_FACTORY.newInstance().toString() );
        entry.add( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        ( ( Partition ) store ).add( addContext );
    }
}
