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
package org.apache.directory.server.core.shared.partition;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.partition.OperationExecutionManager;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.partition.index.ParentIdAndRdn;
import org.apache.directory.server.core.api.partition.index.UUIDComparator;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.server.core.shared.txn.logedit.DataChangeContainer;
import org.apache.directory.server.core.shared.txn.logedit.EntryAddDelete;
import org.apache.directory.server.core.shared.txn.logedit.EntryReplace;
import org.apache.directory.server.core.shared.txn.logedit.IndexChange;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapAliasDereferencingException;
import org.apache.directory.shared.ldap.model.exception.LdapAliasException;
import org.apache.directory.shared.ldap.model.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Ava;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.UsageEnum;


public class DefaultOperationExecutionManager implements OperationExecutionManager
{

    /** Txn log manager kept for fast access */
    private TxnLogManager txnLogManager;

    /** Txn manager Factory */
    private TxnManagerFactory txnManagerFactory;


    public DefaultOperationExecutionManager( TxnManagerFactory txnManagerFactory )
    {
        this.txnManagerFactory = txnManagerFactory;
        txnLogManager = txnManagerFactory.txnLogManagerInstance();
    }


    //---------------------------------------------------------------------------------------------
    // The Add operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void add( Partition partition, AddOperationContext addContext ) throws LdapException
    {
        try
        {
            Entry entry = ( ( ClonedServerEntry ) addContext.getEntry() ).getClonedEntry();
            Dn entryDn = entry.getDn();

            // Add write dependency on the dn
            txnLogManager.addWrite( entryDn, SearchScope.SUBTREE );

            // check if the entry already exists
            if ( getEntryId( partition, entryDn ) != null )
            {
                LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                    I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, entryDn.getName() ) );
                throw ne;
            }

            UUID parentId = null;
            Dn suffixDn = partition.getSuffixDn();

            //
            // Suffix entry cannot have a parent since it is the root. Its parent id
            // is set to a special value.
            //
            Dn parentDn = null;
            ParentIdAndRdn key = null;

            if ( entryDn.equals( suffixDn ) )
            {
                parentId = Partition.rootID;
                key = new ParentIdAndRdn( parentId, suffixDn.getRdns() );
            }
            else
            {
                parentDn = entryDn.getParent();
                parentId = getEntryId( partition, parentDn );

                key = new ParentIdAndRdn( parentId, entryDn.getRdn() );
            }

            // don't keep going if we cannot find the parent Id
            if ( parentId == null )
            {
                throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_216_ID_FOR_PARENT_NOT_FOUND, parentDn ) );
            }

            // Get a new ID for the added entry
            MasterTable master = partition.getMasterTable();
            UUID id = master.getNextId( entry );
            DataChangeContainer changeContainer = new DataChangeContainer( partition );
            changeContainer.setEntryID( id );

            // Update the RDN index
            Index<?> rdnIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            IndexChange indexChange = new IndexChange( rdnIdx, key, id, IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );

            // Update the ObjectClass index
            Attribute objectClass = entry.get( SchemaConstants.OBJECT_CLASS_AT );

            if ( objectClass == null )
            {
                String msg = I18n.err( I18n.ERR_217, entryDn.getName(), entry );
                ResultCodeEnum rc = ResultCodeEnum.OBJECT_CLASS_VIOLATION;
                LdapSchemaViolationException e = new LdapSchemaViolationException( rc, msg );
                //e.setResolvedName( entryDn );
                throw e;
            }

            Index<?> objectClassIdx = partition.getSystemIndex( SchemaConstants.OBJECT_CLASS_AT_OID );

            for ( Value<?> value : objectClass )
            {
                if ( value.equals( SchemaConstants.TOP_OC ) )
                {
                    continue;
                }

                indexChange = new IndexChange( objectClassIdx, value.getString(),
                    id, IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
            }

            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                Attribute aliasAttr = entry.get( SchemaConstants.ALIASED_OBJECT_NAME_AT );
                addAliasIndices( partition, id, entryDn, aliasAttr.getString(), changeContainer );
            }

            // Update the OneLevel index
            Index<?> oneLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            indexChange = new IndexChange( oneLevelIdx, parentId, id, IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );

            // Update the SubLevel index
            UUID tempId = parentId;
            UUID suffixId = getSuffixId( partition );
            Index<?> subLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );

            while ( ( tempId != null ) && ( !tempId.equals( Partition.rootID ) ) && ( !tempId.equals( suffixId ) ) )
            {
                indexChange = new IndexChange( subLevelIdx, tempId, id, IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
                tempId = getParentId( partition, tempId );
            }

            // making entry an ancestor/descendent of itself in sublevel index
            indexChange = new IndexChange( subLevelIdx, id, id, IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );

            // Update the EntryCsn index
            Attribute entryCsn = entry.get( SchemaConstants.ENTRY_CSN_AT );

            if ( entryCsn == null )
            {
                String msg = I18n.err( I18n.ERR_219, entryDn.getName(), entry );
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
            }

            Index<?> entryCsnIdx = partition.getSystemIndex( SchemaConstants.ENTRY_CSN_AT_OID );
            indexChange = new IndexChange( entryCsnIdx, entryCsn.getString(), id, IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );

            // Update the EntryUuid index
            Attribute entryUuid = entry.get( SchemaConstants.ENTRY_UUID_AT );

            if ( entryUuid == null )
            {
                String msg = I18n.err( I18n.ERR_220, entryDn.getName(), entry );
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
            }

            Index<?> entryUuidIdx = partition.getSystemIndex( SchemaConstants.ENTRY_UUID_AT_OID );
            indexChange = new IndexChange( entryUuidIdx, entryUuid.getString(), id, IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );

            // Now work on the user defined userIndices
            Index<?> presenceIdx;
            presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );

            for ( Attribute attribute : entry )
            {
                AttributeType attributeType = attribute.getAttributeType();
                String attributeOid = attributeType.getOid();

                if ( partition.hasUserIndexOn( attributeType ) )
                {
                    Index<?> index = partition.getUserIndex( attributeType );

                    // here lookup by attributeId is OK since we got attributeId from
                    // the entry via the enumeration - it's in there as is for sure

                    for ( Value<?> value : attribute )
                    {
                        indexChange = new IndexChange( index, value.getValue(), id,
                            IndexChange.Type.ADD, false );
                        changeContainer.addChange( indexChange );
                    }

                    // Adds only those attributes that are indexed
                    indexChange = new IndexChange( presenceIdx, attributeOid, id, IndexChange.Type.ADD, true );
                    changeContainer.addChange( indexChange );
                }
            }

            // Add the parentId in the entry
            entry.put( SchemaConstants.ENTRY_PARENT_ID_AT, parentId.toString() );

            // Add the dn
            entry.setDn( entryDn );

            // And finally prepare the entry change
            EntryAddDelete entryAdd = new EntryAddDelete( entry, EntryAddDelete.Type.ADD );
            changeContainer.addChange( entryAdd );

            // Set the modified entry
            addContext.setModifiedEntry( entry );

            // log the change
            txnLogManager.log( changeContainer, false );

            // Mostly for schema partition
            partition.add( addContext );
        }
        catch ( LdapException le )
        {
            throw le;
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Delete operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void delete( Partition partition, DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();

        // Mostly for schema partition
        partition.delete( deleteContext );

        // Add write dependency on the dn
        txnLogManager.addWrite( dn, SearchScope.SUBTREE );

        UUID id = getEntryId( partition, dn );

        // don't continue if id is null
        if ( id == null )
        {
            throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_699, dn ) );
        }

        if ( hasChildren( partition, id ) )
        {
            LdapContextNotEmptyException cnee = new LdapContextNotEmptyException( I18n.err( I18n.ERR_700, dn ) );
            //cnee.setRemainingName( dn );
            throw cnee;
        }

        // We now defer the deletion to the implementing class
        delete( partition, dn, id );
    }


    @SuppressWarnings("unchecked")
    private boolean hasChildren( Partition partition, UUID id ) throws LdapOperationErrorException
    {
        try
        {
            Index<?> oneLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            oneLevelIdx = txnLogManager.wrap( partition.getSuffixDn(), oneLevelIdx );
            return ( ( Index<Object> ) oneLevelIdx ).forward( id );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( Partition partition, Dn entryDn, UUID id ) throws LdapException
    {
        try
        {
            MasterTable master = partition.getMasterTable();
            master = txnLogManager.wrap( partition.getSuffixDn(), master );

            // First get the entry
            Entry entry = getEntry( partition, master, id );

            if ( entry == null )
            {
                // Not allowed
                throw new LdapNoSuchObjectException( "Cannot find an entry for ID " + id );
            }

            Dn suffixDn = partition.getSuffixDn();
            DataChangeContainer changeContainer = new DataChangeContainer( partition );
            changeContainer.setEntryID( id );
            IndexChange indexChange;

            Attribute objectClass = entry.get( SchemaConstants.OBJECT_CLASS_AT );

            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                dropAliasIndices( partition, entryDn, id, null, changeContainer );
            }

            // Update the ObjectClass index
            Index<?> objectClassIdx = partition.getSystemIndex( SchemaConstants.OBJECT_CLASS_AT_OID );

            for ( Value<?> value : objectClass )
            {
                indexChange = new IndexChange( objectClassIdx, value.getString(),
                    id, IndexChange.Type.DELETE, true );
                changeContainer.addChange( indexChange );
            }

            // Handle the rdn idx

            ParentIdAndRdn key = null;
            UUID parentId = UUID.fromString( entry.get( SchemaConstants.ENTRY_PARENT_ID_AT ).getString() );

            if ( entryDn.equals( suffixDn ) )
            {
                key = new ParentIdAndRdn( parentId, suffixDn.getRdns() );
            }
            else
            {
                key = new ParentIdAndRdn( parentId, entryDn.getRdn() );
            }

            Index<?> rdnIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            indexChange = new IndexChange( rdnIdx, key, id,
                IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );

            // Handle one level idx
            Index<?> oneLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            indexChange = new IndexChange( oneLevelIdx, parentId, id, IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );

            // Handle Sublevel idx

            Index<?> subLevelIdx;
            subLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
            subLevelIdx = txnLogManager.wrap( partition.getSuffixDn(), subLevelIdx );
            IndexCursor<?> indexCursor = subLevelIdx.reverseCursor( id );
            IndexEntry<?> indexEntry;

            try
            {
                while ( indexCursor.next() )
                {
                    indexEntry = indexCursor.get();
                    indexChange = new IndexChange( subLevelIdx, indexEntry.getValue(), id, IndexChange.Type.DELETE,
                        true );
                    changeContainer.addChange( indexChange );
                }
            }
            finally
            {
                indexCursor.close();
            }
            // Handle the csn index
            String entryCsn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();
            Index<?> entryCsnIdx = partition.getSystemIndex( SchemaConstants.ENTRY_CSN_AT_OID );
            indexChange = new IndexChange( entryCsnIdx, entryCsn, id, IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );

            // Handle the uuid idx
            Index<?> entryUuidIdx = partition.getSystemIndex( SchemaConstants.ENTRY_UUID_AT_OID );
            indexChange = new IndexChange( entryUuidIdx, id.toString(), id, IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );

            // Update the user indexes
            Index<?> presenceIdx;
            presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );

            for ( Attribute attribute : entry )
            {
                AttributeType attributeType = attribute.getAttributeType();
                String attributeOid = attributeType.getOid();

                if ( partition.hasUserIndexOn( attributeType ) )
                {
                    Index<?> index = partition.getUserIndex( attributeType );

                    // here lookup by attributeId is ok since we got attributeId from
                    // the entry via the enumeration - it's in there as is for sure
                    for ( Value<?> value : attribute )
                    {
                        indexChange = new IndexChange( index, value.getValue(), id, IndexChange.Type.DELETE, false );
                        changeContainer.addChange( indexChange );
                    }

                    // Adds only those attributes that are indexed
                    indexChange = new IndexChange( presenceIdx, attributeOid, id, IndexChange.Type.DELETE, true );
                    changeContainer.addChange( indexChange );
                }
            }

            // And finally prepare the entry change
            EntryAddDelete entryAdd = new EntryAddDelete( entry, EntryAddDelete.Type.DELETE );
            changeContainer.addChange( entryAdd );

            // log the change
            txnLogManager.log( changeContainer, false );

        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Modify operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void modify( Partition partition, ModifyOperationContext modifyContext ) throws LdapException
    {
        try
        {
            Entry modifiedEntry = modify( partition, modifyContext.getDn(),
                modifyContext.getModItems().toArray( new Modification[]
                    {} ) );
            modifyContext.setAlteredEntry( modifiedEntry );

            // Mostly for schema partition
            partition.modify( modifyContext );

        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public Entry modify( Partition partition, Dn dn, Modification... mods ) throws Exception
    {
        UUID id = getEntryId( partition, dn );
        MasterTable master = partition.getMasterTable();
        master = txnLogManager.wrap( partition.getSuffixDn(), master );
        Entry originalEntry = getEntry( partition, master, id );
        Entry entry = originalEntry.clone();

        DataChangeContainer changeContainer = new DataChangeContainer( partition );
        changeContainer.setEntryID( id );

        // Add write dependency on the dn
        txnLogManager.addWrite( dn, SearchScope.OBJECT );

        for ( Modification mod : mods )
        {

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    modifyAdd( dn, partition, id, entry, mod, changeContainer );
                    break;

                case REMOVE_ATTRIBUTE:
                    modifyRemove( dn, partition, id, entry, mod, changeContainer );
                    break;

                case REPLACE_ATTRIBUTE:
                    modifyReplace( dn, partition, id, entry, mod, changeContainer );
                    break;

                default:
                    throw new LdapException( I18n.err( I18n.ERR_221 ) );
            }
        }

        // For now use and EntryReplace change
        EntryReplace entryReplace = new EntryReplace( entry, originalEntry );
        changeContainer.addChange( entryReplace );

        // log the changes
        txnLogManager.log( changeContainer, false );

        return entry;
    }


    /**
     * Adds a set of attribute values while affecting the appropriate userIndices.
     * The entry is not persisted: it is only changed in anticipation for a put
     * into the master table.
     *
     * @param entryDn dn of the entry
     * @param partition partition entry lives in 
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mod the add operation
     * @param changeContainer container to put txn log edits
     * @throws Exception if index alteration or attribute addition fails
     */
    @SuppressWarnings("unchecked")
    private void modifyAdd( Dn entryDn, Partition partition, UUID id, Entry entry, Modification mod,
        DataChangeContainer changeContainer ) throws Exception
    {

        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        Attribute mods = mod.getAttribute();
        SchemaManager schemaManager = partition.getSchemaManager();
        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();
        Index<?> index;
        IndexChange indexChange;
        Attribute changedAttribute = entry.get( attributeType );
        boolean prevValueExists = ( ( changedAttribute != null ) && ( changedAttribute.size() > 0 ) );

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            index = partition.getSystemIndex( attributeType );

            for ( Value<?> value : mods )
            {
                indexChange = new IndexChange( index, value.getString(), id, IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
            }
        }
        else if ( partition.hasUserIndexOn( attributeType ) )
        {
            index = partition.getUserIndex( attributeType );

            for ( Value<?> value : mods )
            {
                ( ( Index<Object> ) index ).add( value.getValue(), id );
                indexChange = new IndexChange( index, value, id, IndexChange.Type.ADD, false );
            }

            // If the attr didn't exist for this id then created a log edit for an add for the presence index
            if ( prevValueExists == false && mods.size() > 0 )
            {
                Index<?> presenceIdx;
                presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
                indexChange = new IndexChange( presenceIdx, modsOid, id, IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
            }
        }

        //        // create log edit for the entry change
        //        Modification undo = null;
        //
        //        if ( prevValueExists )
        //        {
        //            undo = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, changedAttribute );
        //        }
        //
        //        EntryChange entryChange = new EntryChange( mod, null );
        //        changeContainer.addChange( entryChange );

        // add all the values in mods to the same attribute in the entry
        for ( Value<?> value : mods )
        {
            entry.add( mods.getAttributeType(), value );
        }

        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            addAliasIndices( partition, id, entryDn, mods.getString(), changeContainer );
        }
    }


    /**
     * Completely replaces the existing set of values for an attribute with the
     * modified values supplied affecting the appropriate userIndices.  The entry
     * is not persisted: it is only changed in anticipation for a put into the
     * master table.
     *
     * @param entryDn dn of the entry
     * @param partition partition entry lives in 
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mod the replacement operation
     * @param changeContainer container to put txn log edits
     * @throws Exception if index alteration or attribute modification
     * fails.
     */
    private void modifyReplace( Dn entryDn, Partition partition, UUID id, Entry entry, Modification mod,
        DataChangeContainer changeContainer ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        Attribute mods = mod.getAttribute();
        SchemaManager schemaManager = partition.getSchemaManager();
        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();
        Index<?> index;
        IndexChange indexChange;
        Attribute replacedAttribute = entry.get( attributeType );
        boolean prevValueExists = ( ( replacedAttribute != null ) && ( replacedAttribute.size() > 0 ) );

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            Index<?> objectClassIdx;
            objectClassIdx = partition.getSystemIndex( modsOid );
            objectClassIdx = txnLogManager.wrap( partition.getSuffixDn(), objectClassIdx );

            // if the id exists in the index drop all existing attribute
            // value index entries and add new ones
            IndexCursor<?> indexCursor = objectClassIdx.reverseCursor( id );
            IndexEntry<?> indexEntry;

            try
            {
                while ( indexCursor.next() )
                {
                    indexEntry = indexCursor.get();
                    indexChange = new IndexChange( objectClassIdx, indexEntry.getValue(), id,
                        IndexChange.Type.DELETE, true );
                    changeContainer.addChange( indexChange );
                }
            }
            finally
            {
                indexCursor.close();
            }

            for ( Value<?> value : mods )
            {
                if ( value.equals( SchemaConstants.TOP_OC ) )
                {
                    continue;
                }

                indexChange = new IndexChange( objectClassIdx, value.getString(), id, IndexChange.Type.ADD,
                    true );
                changeContainer.addChange( indexChange );
            }
        }
        else if ( partition.hasUserIndexOn( attributeType ) )
        {
            index = partition.getUserIndex( attributeType );
            index = txnLogManager.wrap( partition.getSuffixDn(), index );

            // if the id exists in the index drop all existing attribute
            // value index entries and add new ones
            IndexCursor<?> indexCursor = index.reverseCursor( id );
            IndexEntry<?> indexEntry;

            try
            {
                while ( indexCursor.next() )
                {
                    indexEntry = indexCursor.get();
                    indexChange = new IndexChange( index, indexEntry.getValue(), id, IndexChange.Type.DELETE,
                        false );
                    changeContainer.addChange( indexChange );
                }
            }
            finally
            {
                indexCursor.close();
            }

            for ( Value<?> value : mods )
            {
                indexChange = new IndexChange( index, value.getValue(), id, IndexChange.Type.ADD, false );
                changeContainer.addChange( indexChange );
            }
            /*
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( ( mods.size() == 0 ) && prevValueExists == true )
            {
                Index<?> presenceIdx;
                presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
                indexChange = new IndexChange( presenceIdx, modsOid, id, IndexChange.Type.DELETE, true );
                changeContainer.addChange( indexChange );
            }
            else if ( ( mods.size() > 0 ) && prevValueExists == false )
            {
                Index<?> presenceIdx;
                presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
                indexChange = new IndexChange( presenceIdx, modsOid, id, IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
            }
        }
        else if ( modsOid.equals( SchemaConstants.ENTRY_CSN_AT_OID ) )
        {
            Index<?> entryCsnIdx;
            entryCsnIdx = partition.getSystemIndex( SchemaConstants.ENTRY_CSN_AT_OID );
            indexChange = new IndexChange( entryCsnIdx, entry.get( SchemaConstants.ENTRY_CSN_AT ).getString(),
                id, IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );

            indexChange = new IndexChange( entryCsnIdx, mods.getString(), id, IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );
        }

        String aliasAttributeOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.ALIASED_OBJECT_NAME_AT );

        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            dropAliasIndices( partition, entryDn, id, null, changeContainer );
        }

        //        // create log edit for the entry change
        //        Modification undo = null;
        //
        //        if ( prevValueExists )
        //        {
        //            undo = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, replacedAttribute );
        //        }
        //
        //        EntryChange entryChange = new EntryChange( mod, undo );
        //        changeContainer.addChange( entryChange );

        // replaces old attributes with new modified ones if they exist
        if ( mods.size() > 0 )
        {
            entry.put( mods );
        }
        else
        // removes old attributes if new replacements do not exist
        {
            entry.remove( mods );
        }

        if ( modsOid.equals( aliasAttributeOid ) && mods.size() > 0 )
        {
            addAliasIndices( partition, id, entryDn, mods.getString(), changeContainer );
        }
    }


    /**
     * Completely removes the set of values for an attribute having the values
     * supplied while affecting the appropriate userIndices.  The entry is not
     * persisted: it is only changed in anticipation for a put into the master
     * table.  Note that an empty attribute w/o values will remove all the
     * values within the entry where as an attribute w/ values will remove those
     * attribute values it contains.
     *
     * @param entryDn dn of the entry
     * @param partition partition entry lives in 
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mod the remove operation
     * @param changeContainer container to put txn log edits
     * @throws Exception if index alteration or attribute modification fails.
     */
    private void modifyRemove( Dn entryDn, Partition partition, UUID id, Entry entry, Modification mod,
        DataChangeContainer changeContainer ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        Attribute mods = mod.getAttribute();
        SchemaManager schemaManager = partition.getSchemaManager();
        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();
        Index<?> index;
        IndexChange indexChange;
        Attribute changedAttribute = entry.get( attributeType );
        boolean prevValueExists = ( ( changedAttribute != null ) && ( changedAttribute.size() > 0 ) );

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            Index<?> objectClassIdx;
            objectClassIdx = partition.getSystemIndex( modsOid );

            /*
             * If there are no attribute values in the modifications then this
             * implies the complete removal of the attribute from the index. Else
             * we remove individual tuples from the index.
             */
            if ( mods.size() == 0 )
            {
                objectClassIdx = txnLogManager.wrap( partition.getSuffixDn(), objectClassIdx );
                IndexCursor<?> indexCursor = objectClassIdx.reverseCursor( id );
                IndexEntry<?> indexEntry;

                try
                {
                    while ( indexCursor.next() )
                    {
                        indexEntry = indexCursor.get();
                        indexChange = new IndexChange( objectClassIdx, indexEntry.getValue(), id,
                            IndexChange.Type.DELETE, true );
                        changeContainer.addChange( indexChange );
                    }
                }
                finally
                {
                    indexCursor.close();
                }
            }
            else
            {
                for ( Value<?> value : mods )
                {
                    indexChange = new IndexChange( objectClassIdx, value.getString(), id,
                        IndexChange.Type.DELETE, true );
                    changeContainer.addChange( indexChange );
                }
            }
        }
        else if ( partition.hasUserIndexOn( attributeType ) )
        {
            index = partition.getUserIndex( attributeType );

            /*
             * If there are no attribute values in the modifications then this
             * implies the complete removal of the attribute from the index. Else
             * we remove individual tuples from the index.
             */
            if ( mods.size() == 0 )
            {
                // if the id exists in the index drop all existing attribute
                // value index entries and add new ones
                index = txnLogManager.wrap( partition.getSuffixDn(), index );
                IndexCursor<?> indexCursor = index.reverseCursor( id );
                IndexEntry<?> indexEntry;

                try
                {
                    while ( indexCursor.next() )
                    {
                        indexEntry = indexCursor.get();
                        indexChange = new IndexChange( index, indexEntry.getValue(), id,
                            IndexChange.Type.DELETE, false );
                        changeContainer.addChange( indexChange );
                    }
                }
                finally
                {
                    indexCursor.close();
                }

                if ( prevValueExists )
                {
                    Index<?> presenceIdx;
                    presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
                    indexChange = new IndexChange( presenceIdx, modsOid, id, IndexChange.Type.DELETE, true );
                    changeContainer.addChange( indexChange );
                }
            }
            else
            {
                for ( Value<?> value : mods )
                {
                    indexChange = new IndexChange( index, value.getValue(), id, IndexChange.Type.DELETE, false );
                    changeContainer.addChange( indexChange );
                }

                /*
                 *  If the above modifications are going to remove all the attribute values, then update the presence index as well. Here we rely on the
                 *  fact that only existing values in an entry can be removed.
                 */
                if ( prevValueExists && ( mods.size() == changedAttribute.size() ) )
                {
                    Index<?> presenceIdx;
                    presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
                    indexChange = new IndexChange( presenceIdx, modsOid, id, IndexChange.Type.DELETE, true );
                    changeContainer.addChange( indexChange );
                }
            }
        }

        //        // Prepare the entry change
        //        Modification undo = null;
        //
        //        if ( prevValueExists )
        //        {
        //            undo = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, changedAttribute );
        //        }
        //
        //        EntryChange entryChange = new EntryChange( mod, undo );
        //        changeContainer.addChange( entryChange );

        /*
         * If there are no attribute values in the modifications then this
         * implies the complete removal of the attribute from the entry. Else
         * we remove individual attribute values from the entry in mods one
         * at a time.
         */
        if ( mods.size() == 0 )
        {
            entry.removeAttributes( mods.getAttributeType() );
        }
        else
        {
            Attribute entryAttr = entry.get( mods.getAttributeType() );

            for ( Value<?> value : mods )
            {
                entryAttr.remove( value );
            }

            // if nothing is left just remove empty attribute
            if ( entryAttr.size() == 0 )
            {
                entry.removeAttributes( entryAttr.getId() );
            }
        }

        // Aliases->single valued comp/partial attr removal is not relevant here
        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            dropAliasIndices( partition, entryDn, id, null, changeContainer );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Rename operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void rename( Partition partition, RenameOperationContext renameContext ) throws LdapException
    {
        try
        {
            Dn oldDn = renameContext.getDn();
            Rdn newRdn = renameContext.getNewRdn();
            boolean deleteOldRdn = renameContext.getDeleteOldRdn();
            Entry originalEntry = ( ( ClonedServerEntry ) renameContext.getOriginalEntry() );

            if ( originalEntry != null )
            {
                originalEntry = ( ( ClonedServerEntry ) originalEntry ).getOriginalEntry();
            }

            if ( renameContext.getEntry() != null )
            {
                Entry modifiedEntry = renameContext.getModifiedEntry();
                rename( partition, oldDn, newRdn, deleteOldRdn, modifiedEntry, originalEntry );
            }
            else
            {
                rename( partition, oldDn, newRdn, deleteOldRdn, null, originalEntry );
            }

            // Mostly for schema partition
            partition.rename( renameContext );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void rename( Partition partition, Dn dn, Rdn newRdn, boolean deleteOldRdn, Entry entry, Entry originalEntry )
        throws Exception
    {
        if ( partition.updateEntryOnDnChange() )
        {
            UUID id = getEntryId( partition, dn );
            Dn parentDn = dn.getParent();
            Dn newDn = new Dn( newRdn, parentDn );
            newDn.apply( partition.getSchemaManager() );
            handleDnChange( partition, dn, newDn, id );
        }

        renameInternal( partition, dn, newRdn, deleteOldRdn, entry, originalEntry );
    }


    //---------------------------------------------------------------------------------------------
    // Alias index manipulation
    //---------------------------------------------------------------------------------------------
    /**
     * Adds userIndices for an aliasEntry to be added to the database while checking
     * for constrained alias constructs like alias cycles and chaining.
     *     
     * @param partition partition alias entry lives in 
     * @param aliasId  uuid of the alias entry
     * @param aliasDn normalized distinguished name for the alias entry
     * @param aliasTarget the user provided aliased entry dn as a string
     * @param aliasId the id of alias entry to add
     * @param changeContainer container to put txn log edits
     * @throws LdapException if index addition fails, and if the alias is
     * not allowed due to chaining or cycle formation.
     * @throws Exception if the wrappedCursor btrees cannot be altered
     */
    private void addAliasIndices( Partition partition, UUID aliasId, Dn aliasDn, String aliasTarget,
        DataChangeContainer changeContainer ) throws Exception
    {
        Dn normalizedAliasTargetDn; // Name value of aliasedObjectName
        UUID targetId; // Id of the aliasedObjectName
        Dn ancestorDn; // Name of an alias entry relative
        UUID ancestorId; // Id of an alias entry relative
        IndexChange indexChange;

        SchemaManager schemaManager = partition.getSchemaManager();
        Dn suffixDn = partition.getSuffixDn();

        // Access aliasedObjectName, normalize it and generate the Name
        normalizedAliasTargetDn = new Dn( schemaManager, aliasTarget );

        /*
         * Check For Aliases External To Naming Context
         *
         * id may be null but the alias may be to a valid entry in
         * another namingContext.  Such aliases are not allowed and we
         * need to point it out to the user instead of saying the target
         * does not exist when it potentially could outside of this upSuffix.
         */
        if ( !normalizedAliasTargetDn.isDescendantOf( suffixDn ) )
        {
            String msg = I18n.err( I18n.ERR_225, partition.getSuffixDn().getName() );
            LdapAliasDereferencingException e = new LdapAliasDereferencingException( msg );
            //e.setResolvedName( aliasDn );
            throw e;
        }

        // Add read dependency on the target dn
        txnLogManager.addRead( normalizedAliasTargetDn, SearchScope.OBJECT );

        // L O O K U P   T A R G E T   I D
        targetId = getEntryId( partition, normalizedAliasTargetDn );

        /*
         * Check For Target Existence
         *
         * We do not allow the creation of inconsistent aliases.  Aliases should
         * not be broken links.  If the target does not exist we start screaming
         */
        if ( null == targetId )
        {
            // Complain about target not existing
            String msg = I18n.err( I18n.ERR_581, aliasDn.getName(), aliasTarget );
            LdapAliasException e = new LdapAliasException( msg );
            //e.setResolvedName( aliasDn );
            throw e;
        }

        Index<?> aliasIdx;
        aliasIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        aliasIdx = txnLogManager.wrap( partition.getSuffixDn(), aliasIdx );

        /*
         * Detect Direct Alias Chain Creation
         *
         * Rather than resusitate the target to test if it is an alias and fail
         * due to chaing creation we use the alias index to determine if the
         * target is an alias.  Hence if the alias we are about to create points
         * to another alias as its target in the aliasedObjectName attribute,
         * then we have a situation where an alias chain is being created.
         * Alias chaining is not allowed so we throw and exception.
         */
        if ( null != aliasIdx.reverseLookup( targetId ) )
        {
            String msg = I18n.err( I18n.ERR_227 );
            LdapAliasDereferencingException e = new LdapAliasDereferencingException( msg );
            //e.setResolvedName( aliasDn );
            throw e;
        }

        // Add the alias to the simple alias index
        indexChange = new IndexChange( aliasIdx, normalizedAliasTargetDn.getNormName(), aliasId, IndexChange.Type.ADD,
            true );
        changeContainer.addChange( indexChange );

        /*
         * Handle One Level Scope Alias Index
         *
         * The first relative is special with respect to the one level alias
         * index.  If the target is not a sibling of the alias then we add the
         * index entry maping the parent's id to the aliased target id.
         */
        ancestorDn = aliasDn.getParent();
        ancestorId = getEntryId( partition, ancestorDn );

        // check if alias parent and aliased entry are the same
        Dn normalizedAliasTargetParentDn = normalizedAliasTargetDn.getParent();

        if ( !aliasDn.isDescendantOf( normalizedAliasTargetParentDn ) )
        {
            Index<?> oneAliasIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
            indexChange = new IndexChange( oneAliasIdx, ancestorId, targetId, IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );
        }

        /*
         * Handle Sub Level Scope Alias Index
         *
         * Walk the list of relatives from the parents up to the upSuffix, testing
         * to see if the alias' target is a descendant of the relative.  If the
         * alias target is not a descentant of the relative it extends the scope
         * and is added to the sub tree scope alias index.  The upSuffix node is
         * ignored since everything is under its scope.  The first loop
         * iteration shall handle the parents.
         */
        Index<?> subAliasIdx;
        subAliasIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );

        while ( !ancestorDn.equals( suffixDn ) && null != ancestorId )
        {
            if ( !normalizedAliasTargetDn.isDescendantOf( ancestorDn ) )
            {
                indexChange = new IndexChange( subAliasIdx, ancestorId, targetId, IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
            }

            ancestorDn = ancestorDn.getParent();
            ancestorId = getEntryId( partition, ancestorDn );
        }
    }


    /**
     * Removes the index entries for an alias before the entry is deleted from
     * the master table.
     *
     * @param partition partition alias entry lives in 
     * @param aliasDn  dn of the alias entry
     * @param aliasId the id of the alias entry in the master table
     * @param changeContainer container to put txn log edits
     * @throws LdapException if we cannot parse ldap names
     * @throws Exception if we cannot delete index values in the database
     */
    @SuppressWarnings("unchecked")
    private void dropAliasIndices( Partition partition, Dn aliasDn, UUID aliasId, String targetDn,
        DataChangeContainer changeContainer )
        throws Exception
    {
        SchemaManager schemaManager = partition.getSchemaManager();

        Index<?> aliasIdx;
        aliasIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        aliasIdx = txnLogManager.wrap( partition.getSuffixDn(), aliasIdx );

        if ( targetDn == null )
        {
            targetDn = ( ( Index<String> ) aliasIdx ).reverseLookup( aliasId );
        }

        UUID targetId = getEntryId( partition, new Dn( schemaManager, targetDn ) );
        IndexChange indexChange;

        if ( targetId == null )
        {
            // the entry doesn't exist, probably it has been deleted or renamed
            // TODO: this is just a workaround for now, the alias indices should be updated when target entry is deleted or removed
            return;
        }

        Dn ancestorDn = aliasDn.getParent();
        UUID ancestorId = getEntryId( partition, ancestorDn );

        /*
         * We cannot just drop all tuples in the one level and subtree userIndices
         * linking baseIds to the targetId.  If more than one alias refers to
         * the target then droping all tuples with a value of targetId would
         * make all other aliases to the target inconsistent.
         *
         * We need to walk up the path of alias ancestors until we reach the
         * upSuffix, deleting each ( ancestorId, targetId ) tuple in the
         * subtree scope alias.  We only need to do this for the direct parent
         * of the alias on the one level subtree.
         */

        Index<?> oneAliasIdx;
        oneAliasIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
        oneAliasIdx = txnLogManager.wrap( partition.getSuffixDn(), oneAliasIdx );

        if ( ( ( Index<Object> ) oneAliasIdx ).forward( ancestorId, targetId ) )
        {
            indexChange = new IndexChange( oneAliasIdx, ancestorId, targetId, IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );
        }

        Index<?> subAliasIdx;
        subAliasIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
        subAliasIdx = txnLogManager.wrap( partition.getSuffixDn(), subAliasIdx );

        if ( ( ( Index<Object> ) subAliasIdx ).forward( ancestorId, targetId ) )
        {
            indexChange = new IndexChange( subAliasIdx, ancestorId, targetId, IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );
        }

        Dn suffixDn = partition.getSuffixDn();

        while ( !ancestorDn.equals( suffixDn ) && ancestorDn.size() > suffixDn.size() )
        {
            ancestorDn = ancestorDn.getParent();
            ancestorId = getEntryId( partition, ancestorDn );

            if ( ( ( Index<Object> ) subAliasIdx ).forward( ancestorId, targetId ) )
            {
                indexChange = new IndexChange( subAliasIdx, ancestorId, targetId, IndexChange.Type.DELETE, true );
                changeContainer.addChange( indexChange );
            }
        }

        // Drops the alias index entry
        indexChange = new IndexChange( aliasIdx, targetDn, aliasId, IndexChange.Type.DELETE, true );
        changeContainer.addChange( indexChange );
    }


    /**
     * For all aliases including and under the moved base, this method removes
     * one and subtree alias index tuples for old ancestors above the moved base
     * that will no longer be ancestors after the move.
     *
     * @param movedBase the base at which the move occured - the moved node
     * @throws Exception if system userIndices fail
     */
    @SuppressWarnings("unchecked")
    private String dropMovedAliasIndices( Partition partition, final Dn movedBase, DataChangeContainer changeContainer )
        throws Exception
    {
        UUID movedBaseId = getEntryId( partition, movedBase );
        boolean isAlias = false;

        Index<?> aliasIdx;
        aliasIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        aliasIdx = txnLogManager.wrap( partition.getSuffixDn(), aliasIdx );
        String targetDn = ( ( Index<String> ) aliasIdx ).reverseLookup( movedBaseId );

        if ( targetDn != null )
        {
            dropAliasIndices( partition, movedBase, movedBaseId, targetDn, changeContainer );
            isAlias = true;
        }

        return targetDn;
    }


    //---------------------------------------------------------------------------------------------
    // The Move operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void move( Partition partition, MoveOperationContext moveContext ) throws LdapException
    {
        if ( moveContext.getNewSuperior().isDescendantOf( moveContext.getDn() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }

        try
        {
            Dn oldDn = moveContext.getDn();
            Dn newSuperior = moveContext.getNewSuperior();
            Dn newDn = moveContext.getNewDn();
            Entry modifiedEntry = moveContext.getModifiedEntry();
            Entry originalEntry = ( ClonedServerEntry ) moveContext.getOriginalEntry();

            if ( originalEntry != null )
            {
                originalEntry = ( ( ClonedServerEntry ) originalEntry ).getOriginalEntry();
            }

            move( partition, oldDn, newSuperior, newDn, modifiedEntry, originalEntry );

            // Mostly for schema partition
            partition.move( moveContext );

        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void move( Partition partition, Dn oldDn, Dn newSuperiorDn, Dn newDn, Entry modifiedEntry,
        Entry originalEntry ) throws Exception
    {
        // Check that the parent Dn exists
        UUID newParentId = getEntryId( partition, newSuperiorDn );

        if ( newParentId == null )
        {
            // This is not allowed : the parent must exist
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, newSuperiorDn.getName() ) );
            throw ne;
        }

        // Now check that the new entry does not exist
        UUID newId = getEntryId( partition, newDn );

        if ( newId != null )
        {
            // This is not allowed : we should not be able to move an entry
            // to an existing position
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newSuperiorDn.getName() ) );
            throw ne;
        }

        // Add subtree dependency to old and new dn
        txnLogManager.addWrite( oldDn, SearchScope.SUBTREE );
        txnLogManager.addWrite( newDn, SearchScope.SUBTREE );

        // Get the entry and the old parent IDs
        UUID entryId = getEntryId( partition, oldDn );
        UUID oldParentId = getParentId( partition, entryId );

        // the below case arises only when the move( Dn oldDn, Dn newSuperiorDn, Dn newDn  ) is called
        // directly using the Store API, in this case the value of modified entry will be null
        // we need to lookup the entry to update the parent ID

        if ( originalEntry == null )
        {
            MasterTable master = partition.getMasterTable();
            master = txnLogManager.wrap( partition.getSuffixDn(), master );

            // First get the entry
            originalEntry = getEntry( partition, master, entryId );
        }

        if ( modifiedEntry == null )
        {
            modifiedEntry = originalEntry.clone();
        }

        if ( partition.updateEntryOnDnChange() )
        {
            handleDnChange( partition, oldDn, newDn, entryId );
        }

        moveInternal( partition, oldDn, newSuperiorDn, newDn, modifiedEntry, originalEntry, newParentId, entryId,
            oldParentId, false );
    }


    //---------------------------------------------------------------------------------------------
    // The MoveAndRename operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void moveAndRename( Partition partition, MoveAndRenameOperationContext moveAndRenameContext )
        throws LdapException
    {
        if ( moveAndRenameContext.getNewSuperiorDn().isDescendantOf( moveAndRenameContext.getDn() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }

        try
        {
            Dn oldDn = moveAndRenameContext.getDn();
            Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();
            Rdn newRdn = moveAndRenameContext.getNewRdn();
            boolean deleteOldRdn = moveAndRenameContext.getDeleteOldRdn();
            Entry modifiedEntry = moveAndRenameContext.getModifiedEntry();
            Entry originalEntry = ( ClonedServerEntry ) moveAndRenameContext.getOriginalEntry();

            if ( originalEntry != null )
            {
                originalEntry = ( ( ClonedServerEntry ) originalEntry ).getOriginalEntry();
            }

            moveAndRename( partition, oldDn, newSuperiorDn, newRdn, modifiedEntry, originalEntry, deleteOldRdn );
        }
        catch ( LdapException le )
        {
            // In case we get an LdapException, just rethrow it as is to 
            // avoid having it lost
            throw le;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( Partition partition, Dn oldDn, Dn newSuperiorDn, Rdn newRdn, Entry modifiedEntry,
        Entry originalEntry, boolean deleteOldRdn ) throws Exception
    {
        // Check that the old entry exists
        UUID oldId = getEntryId( partition, oldDn );

        if ( oldId == null )
        {
            // This is not allowed : the old entry must exist
            LdapNoSuchObjectException nse = new LdapNoSuchObjectException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, oldDn ) );
            throw nse;
        }

        // Check that the new superior exist
        UUID newSuperiorId = getEntryId( partition, newSuperiorDn );

        if ( newSuperiorId == null )
        {
            // This is not allowed : the new superior must exist
            LdapNoSuchObjectException nse = new LdapNoSuchObjectException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, newSuperiorDn ) );
            throw nse;
        }

        Dn newDn = newSuperiorDn.add( newRdn );

        UUID oldParentId = getParentId( partition, oldId );

        // Now check that the new entry does not exist
        UUID newId = getEntryId( partition, newDn );

        if ( newId != null )
        {
            // This is not allowed : we should not be able to move an entry
            // to an existing position
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newSuperiorDn.getName() ) );
            throw ne;
        }

        if ( originalEntry == null )
        {
            MasterTable master = partition.getMasterTable();
            master = txnLogManager.wrap( partition.getSuffixDn(), master );

            // First get the entry
            originalEntry = getEntry( partition, master, oldId );
        }

        if ( modifiedEntry == null )
        {
            modifiedEntry = originalEntry.clone();
        }

        if ( partition.updateEntryOnDnChange() )
        {
            handleDnChange( partition, oldDn, newDn, oldId );
        }

        renameInternal( partition, oldDn, newRdn, deleteOldRdn, modifiedEntry, originalEntry );

        Dn parentDn = oldDn.getParent();
        oldDn = new Dn( newRdn, parentDn );
        oldDn.apply( partition.getSchemaManager() );

        moveInternal( partition, oldDn, newSuperiorDn, newDn, modifiedEntry, originalEntry, newSuperiorId, oldId,
            oldParentId, true );
    }


    //---------------------------------------------------------------------------------------------
    // The Lookup operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public Entry lookup( Partition partition, LookupOperationContext lookupContext ) throws LdapException
    {
        UUID id = getEntryId( partition, lookupContext.getDn() );

        if ( id == null )
        {
            return null;
        }

        Entry entry = lookup( partition, id );

        // Remove all the attributes if the NO_ATTRIBUTE flag is set
        if ( lookupContext.hasNoAttribute() )
        {
            entry.clear();

            return entry;
        }

        if ( lookupContext.hasAllUser() )
        {
            if ( lookupContext.hasAllOperational() )
            {
                return entry;
            }
            else
            {
                for ( Attribute attribute : ( ( ( ClonedServerEntry ) entry ).getOriginalEntry() ).getAttributes() )
                {
                    AttributeType attributeType = attribute.getAttributeType();
                    String oid = attributeType.getOid();

                    if ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS )
                    {
                        if ( !lookupContext.getAttrsId().contains( oid ) )
                        {
                            entry.removeAttributes( attributeType );
                        }
                    }
                }
            }
        }
        else
        {
            if ( lookupContext.hasAllOperational() )
            {
                for ( Attribute attribute : ( ( ( ClonedServerEntry ) entry ).getOriginalEntry() ).getAttributes() )
                {
                    AttributeType attributeType = attribute.getAttributeType();

                    if ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS )
                    {
                        entry.removeAttributes( attributeType );
                    }
                }
            }
            else
            {
                if ( lookupContext.getAttrsId().size() == 0 )
                {
                    for ( Attribute attribute : ( ( ( ClonedServerEntry ) entry ).getOriginalEntry() ).getAttributes() )
                    {
                        AttributeType attributeType = attribute.getAttributeType();

                        if ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS )
                        {
                            entry.removeAttributes( attributeType );
                        }
                    }
                }
                else
                {
                    for ( Attribute attribute : ( ( ( ClonedServerEntry ) entry ).getOriginalEntry() ).getAttributes() )
                    {
                        AttributeType attributeType = attribute.getAttributeType();
                        String oid = attributeType.getOid();

                        if ( !lookupContext.getAttrsId().contains( oid ) )
                        {
                            entry.removeAttributes( attributeType );
                        }
                    }
                }
            }
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( Partition partition, UUID id ) throws LdapException
    {
        try
        {
            MasterTable master = partition.getMasterTable();
            master = txnLogManager.wrap( partition.getSuffixDn(), master );
            Entry entry = getEntry( partition, master, id );

            if ( entry != null )
            {
                return new ClonedServerEntry( partition.getSchemaManager(), entry );
            }

            return null;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( Partition partition, HasEntryOperationContext entryContext ) throws LdapException
    {
        try
        {
            UUID id = getEntryId( partition, entryContext.getDn() );

            Entry entry = lookup( partition, id );

            return entry != null;
        }
        catch ( LdapException e )
        {
            return false;
        }
    }


    //---------------------------------------------------------------------------------------------
    // The List operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( Partition partition, ListOperationContext listContext ) throws LdapException
    {
        try
        {
            return new BaseEntryFilteringCursor(
                new EntryCursorAdaptor( partition,
                    list( partition, getEntryId( partition, listContext.getDn() ) ),
                    txnManagerFactory ), listContext );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    public IndexCursor<UUID> list( Partition partition, UUID id ) throws LdapException
    {
        try
        {
            Index<UUID> oneLevelIdx = ( Index<UUID> ) partition
                .getSystemIndex( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            oneLevelIdx = ( Index<UUID> ) txnLogManager.wrap( partition.getSuffixDn(), oneLevelIdx );

            // We use the OneLevel index to get all the entries from a starting point
            // and below
            IndexCursor<UUID> cursor = oneLevelIdx.forwardCursor( id );
            cursor.beforeFirst();

            return cursor;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    //---------------------------------------------------------------------------------------------
    // ID and DN operations
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public UUID getEntryId( Partition partition, Dn dn ) throws LdapException
    {
        try
        {
            if ( Dn.isNullOrEmpty( dn ) )
            {
                return Partition.rootID;
            }

            Dn suffixDn = partition.getSuffixDn();
            ParentIdAndRdn suffixKey = new ParentIdAndRdn( Partition.rootID, suffixDn.getRdns() );

            Index<?> rdnIdx;
            rdnIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            rdnIdx = txnLogManager.wrap( partition.getSuffixDn(), rdnIdx );

            // Check into the Rdn index, starting with the partition Suffix
            UUID currentId = ( ( Index<Object> ) rdnIdx ).forwardLookup( suffixKey );

            for ( int i = dn.size() - suffixDn.size(); i > 0; i-- )
            {
                Rdn rdn = dn.getRdn( i - 1 );
                ParentIdAndRdn currentRdn = new ParentIdAndRdn( currentId, rdn );
                currentId = ( ( Index<Object> ) rdnIdx ).forwardLookup( currentRdn );

                if ( currentId == null )
                {
                    break;
                }
            }

            return currentId;
        }
        catch ( Exception e )
        {
            throw new LdapException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public Dn buildEntryDn( Partition partition, UUID id ) throws Exception
    {
        UUID parentId = id;
        UUID rootId = Partition.rootID;
        SchemaManager schemaManager = partition.getSchemaManager();

        StringBuilder upName = new StringBuilder();
        boolean isFirst = true;

        do
        {
            Index<?> rdnIdx;
            rdnIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            rdnIdx = txnLogManager.wrap( partition.getSuffixDn(), rdnIdx );
            ParentIdAndRdn cur = ( ( Index<ParentIdAndRdn> ) rdnIdx ).reverseLookup( parentId );

            Rdn[] rdns = cur.getRdns();

            for ( Rdn rdn : rdns )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    upName.append( ',' );
                }

                upName.append( rdn.getName() );
            }

            parentId = cur.getParentId();
        }
        while ( !parentId.equals( rootId ) );

        Dn dn = new Dn( schemaManager, upName.toString() );

        return dn;
    }


    private Entry getEntry( Partition partition, MasterTable wrappedTable, UUID id ) throws Exception
    {
        Entry entry = wrappedTable.get( id );

        if ( entry != null )
        {
            Dn dn = buildEntryDn( partition, id );
            entry.setDn( dn );
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public UUID getParentId( Partition partition, UUID childId ) throws Exception
    {
        Index<ParentIdAndRdn> rdnIdx;
        rdnIdx = ( Index<ParentIdAndRdn> ) partition.getSystemIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID );
        rdnIdx = ( Index<ParentIdAndRdn> ) txnLogManager.wrap( partition.getSuffixDn(), rdnIdx );
        ParentIdAndRdn key = rdnIdx.reverseLookup( childId );

        if ( key == null )
        {
            return null;
        }

        return key.getParentId();
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public int getChildCount( Partition partition, UUID id ) throws LdapOperationErrorException
    {
        IndexCursor<?> cursor = null;
        try
        {
            Index<?> oneLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            oneLevelIdx = txnLogManager.wrap( partition.getSuffixDn(), oneLevelIdx );
            cursor = ( ( Index<Object> ) oneLevelIdx ).forwardCursor( id );

            int count = 0;

            while ( cursor.next() )
            {
                count++;
            }

            return count;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
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
                    throw new LdapOperationErrorException( e.getMessage(), e );
                }
            }
        }
    }


    /**
     * Returns the id of the suffix entry for the given partition.
     *
     * @param partition partition for which we want to get the suffix dn
     * @return id of the suffix entry
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private UUID getSuffixId( Partition partition ) throws Exception
    {
        UUID suffixId;

        // TODO maybe store suffix id in partition 
        ParentIdAndRdn key = new ParentIdAndRdn( Partition.rootID, partition.getSuffixDn().getRdns() );
        Index<ParentIdAndRdn> rdnIdx;
        rdnIdx = ( Index<ParentIdAndRdn> ) partition.getSystemIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID );
        rdnIdx = ( Index<ParentIdAndRdn> ) txnLogManager.wrap( partition.getSuffixDn(), rdnIdx );

        suffixId = rdnIdx.forwardLookup( key );
        return suffixId;
    }


    /**
     * Updates the SubLevel Index as part of a move operation.
     *
     * @param partition partition of the moved base
     * @param entryId child id to be moved
     * @param oldParentId old parent's id
     * @param newParentId new parent's id
     * @param changeContainer container for the txn log edits
     * @throws Exception
     */
    private void updateSubLevelIndex( Partition partition, UUID entryId, UUID oldParentId, UUID newParentId,
        DataChangeContainer changeContainer ) throws Exception
    {
        UUID tempId = oldParentId;
        List<UUID> parentIds = new ArrayList<UUID>();
        UUID suffixId = getSuffixId( partition );
        IndexChange indexChange;

        // find all the parents of the oldParentId
        while ( ( tempId != null ) && !tempId.equals( Partition.rootID ) && !tempId.equals( suffixId ) )
        {
            parentIds.add( tempId );
            tempId = getParentId( partition, tempId );
        }

        Index<?> subLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
        subLevelIdx = txnLogManager.wrap( partition.getSuffixDn(), subLevelIdx );

        // find all the children of the childId
        Cursor<IndexEntry<UUID>> cursor = ( ( Index<UUID> ) subLevelIdx ).forwardCursor( entryId );

        List<UUID> childIds = new ArrayList<UUID>();
        childIds.add( entryId );

        try
        {
            while ( cursor.next() )
            {
                childIds.add( cursor.get().getId() );
            }
        }
        finally
        {
            cursor.close();
        }

        // detach the childId and all its children from oldParentId and all it parents excluding the root
        for ( UUID pid : parentIds )
        {
            for ( UUID cid : childIds )
            {
                indexChange = new IndexChange( subLevelIdx, pid, cid, IndexChange.Type.DELETE, true );
                changeContainer.addChange( indexChange );
            }
        }

        parentIds.clear();
        tempId = newParentId;

        // find all the parents of the newParentId
        while ( ( tempId != null ) && !tempId.equals( Partition.rootID ) && !tempId.equals( suffixId ) )
        {
            parentIds.add( tempId );
            tempId = getParentId( partition, tempId );
        }

        // attach the childId and all its children to newParentId and all it parents excluding the root
        for ( UUID id : parentIds )
        {
            for ( UUID cid : childIds )
            {
                indexChange = new IndexChange( subLevelIdx, id, cid, IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
            }
        }
    }


    private void moveInternal( Partition partition, Dn oldDn, Dn newSuperiorDn, Dn newDn, Entry modifiedEntry,
        Entry originalEntry, UUID newParentId, UUID entryId, UUID oldParentId, boolean afterRename ) throws Exception
    {
        DataChangeContainer changeContainer = new DataChangeContainer( partition );
        changeContainer.setEntryID( entryId );
        IndexChange indexChange;

        /*
         * All aliases including and below oldChildDn, will be affected by
         * the move operation with respect to one and subtree userIndices since
         * their relationship to ancestors above oldChildDn will be
         * destroyed.  For each alias below and including oldChildDn we will
         * drop the index tuples mapping ancestor ids above oldChildDn to the
         * respective target ids of the aliases.
         */
        String aliasTargetDn = dropMovedAliasIndices( partition, oldDn, changeContainer );

        /*
         * Drop the old parent child relationship and add the new one
         * Set the new parent id for the child replacing the old parent id
         */
        Index<?> oneLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );

        indexChange = new IndexChange( oneLevelIdx, oldParentId, entryId, IndexChange.Type.DELETE, true );
        changeContainer.addChange( indexChange );

        indexChange = new IndexChange( oneLevelIdx, newParentId, entryId, IndexChange.Type.ADD, true );
        changeContainer.addChange( indexChange );

        updateSubLevelIndex( partition, entryId, oldParentId, newParentId, changeContainer );

        // Update the Rdn index
        Index<?> rdnIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID );

        ParentIdAndRdn oldKey = new ParentIdAndRdn( oldParentId, newDn.getRdn() );
        indexChange = new IndexChange( rdnIdx, oldKey, entryId, IndexChange.Type.DELETE, true );
        changeContainer.addChange( indexChange );

        ParentIdAndRdn newKey = new ParentIdAndRdn( newParentId, newDn.getRdn() );
        indexChange = new IndexChange( rdnIdx, newKey, entryId, IndexChange.Type.ADD, true );
        changeContainer.addChange( indexChange );

        /*
         * Read Alias Index Tuples
         *
         * If this is a name change due to a move operation then the one and
         * subtree userIndices for aliases were purged before the aliases were
         * moved.  Now we must add them for each alias entry we have moved.
         *
         * aliasTarget is used as a marker to tell us if we're moving an
         * alias.  If it is null then the moved entry is not an alias.
         */
        if ( aliasTargetDn != null )
        {
            addAliasIndices( partition, entryId, newDn, aliasTargetDn, changeContainer );
        }

        // Update the master table with the modified entry
        modifiedEntry.put( SchemaConstants.ENTRY_PARENT_ID_AT, newParentId.toString() );

        /*
         * Finally prepare the log edit for the entry change
         */
        modifiedEntry.setDn( newDn );
        EntryReplace entryReplace = new EntryReplace( modifiedEntry, originalEntry );
        changeContainer.addChange( entryReplace );

        // log the change
        txnLogManager.log( changeContainer, false );
    }


    @SuppressWarnings("unchecked")
    private void renameInternal( Partition partition, Dn dn, Rdn newRdn, boolean deleteOldRdn, Entry entry,
        Entry originalEntry )
        throws Exception
    {
        UUID id = getEntryId( partition, dn );
        SchemaManager schemaManager = partition.getSchemaManager();
        Dn suffixDn = partition.getSuffixDn();

        DataChangeContainer changeContainer = new DataChangeContainer( partition );
        changeContainer.setEntryID( id );
        IndexChange indexChange;

        if ( originalEntry == null )
        {
            MasterTable master = partition.getMasterTable();
            master = txnLogManager.wrap( partition.getSuffixDn(), master );

            // First get the entry
            originalEntry = getEntry( partition, master, id );
        }

        if ( entry == null )
        {
            entry = originalEntry.clone();
        }

        Dn updn = originalEntry.getDn();

        newRdn.apply( schemaManager );

        Dn parentDn = updn.getParent();
        Dn newDn = new Dn( newRdn, parentDn );
        newDn.apply( schemaManager );

        // Add subtree dependency to old and new dn
        txnLogManager.addWrite( updn, SearchScope.SUBTREE );
        txnLogManager.addWrite( newDn, SearchScope.SUBTREE );

        /*
         * H A N D L E   N E W   R D N
         * ====================================================================
         * Add the new Rdn attribute to the entry.  If an index exists on the
         * new Rdn attribute we add the index for this attribute value pair.
         * Also we make sure that the presence index shows the existence of the
         * new Rdn attribute within this entry.
         */

        for ( Ava newAtav : newRdn )
        {
            String newNormType = newAtav.getNormType();
            Object newNormValue = newAtav.getNormValue().getValue();

            AttributeType newRdnAttrType = schemaManager.lookupAttributeTypeRegistry( newNormType );

            if ( partition.hasUserIndexOn( newRdnAttrType ) )
            {
                Index<?> index = partition.getUserIndex( newRdnAttrType );
                index = txnLogManager.wrap( partition.getSuffixDn(), index );

                if ( !( ( Index<Object> ) index ).forward( newNormValue, id ) )
                {
                    indexChange = new IndexChange( index, newNormValue, id, IndexChange.Type.ADD, false );
                    changeContainer.addChange( indexChange );
                }

                // Check the entry before modifying it below so that we can check if we need to update the presence index.
                Attribute curAttribute = entry.get( newRdnAttrType );

                if ( ( curAttribute == null ) || ( curAttribute.size() == 0 ) )
                {
                    Index<?> presenceIdx;
                    presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
                    indexChange = new IndexChange( presenceIdx, newNormType, id, IndexChange.Type.ADD, true );
                    changeContainer.addChange( indexChange );
                }
            }

            // Change the entry
            entry.add( newRdnAttrType, newAtav.getNormValue() );

        }

        /*
         * H A N D L E   O L D   R D N
         * ====================================================================
         * If the old Rdn is to be removed we need to get the attribute and
         * value for it.  Keep in mind the old Rdn need not be based on the
         * same attr as the new one.  We remove the Rdn value from the entry
         * and remove the value/id tuple from the index on the old Rdn attr
         * if any.  We also test if the delete of the old Rdn index tuple
         * removed all the attribute values of the old Rdn using a reverse
         * lookup.  If so that means we blew away the last value of the old
         * Rdn attribute.  In this case we need to remove the attrName/id
         * tuple from the presence index.
         *
         * We only remove an ATAV of the old Rdn if it is not included in the
         * new Rdn.
         */

        if ( deleteOldRdn )
        {
            Rdn oldRdn = updn.getRdn();

            for ( Ava oldAtav : oldRdn )
            {
                // check if the new ATAV is part of the old Rdn
                // if that is the case we do not remove the ATAV
                boolean mustRemove = true;

                for ( Ava newAtav : newRdn )
                {
                    if ( oldAtav.equals( newAtav ) )
                    {
                        mustRemove = false;
                        break;
                    }
                }

                if ( mustRemove )
                {
                    String oldNormType = oldAtav.getNormType();
                    String oldNormValue = oldAtav.getNormValue().getString();
                    AttributeType oldRdnAttrType = schemaManager.lookupAttributeTypeRegistry( oldNormType );
                    entry.remove( oldRdnAttrType, oldNormValue );

                    if ( partition.hasUserIndexOn( oldRdnAttrType ) )
                    {
                        Index<?> index = partition.getUserIndex( oldRdnAttrType );
                        indexChange = new IndexChange( index, oldNormValue, id, IndexChange.Type.DELETE, false );
                        changeContainer.addChange( indexChange );

                        /*
                         * If there is no value for id in this index due to our
                         * drop above we remove the oldRdnAttr from the presence idx
                         */
                        Attribute curAttribute = entry.get( oldRdnAttrType );

                        if ( ( curAttribute == null ) || ( curAttribute.size() == 0 ) )
                        {
                            Index<?> presenceIdx;
                            presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
                            indexChange = new IndexChange( presenceIdx, oldNormType, id, IndexChange.Type.DELETE, true );
                            changeContainer.addChange( indexChange );
                        }
                    }
                }
            }
        }

        /*
         * H A N D L E   D N   C H A N G E
         * ====================================================================
         * We only need to update the Rdn index.
         * No need to calculate the new Dn.
         */

        Index<?> rdnIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID );
        UUID parentId = UUID.fromString( entry.get( SchemaConstants.ENTRY_PARENT_ID_AT ).getString() );

        ParentIdAndRdn oldKey = new ParentIdAndRdn( parentId, updn.getRdn() );
        indexChange = new IndexChange( rdnIdx, oldKey, id, IndexChange.Type.DELETE, true );
        changeContainer.addChange( indexChange );

        ParentIdAndRdn key = new ParentIdAndRdn( parentId, newRdn );
        indexChange = new IndexChange( rdnIdx, key, id, IndexChange.Type.ADD, true );
        changeContainer.addChange( indexChange );

        /*
         * Finally prepare the log edit for the entry change
         */
        entry.setDn( newDn );
        EntryReplace entryReplace = new EntryReplace( entry, originalEntry );
        changeContainer.addChange( entryReplace );

        // log the change
        txnLogManager.log( changeContainer, false );

    }


    private void handleDnChange( Partition partition, Dn oldDn, Dn newDn, UUID baseId ) throws Exception
    {
        Dn suffixDn = partition.getSuffixDn();
        SchemaManager schemaManager = partition.getSchemaManager();
        Index<?> subLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
        subLevelIdx = txnLogManager.wrap( suffixDn, subLevelIdx );

        MasterTable master = partition.getMasterTable();
        master = txnLogManager.wrap( partition.getSuffixDn(), master );

        Entry childEntry;
        Entry clonedChildEntry;
        UUID childId;
        Dn childDn;
        Dn childNewDn = null;
        IndexCursor<UUID> cursor = ( ( Index<UUID> ) subLevelIdx ).forwardCursor( baseId );

        try
        {
            while ( cursor.next() )
            {
                childId = cursor.get().getId();

                // Skip base entry

                if ( UUIDComparator.INSTANCE.compare( baseId, childId ) == 0 )
                {
                    continue;
                }

                childEntry = getEntry( partition, master, childId );
                clonedChildEntry = childEntry.clone();

                childDn = childEntry.getDn();
                int startIdx = childDn.size() - oldDn.size() - 1;

                for ( int idx = startIdx; idx >= 0; idx-- )
                {
                    if ( idx == startIdx )
                    {
                        childNewDn = new Dn( childDn.getRdn( idx ), newDn );
                        childNewDn.apply( schemaManager );
                    }
                    else
                    {
                        childNewDn = childNewDn.add( childDn.getRdn( idx ) );
                    }
                }

                clonedChildEntry.setDn( childNewDn );

                /*
                 * Prepare the log edit for the entry change
                 */
                DataChangeContainer changeContainer = new DataChangeContainer( partition );
                changeContainer.setEntryID( childId );
                EntryReplace entryReplace = new EntryReplace( clonedChildEntry, childEntry );
                changeContainer.addChange( entryReplace );
                txnLogManager.log( changeContainer, false );
            }
        }
        finally
        {
            cursor.close();
        }
    }

}
