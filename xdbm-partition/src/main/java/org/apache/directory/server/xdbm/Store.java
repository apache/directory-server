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
package org.apache.directory.server.xdbm;


import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * Represents an entry store based on the Table, Index, and MasterTable
 * database structure.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Store<E, ID extends Comparable<ID>>
{
    /*
     * W H Y   H A V E   A   S T O R E   I N T E R F A C E  ?
     * ------------------------------------------------------
     *
     * Some may question why we have this Store interface when the Partition
     * interface abstracts away partition implementation details in the server
     * core.  This is due to a complicated chicken and egg problem with the
     * additional need to abstract stores for the SearchEngine.  This way the
     * SearchEngine and it's default implementation can be independent of the
     * Partition interface.  Once this is achieved the default SearchEngine
     * implementation can be removed from the core.  This will allow for
     * better modularization, with the ability to easily substitute new
     * SearchEngine implementations into ApacheDS.
     *
     *
     * H I S T O R Y
     * -------------
     *
     * Originally the JdbmStore class came about due to a cyclic dependency.
     * The bootstrap-partition module is created by the bootstrap-plugin
     * module.  The core depends on the bootstrap-partition module to
     * bootstrap the server.  The bootstrap-partition module depends on the
     * bootstrap-plugin which builds a JdbmStore stuffing it with all the
     * information needed for the server to bootstrap.  The bootstrap-plugin
     * hence must be built before it can generate the bootstrap-partition and
     * it cannot have a dependency on the core.  We could not use the
     * JdbmPartition because it depends on the Partition interface and this
     * is an integral part of the core.  If we did then there would be a
     * cyclic dependency between modules in the apacheds pom.  To avoid this
     * the JdbmStore class was created and the guts of the JDBM partition were
     * put into the jdbm-store module.  This jdbm-store module does not depend
     * on core and can be used by the bootstrap-plugin to build the
     * bootstrap-partition.
     *
     * Hence it's project dependencies that drove the creation of the
     * JdbmStore class.  Later we realized, the default SeachEngine used by
     * all Table, Index, MasterTable scheme based partitions depends on
     * BTreePartition which depends on Partition.  We would like to remove
     * this search engine out of the core so it can easily be swapped out,
     * but most importantly so we can have the search depend on any kind of
     * store.  There's no reason why the SearchEngine should depend on a
     * Partition (store with search capabilities) when it just needs a simple
     * store and it's indices to conduct search operations.
     */

    public static final String[] SYS_INDEX_OID_ARRAY =
        { ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID,

        ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID,

        ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID,

        ApacheSchemaConstants.APACHE_RDN_AT_OID,

        ApacheSchemaConstants.APACHE_N_DN_AT_OID,

        ApacheSchemaConstants.APACHE_ALIAS_AT_OID,

        ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID,

        ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID,

        SchemaConstants.ENTRY_CSN_AT_OID,

        SchemaConstants.ENTRY_UUID_AT_OID,

        SchemaConstants.OBJECT_CLASS_AT_OID };
    public static final Set<String> SYS_INDEX_OIDS = Collections.unmodifiableSet( new HashSet<String>( Arrays
        .asList( SYS_INDEX_OID_ARRAY ) ) );


    /**
     * Sets the partition path (working directory) for the store.
     * 
     * @param partitionDir the new partition path
     */
    void setPartitionPath( URI partitionPath );


    /**
     * Gets the partition path (working directory) for the store.
     * 
     * @return The current partition path (working directory) for the store
     */
    URI getPartitionPath();


    /**
     * Gets the user indices.
     * 
     * @return The list of user index
     */
    Set<Index<?, E, ID>> getUserIndices();


    /**
     * Sets the suffix Dn, must be normalized.
     * 
     * @param suffixDn the new suffix Dn
     */
    void setSuffixDn( Dn suffixDn );


    /**
     * Gets the suffix Dn.
     * 
     * @return the suffix Dn
     */
    Dn getSuffixDn();


    /**
     * Sets the flag telling the server to flush on disk when some
     * modification has been done.
     * @param isSyncOnWrite A boolean set to true if we have to flush on disk 
     * when a modification occurs
     */
    void setSyncOnWrite( boolean isSyncOnWrite );


    /**
     * @return <code>true</code> if we write to disk for every modification 
     */
    boolean isSyncOnWrite();


    /**
     * Sets the cache size for this store.
     * @param cacheSize The cache size
     */
    void setCacheSize( int cacheSize );


    /**
     * Gets the cache size for this store.
     * 
     * @return The cache size
     */
    int getCacheSize();


    /**
     * Sets the store's unique identifier.
     * @param id The store's unique identifier
     */
    void setId( String id );


    /**
     * Gets the store's unique identifier.
     * 
     * @return The store's unique identifier
     */
    String getId();


    /**
     * Initialize the JDBM storage system.
     *
     * @param schemaManager the schema schemaManager
     * @throws Exception on failure to lookup elements in schemaManager
     * @throws Exception on failure to create database files
     */
    void init( SchemaManager schemaManager ) throws Exception;


    /**
     * Close the store : we have to close all the userIndices and the master table.
     *
     * @throws Exception lazily thrown on any closer failures to avoid leaving
     * open files
     */
    void destroy() throws Exception;


    /**
     * Gets whether the store is initialized.
     *
     * @return true if the partition store is initialized
     */
    boolean isInitialized();


    /**
     * This method is called when the synch thread is waking up, to write
     * the modified data.
     *
     * @throws Exception on failures to sync database files to disk
     */
    void sync() throws Exception;


    /**
     * Adds a (system or user) index to the list of index for this store. 
     * Note that the attribute id returned by Index.getAttributeId() must be
     * the numeric OID. 
     * @param index The index to add
     * @throws Exception If the addition failed
     */
    void addIndex( Index<?, E, ID> index ) throws Exception;


    //------------------------------------------------------------------------
    // System index
    //------------------------------------------------------------------------
    /**
     * @return The Presence system index
     */
    Index<String, E, ID> getPresenceIndex();


    /**
     * @return The OneLevel system index
     */
    Index<ID, E, ID> getOneLevelIndex();


    /**
     * @return The SubLevel system index
     */
    Index<ID, E, ID> getSubLevelIndex();


    /**
     * @return The Alias system index
     */
    Index<String, E, ID> getAliasIndex();


    /**
     * @return The OneAlias system index
     */
    Index<ID, E, ID> getOneAliasIndex();


    /**
     * @return The SubAlias system index
     */
    Index<ID, E, ID> getSubAliasIndex();


    /**
     * @return The Rdn system index
     */
    Index<ParentIdAndRdn<ID>, E, ID> getRdnIndex();


    /**
     * @return The Ndn system index
     */
    Index<String, E, ID> getNdnIndex();


    /**
     * @return The ObjectClass system index
     */
    Index<String, E, ID> getObjectClassIndex();


    /**
     * @return The EntryUUID system index
     */
    Index<String, E, ID> getEntryUuidIndex();


    /**
     * @return The EntryCSN system index
     */
    Index<String, E, ID> getEntryCsnIndex();


    /**
     * An iterator build on top of the User's index
     */
    Iterator<String> userIndices();


    /**
     * An iterator build on top of the System's index
     */
    Iterator<String> systemIndices();


    /**
     * Tells if an index is already present in the User's <strong>or</strong> System's index list
     * @param id The index we are looking for
     * @return <code>true</code> if the index is already present in the
     * User's <strong>or</strong> System's index list 
     * @throws Exception If something went wrong
     */
    boolean hasIndexOn( String id ) throws Exception;


    /**
     * Tells if an index is already present in the User's <strong>or</strong> System's index list
     * @param attributeType The index we are looking for
     * @return <code>true</code> if the index is already present in the
     * User's <strong>or</strong> System's index list 
     * @throws Exception If something went wrong
     */
    boolean hasIndexOn( AttributeType attributeType ) throws Exception;


    /**
     * Tells if an index is already present in the User's index list
     * @param id The index we are looking for
     * @return <code>true</code> if the index is already present in the
     * User's index list 
     * @throws Exception If something went wrong
     */
    boolean hasUserIndexOn( String id ) throws Exception;


    /**
     * Tells if an index is already present in the User's index list
     * @param attributeType The attributeType index we are looking for
     * @return <code>true</code> if the index is already present in the
     * User's index list 
     * @throws Exception If something went wrong
     */
    boolean hasUserIndexOn( AttributeType attributeType ) throws Exception;


    /**
     * Tells if an index is already present in the System's index list
     * @param id The index we are looking for
     * @return <code>true</code> if the index is already present in the
     * System's index list 
     * @throws Exception If something went wrong
     */
    boolean hasSystemIndexOn( String id ) throws Exception;


    /**
     * Tells if an index is already present in the System's index list
     * @param attributeType The index we are looking for
     * @return <code>true</code> if the index is already present in the
     * System's index list 
     * @throws Exception If something went wrong
     */
    boolean hasSystemIndexOn( AttributeType attributeType ) throws Exception;


    /**
     * Get the user <strong>or</strong> system index associated with the given name
     * @param id The index name we are looking for
     * @return The associated user <strong>or</strong> system index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, E, ID> getIndex( String id ) throws IndexNotFoundException;


    /**
     * Get the user <strong>or</strong> system index associated with the given attributeType
     * @param attributeType The index attributeType we are looking for
     * @return The associated user <strong>or</strong> system index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, E, ID> getIndex( AttributeType attributeType ) throws IndexNotFoundException;


    /**
     * Get the user index associated with the given name
     * @param id The index name we are looking for
     * @return The associated user index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, E, ID> getUserIndex( String id ) throws IndexNotFoundException;


    /**
     * Get the user index associated with the given attributeType
     * @param attributeType The index attributeType we are looking for
     * @return The associated user index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, E, ID> getUserIndex( AttributeType attributeType ) throws IndexNotFoundException;


    /**
     * Get the system index associated with the given name
     * @param id The index name we are looking for
     * @return The associated system index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, E, ID> getSystemIndex( String id ) throws IndexNotFoundException;


    /**
     * Get the system index associated with the given attributeType
     * @param attributeType The index attributeType we are looking for
     * @return The associated system index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, E, ID> getSystemIndex( AttributeType attributeType ) throws IndexNotFoundException;


    /**
     * Gets the entry's id. Returns <code>null</code> if the Dn doesn't exist in this store.
     * Note that the Dn must be normalized!
     * @param dn the normalized entry Dn
     * @return the entry's id, or <code>null</code> if the Dn doesn't exists
     */
    ID getEntryId( Dn dn ) throws Exception;


    /**
     * Gets the normalized Dn of the entry identified by the given id.
     * @param id the entry's id
     * @return the normalized entry Dn
     */
    Dn getEntryDn( ID id ) throws Exception;


    /**
     * Gets the ID of an entry's parent using the child entry's ID.
     * Note that the suffix entry returns 0, which does not map to any entry.
     *
     * @param childId the ID of the entry
     * @return the id of the parent entry or zero if the suffix entry ID is used
     * @throws Exception on failures to access the underlying store
     */
    ID getParentId( ID childId ) throws Exception;


    /**
     * Gets the total count of entries within this store.
     *
     * @return the total count of entries within this store
     * @throws Exception on failures to access the underlying store
     */
    int count() throws Exception;


    /**
     * Add an entry into the store. 
     * 
     * @param entry The entry to add
     * 
     * @throws Exception If the addition failed.
     */
    void add( Entry entry ) throws Exception;


    /**
     * Get back an entry knowing its ID
     *
     * @param id The Entry ID we want to get back
     * @return The found Entry, or null if not found
     * @throws Exception If the lookup failed for any reason (except a not found entry)
     */
    Entry lookup( ID id ) throws Exception;


    /**
     * Delete the entry associated with a given Id
     * @param id The id of the entry to delete
     * @throws Exception If the deletion failed
     */
    void delete( ID id ) throws Exception;


    /**
     * Gets an IndexEntry Cursor over the child nodes of an entry.
     *
     * @param id the id of the parent entry
     * @return an IndexEntry Cursor over the child entries
     * @throws Exception on failures to access the underlying store
     */
    IndexCursor<ID, E, ID> list( ID id ) throws Exception;


    /**
     * Gets the count of immediate children of the given entry ID.
     *
     * @param id the entry ID
     * @return the child count 
     * @throws Exception on failures to access the underlying store
     */
    int getChildCount( ID id ) throws Exception;


    void setProperty( String propertyName, String propertyValue ) throws Exception;


    String getProperty( String propertyName ) throws Exception;


    void modify( Dn dn, ModificationOperation modOp, Entry mods ) throws Exception;


    /**
     * Modify an entry applying the given list of modifications.
     *
     * @param dn The Entry's Dn
     * @param mods The list of modifications
     * @return The modified entry
     * @throws Exception If the modification failed
     */
    Entry modify( Dn dn, List<Modification> mods ) throws Exception;


    /**
     * Changes the relative distinguished name of an entry specified by a
     * distinguished name with the optional removal of the old Rdn attribute
     * value from the entry.  Name changes propagate down as dn changes to the
     * descendants of the entry where the Rdn changed.
     *
     * An Rdn change operation does not change parent child relationships.  It
     * merely propagates a name change at a point in the DIT where the Rdn is
     * changed. The change propagates down the subtree rooted at the
     * distinguished name specified.
     *
     * @param dn the normalized distinguished name of the entry to alter
     * @param newRdn the new Rdn to set
     * @param deleteOldRdn whether or not to remove the old Rdn attr/val
     * @param entry the modified entry
     * @throws Exception if there are any errors propagating the name changes
     */
    void rename( Dn dn, Rdn newRdn, boolean deleteOldRdn, Entry entry ) throws Exception;


    /**
     * Changes the relative distinguished name of an entry specified by a
     * distinguished name with the optional removal of the old Rdn attribute
     * value from the entry.  Name changes propagate down as dn changes to the
     * descendants of the entry where the Rdn changed.
     *
     * An Rdn change operation does not change parent child relationships.  It
     * merely propagates a name change at a point in the DIT where the Rdn is
     * changed. The change propagates down the subtree rooted at the
     * distinguished name specified.
     *
     * @param dn the normalized distinguished name of the entry to alter
     * @param newRdn the new Rdn to set
     * @param deleteOldRdn whether or not to remove the old Rdn attr/val
     * @throws Exception if there are any errors propagating the name changes
     */
    void rename( Dn dn, Rdn newRdn, boolean deleteOldRdn ) throws Exception;

    
    void moveAndRename( Dn oldChildDn, Dn newParentDn, Rdn newRdn, Entry entry, boolean deleteOldRdn ) throws Exception;


    /**
     * <p>Move an entry from one place to the other. The Rdn remains unchanged,
     * the parent Dn changes</p>
     * <p>We have to update some of the index when moving an entry. Assuming
     * that the target destination does not exist, the following index must
     * be updated :</p>
     * <ul>
     * <li><b>oneLevel</b> index</li>
     * <li><b>subLevel</b> index</li>
     * </ul>
     * <p>If the moved entry is an alias, then we also have to update the
     * following index :</p>
     * <ul>
     * <li><b>oneAlias</b> index</li>
     * <li><b>subAlias</b> index</li>
     * </ul>
     * <p>The <b>Alias</b> index is not updated, as the entry ID won't change.</p> 
     * <p>We have a few check we must do before moving the entry :
     * <ul>
     * <li>The destination must not exist
     * <li>The moved entry must exist (this has already been checked)
     * <li>The moved entry must not inherit from a referral (already checked)
     * </ul>
     *
     * @param oldDn The previous entry Dn
     * @param newSuperior The new superior Dn
     * @param newDn The new Dn
     * @throws Exception If the move failed
     */
    void move( Dn oldDn, Dn newSuperior, Dn newDn ) throws Exception;


    /**
     * <p>Move an entry from one place to the other. The Rdn remains unchanged,
     * the parent Dn changes</p>
     * <p>We have to update some of the index when moving an entry. Assuming
     * that the target destination does not exist, the following index must
     * be updated :</p>
     * <ul>
     * <li><b>oneLevel</b> index</li>
     * <li><b>subLevel</b> index</li>
     * </ul>
     * <p>If the moved entry is an alias, then we also have to update the
     * following index :</p>
     * <ul>
     * <li><b>oneAlias</b> index</li>
     * <li><b>subAlias</b> index</li>
     * </ul>
     * <p>The <b>Alias</b> index is not updated, as the entry ID won't change.</p> 
     * <p>We have a few check we must do before moving the entry :
     * <ul>
     * <li>The destination must not exist
     * <li>The moved entry must exist (this has already been checked)
     * <li>The moved entry must not inherit from a referral (already checked)
     * </ul>
     *
     * @param oldDn The previous entry Dn
     * @param newSuperior The new superior Dn
     * @param newDn The new Dn
     * @param entry The entry to move
     * @throws Exception If the move failed
     */
    void move( Dn oldDn, Dn newSuperior, Dn newDn, Entry entry ) throws Exception;


    /**
     * Gets the default ID.
     *
     * @return the default ID.
     */
    ID getDefaultId() throws Exception;
}
