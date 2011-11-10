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
import org.apache.directory.shared.util.Strings;


/**
 * A utility class for loading example LDIF data.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class XdbmStoreUtils
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
    public static void loadExampleData( Store store, SchemaManager schemaManager ) throws Exception
    {
        int idx = 1;
        Dn suffixDn = new Dn( schemaManager, "o=Good Times Co." );

        // Entry #1
        Entry entry = new DefaultEntry( schemaManager, suffixDn );
        entry.add( "objectClass", "organization" );
        entry.add( "o", "Good Times Co." );
        entry.add( "postalCode", "1" );
        entry.add( "postOfficeBox", "1" );
        injectEntryInStore( store, entry, idx++ );

        // Entry #2
        Dn dn = new Dn( schemaManager, "ou=Sales,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Sales" );
        entry.add( "postalCode", "1" );
        entry.add( "postOfficeBox", "1" );
        injectEntryInStore( store, entry, idx++ );

        // Entry #3
        dn = new Dn( schemaManager, "ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Board of Directors" );
        entry.add( "postalCode", "1" );
        entry.add( "postOfficeBox", "1" );
        injectEntryInStore( store, entry, idx++ );

        // Entry #4
        dn = new Dn( schemaManager, "ou=Engineering,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Engineering" );
        entry.add( "postalCode", "2" );
        entry.add( "postOfficeBox", "2" );
        injectEntryInStore( store, entry, idx++ );

        // Entry #5
        dn = new Dn( schemaManager, "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Sales" );
        entry.add( "cn", "JOhnny WAlkeR" );
        entry.add( "sn", "WAlkeR" );
        entry.add( "postalCode", "3" );
        entry.add( "postOfficeBox", "3" );
        injectEntryInStore( store, entry, idx++ );

        // Entry #6
        dn = new Dn( schemaManager, "cn=JIM BEAN,ou=Sales,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Sales" );
        entry.add( "cn", "JIM BEAN" );
        entry.add( "surName", "BEAN" );
        entry.add( "postalCode", "4" );
        entry.add( "postOfficeBox", "4" );
        injectEntryInStore( store, entry, idx++ );

        // Entry #7
        dn = new Dn( schemaManager, "ou=Apache,ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Apache" );
        entry.add( "postalCode", "5" );
        entry.add( "postOfficeBox", "5" );
        injectEntryInStore( store, entry, idx++ );

        // Entry #8
        dn = new Dn( schemaManager, "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Engineering" );
        entry.add( "cn", "Jack Daniels" );
        entry.add( "SN", "Daniels" );
        entry.add( "postalCode", "6" );
        entry.add( "postOfficeBox", "6" );
        injectEntryInStore( store, entry, idx++ );

        // aliases -------------

        // Entry #9
        dn = new Dn( schemaManager, "commonName=Jim Bean,ou=Apache,ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "alias", "extensibleObject" );
        entry.add( "ou", "Apache" );
        entry.add( "commonName", "Jim Bean" );
        entry.add( "aliasedObjectName", "cn=Jim Bean,ou=Sales,o=Good Times Co." );
        injectEntryInStore( store, entry, idx++ );

        // Entry #10
        dn = new Dn( schemaManager, "commonName=Jim Bean,ou=Board of Directors,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "alias", "extensibleObject" );
        entry.add( "commonName", "Jim Bean" );
        entry.add( "aliasedObjectName", "cn=Jim Bean,ou=Sales,o=Good Times Co." );
        injectEntryInStore( store, entry, idx++ );

        // Entry #11
        dn = new Dn( schemaManager, "2.5.4.3=Johnny Walker,ou=Engineering,o=Good Times Co." );
        entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "top", "alias", "extensibleObject" );
        entry.add( "ou", "Engineering" );
        entry.add( "2.5.4.3", "Johnny Walker" );
        entry.add( "aliasedObjectName", "cn=Johnny Walker,ou=Sales,o=Good Times Co." );
        injectEntryInStore( store, entry, idx++ );
    }
    
    
    /**
     * 
     * adds a given <i>ServerEntry</i> to the store after injecting entryCSN and entryUUID operational
     * attributes
     *
     * @param store the store
     * @param dn the normalized Dn
     * @param entry the server entry
     * @param idx index used to build the entry uuid
     * @throws Exception in case of any problems in adding the entry to the store
     */
    public static void injectEntryInStore( Store store, Entry entry, int idx ) throws Exception
    {
        entry.add( SchemaConstants.ENTRY_CSN_AT, CSN_FACTORY.newInstance().toString() );
        entry.add( SchemaConstants.ENTRY_UUID_AT, Strings.getUUIDString( idx ).toString() );

        AddOperationContext addContext = new AddOperationContext( null, entry );
        ((Partition)store).add( addContext );
    }
}
