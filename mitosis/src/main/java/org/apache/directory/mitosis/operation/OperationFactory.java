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
package org.apache.directory.mitosis.operation;


import java.util.Map;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNFactory;
import org.apache.directory.mitosis.common.Constants;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.UUIDFactory;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;


/**
 * Converts a complex JNDI operations into multiple simple operations. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OperationFactory
{
    private final ReplicaId replicaId;
    private final Map environment;
    private final PartitionNexus nexus;
    private final UUIDFactory uuidFactory;
    private final CSNFactory csnFactory;


    public OperationFactory( DirectoryServiceConfiguration serviceCfg, ReplicationConfiguration cfg )
    {
        this.replicaId = cfg.getReplicaId();
        this.environment = serviceCfg.getEnvironment();
        this.nexus = serviceCfg.getPartitionNexus();
        this.uuidFactory = cfg.getUuidFactory();
        this.csnFactory = cfg.getCsnFactory();
    }


    public Operation newAdd( LdapDN normalizedName, Attributes entry ) throws NamingException
    {
        return newAdd( newCSN(), normalizedName, entry );
    }


    private Operation newAdd( CSN csn, LdapDN normalizedName, Attributes entry ) throws NamingException
    {
        // Check an entry already exists.
        checkBeforeAdd( normalizedName );

        CompositeOperation result = new CompositeOperation( csn );

        // Insert 'entryUUID' and 'entryDeleted'.
        entry = ( Attributes ) entry.clone();
        entry.remove( Constants.ENTRY_UUID );
        entry.remove( Constants.ENTRY_DELETED );
        entry.put( Constants.ENTRY_UUID, uuidFactory.newInstance().toOctetString() );
        entry.put( Constants.ENTRY_DELETED, "false" );

        // NOTE: We inlined addDefaultOperations() because ApacheDS currently
        // creates an index entry only for ADD operation (and not for
        // MODIFY operation)
        entry.put( Constants.ENTRY_CSN, csn.toOctetString() );

        result.add( new AddEntryOperation( csn, normalizedName, entry ) );
        return result;
    }


    public Operation newDelete( LdapDN normalizedName )
    {
        CSN csn = newCSN();
        CompositeOperation result = new CompositeOperation( csn );

        // Transform into replace operation.
        result.add( new ReplaceAttributeOperation( csn, normalizedName, new BasicAttribute( Constants.ENTRY_DELETED,
            "true" ) ) );

        return addDefaultOperations( result, csn, normalizedName );
    }


    public Operation newModify( LdapDN normalizedName, int modOp, Attributes attributes )
    {
        CSN csn = newCSN();
        CompositeOperation result = new CompositeOperation( csn );
        NamingEnumeration e = attributes.getAll();
        // Transform into multiple {@link AttributeOperation}s.
        while ( e.hasMoreElements() )
        {
            Attribute attr = ( Attribute ) e.nextElement();
            result.add( newModify( csn, normalizedName, modOp, attr ) );
        }

        // Resurrect the entry in case it is deleted.
        result.add( new ReplaceAttributeOperation( csn, normalizedName, new BasicAttribute( Constants.ENTRY_DELETED,
            "false" ) ) );

        return addDefaultOperations( result, null, normalizedName );
    }


    public Operation newModify( LdapDN normalizedName, ModificationItem[] items )
    {
        CSN csn = newCSN();
        CompositeOperation result = new CompositeOperation( csn );
        final int length = items.length;
        // Transform into multiple {@link AttributeOperation}s.
        for ( int i = 0; i < length; i++ )
        {
            ModificationItem item = items[i];
            result.add( newModify( csn, normalizedName, item.getModificationOp(), item.getAttribute() ) );
        }

        // Resurrect the entry in case it is deleted.
        result.add( new ReplaceAttributeOperation( csn, normalizedName, new BasicAttribute( Constants.ENTRY_DELETED,
            "false" ) ) );

        return addDefaultOperations( result, csn, normalizedName );
    }


    private Operation newModify( CSN csn, LdapDN normalizedName, int modOp, Attribute attribute )
    {
        switch ( modOp )
        {
            case DirContext.ADD_ATTRIBUTE:
                return new AddAttributeOperation( csn, normalizedName, attribute );
            case DirContext.REPLACE_ATTRIBUTE:
                return new ReplaceAttributeOperation( csn, normalizedName, attribute );
            case DirContext.REMOVE_ATTRIBUTE:
                return new DeleteAttributeOperation( csn, normalizedName, attribute );
            default:
                throw new IllegalArgumentException( "Unknown modOp: " + modOp );
        }
    }


    public Operation newModifyRn( LdapDN oldName, String newRdn, boolean deleteOldRn ) throws NamingException
    {
        return newMove( oldName, ( LdapDN ) oldName.getSuffix( 1 ), newRdn, deleteOldRn );
    }


    public Operation newMove( LdapDN oldName, LdapDN newParentName ) throws NamingException
    {
        return newMove( oldName, newParentName, oldName.get( oldName.size() - 1 ), true );
    }


    public Operation newMove( LdapDN oldName, LdapDN newParentName, String newRdn, boolean deleteOldRn )
        throws NamingException
    {
        if ( !deleteOldRn )
        {
            throw new OperationNotSupportedException( "deleteOldRn must be true." );
        }

        // Prepare to create composite operations
        CSN csn = newCSN();
        CompositeOperation result = new CompositeOperation( csn );

        // Retrieve all subtree including the base entry
        SearchControls ctrl = new SearchControls();
        ctrl.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration e = nexus.search( oldName, environment, new PresenceNode( Constants.OBJECT_CLASS_OID ), ctrl );

        while ( e.hasMore() )
        {
            SearchResult sr = ( SearchResult ) e.next();

            // Get the name of the old entry
            LdapDN oldEntryName = new LdapDN( sr.getName() );

            // Delete the old entry
            result.add( new ReplaceAttributeOperation( csn, oldEntryName, new BasicAttribute( Constants.ENTRY_DELETED,
                "true" ) ) );

            // Get the old entry attributes and replace RDN if required
            Attributes entry = sr.getAttributes();
            if ( oldEntryName.size() == oldName.size() )
            {
                entry.remove( NamespaceTools.getRdnAttribute( oldName.get( oldName.size() - 1 ) ) );
                entry.put( NamespaceTools.getRdnAttribute( newRdn ), NamespaceTools.getRdnValue( newRdn ) );
            }

            // Calculate new name from newParentName, oldEntryName, and newRdn.
            LdapDN newEntryName = ( LdapDN ) newParentName.clone();
            newEntryName.add( newRdn );
            for ( int i = oldEntryName.size() - newEntryName.size(); i > 0; i-- )
            {
                newEntryName.add( oldEntryName.get( oldEntryName.size() - i ) );
            }

            // Add the new entry
            result.add( newAdd( csn, new LdapDN( newEntryName.getUpName() ), entry ) );

            // Add default operations to the old entry.
            // Please note that newAdd() already added default operations
            // to the new entry. 
            addDefaultOperations( result, csn, oldEntryName );
        }

        return result;
    }


    private void checkBeforeAdd( LdapDN newEntryName ) throws NamingException
    {
        if ( nexus.hasEntry( newEntryName ) )
        {
            throw new NameAlreadyBoundException( newEntryName.toString() + " already exists." );
        }
    }


    private CompositeOperation addDefaultOperations( CompositeOperation result, CSN csn, LdapDN normalizedName )
    {
        result.add( new ReplaceAttributeOperation( csn, normalizedName, new BasicAttribute( Constants.ENTRY_CSN, csn
            .toOctetString() ) ) );
        return result;
    }


    private CSN newCSN()
    {
        return csnFactory.newInstance( replicaId );
    }
}
