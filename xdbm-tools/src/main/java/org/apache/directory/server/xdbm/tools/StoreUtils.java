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
package org.apache.directory.server.xdbm.tools;


import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A utility class for loading example LDIF data.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StoreUtils
{

    /**
     * Initializes and loads a store with the example data shown in
     * <a href="http://cwiki.apache.org/confluence/display/DIRxSRVx11/Structure+and+Organization">
     * Structure and Organization</a>
     *
     * TODO might want to make this load an LDIF instead in the future
     * TODO correct size of spaces in user provided DN
     * 
     * @param store the store object to be initialized
     * @param registries oid registries
     * @throws Exception on access exceptions
     */
    public static void loadExampleData( Store store, Registries registries ) throws Exception
    {
        store.setSuffixDn( "o=Good Times Co." );
        
        DefaultServerEntry contextEntry = new DefaultServerEntry( registries, new LdapDN( "o=Good Times Co." ) );
        contextEntry.add( "objectClass", "organization" );
        store.setContextEntry( contextEntry );
        
        AttributeTypeRegistry attributeRegistry = registries.getAttributeTypeRegistry();
        
        store.init( registries.getOidRegistry(), attributeRegistry );
        
        LdapDN dn = new LdapDN( "ou=Sales,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        DefaultServerEntry entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Sales" );
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );

        dn = new LdapDN( "ou=Board of Directors,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Board of Directors" );
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );
        
        dn = new LdapDN( "ou=Engineering,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Engineering" );
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );
        
        dn = new LdapDN( "cn=JOhnny WAlkeR,ou=Sales,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Sales" );
        entry.add( "cn",  "JOhnny WAlkeR");
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );
        
        dn = new LdapDN( "cn=JIM BEAN,ou=Sales,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Sales" );
        entry.add( "cn",  "JIM BEAN");
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );
        
        dn = new LdapDN( "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Engineering" );
        entry.add( "cn",  "Jack Daniels");
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );

        // aliases
        dn = new LdapDN( "commonName=Jim Bean,ou=Board of Directors,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "alias", "extensibleObject" );
        entry.add( "ou", "Board of Directors" );
        entry.add( "commonName",  "Jim Bean");
        entry.add( "aliasedObjectName", "cn=Jim Bean,ou=Sales,o=Good Times Co." );
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );

        dn = new LdapDN( "2.5.4.3=Johnny Walker,ou=Engineering,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "alias", "extensibleObject" );
        entry.add( "ou", "Engineering" );
        entry.add( "2.5.4.3",  "Johnny Walker");
        entry.add( "aliasedObjectName", "cn=Johnny Walker,ou=Sales,o=Good Times Co." );
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );
    }
}
