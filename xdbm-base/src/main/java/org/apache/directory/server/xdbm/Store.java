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


import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * Represents an entry store based on the Table, Index, and MasterTable
 * database structure.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public interface Store<E, ID>
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

    /**
     * Sets the working directory for the store
     */
    void setWorkingDirectory( File workingDirectory );


    /**
     * @return The current working directory for the store
     */
    File getWorkingDirectory();


    /**
     * Stores the list of user index
     * @param userIndices The list of user index
     */
    void setUserIndices( Set<Index<?, E, ID>> userIndices );


    /**
     * @return The list of user index
     */
    Set<Index<?, E, ID>> getUserIndices();


    void setSuffixDn( String suffixDn );


    String getSuffixDn();


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
     * Sets the cache size for this store
     * @param cacheSize The cache size
     */
    void setCacheSize( int cacheSize );


    /**
     * @return The cache size
     */
    int getCacheSize();


    /**
     * Sets the store's name
     * @param name The store's name
     */
    void setName( String name );


    /**
     * @return The store's name
     */
    String getName();


    /**
     * Initialize the JDBM storage system.
     *
     * @param schemaManager the schema schemaManager
     * @throws Exception on failure to lookup elements in schemaManager
     * @throws Exception on failure to create database files
     */
    void init( SchemaManager schemaManager ) throws Exception;


    /**
     * Close the parttion : we have to close all the userIndices and the master table.
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
     * Adds a user index to the list of index for this store
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
     * Set the Presence index
     * @param index The Presence index
     * @throws Exception If the addition failed
     */
    void setPresenceIndex( Index<String, E, ID> index ) throws Exception;


    /**
     * @return The OneLevel system index
     */
    Index<ID, E, ID> getOneLevelIndex();


    /**
     * Set the OneLevel index
     * @param index The OneLevel index
     * @throws Exception If the addition failed
     */
    void setOneLevelIndex( Index<ID, E, ID> index ) throws Exception;


    /**
     * @return The SubLevel system index
     */
    Index<ID, E, ID> getSubLevelIndex();


    /**
     * Set the SubLevel index
     * @param index The SubLevel index
     * @throws Exception If the addition failed
     */
    void setSubLevelIndex( Index<ID, E, ID> index ) throws Exception;


    /**
     * @return The Alias system index
     */
    Index<String, E, ID> getAliasIndex();


    /**
     * Set the Alias index
     * @param index The Alias index
     * @throws Exception If the addition failed
     */
    void setAliasIndex( Index<String, E, ID> index ) throws Exception;


    /**
     * @return The OneAlias system index
     */
    Index<ID, E, ID> getOneAliasIndex();


    /**
     * Set the OneAlias index
     * @param index The OneAlias index
     * @throws Exception If the addition failed
     */
    void setOneAliasIndex( Index<ID, E, ID> index ) throws Exception;


    /**
     * @return The SubAlias system index
     */
    Index<ID, E, ID> getSubAliasIndex();


    /**
     * Set the SubAlias index
     * @param index The SubAlias index
     * @throws Exception If the addition failed
     */
    void setSubAliasIndex( Index<ID, E, ID> index ) throws Exception;


    /**
     * @return The UpDN system index
     */
    Index<String, E, ID> getUpdnIndex();


    /**
     * Set the UpDn index
     * @param index The UpDn index
     * @throws Exception If the addition failed
     */
    void setUpdnIndex( Index<String, E, ID> index ) throws Exception;


    /**
     * @return The Ndn system index
     */
    Index<String, E, ID> getNdnIndex();


    /**
     * Set the NDN index
     * @param index The NDN index
     * @throws Exception If the addition failed
     */
    void setNdnIndex( Index<String, E, ID> index ) throws Exception;


    /**
     * @return The ObjectClass system index
     */
    Index<String, E, ID> getObjectClassIndex();


    /**
     * Set the ObjectClass index
     * @param index The ObjectClass index
     * @throws Exception If the addition failed
     */
    void setObjectClassIndex( Index<String, E, ID> index ) throws Exception;


    /**
     * @return The EntryUUID system index
     */
    Index<String, E, ID> getEntryUuidIndex();


    /**
     * Set the EntryUUID index
     * @param index The EntryUUID index
     * @throws Exception If the addition failed
     */
    void setEntryUuidIndex( Index<String, E, ID> index ) throws Exception;


    /**
     * @return The EntryCSN system index
     */
    Index<String, E, ID> getEntryCsnIndex();


    /**
     * Set the EntryCSN index
     * @param index The EntryCSN index
     * @throws Exception If the addition failed
     */
    void setEntryCsnIndex( Index<String, E, ID> index ) throws Exception;


    //------------------------------------------------------------------------
    // End of the system index
    //------------------------------------------------------------------------

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
     * Tells if an index is already present in the User's index list
     * @param id The index we are looking for
     * @return <code>true</code> if the index is already present in the
     * User's index list 
     * @throws Exception If something went wrong
     */
    boolean hasUserIndexOn( String id ) throws Exception;


    /**
     * Tells if an index is already present in the System's index list
     * @param id The index we are looking for
     * @return <code>true</code> if the index is already present in the
     * System's index list 
     * @throws Exception If something went wrong
     */
    boolean hasSystemIndexOn( String id ) throws Exception;


    /**
     * Get the user <strong>or</strong> system index associated with the given name
     * @param id The index name we are looking for
     * @return The associated user <strong>or</strong> system index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, E, ID> getIndex( String id ) throws IndexNotFoundException;


    /**
     * Get the user index associated with the given name
     * @param id The index name we are looking for
     * @return The associated user index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, E, ID> getUserIndex( String id ) throws IndexNotFoundException;


    /**
     * Get the system index associated with the given name
     * @param id The index name we are looking for
     * @return The associated system index
     * @throws IndexNotFoundException If the index does not exist
     */
    Index<?, E, ID> getSystemIndex( String id ) throws IndexNotFoundException;


    ID getEntryId( String dn ) throws Exception;


    String getEntryDn( ID id ) throws Exception;


    /**
     * Gets the Long id of an entry's parent using the child entry's
     * normalized dn. Note that the suffix entry returns 0, which does not
     * map to any entry.
     *
     * @param dn the normalized distinguished name of the child
     * @return the id of the parent entry or zero if the suffix entry the
     * normalized suffix dn string is used
     * @throws Exception on failures to access the underlying store
     */
    ID getParentId( String dn ) throws Exception;


    ID getParentId( ID childId ) throws Exception;


    String getEntryUpdn( ID id ) throws Exception;


    String getEntryUpdn( String dn ) throws Exception;


    int count() throws Exception;


    /**
     * Add an entry into the store. 
     * 
     * @param entry The entry to add
     * 
     * @throws Exception If the addition failed.
     */
    void add( ServerEntry entry ) throws Exception;


    ServerEntry lookup( ID id ) throws Exception;


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


    int getChildCount( ID id ) throws Exception;


    DN getSuffix();


    DN getUpSuffix();


    void setProperty( String propertyName, String propertyValue ) throws Exception;


    String getProperty( String propertyName ) throws Exception;


    void modify( DN dn, ModificationOperation modOp, ServerEntry mods ) throws Exception;


    void modify( DN dn, List<Modification> mods ) throws Exception;


    /**
     * Changes the relative distinguished name of an entry specified by a
     * distinguished name with the optional removal of the old RDN attribute
     * value from the entry.  Name changes propagate down as dn changes to the
     * descendants of the entry where the RDN changed.
     *
     * An RDN change operation does not change parent child relationships.  It
     * merely propagates a name change at a point in the DIT where the RDN is
     * changed. The change propagates down the subtree rooted at the
     * distinguished name specified.
     *
     * @param dn the normalized distinguished name of the entry to alter
     * @param newRdn the new RDN to set
     * @param deleteOldRdn whether or not to remove the old RDN attr/val
     * @throws Exception if there are any errors propagating the name changes
     */
    void rename( DN dn, RDN newRdn, boolean deleteOldRdn ) throws Exception;


    void move( DN oldChildDn, DN newParentDn, RDN newRdn, boolean deleteOldRdn ) throws Exception;


    void move( DN oldChildDn, DN newParentDn ) throws Exception;


    /**
     * Gets the default ID.
     *
     * @return the default ID.
     */
    ID getDefaultId() throws Exception;
}
