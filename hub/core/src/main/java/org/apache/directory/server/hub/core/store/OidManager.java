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

package org.apache.directory.server.hub.core.store;


import java.util.Hashtable;

import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.hub.api.exception.StoreNotValidException;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;


public class OidManager
{

    private Hashtable<String, Integer> metaOCBases = new Hashtable<String, Integer>();
    private Integer maxOCOIDBase = 0;

    private Hashtable<String, Integer> metaAttributeBases = new Hashtable<String, Integer>();
    private Integer maxAttributeOIDBase = 0;


    public void init( SchemaPartition schemaPartition ) throws StoreNotValidException
    {
        metaOCBases.clear();
        metaAttributeBases.clear();

        EntryFilteringCursor cursor = null;

        try
        {
            AttributeType oidAttrib = schemaPartition.getSchemaManager().getAttributeType( "m-oid" );
            AttributeType nameAttrib = schemaPartition.getSchemaManager().getAttributeType( "m-name" );

            SearchOperationContext soc = new SearchOperationContext( null );
            soc.setScope( SearchScope.SUBTREE );
            AttributeType moidat = schemaPartition.getSchemaManager().getAttributeType( "m-oid" );
            soc.setFilter( new PresenceNode( moidat ) );
            // Set to all/none, because individual names will require CoreSession in OperationContext which we don't have.
            soc.setReturningAttributes( new String[]
                { SchemaConstants.ALL_USER_ATTRIBUTES } );

            Dn ocsDn = new Dn( schemaPartition.getSchemaManager(),
                SchemaConstants.OBJECT_CLASSES_PATH, StoreSchemaManager.METADATAS_SCHEMA_BASE
                , SchemaConstants.OU_SCHEMA );

            soc.setDn( ocsDn );

            try
            {
                cursor = schemaPartition.search( soc );

                int baseOCOIDLen = StoreSchemaManager.OID_BASE.length() + ".2".length();

                while ( cursor.next() )
                {
                    Entry currentEntry = cursor.get();

                    Attribute oid = currentEntry.get( oidAttrib );
                    Attribute name = currentEntry.get( nameAttrib );

                    String _ocBase = oid.getString().substring( baseOCOIDLen + 1 );
                    Integer ocBase = Integer.parseInt( _ocBase );

                    metaOCBases.put( name.getString(), ocBase );

                    maxOCOIDBase = ( ocBase > maxOCOIDBase ) ? ocBase : maxOCOIDBase;
                }
            }
            catch ( LdapNoSuchObjectException e )
            {
                // No OC installed yet.
            }

            Dn attribsDn = new Dn( schemaPartition.getSchemaManager(),
                SchemaConstants.ATTRIBUTE_TYPES_PATH, StoreSchemaManager.METADATAS_SCHEMA_BASE
                , SchemaConstants.OU_SCHEMA );

            soc.setDn( attribsDn );

            try
            {
                cursor = schemaPartition.search( soc );

                int baseAttribOIDLen = StoreSchemaManager.OID_BASE.length() + ".1".length();

                while ( cursor.next() )
                {
                    Entry currentEntry = cursor.get();

                    Attribute oid = currentEntry.get( oidAttrib );
                    Attribute name = currentEntry.get( nameAttrib );

                    String _attribBase = oid.getString().substring( baseAttribOIDLen + 1 );
                    Integer attribBase = Integer.parseInt( _attribBase );

                    metaAttributeBases.put( name.getString(), attribBase );

                    maxAttributeOIDBase = ( attribBase > maxAttributeOIDBase ) ? attribBase : maxAttributeOIDBase;
                }
            }
            catch ( LdapNoSuchObjectException e )
            {
                // No attrib installed yet
            }
        }
        catch ( Exception e )
        {
            throw new StoreNotValidException( "SchemaPartition couldn't be parsed for OID assignments", e );
        }
        finally
        {
            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        }
    }


    public String getAttributeOID( String propertyName )
    {
        Integer attributeBase = metaAttributeBases.get( propertyName );
        if ( attributeBase == null )
        {
            return null;
        }

        return StoreSchemaManager.OID_BASE + ".1." + attributeBase;
    }


    public String getOCBase( String metaPID )
    {
        Integer ocBase = metaOCBases.get( metaPID );
        if ( ocBase == null )
        {
            return null;
        }

        return StoreSchemaManager.OID_BASE + ".2." + ocBase;
    }


    public String generateNewOCOID( String metaPID )
    {
        Integer ocBase = ( ++maxOCOIDBase );

        metaOCBases.put( metaPID, ocBase );

        return StoreSchemaManager.OID_BASE + ".2." + ocBase;
    }


    public String generateNewAttributeOID( String propertyName )
    {
        Integer attribBase = ( ++maxAttributeOIDBase );

        metaAttributeBases.put( propertyName, attribBase );

        return StoreSchemaManager.OID_BASE + ".1." + attribBase;
    }
}
