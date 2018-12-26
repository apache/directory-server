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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import org.ehcache.Cache;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.interceptor.context.ModDnAva;
import org.apache.directory.server.core.api.partition.PartitionTxn;


/**
 * Represents an entry store based on the Table, Index, and MasterTable
 * database structure.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Store
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
    String[] SYS_INDEX_OID_ARRAY =
        {
            ApacheSchemaConstants.APACHE_PRESENCE_AT_OID,
            ApacheSchemaConstants.APACHE_RDN_AT_OID,
            ApacheSchemaConstants.APACHE_ALIAS_AT_OID,
            ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID,
            ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID,
            SchemaConstants.ENTRY_CSN_AT_OID,
            SchemaConstants.OBJECT_CLASS_AT_OID,
            SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID
    };

    Set<String> SYS_INDEX_OIDS = Collections.unmodifiableSet( new HashSet<String>( Arrays
        .asList( SYS_INDEX_OID_ARRAY ) ) );


    /**
     * Sets the partition path (working directory) for the store.
     * 
     * @param partitionPath the new partition path
     */
    void setPartitionPath( URI partitionPath );


    /**
     * Gets the partition path (working directory) for the store.
     * 
     * @return The current partition path (working directory) for the store
     */
    URI getPartitionPath();


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
     * Adds a (system or user) index to the list of index for this store.
     * Note that the attribute id returned by Index.getAttributeId() must be
     * the numeric OID.
     * @param index The index to add
     * @throws Exception If the addition failed
     */
    void addIndex( Index<?, String> index ) throws Exception;


    //------------------------------------------------------------------------
    // System index
    //------------------------------------------------------------------------
    /**
     * @return The Presence system index
     */
    Index<String, String> getPresenceIndex();


    /**
     * @return The Alias system index
     */
    Index<Dn, String> getAliasIndex();


    /**
     * @return The OneAlias system index
     */
    Index<String, String> getOneAliasIndex();


    /**
     * @return The SubAlias system index
     */
    Index<String, String> getSubAliasIndex();


    /**
     * Retrieve the SuffixID
     * 
     * @param partitionTxn The transaction to use
     * @return The suddix ID
     * @throws LdapException If we can't get the suffix ID
     */
    String getSuffixId( PartitionTxn partitionTxn ) throws LdapException;


    /**
     * @return The Rdn system index
     */
    Index<ParentIdAndRdn, String> getRdnIndex();


    /**
     * @return The ObjectClass system index
     */
    Index<String, String> getObjectClassIndex();


    /**
     * @return The EntryCSN system index
     */
    Index<String, String> getEntryCsnIndex();


    /**
     * @return An iterator build on top of the User's index
     */
    Iterator<String> getUserIndices();


    /**
     * @return An iterator build on top of the System's index
     */
    Iterator<String> getSystemIndices();


    /**
     * Tells if an index is already present in the User's <strong>or</strong> System's index list
     * 
     * @param attributeType The attributeType we are looking for
     * @return <code>true</code> if the index is already present in the
     * User's <strong>or</strong> System's index list
     * @throws LdapException If something went wrong
     */
    boolean hasIndexOn( AttributeType attributeType ) throws LdapException;


    /**
     * Tells if an index is already present in the User's index list
     * 
     * @param attributeType The attributeType index we are looking for
     * @return <code>true</code> if the index is already present in the
     * User's index list
     * @throws LdapException If something went wrong
     */
    boolean hasUserIndexOn( AttributeType attributeType ) throws LdapException;


    /**
     * Tells if an index is already present in the System's index list
     * @param attributeType The index we are looking for
     * @return <code>true</code> if the index is already present in the
     * System's index list
     * @throws LdapException If something went wrong
     */
    boolean hasSystemIndexOn( AttributeType attributeType ) throws LdapException;


    /**
     * Get the user <strong>or</strong> system index associated with the given attributeType
     * 
     * @param attributeType The index attributeType we are looking for
     * @return The associated user <strong>or</strong> system index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, String> getIndex( AttributeType attributeType ) throws IndexNotFoundException;


    /**
     * Get the user index associated with the given name
     * @param attributeType The index name we are looking for
     * @return The associated user index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, String> getUserIndex( AttributeType attributeType ) throws IndexNotFoundException;


    /**
     * Get the system index associated with the given name
     * @param attributeType The index name we are looking for
     * @return The associated system index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, String> getSystemIndex( AttributeType attributeType ) throws IndexNotFoundException;


    /**
     * Gets the entry's id. Returns <code>null</code> if the Dn doesn't exist in this store.
     * Note that the Dn must be normalized!
     * 
     * @param partitionTxn The transaction to use
     * @param dn the normalized entry Dn
     * @return the entry's id, or <code>null</code> if the Dn doesn't exists
     * @throws LdapException If we can't get the entry ID
     */
    String getEntryId( PartitionTxn partitionTxn, Dn dn ) throws LdapException;


    /**
     * Gets the Entry's Dn identified by the given id.
     * 
     * @param partitionTxn The transaction to use
     * @param id the entry's id
     * @return the entry's Dn
     * @throws LdapException If we can't get the entry Dn
     */
    Dn getEntryDn( PartitionTxn partitionTxn, String id ) throws LdapException;


    /**
     * Gets the UUID of an entry's parent using the child entry's UUID.
     * Note that the suffix entry returns 0, which does not map to any entry.
     *
     * @param partitionTxn The transaction to use
     * @param childId the UUID of the entry
     * @return the id of the parent entry or zero if the suffix entry UUID is used
     * @throws LdapException on failures to access the underlying store
     */
    String getParentId( PartitionTxn partitionTxn, String childId ) throws LdapException;


    /**
     * Gets the total count of entries within this store.
     *
     * @param partitionTxn The transaction to use
     * @return the total count of entries within this store
     * @throws LdapException on failures to access the underlying store
     */
    long count( PartitionTxn partitionTxn ) throws LdapException;


    /**
     * Delete an entry from the store
     *
     * @param partitionTxn The transaction to use
     * @param id The Entry UUID we want to delete
     * @return the deleted entry if found
     * @throws LdapException If the deletion failed for any reason
     */
    Entry delete( PartitionTxn partitionTxn, String id ) throws LdapException;


    /**
     * Get back an entry knowing its UUID
     *
     * @param partitionTxn The transaction to use
     * @param id The Entry UUID we want to get back
     * @return The found Entry, or null if not found
     * @throws LdapException If the lookup failed for any reason (except a not found entry)
     */
    Entry fetch( PartitionTxn partitionTxn, String id ) throws LdapException;


    /**
     * Get back an entry knowing its UUID
     *
     * @param partitionTxn The transaction to use
     * @param id The Entry UUID we want to get back
     * @param dn The entry DN when we have it
     * @return The found Entry, or null if not found
     * @throws LdapException If the lookup failed for any reason (except a not found entry)
     */
    Entry fetch( PartitionTxn partitionTxn, String id, Dn dn ) throws LdapException;


    /**
     * Gets the count of immediate children of the given entry UUID.
     *
     * @param partitionTxn The transaction to use
     * @param id the entry UUID
     * @return the child count
     * @throws LdapException on failures to access the underlying store
     */
    long getChildCount( PartitionTxn partitionTxn, String id ) throws LdapException;


    /**
     * Modify an entry applying the given list of modifications.
     *
     * @param partitionTxn The transaction to use
     * @param dn The Entry's Dn
     * @param mods The list of modifications
     * @return The modified entry
     * @throws LdapException If the modification failed
     */
    Entry modify( PartitionTxn partitionTxn, Dn dn, Modification... mods ) throws LdapException;


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
     * @param partitionTxn The transaction to use
     * @param dn the normalized distinguished name of the entry to alter
     * @param newRdn the new Rdn to set
     * @param deleteOldRdn whether or not to remove the old Rdn attr/val
     * @param entry the modified entry
     * @throws LdapException if there are any errors propagating the name changes
     */
    void rename( PartitionTxn partitionTxn, Dn dn, Rdn newRdn, boolean deleteOldRdn, Entry entry ) throws LdapException;


    /**
     * Move and Rename operation. The entry is moved from one part of the DIT to another part of 
     * the DIT. Its RDN is also changed in the process.
     * 
     * @param partitionTxn The transaction to use
     * @param oldDn The previous DN
     * @param newSuperiorDn The previous parent's DN
     * @param newRdn The new DN
     * @param modAvas The changed Attributes caused by the renaming (added and removed attributes)
     * @param modifiedEntry the entry to move
     * @throws LdapException If the modification failed
     */
    void moveAndRename( PartitionTxn partitionTxn, Dn oldDn, Dn newSuperiorDn, Rdn newRdn, Map<String, List<ModDnAva>> modAvas, 
        Entry modifiedEntry ) throws LdapException;


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
     * <p>The <b>Alias</b> index is not updated, as the entry UUID won't change.</p>
     * <p>We have a few check we must do before moving the entry :
     * <ul>
     * <li>The destination must not exist
     * <li>The moved entry must exist (this has already been checked)
     * <li>The moved entry must not inherit from a referral (already checked)
     * </ul>
     *
     * @param partitionTxn The transaction to use
     * @param oldDn The previous entry Dn
     * @param newSuperior The new superior Dn
     * @param newDn The new Dn
     * @param entry The entry to move
     * @throws LdapException If the move failed
     */
    void move( PartitionTxn partitionTxn, Dn oldDn, Dn newSuperior, Dn newDn, Entry entry ) throws LdapException;


    /**
     * Expose the Master table
     * @return The masterTable instance
     */
    MasterTable getMasterTable();


    /**
     * @return The ReadWrite lock used to protect the server against concurrent read and writes
     */
    ReadWriteLock getReadWriteLock();
    
    
    /**
     * @return the Alias cache
     * @return The cache
     */
    Cache< String, Dn > getAliasCache();
}
