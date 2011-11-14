package org.apache.directory.server.core.shared.partition;


import java.util.UUID;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.partition.index.ParentIdAndRdn;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.server.core.shared.txn.logedit.DataChangeContainer;
import org.apache.directory.server.core.shared.txn.logedit.EntryAddDelete;
import org.apache.directory.server.core.shared.txn.logedit.EntryChange;
import org.apache.directory.server.core.shared.txn.logedit.IndexChange;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapAliasDereferencingException;
import org.apache.directory.shared.ldap.model.exception.LdapAliasException;
import org.apache.directory.shared.ldap.model.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


public class DefaultOperationExecutionManager
{

    /** Txn log manager kept for fast access */
    private TxnLogManager txnLogManager;


    //---------------------------------------------------------------------------------------------
    // The Add operation
    //---------------------------------------------------------------------------------------------
    /**
     * Adds an entry to the given partition.
     *
     * @param partition 
     * @param addContext the context used  to add and entry to this ContextPartition
     * @throws LdapException if there are any problems
     */
    public void add( Partition partition, AddOperationContext addContext ) throws LdapException
    {
        try
        {
            Entry entry = ( ( ClonedServerEntry ) addContext.getEntry() ).getClonedEntry();
            Dn entryDn = entry.getDn();

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
            DataChangeContainer changeContainer = new DataChangeContainer( suffixDn );
            changeContainer.setEntryID( id );
            IndexChange indexChange;

            // Update the RDN index
            Index<?> rdnIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            indexChange = new IndexChange( rdnIdx, ApacheSchemaConstants.APACHE_RDN_AT_OID, key, id,
                IndexChange.Type.ADD, true );
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
                indexChange = new IndexChange( objectClassIdx, SchemaConstants.OBJECT_CLASS_AT_OID, value.getString(),
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
            indexChange = new IndexChange( oneLevelIdx, ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID, parentId, id,
                IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );

            // Update the SubLevel index
            UUID tempId = parentId;
            UUID suffixId = getSuffixId( partition );
            Index<?> subLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );

            while ( ( tempId != null ) && ( !tempId.equals( Partition.rootID ) ) && ( !tempId.equals( suffixId ) ) )
            {
                indexChange = new IndexChange( subLevelIdx, ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID, tempId, id,
                    IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
                tempId = getParentId( partition, tempId );
            }

            // making entry an ancestor/descendent of itself in sublevel index
            indexChange = new IndexChange( subLevelIdx, ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID, id, id,
                IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );

            // Update the EntryCsn index
            Attribute entryCsn = entry.get( SchemaConstants.ENTRY_CSN_AT );

            if ( entryCsn == null )
            {
                String msg = I18n.err( I18n.ERR_219, entryDn.getName(), entry );
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
            }

            Index<?> entryCsnIdx = partition.getSystemIndex( SchemaConstants.ENTRY_CSN_AT_OID );
            indexChange = new IndexChange( entryCsnIdx, SchemaConstants.ENTRY_CSN_AT_OID, entryCsn.getString(), id,
                IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );

            // Update the EntryUuid index
            Attribute entryUuid = entry.get( SchemaConstants.ENTRY_UUID_AT );

            if ( entryUuid == null )
            {
                String msg = I18n.err( I18n.ERR_220, entryDn.getName(), entry );
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
            }

            Index<?> entryUuidIdx = partition.getSystemIndex( SchemaConstants.ENTRY_UUID_AT_OID );
            indexChange = new IndexChange( entryUuidIdx, SchemaConstants.ENTRY_UUID_AT_OID, entryUuid.getString(), id,
                IndexChange.Type.ADD, true );
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
                        indexChange = new IndexChange( index, attributeOid, value.getValue(), id,
                            IndexChange.Type.ADD, false );
                        changeContainer.addChange( indexChange );
                    }

                    // Adds only those attributes that are indexed
                    indexChange = new IndexChange( presenceIdx, ApacheSchemaConstants.APACHE_PRESENCE_AT_OID,
                        attributeOid, id, IndexChange.Type.ADD, true );
                    changeContainer.addChange( indexChange );
                }
            }

            // Add the parentId in the entry
            entry.put( SchemaConstants.ENTRY_PARENT_ID_AT, parentId.toString() );

            // And finally prepare the entry change
            EntryAddDelete entryAdd = new EntryAddDelete( entry, EntryAddDelete.Type.ADD );
            changeContainer.addChange( entryAdd );
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
     * Deletes a leaf entry from this ContextPartition: non-leaf entries cannot be 
     * deleted until this operation has been applied to their children.
     *
     * @param partition partition entry lives in
     * @param deleteContext the context of the entry to
     * delete from this ContextPartition.
     * @throws Exception if there are any problems
     */
    public void delete( Partition partition, DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();

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
     * Delete the entry associated with a given Id
     * @param partition partition entry lives in
     * @param entryDn dn of the entry to be deleted
     * @param id The id of the entry to delete
     * @throws Exception If the deletion failed
     */
    public void delete( Partition partition, Dn entryDn, UUID id ) throws LdapException
    {
        try
        {
            MasterTable master = partition.getMasterTable();
            master = txnLogManager.wrap( partition.getSuffixDn(), master );

            // First get the entry
            Entry entry = master.get( id );

            if ( entry == null )
            {
                // Not allowed
                throw new LdapNoSuchObjectException( "Cannot find an entry for ID " + id );
            }

            Dn suffixDn = partition.getSuffixDn();
            DataChangeContainer changeContainer = new DataChangeContainer( suffixDn );
            changeContainer.setEntryID( id );
            IndexChange indexChange;

            Attribute objectClass = entry.get( SchemaConstants.OBJECT_CLASS_AT );

            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                dropAliasIndices( partition, entryDn, id, changeContainer );
            }

            // Update the ObjectClass index
            Index<?> objectClassIdx = partition.getSystemIndex( SchemaConstants.OBJECT_CLASS_AT_OID );

            for ( Value<?> value : objectClass )
            {
                indexChange = new IndexChange( objectClassIdx, SchemaConstants.OBJECT_CLASS_AT_OID, value.getString(),
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
            indexChange = new IndexChange( rdnIdx, ApacheSchemaConstants.APACHE_RDN_AT_OID, key, id,
                IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );

            // Handle one level idx
            Index<?> oneLevelIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            indexChange = new IndexChange( oneLevelIdx, ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID, parentId, id,
                IndexChange.Type.DELETE, true );
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
                    indexChange = new IndexChange( subLevelIdx, ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID,
                        indexEntry.getValue(), id, IndexChange.Type.DELETE, true );
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
            indexChange = new IndexChange( entryCsnIdx, SchemaConstants.ENTRY_CSN_AT_OID, entryCsn, id,
                IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );

            // Handle the uuid idx
            Index<?> entryUuidIdx = partition.getSystemIndex( SchemaConstants.ENTRY_UUID_AT_OID );
            indexChange = new IndexChange( entryUuidIdx, SchemaConstants.ENTRY_UUID_AT_OID, id.toString(), id,
                IndexChange.Type.DELETE, true );
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
                        indexChange = new IndexChange( index, attributeOid, value.getValue(), id,
                            IndexChange.Type.DELETE, false );
                        changeContainer.addChange( indexChange );
                    }

                    // Adds only those attributes that are indexed
                    indexChange = new IndexChange( presenceIdx, ApacheSchemaConstants.APACHE_PRESENCE_AT_OID,
                        attributeOid, id, IndexChange.Type.DELETE, true );
                    changeContainer.addChange( indexChange );
                }
            }

            master.remove( id );

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
     * Modifies an entry by adding, removing or replacing a set of attributes.
     *
     * @param partition partition entry lives in
     * @param modifyContext The context containing the modification operation 
     * to perform on the entry which is one of constants specified by the 
     * DirContext interface:
     * <code>ADD_ATTRIBUTE, REMOVE_ATTRIBUTE, REPLACE_ATTRIBUTE</code>.
     * 
     * @throws Exception if there are any problems
     */
    public void modify( Partition partition, ModifyOperationContext modifyContext ) throws LdapException
    {
        try
        {
            Entry modifiedEntry = modify( partition, modifyContext.getDn(),
                modifyContext.getModItems().toArray( new Modification[]
                    {} ) );
            modifyContext.setAlteredEntry( modifiedEntry );
        }
        catch ( Exception e )
        {
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
        Entry entry = master.get( id );

        DataChangeContainer changeContainer = new DataChangeContainer( partition.getSuffixDn() );
        changeContainer.setEntryID( id );

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

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            index = partition.getSystemIndex( attributeType );
            for ( Value<?> value : mods )
            {
                indexChange = new IndexChange( index, modsOid, value.getString(), id, IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
            }
        }
        else if ( partition.hasUserIndexOn( attributeType ) )
        {
            index = partition.getUserIndex( attributeType );

            for ( Value<?> value : mods )
            {
                ( ( Index<Object> ) index ).add( value.getValue(), id );
                indexChange = new IndexChange( index, modsOid, value, id, IndexChange.Type.ADD, false );
            }

            // If the attr didn't exist for this id then created a log edit for an add for the presence index
            Index<?> presenceIdx;
            presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
            presenceIdx = txnLogManager.wrap( partition.getSuffixDn(), presenceIdx );

            if ( !( ( Index<Object> ) presenceIdx ).forward( modsOid, id ) )
            {
                indexChange = new IndexChange( presenceIdx, ApacheSchemaConstants.APACHE_PRESENCE_AT_OID, modsOid, id,
                    IndexChange.Type.ADD, true );
                changeContainer.addChange( indexChange );
            }
        }

        // create log edit for the entry change
        EntryChange entryChange = new EntryChange( mod, null );
        changeContainer.addChange( entryChange );

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

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            Index<?> objectClassIdx;
            objectClassIdx = partition.getSystemIndex( modsOid );
            objectClassIdx = txnLogManager.wrap( partition.getSuffixDn(), objectClassIdx );

            // TODO check if 

            // if the id exists in the index drop all existing attribute
            // value index entries and add new ones
            IndexCursor<?> indexCursor = objectClassIdx.reverseCursor( id );
            IndexEntry<?> indexEntry;

            try
            {
                while ( indexCursor.next() )
                {
                    indexEntry = indexCursor.get();
                    indexChange = new IndexChange( objectClassIdx, modsOid, indexEntry.getValue(), id,
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
                indexChange = new IndexChange( objectClassIdx, modsOid, value.getString(), id, IndexChange.Type.ADD,
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
                    indexChange = new IndexChange( index, modsOid, indexEntry.getValue(), id, IndexChange.Type.DELETE,
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
                indexChange = new IndexChange( index, modsOid, value.getValue(), id, IndexChange.Type.ADD, false );
                changeContainer.addChange( indexChange );
            }
            /*
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( mods.size() == 0 )
            {
                Index<?> presenceIdx;
                presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
                indexChange = new IndexChange( presenceIdx, ApacheSchemaConstants.APACHE_PRESENCE_AT_OID, modsOid, id,
                    IndexChange.Type.DELETE, true );
                changeContainer.addChange( indexChange );
            }
        }
        else if ( modsOid.equals( SchemaConstants.ENTRY_CSN_AT_OID ) )
        {
            Index<?> entryCsnIdx;
            entryCsnIdx = partition.getSystemIndex( SchemaConstants.ENTRY_CSN_AT_OID );
            indexChange = new IndexChange( entryCsnIdx, modsOid, entry.get( SchemaConstants.ENTRY_CSN_AT ).getString(),
                id, IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );

            indexChange = new IndexChange( entryCsnIdx, modsOid, mods.getString(), id, IndexChange.Type.ADD, true );
            changeContainer.addChange( indexChange );
        }

        String aliasAttributeOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.ALIASED_OBJECT_NAME_AT );

        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            dropAliasIndices( partition, entryDn, id, changeContainer );
        }

        // create log edit for the entry change
        Modification undo = null;
        Attribute replacedAttribute = entry.get( attributeType );

        if ( replacedAttribute != null )
        {
            undo = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, replacedAttribute );
        }

        EntryChange entryChange = new EntryChange( mod, undo );
        changeContainer.addChange( entryChange );

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
                        indexChange = new IndexChange( objectClassIdx, modsOid, indexEntry.getValue(), id,
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
                    indexChange = new IndexChange( objectClassIdx, modsOid, value.getString(), id,
                        IndexChange.Type.DELETE, true );
                    changeContainer.addChange( indexChange );
                }
            }
        }
        else if ( partition.hasUserIndexOn( attributeType ) )
        {
            index = partition.getUserIndex( attributeType );
            index = txnLogManager.wrap( partition.getSuffixDn(), index );

            /*
             * If there are no attribute values in the modifications then this
             * implies the complete removal of the attribute from the index. Else
             * we remove individual tuples from the index.
             */
            if ( mods.size() == 0 )
            {
                // if the id exists in the index drop all existing attribute
                // value index entries and add new ones
                IndexCursor<?> indexCursor = index.reverseCursor( id );
                IndexEntry<?> indexEntry;

                try
                {
                    while ( indexCursor.next() )
                    {
                        indexEntry = indexCursor.get();
                        indexChange = new IndexChange( index, modsOid, indexEntry.getValue(), id,
                            IndexChange.Type.DELETE, false );
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
                    indexChange = new IndexChange( index, modsOid, value.getValue(), id, IndexChange.Type.DELETE, false );
                    changeContainer.addChange( indexChange );
                }
            }

            /*
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( null == index.reverseLookup( id ) )
            {
                Index<?> presenceIdx;
                presenceIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
                indexChange = new IndexChange( presenceIdx, ApacheSchemaConstants.APACHE_PRESENCE_AT_OID, modsOid, id,
                    IndexChange.Type.DELETE, true );
                changeContainer.addChange( indexChange );
            }
        }

        // Prepare the entry change
        Modification undo = null;
        Attribute replacedAttribute = entry.get( attributeType );

        if ( replacedAttribute != null )
        {
            undo = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, replacedAttribute );
        }

        EntryChange entryChange = new EntryChange( mod, undo );
        changeContainer.addChange( entryChange );

        // Aliases->single valued comp/partial attr removal is not relevant here
        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            dropAliasIndices( partition, entryDn, id, changeContainer );
        }
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
        indexChange = new IndexChange( aliasIdx, ApacheSchemaConstants.APACHE_ALIAS_AT_OID,
            normalizedAliasTargetDn.getNormName(), aliasId, IndexChange.Type.ADD, true );
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
            indexChange = new IndexChange( oneAliasIdx, ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID, ancestorId,
                targetId, IndexChange.Type.ADD, true );
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
                indexChange = new IndexChange( subAliasIdx, ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID, ancestorId,
                    targetId, IndexChange.Type.ADD, true );
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
    private void dropAliasIndices( Partition partition, Dn aliasDn, UUID aliasId, DataChangeContainer changeContainer )
        throws Exception
    {
        SchemaManager schemaManager = partition.getSchemaManager();

        Index<?> aliasIdx;
        aliasIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        aliasIdx = txnLogManager.wrap( partition.getSuffixDn(), aliasIdx );
        String targetDn = ( ( Index<String> ) aliasIdx ).reverseLookup( aliasId );
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
            indexChange = new IndexChange( oneAliasIdx, ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID, ancestorId,
                targetId, IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );
        }

        Index<?> subAliasIdx;
        subAliasIdx = partition.getSystemIndex( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
        subAliasIdx = txnLogManager.wrap( partition.getSuffixDn(), subAliasIdx );

        if ( ( ( Index<Object> ) subAliasIdx ).forward( ancestorId, targetId ) )
        {
            indexChange = new IndexChange( subAliasIdx, ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID, ancestorId,
                targetId, IndexChange.Type.DELETE, true );
            changeContainer.addChange( indexChange );
        }

        Dn suffixDn = partition.getSuffixDn();

        while ( !ancestorDn.equals( suffixDn ) && ancestorDn.size() > suffixDn.size() )
        {
            ancestorDn = ancestorDn.getParent();
            ancestorId = getEntryId( partition, ancestorDn );

            if ( ( ( Index<Object> ) subAliasIdx ).forward( ancestorId, targetId ) )
            {
                indexChange = new IndexChange( subAliasIdx, ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID, ancestorId,
                    targetId, IndexChange.Type.DELETE, true );
                changeContainer.addChange( indexChange );
            }
        }

        // Drops the alias index entry
        indexChange = new IndexChange( aliasIdx, ApacheSchemaConstants.APACHE_ALIAS_AT_OID, targetDn, aliasId,
            IndexChange.Type.DELETE, true );
        changeContainer.addChange( indexChange );
    }


    //---------------------------------------------------------------------------------------------
    // ID and DN operations
    //---------------------------------------------------------------------------------------------
    /**
     * Returns the entry id for the given dn
     *
     * @param partition partition the given dn corresponds to
     * @param dn dn for which we want to get the id
     * @return entry id
     * @throws LdapException
     */
    @SuppressWarnings("unchecked")
    private UUID getEntryId( Partition partition, Dn dn ) throws LdapException
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
     * Gets the parent id of the given child id.
     *
     * @param partition partitin childId lives in.
     * @param childId id of the entry for which we want to get the parent id.
     * @return parent id
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private UUID getParentId( Partition partition, UUID childId ) throws Exception
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

}
