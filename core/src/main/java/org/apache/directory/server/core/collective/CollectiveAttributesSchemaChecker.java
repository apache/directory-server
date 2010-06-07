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
package org.apache.directory.server.core.collective;


import java.util.List;
import java.util.Set;

import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaUtils;


/**
 * Schema checking utilities specifically for operations on collective attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CollectiveAttributesSchemaChecker
{
    private SchemaManager schemaManager = null;


    public CollectiveAttributesSchemaChecker( PartitionNexus nexus, SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    /* package scope*/void checkAdd( DN normName, Entry entry ) throws LdapException
    {
        if ( entry.hasObjectClass( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            return;
        }

        if ( containsAnyCollectiveAttributes( entry ) )
        {
            /*
             * TODO: Replace the Exception and the ResultCodeEnum with the correct ones.
             */
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER, I18n.err( I18n.ERR_241 ) );
        }
    }


    public void checkModify( ModifyOperationContext opContext ) throws LdapException
    {
        List<Modification> mods = opContext.getModItems();
        Entry originalEntry = opContext.getEntry();
        Entry targetEntry = ( Entry ) SchemaUtils.getTargetEntry( mods, originalEntry );

        EntryAttribute targetObjectClasses = targetEntry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( targetObjectClasses == null )
        {
            // This is not allowed 
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER, I18n.err(
                I18n.ERR_272_MODIFY_LEAVES_NO_STRUCTURAL_OBJECT_CLASS, originalEntry.getDn() ) );
        }

        if ( targetObjectClasses.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            return;
        }

        if ( addsAnyCollectiveAttributes( mods ) )
        {
            /*
             * TODO: Replace the Exception and the ResultCodeEnum with the correct ones.
             */
            throw new LdapSchemaViolationException( ResultCodeEnum.OTHER, I18n.err( I18n.ERR_242 ) );
        }
    }


    private boolean addsAnyCollectiveAttributes( List<Modification> mods ) throws LdapException
    {
        for ( Modification mod : mods )
        {
            // TODO: handle http://issues.apache.org/jira/browse/DIRSERVER-1198
            EntryAttribute attr = mod.getAttribute();
            AttributeType attrType = attr.getAttributeType();

            if ( attrType == null )
            {
                try
                {
                    attrType = schemaManager.lookupAttributeTypeRegistry( attr.getUpId() );
                }
                catch ( LdapException le )
                {
                    throw new LdapInvalidAttributeTypeException();
                }
            }

            ModificationOperation modOp = mod.getOperation();

            if ( ( ( modOp == ModificationOperation.ADD_ATTRIBUTE ) || ( modOp == ModificationOperation.REPLACE_ATTRIBUTE ) )
                && attrType.isCollective() )
            {
                return true;
            }
        }

        return false;
    }


    private boolean containsAnyCollectiveAttributes( Entry entry ) throws LdapException
    {
        Set<AttributeType> attributeTypes = entry.getAttributeTypes();

        for ( AttributeType attributeType : attributeTypes )
        {
            if ( attributeType.isCollective() )
            {
                return true;
            }
        }

        return false;
    }
}
