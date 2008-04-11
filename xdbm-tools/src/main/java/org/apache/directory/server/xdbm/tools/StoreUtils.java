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


import java.util.Set;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
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
        
        
        dn = new LdapDN( "ou=Apache,ou=Board of Directors,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "organizationalUnit" );
        entry.add( "ou", "Apache" );
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );
        
        dn = new LdapDN( "cn=Jack Daniels,ou=Engineering,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "person", "organizationalPerson" );
        entry.add( "ou", "Engineering" );
        entry.add( "cn",  "Jack Daniels");
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );

        // aliases
        dn = new LdapDN( "commonName=Jim Bean,ou=Apache,ou=Board of Directors,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "alias", "extensibleObject" );
        entry.add( "ou", "Apache" );
        entry.add( "commonName",  "Jim Bean");
        entry.add( "aliasedObjectName", "cn=Jim Bean,ou=Sales,o=Good Times Co." );
        store.add( dn, ServerEntryUtils.toAttributesImpl( entry ) );

        dn = new LdapDN( "commonName=Jim Bean,ou=Board of Directors,o=Good Times Co." );
        dn.normalize( attributeRegistry.getNormalizerMapping() );
        entry = new DefaultServerEntry( registries, dn );
        entry.add( "objectClass", "top", "alias", "extensibleObject" );
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
    
    
    /**
     * This is primarily a convenience method used to extract all the attributes
     * associated with an entry.
     *
     * @param id the id of the entry to get index information for
     * @return the index names and values as an Attributes object
     * @throws Exception if there are failures accessing the underlying store
     */
    @SuppressWarnings("unchecked")
    public Attributes getAttributes( Store store, Long id ) throws Exception
    {
        Attributes attributes = new AttributesImpl();

        // Get the distinguishedName to id mapping
        attributes.put( "_nDn", store.getEntryDn( id ) );
        attributes.put( "_upDn", store.getEntryUpdn( id ) );
        attributes.put( "_parent", store.getParentId( id ) );

        // Get all standard index attribute to value mappings
        for ( Index index : ( Set<Index> )store.getUserIndices() )
        {
            Cursor<ForwardIndexEntry> list = index.reverseCursor();
            ForwardIndexEntry recordForward = new ForwardIndexEntry();
            recordForward.setId( id );
            list.before( recordForward );

            while ( list.next() )
            {
                IndexEntry rec = list.get();
                String val = rec.getValue().toString();
                String attrId = index.getAttribute().getName();
                Attribute attr = attributes.get( attrId );

                if ( attr == null )
                {
                    attr = new AttributeImpl( attrId );
                }
                
                attr.add( val );
                attributes.put( attr );
            }
        }

        // Get all existance mappings for this id creating a special key
        // that looks like so 'existance[attribute]' and the value is set to id
        Cursor<IndexEntry> list = store.getPresenceIndex().reverseCursor();
        ForwardIndexEntry recordForward = new ForwardIndexEntry();
        recordForward.setId( id );
        list.before( recordForward );
        StringBuffer val = new StringBuffer();
        
        while ( list.next() )
        {
            IndexEntry rec = list.get();
            val.append( "_existance[" );
            val.append( rec.getValue().toString() );
            val.append( "]" );

            String valStr = val.toString();
            Attribute attr = attributes.get( valStr );
            
            if ( attr == null )
            {
                attr = new AttributeImpl( valStr );
            }
            
            attr.add( rec.getId().toString() );
            attributes.put( attr );
            val.setLength( 0 );
        }

        // Get all parent child mappings for this entry as the parent using the
        // key 'child' with many entries following it.
        Cursor<IndexEntry> children = store.getOneLevelIndex().forwardCursor();
        ForwardIndexEntry longRecordForward = new ForwardIndexEntry();
        recordForward.setId( id );
        children.before( longRecordForward );

        Attribute childAttr = new AttributeImpl( "_child" );
        attributes.put( childAttr );
        
        while ( children.next() )
        {
            IndexEntry rec = children.get();
            childAttr.add( rec.getId().toString() );
        }

        return attributes;
    }

}
