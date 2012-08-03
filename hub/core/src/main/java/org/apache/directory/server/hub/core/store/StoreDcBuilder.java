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


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.directory.server.hub.api.component.DcConfiguration;
import org.apache.directory.server.hub.api.component.DcProperty;
import org.apache.directory.server.hub.api.component.DirectoryComponent;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.ObjectClass;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


public class StoreDcBuilder
{
    private SchemaManager schemaManager;


    public StoreDcBuilder( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    public DirectoryComponent buildComponentFromEntry( Entry componentEntry ) throws LdapException
    {
        String componentName = null;
        String managerPID = null;
        Integer collectionIndex = null;

        Attribute ocAttrib = componentEntry.get( schemaManager.getAttributeType( "objectclass" ) );
        for ( Value<?> val : ocAttrib )
        {
            String ocName = val.getString();
            String ocOID = schemaManager.getObjectClassRegistry().getOidByName( ocName );

            ObjectClass oc = schemaManager.getObjectClassRegistry().get( ocOID );

            if ( oc.isStructural() )
            {
                managerPID = ocName;
            }

            if ( oc.isAuxiliary() )
            {
                if ( ocName.equals( StoreSchemaConstants.HUB_OC_COLLECTION_ITEM ) )
                {
                    collectionIndex = 0;
                }
            }
        }

        // Parsing name and namer attribute out of Dn.
        Rdn name = componentEntry.getDn().getRdn();
        componentName = name.getUpValue().getString();

        String namer = name.getUpType();

        List<DcProperty> properties = new ArrayList<DcProperty>();

        Collection<Attribute> attribs = componentEntry.getAttributes();
        for ( Attribute attrib : attribs )
        {

            if ( attrib.getUpId().equals( StoreSchemaConstants.HUB_AT_COLL_ITEM_INDEX.toLowerCase() ) )
            {
                collectionIndex = Integer.parseInt( attrib.getString() );
            }
            else if ( attrib.getUpId().equals( namer )
                || attrib.getUpId().equals( SchemaConstants.ENTRY_UUID_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.ENTRY_CSN_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.CREATORS_NAME_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.CREATE_TIMESTAMP_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.OBJECT_CLASS_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.MODIFIERS_NAME_AT.toLowerCase() )
                || attrib.getUpId().equals( SchemaConstants.MODIFY_TIMESTAMP_AT.toCharArray() )
                || attrib.getUpId().equals( SchemaConstants.ENTRY_PARENT_ID_AT ) )
            {
                continue;
            }
            else
            {
                properties.add( new DcProperty( attrib.getUpId(), attrib.getString() ) );
            }
        }

        DcConfiguration componentConf = new DcConfiguration( properties );
        componentConf.setCollectionIndex( collectionIndex );

        DirectoryComponent component = new DirectoryComponent( managerPID, componentName, componentConf );
        component.setConfigLocation( componentEntry.getDn().getName() );
        component.setNamerAttribute( namer );

        return component;
    }
}
