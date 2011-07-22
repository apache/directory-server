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
package org.apache.directory.server.core.partition.impl.xdbm;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.model.name.Ava;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.exception.MultiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for XDBM partitions that use an {@link Store}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractXdbmPartition<ID extends Comparable<ID>> extends AbstractBTreePartition<ID>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractXdbmPartition.class );

    protected boolean optimizerEnabled = true;

    protected AbstractXdbmPartition( SchemaManager schemaManager )
    {
        super( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    protected void doDestroy() throws LdapException, Exception
    {
        LOG.debug( "destroy() called on store for {}", this.suffixDn );

        if ( !initialized )
        {
            return;
        }

        // don't reset initialized flag
        initialized = false;

        MultiException errors = new MultiException( I18n.err( I18n.ERR_577 ) );

        for ( Index<?, Entry, ID> index : userIndices.values() )
        {
            try
            {
                index.close();
                LOG.debug( "Closed {} user index for {} partition.", index.getAttributeId(), suffixDn );
            }
            catch ( Throwable t )
            {
                LOG.error( I18n.err( I18n.ERR_124 ), t );
                errors.addThrowable( t );
            }
        }

        for ( Index<?, Entry, ID> index : systemIndices.values() )
        {
            try
            {
                index.close();
                LOG.debug( "Closed {} system index for {} partition.", index.getAttributeId(), suffixDn );
            }
            catch ( Throwable t )
            {
                LOG.error( I18n.err( I18n.ERR_124 ), t );
                errors.addThrowable( t );
            }
        }

        try
        {
            master.close();
            LOG.debug( I18n.err( I18n.ERR_125, suffixDn ) );
        }
        catch ( Throwable t )
        {
            LOG.error( I18n.err( I18n.ERR_126 ), t );
            errors.addThrowable( t );
        }

        if ( errors.size() > 0 )
        {
            throw errors;
        }
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------

    public boolean isOptimizerEnabled()
    {
        return optimizerEnabled;
    }


    public void setOptimizerEnabled( boolean optimizerEnabled )
    {
        this.optimizerEnabled = optimizerEnabled;
    }


    // ------------------------------------------------------------------------
    // I N D E X   M E T H O D S
    // ------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public final ID getEntryId( Dn dn ) throws LdapException
    {
        try
        {
            if ( Dn.isNullOrEmpty( dn ) )
            {
                return getRootId();
            }

            ParentIdAndRdn<ID> suffixKey = new ParentIdAndRdn<ID>( getRootId(), suffixDn.getRdns() );

            // Check into the Rdn index, starting with the partition Suffix
            ID currentId = rdnIdx.forwardLookup( suffixKey );

            for ( int i = dn.size() - suffixDn.size(); i > 0; i-- )
            {
                Rdn rdn = dn.getRdn( i - 1 );
                ParentIdAndRdn<ID> currentRdn = new ParentIdAndRdn<ID>( currentId, rdn );
                currentId = rdnIdx.forwardLookup( currentRdn );

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
    public final Dn getEntryDn( ID id ) throws Exception
    {
        return buildEntryDn( id );
    }


    //---------------------------------------------------------------------------------------------
    // Operations 
    //---------------------------------------------------------------------------------------------
    /**
     * Removes the index entries for an alias before the entry is deleted from
     * the master table.
     *
     * @todo Optimize this by walking the hierarchy index instead of the name
     * @param aliasId the id of the alias entry in the master table
     * @throws LdapException if we cannot parse ldap names
     * @throws Exception if we cannot delete index values in the database
     */
    protected void dropAliasIndices( ID aliasId ) throws Exception
    {
        String targetDn = aliasIdx.reverseLookup( aliasId );
        ID targetId = getEntryId( new Dn( schemaManager, targetDn ) );

        if ( targetId == null )
        {
            // the entry doesn't exist, probably it has been deleted or renamed
            // TODO: this is just a workaround for now, the alias indices should be updated when target entry is deleted or removed
            return;
        }

        Dn aliasDn = getEntryDn( aliasId );

        Dn ancestorDn = aliasDn.getParent();
        ID ancestorId = getEntryId( ancestorDn );

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
        oneAliasIdx.drop( ancestorId, targetId );
        subAliasIdx.drop( ancestorId, targetId );

        while ( !ancestorDn.equals( suffixDn ) && ancestorDn.size() > suffixDn.size() )
        {
            ancestorDn = ancestorDn.getParent();
            ancestorId = getEntryId( ancestorDn );

            subAliasIdx.drop( ancestorId, targetId );
        }

        // Drops all alias tuples pointing to the id of the alias to be deleted
        aliasIdx.drop( aliasId );
    }


    /**
     * For all aliases including and under the moved base, this method removes
     * one and subtree alias index tuples for old ancestors above the moved base
     * that will no longer be ancestors after the move.
     *
     * @param movedBase the base at which the move occured - the moved node
     * @throws Exception if system userIndices fail
     */
    protected void dropMovedAliasIndices( final Dn movedBase ) throws Exception
    {
        ID movedBaseId = getEntryId( movedBase );

        if ( aliasIdx.reverseLookup( movedBaseId ) != null )
        {
            dropAliasIndices( movedBaseId, movedBase );
        }
    }


    /**
     * For the alias id all ancestor one and subtree alias tuples are moved
     * above the moved base.
     *
     * @param aliasId the id of the alias
     * @param movedBase the base where the move occured
     * @throws Exception if userIndices fail
     */
    protected void dropAliasIndices( ID aliasId, Dn movedBase ) throws Exception
    {
        String targetDn = aliasIdx.reverseLookup( aliasId );
        ID targetId = getEntryId( new Dn( schemaManager, targetDn ) );
        Dn aliasDn = getEntryDn( aliasId );

        /*
         * Start droping index tuples with the first ancestor right above the
         * moved base.  This is the first ancestor effected by the move.
         */
        Dn ancestorDn = new Dn( schemaManager, movedBase.getRdn( movedBase.size() - 1 ) );
        ID ancestorId = getEntryId( ancestorDn );

        /*
         * We cannot just drop all tuples in the one level and subtree userIndices
         * linking baseIds to the targetId.  If more than one alias refers to
         * the target then droping all tuples with a value of targetId would
         * make all other aliases to the target inconsistent.
         *
         * We need to walk up the path of alias ancestors right above the moved
         * base until we reach the upSuffix, deleting each ( ancestorId,
         * targetId ) tuple in the subtree scope alias.  We only need to do
         * this for the direct parent of the alias on the one level subtree if
         * the moved base is the alias.
         */
        if ( aliasDn.equals( movedBase ) )
        {
            oneAliasIdx.drop( ancestorId, targetId );
        }

        subAliasIdx.drop( ancestorId, targetId );

        while ( !ancestorDn.equals( suffixDn ) )
        {
            ancestorDn = new Dn( schemaManager, ancestorDn.getRdn( ancestorDn.size() - 1 ) );
            ancestorId = getEntryId( ancestorDn );

            subAliasIdx.drop( ancestorId, targetId );
        }
    }


    //---------------------------------------------------------------------------------------------
    // Specific operation implementations
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void delete( ID id ) throws LdapException
    {
        try
        {
            // First get the entry
            Entry entry = master.get( id );
            
            if ( entry == null )
            {
                // Not allowed
                throw new LdapNoSuchObjectException( "Cannot find an entry for ID " + id );
            }

            Attribute objectClass = entry.get( OBJECT_CLASS_AT );

            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                dropAliasIndices( id );
            }

            // Update the ObjectClass index
            for ( Value<?> value : objectClass )
            {
                objectClassIdx.drop( value.getString(), id );
            }

            // Update the rdn, oneLevel, subLevel, entryCsn and entryUuid indexes
            rdnIdx.drop( id );
            oneLevelIdx.drop( id );
            subLevelIdx.drop( id );
            entryCsnIdx.drop( id );
            entryUuidIdx.drop( id );

            // Update the user indexes
            for ( Attribute attribute : entry )
            {
                AttributeType attributeType = attribute.getAttributeType();
                String attributeOid = attributeType.getOid();

                if ( hasUserIndexOn( attributeType ) )
                {
                    Index<?, Entry, ID> index = getUserIndex( attributeType );

                    // here lookup by attributeId is ok since we got attributeId from
                    // the entry via the enumeration - it's in there as is for sure
                    for ( Value<?> value : attribute )
                    {
                        ( ( Index ) index ).drop( value.getValue(), id );
                    }

                    presenceIdx.drop( attributeOid, id );
                }
            }

            master.remove( id );
            
            // if this is a context entry reset the master table counter
            if ( id.equals( getDefaultId() ) )
            {
                master.resetCounter();
            }

            if ( isSyncOnWrite.get() )
            {
                sync();
            }
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public final IndexCursor<ID, Entry, ID> list( ID id ) throws LdapException
    {
        try
        {
            // We use the OneLevel index to get all the entries from a starting point
            // and below
            IndexCursor<ID, Entry, ID> cursor = oneLevelIdx.forwardCursor( id );
            cursor.beforeValue( id, null );
            
            return cursor;
         }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public final int getChildCount( ID id ) throws LdapException
    {
        try
        {
            return oneLevelIdx.count( id );
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
    public synchronized final void rename( Dn dn, Rdn newRdn, boolean deleteOldRdn, Entry entry ) throws Exception
    {
        ID id = getEntryId( dn );

        if ( entry == null )
        {
            entry = lookup( id );
        }

        Dn updn = entry.getDn();

        newRdn.apply( schemaManager );

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

            entry.add( newRdnAttrType, newAtav.getNormValue() );

            if ( hasUserIndexOn( newRdnAttrType ) )
            {
                Index<?, Entry, ID> index = getUserIndex( newRdnAttrType );
                ( ( Index ) index ).add( newNormValue, id );

                // Make sure the altered entry shows the existence of the new attrib
                if ( !presenceIdx.forward( newNormType, id ) )
                {
                    presenceIdx.add( newNormType, id );
                }
            }
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

                    if ( hasUserIndexOn( oldRdnAttrType ) )
                    {
                        Index<?, Entry, ID> index = getUserIndex( oldRdnAttrType );
                        ( ( Index ) index ).drop( oldNormValue, id );

                        /*
                         * If there is no value for id in this index due to our
                         * drop above we remove the oldRdnAttr from the presence idx
                         */
                        if ( null == index.reverseLookup( id ) )
                        {
                            presenceIdx.drop( oldNormType, id );
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

        ID parentId = getParentId( id );
        rdnIdx.drop( id );
        ParentIdAndRdn<ID> key = new ParentIdAndRdn<ID>( parentId, newRdn );
        rdnIdx.add( key, id );

        master.put( id, entry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    /**
     * {@inheritDoc}
     */
    public synchronized final void moveAndRename( Dn oldDn, Dn newSuperiorDn, Rdn newRdn, Entry modifiedEntry, boolean deleteOldRdn ) throws Exception
    {
        // Check that the old entry exists
        ID oldId = getEntryId( oldDn );

        if ( oldId == null )
        {
            // This is not allowed : the old entry must exist
            LdapNoSuchObjectException nse = new LdapNoSuchObjectException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, oldDn ) );
            throw nse;
        }

        // Check that the new superior exist
        ID newSuperiorId = getEntryId( newSuperiorDn );

        if ( newSuperiorId == null )
        {
            // This is not allowed : the new superior must exist
            LdapNoSuchObjectException nse = new LdapNoSuchObjectException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, newSuperiorDn ) );
            throw nse;
        }

        Dn newDn = newSuperiorDn.add( newRdn );

        // Now check that the new entry does not exist
        ID newId = getEntryId( newDn );

        if ( newId != null )
        {
            // This is not allowed : we should not be able to move an entry
            // to an existing position
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newSuperiorDn.getName() ) );
            throw ne;
        }

        rename( oldDn, newRdn, deleteOldRdn, modifiedEntry );
        moveAndRename( oldDn, oldId, newSuperiorDn, newRdn, modifiedEntry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    /**
     * Moves an entry under a new parent.  The operation causes a shift in the
     * parent child relationships between the old parent, new parent and the
     * child moved.  All other descendant entries under the child never change
     * their direct parent child relationships.  Hence after the parent child
     * relationship changes are broken at the old parent and set at the new
     * parent a modifyDn operation is conducted to handle name changes
     * propagating down through the moved child and its descendants.
     *
     * @param oldDn the normalized dn of the child to be moved
     * @param childId the id of the child being moved
     * @param newRdn the normalized dn of the new parent for the child
     * @param modifiedEntry the modified entry
     * @throws Exception if something goes wrong
     */
    private void moveAndRename( Dn oldDn, ID childId, Dn newSuperior, Rdn newRdn, Entry modifiedEntry ) throws Exception
    {
        // Get the child and the new parent to be entries and Ids
        ID newParentId = getEntryId( newSuperior );
        ID oldParentId = getParentId( childId );

        /*
         * All aliases including and below oldChildDn, will be affected by
         * the move operation with respect to one and subtree userIndices since
         * their relationship to ancestors above oldChildDn will be
         * destroyed.  For each alias below and including oldChildDn we will
         * drop the index tuples mapping ancestor ids above oldChildDn to the
         * respective target ids of the aliases.
         */
        dropMovedAliasIndices( oldDn );

        /*
         * Drop the old parent child relationship and add the new one
         * Set the new parent id for the child replacing the old parent id
         */
        oneLevelIdx.drop( oldParentId, childId );
        oneLevelIdx.add( newParentId, childId );

        updateSubLevelIndex( childId, oldParentId, newParentId );

        /*
         * Update the Rdn index
         */
        rdnIdx.drop( childId );
        ParentIdAndRdn<ID> key = new ParentIdAndRdn<ID>( newParentId, newRdn );
        rdnIdx.add( key, childId );

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
        String aliasTarget = aliasIdx.reverseLookup( childId );

        if ( null != aliasTarget )
        {
            addAliasIndices( childId, buildEntryDn( childId ), aliasTarget );
        }

        // Update the master table with the modified entry
        // Warning : this test is an hack. As we may call the Store API directly
        // we may not have a modified entry to update. For instance, if the ModifierName
        // or ModifyTimeStamp AT are not updated, there is no reason we want to update the
        // master table.
        if ( modifiedEntry != null )
        {
            modifiedEntry.put( SchemaConstants.ENTRY_PARENT_ID_AT, newParentId.toString() );
            master.put( childId, modifiedEntry );
        }
    }


    /**
     * Updates the SubLevel Index as part of a move operation.
     *
     * @param entryId child id to be moved
     * @param oldParentId old parent's id
     * @param newParentId new parent's id
     * @throws Exception
     */
    private void updateSubLevelIndex( ID entryId, ID oldParentId, ID newParentId ) throws Exception
    {
        ID tempId = oldParentId;
        List<ID> parentIds = new ArrayList<ID>();

        // find all the parents of the oldParentId
        while ( ( tempId != null ) && !tempId.equals( getRootId() ) && !tempId.equals( getSuffixId() ) )
        {
            parentIds.add( tempId );
            tempId = getParentId( tempId );
        }

        // find all the children of the childId
        Cursor<IndexEntry<ID, Entry, ID>> cursor = subLevelIdx.forwardCursor( entryId );

        List<ID> childIds = new ArrayList<ID>();
        childIds.add( entryId );

        while ( cursor.next() )
        {
            childIds.add( cursor.get().getId() );
        }

        // detach the childId and all its children from oldParentId and all it parents excluding the root
        for ( ID pid : parentIds )
        {
            for ( ID cid : childIds )
            {
                subLevelIdx.drop( pid, cid );
            }
        }

        parentIds.clear();
        tempId = newParentId;

        // find all the parents of the newParentId
        while ( ( tempId != null)  && !tempId.equals( getRootId() ) && !tempId.equals( getSuffixId() ) )
        {
            parentIds.add( tempId );
            tempId = getParentId( tempId );
        }

        // attach the childId and all its children to newParentId and all it parents excluding the root
        for ( ID id : parentIds )
        {
            for ( ID cid : childIds )
            {
                subLevelIdx.add( id, cid );
            }
        }
    }


    /**
     * updates the CSN index
     *
     * @param entry the entry having entryCSN attribute
     * @param id ID of the entry
     * @throws Exception
     */
    private void updateCsnIndex( Entry entry, ID id ) throws Exception
    {
        entryCsnIdx.drop( id );
        entryCsnIdx.add( entry.get( SchemaConstants.ENTRY_CSN_AT ).getString(), id );
    }


    /**
     * {@inheritDoc}
     */
    public synchronized final Entry modify( Dn dn, Modification... mods ) throws Exception
    {
        ID id = getEntryId( dn );
        Entry entry = master.get( id );
        
        for ( Modification mod : mods )
        {
            Attribute attrMods = mod.getAttribute();

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    modifyAdd( id, entry, attrMods );
                    break;

                case REMOVE_ATTRIBUTE:
                    modifyRemove( id, entry, attrMods );
                    break;

                case REPLACE_ATTRIBUTE:
                    modifyReplace( id, entry, attrMods );
                    break;

                default:
                    throw new LdapException( I18n.err( I18n.ERR_221 ) );
            }
        }

        updateCsnIndex( entry, id );
        master.put( id, entry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }

        return entry;
    }


    /**
     * Adds a set of attribute values while affecting the appropriate userIndices.
     * The entry is not persisted: it is only changed in anticipation for a put
     * into the master table.
     *
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the attribute and values to add
     * @throws Exception if index alteration or attribute addition fails
     */
    @SuppressWarnings("unchecked")
    private void modifyAdd( ID id, Entry entry, Attribute mods ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            for ( Value<?> value : mods )
            {
                objectClassIdx.add( value.getString(), id );
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, Entry, ID> index = getUserIndex( attributeType );

            for ( Value<?> value : mods )
            {
                ( ( Index ) index ).add( value.getValue(), id );
            }

            // If the attr didn't exist for this id add it to presence index
            if ( !presenceIdx.forward( modsOid, id ) )
            {
                presenceIdx.add( modsOid, id );
            }
        }

        // add all the values in mods to the same attribute in the entry

        for ( Value<?> value : mods )
        {
            entry.add( mods.getAttributeType(), value );
        }

        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            Dn ndn = getEntryDn( id );
            addAliasIndices( id, ndn, mods.getString() );
        }
    }


    /**
     * Completely replaces the existing set of values for an attribute with the
     * modified values supplied affecting the appropriate userIndices.  The entry
     * is not persisted: it is only changed in anticipation for a put into the
     * master table.
     *
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the replacement attribute and values
     * @throws Exception if index alteration or attribute modification
     * fails.
     */
    @SuppressWarnings("unchecked")
    private void modifyReplace( ID id, Entry entry, Attribute mods ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();

        // Special case for the ObjectClass index
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            // if the id exists in the index drop all existing attribute
            // value index entries and add new ones
            if ( objectClassIdx.reverse( id ) )
            {
                objectClassIdx.drop( id );
            }

            for ( Value<?> value : mods )
            {
                objectClassIdx.add( value.getString(), id );
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, Entry, ID> index = getUserIndex( attributeType );

            // if the id exists in the index drop all existing attribute
            // value index entries and add new ones
            if ( index.reverse( id ) )
            {
                ( ( Index<?, Entry, ID> ) index ).drop( id );
            }

            for ( Value<?> value : mods )
            {
                ( ( Index<Object, Entry, ID> ) index ).add( value.getValue(), id );
            }

            /*
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( null == index.reverseLookup( id ) )
            {
                presenceIdx.drop( modsOid, id );
            }
        }

        String aliasAttributeOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.ALIASED_OBJECT_NAME_AT );

        if ( mods.getAttributeType().equals( ALIASED_OBJECT_NAME_AT ) )
        {
            dropAliasIndices( id );
        }

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
            Dn entryDn = getEntryDn( id );
            addAliasIndices( id, entryDn, mods.getString() );
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
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the attribute and its values to delete
     * @throws Exception if index alteration or attribute modification fails.
     */
    @SuppressWarnings("unchecked")
    private void modifyRemove( ID id, Entry entry, Attribute mods ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();

        // Special case for the ObjectClass index
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            /*
             * If there are no attribute values in the modifications then this
             * implies the complete removal of the attribute from the index. Else
             * we remove individual tuples from the index.
             */
            if ( mods.size() == 0 )
            {
                objectClassIdx.drop( id );
            }
            else
            {
                for ( Value<?> value : mods )
                {
                    objectClassIdx.drop( value.getString(), id );
                }
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, Entry, ID> index = getUserIndex( attributeType );

            /*
             * If there are no attribute values in the modifications then this
             * implies the complete removal of the attribute from the index. Else
             * we remove individual tuples from the index.
             */
            if ( mods.size() == 0 )
            {
                ( ( Index ) index ).drop( id );
            }
            else
            {
                for ( Value<?> value : mods )
                {
                    ( ( Index ) index ).drop( value.getValue(), id );
                }
            }

            /*
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( null == index.reverseLookup( id ) )
            {
                presenceIdx.drop( modsOid, id );
            }
        }

        AttributeType attrType = schemaManager.lookupAttributeTypeRegistry( modsOid );

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
        if ( mods.getAttributeType().equals( ALIASED_OBJECT_NAME_AT ) )
        {
            dropAliasIndices( id );
        }
    }


    /**
     * {@inheritDoc}
     */
    public synchronized final void move( Dn oldDn, Dn newSuperiorDn, Dn newDn, Entry modifiedEntry ) throws Exception
    {
        // Check that the parent Dn exists
        ID newParentId = getEntryId( newSuperiorDn );

        if ( newParentId == null )
        {
            // This is not allowed : the parent must exist
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, newSuperiorDn.getName() ) );
            throw ne;
        }

        // Now check that the new entry does not exist
        ID newId = getEntryId( newDn );

        if ( newId != null )
        {
            // This is not allowed : we should not be able to move an entry
            // to an existing position
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newSuperiorDn.getName() ) );
            throw ne;
        }

        // Get the entry and the old parent IDs
        ID entryId = getEntryId( oldDn );
        ID oldParentId = getParentId( entryId );

        /*
         * All aliases including and below oldChildDn, will be affected by
         * the move operation with respect to one and subtree userIndices since
         * their relationship to ancestors above oldChildDn will be
         * destroyed.  For each alias below and including oldChildDn we will
         * drop the index tuples mapping ancestor ids above oldChildDn to the
         * respective target ids of the aliases.
         */
        dropMovedAliasIndices( oldDn );

        /*
         * Drop the old parent child relationship and add the new one
         * Set the new parent id for the child replacing the old parent id
         */
        oneLevelIdx.drop( oldParentId, entryId );
        oneLevelIdx.add( newParentId, entryId );

        updateSubLevelIndex( entryId, oldParentId, newParentId );

        // Update the Rdn index
        rdnIdx.drop( entryId );
        ParentIdAndRdn<ID> key = new ParentIdAndRdn<ID>( newParentId, oldDn.getRdn() );
        rdnIdx.add( key, entryId );


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
        String aliasTarget = aliasIdx.reverseLookup( entryId );

        if ( null != aliasTarget )
        {
            addAliasIndices( entryId, buildEntryDn( entryId ), aliasTarget );
        }

        // the below case arises only when the move( Dn oldDn, Dn newSuperiorDn, Dn newDn  ) is called
        // directly using the Store API, in this case the value of modified entry will be null
        // we need to lookup the entry to update the parent ID
        if ( modifiedEntry == null )
        {
            modifiedEntry = lookup( entryId );
        }
        
        // Update the master table with the modified entry
        modifiedEntry.put( SchemaConstants.ENTRY_PARENT_ID_AT, newParentId.toString() );
        master.put( entryId, modifiedEntry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "Partition<" + id + ">";
    }
}
