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

import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeIdentifierException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaUtils;


/**
 * Schema checking utilities specifically for operations on collective attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class CollectiveAttributesSchemaChecker
{
    private PartitionNexus nexus = null;
    private AttributeTypeRegistry attrTypeRegistry = null;
    
    public CollectiveAttributesSchemaChecker( PartitionNexus nexus, AttributeTypeRegistry attrTypeRegistry )
    {
        this.nexus = nexus;
        this.attrTypeRegistry = attrTypeRegistry;
    }
    
    /* package scope*/ void checkAdd( LdapDN normName, ServerEntry entry ) throws Exception
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
            throw new LdapSchemaViolationException(
                "Collective attributes cannot be stored in non-collectiveAttributeSubentries",
                ResultCodeEnum.OTHER);
        }
    }
    
    
    public void checkModify( OperationContext opContext, LdapDN normName, List<Modification> mods ) throws Exception
    {
        ServerEntry originalEntry = opContext.lookup( normName, ByPassConstants.LOOKUP_BYPASS );
        ServerEntry targetEntry = (ServerEntry)SchemaUtils.getTargetEntry( mods, originalEntry );
        
        EntryAttribute targetObjectClasses = targetEntry.get( SchemaConstants.OBJECT_CLASS_AT );
        
        if ( targetObjectClasses.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRY_OC ) )
        {
            return;
        }
        
        if ( addsAnyCollectiveAttributes( mods ) )
        {
            /*
             * TODO: Replace the Exception and the ResultCodeEnum with the correct ones.
             */
            throw new LdapSchemaViolationException(
                "Cannot operate on collective attributes in non-collectiveAttributeSubentries",
                ResultCodeEnum.OTHER);
        }
    }
    
    
    private boolean addsAnyCollectiveAttributes( List<Modification> mods ) throws NamingException
    {
        for ( Modification mod:mods )
        {
            // TODO: handle http://issues.apache.org/jira/browse/DIRSERVER-1198
            ServerAttribute attr = (ServerAttribute)mod.getAttribute();
            AttributeType attrType = attr.getAttributeType();

            if ( attrType == null )
            {
                if ( !attrTypeRegistry.hasAttributeType( attr.getUpId() ) )
                {
                    throw new LdapInvalidAttributeIdentifierException();
                }
                else
                {
                    attrType = attrTypeRegistry.lookup( attr.getUpId() );
                }
            }
            
            
            ModificationOperation modOp = mod.getOperation();
            
            if ( ( ( modOp == ModificationOperation.ADD_ATTRIBUTE ) || ( modOp == ModificationOperation.REPLACE_ATTRIBUTE ) ) &&
                attrType.isCollective() )
            {
                return true;
            }
        }
        
        return false;
    }
    
    
    private boolean containsAnyCollectiveAttributes( ServerEntry entry ) throws NamingException
    {
        Set<AttributeType> attributeTypes = entry.getAttributeTypes();
        
        for ( AttributeType attributeType:attributeTypes )
        {
            if ( attributeType.isCollective() )
            {
                return true;
            }
        }
        
        return false;
    }
}
