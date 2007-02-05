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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.util.AttributeUtils;

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

    /**
     * Check that the attribute does not contain collective attributes if it does not
     * have the collectiveAttributeSubentry ObjectClass declared
     * 
     * @param normName The entry DN
     * @param entry The entry attributes
     * @throws LdapSchemaViolationException 
     * @throws NamingException
     */
    public void checkAdd( LdapDN normName, Attributes entry ) throws LdapSchemaViolationException, NamingException
    {
        Attribute objectClass = entry.get( "objectClass" );

        // If the objectclass contains "collectiveAttributeSubentry", then we don't have to
        // check for the existence on collectivbe attributes : it's already allowed
        if ( AttributeUtils.containsValueCaseIgnore( objectClass, "collectiveAttributeSubentry" ) )
        {
            return;
        }
        else if ( containsAnyCollectiveAttributes( entry ) )
        {
            // We have some collective attributes, which is not allowed
            throw new LdapSchemaViolationException(
                "Collective attributes cannot be stored in non-collectiveAttributeSubentries",
                ResultCodeEnum.OBJECTCLASSVIOLATION );
        }
    }

    public void checkModify( LdapDN normName, int modOp, Attributes mods ) throws NamingException
    {
        ModificationItemImpl[] modsAsArray = new ModificationItemImpl[ mods.size() ];
        NamingEnumeration allAttrs = mods.getAll();
        int i = 0;
        
        while ( allAttrs.hasMoreElements() )
        {
            Attribute attr = ( Attribute ) allAttrs.nextElement();
            modsAsArray[i] = new ModificationItemImpl( modOp, attr );
            i++;
        }

        checkModify( normName, modsAsArray );
    }

    public void checkModify( LdapDN normName, ModificationItemImpl[] mods ) throws NamingException
    {
        Attributes originalEntry = nexus.lookup( normName );
        Attributes targetEntry = SchemaUtils.getTargetEntry( mods, originalEntry );
        Attribute targetObjectClasses = targetEntry.get( "objectClass" );

        if ( AttributeUtils.containsValueCaseIgnore( targetObjectClasses, "collectiveAttributeSubentry" ) )
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


    private boolean addsAnyCollectiveAttributes( ModificationItemImpl[] mods ) throws NamingException
    {
        for ( int i = 0; i < mods.length; i++ )
        {
            Attribute attr = mods[i].getAttribute();
            String attrID = attr.getID();
            AttributeType attrType = attrTypeRegistry.lookup( attrID );
            int modOp = mods[i].getModificationOp();

            if ( ( modOp == DirContext.ADD_ATTRIBUTE || modOp == DirContext.REPLACE_ATTRIBUTE ) &&
                attrType.isCollective() )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if an entry contains some collective attributes or not
     *
     * @param entry The entry to be checked
     * @return <code>true</code> if the entry contains a collective attribute, <code>false</code> otherwise
     * @throws NamingException If something went wrong
     */
    private boolean containsAnyCollectiveAttributes( Attributes entry ) throws NamingException
    {
        NamingEnumeration allIDs = entry.getIDs();
        
        while ( allIDs.hasMoreElements() )
        {
            String attrTypeStr = ( String ) allIDs.nextElement();
            AttributeType attrType = attrTypeRegistry.lookup( attrTypeStr );
            
            if ( attrType.isCollective() )
            {
                return true;
            }
        }

        return false;
    }
}
