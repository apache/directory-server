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
package org.apache.directory.server.core.api.partition;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.entry.ServerSearchResult;
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
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.partition.index.ParentIdAndRdn;
import org.apache.directory.server.core.api.txn.logedit.DataChange;
import org.apache.directory.server.core.api.txn.logedit.EntryModification;
import org.apache.directory.server.core.api.txn.logedit.IndexModification;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
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
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Ava;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.UsageEnum;


public interface OperationExecutionManager
{

    /**
     * Adds an entry to the given partition.
     *
     * @param partition 
     * @param addContext the context used  to add and entry to this ContextPartition
     * @throws LdapException if there are any problems
     */
    void add( Partition partition, AddOperationContext addContext ) throws LdapException;


    /**
     * Deletes a leaf entry from this ContextPartition: non-leaf entries cannot be 
     * deleted until this operation has been applied to their children.
     *
     * @param partition partition entry lives in
     * @param deleteContext the context of the entry to
     * delete from this ContextPartition.
     * @throws Exception if there are any problems
     */
    void delete( Partition partition, DeleteOperationContext deleteContext ) throws LdapException;


    /**
     * Delete the entry associated with a given Id
     * @param partition partition entry lives in
     * @param entryDn dn of the entry to be deleted
     * @param id The id of the entry to delete
     * @throws Exception If the deletion failed
     */
    void delete( Partition partition, Dn entryDn, UUID id ) throws LdapException;


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
    void modify( Partition partition, ModifyOperationContext modifyContext ) throws LdapException;


    Entry modify( Partition partition, Dn dn, Modification... mods ) throws Exception;


    /**
     * Modifies an entry by changing its relative name. Optionally attributes
     * associated with the old relative name can be removed from the entry.
     * This makes sense only in certain namespaces like LDAP and will be ignored
     * if it is irrelevant.
     *
     * @param partition partition where the renamed entry lives
     * @param renameContext the modify Dn context
     * @throws Exception if there are any problems
     */
    void rename( Partition partition, RenameOperationContext renameContext ) throws LdapException;


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
     * @param partition partition where the renamed entry lives
     * @param dn the normalized distinguished name of the entry to alter
     * @param newRdn the new Rdn to set
     * @param deleteOldRdn whether or not to remove the old Rdn attr/val
     * @param entry the modified entry
     * @param originalEntry entry to be renamed as read from the underlying partition
     * @throws Exception if there are any errors propagating the name changes
     */
    void rename( Partition partition, Dn dn, Rdn newRdn, boolean deleteOldRdn, Entry entry, Entry originalEntry )
        throws Exception;


    /**
     * Transplants a child entry, to a position in the namespace under a new
     * parent entry.
     *
     * @param partition partition where the moved entry lives in
     * @param moveContext The context containing the DNs to move
     * @throws Exception if there are any problems
     */
    void move( Partition partition, MoveOperationContext moveContext ) throws LdapException;


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
     * @param partition partition of the oldDn
     * @param oldDn The previous entry Dn
     * @param newSuperior The new superior Dn
     * @param newDn The new Dn
     * @param modifiedEntry Entry to be moved. Modifications might have been performed on the entry
     * @param originalEntry entry to be moved. Version of the entry as read from the underlying partition.
     * @throws Exception If the move failed
     */
    void move( Partition partition, Dn oldDn, Dn newSuperiorDn, Dn newDn, Entry modifiedEntry,
        Entry originalEntry ) throws Exception;


    /**
     * Transplants a child entry, to a position in the namespace under a new
     * parent entry and changes the RN of the child entry which can optionally
     * have its old RN attributes removed.  The removal of old RN attributes
     * may not make sense in all namespaces.  If the concept is undefined in a
     * namespace this parameters is ignored.  An example of a namespace where
     * this parameter is significant is the LDAP namespace.
     *
     * @param partition partition where the moved and renamed entry lives in
     * @param moveAndRenameContext The context contain all the information about
     * the modifyDN operation
     * @throws Exception if there are any problems
     */
    void moveAndRename( Partition partition, MoveAndRenameOperationContext moveAndRenameContext )
        throws LdapException;


    void moveAndRename( Partition partition, Dn oldDn, Dn newSuperiorDn, Rdn newRdn, Entry modifiedEntry,
        Entry originalEntry, boolean deleteOldRdn ) throws Exception;


    /**
     * Looks up an entry by distinguished/absolute name.  This is a simplified
     * version of the search operation used to point read an entry used for
     * convenience.
     * 
     * Depending on the context parameters, we my look for a simple entry,
     * or for a restricted set of attributes for this entry
     *
     * @param partition partition where from which the lookup will be done
     * @param lookupContext The context containing the parameters
     * @return an Attributes object representing the entry
     * @throws Exception if there are any problems
     */
    Entry lookup( Partition partition, LookupOperationContext lookupContext ) throws LdapException;


    /**
     * Get back an entry knowing its ID
     *
     * @param partition partition entry lives in
     * @param id The Entry ID we want to get back
     * @return The found Entry, or null if not found
     * @throws Exception If the lookup failed for any reason (except a not found entry)
     */
    Entry lookup( Partition partition, UUID id ) throws LdapException;
    
    
   /**
    * Looksups the entry identified in entryContext.
    *
    * @param partition partition lookup will be done
    * @param entryContext operation parameters
    * @return true if the entry can be found
    * @throws LdapException
    */
    boolean hasEntry( Partition partition, HasEntryOperationContext entryContext ) throws LdapException;
    
    

    /**
     * A specialized form of one level search used to return a minimal set of 
     * information regarding child entries under a base.  Convenience method
     * used to optimize operations rather than conducting a full search with 
     * retrieval.
     *
     * @param partition partition lookup will be done
     * @param listContext the context containing the distinguished/absolute name for the search/listing
     * @return a NamingEnumeration containing objects of type {@link ServerSearchResult}
     * @throws Exception if there are any problems
     */
    EntryFilteringCursor list( Partition partition, ListOperationContext listContext ) throws LdapException;
    
    
    IndexCursor<UUID> list( Partition partition, UUID id ) throws LdapException;
    
    
    /**
     * Returns the entry id for the given dn
     *
     * @param partition partition the given dn corresponds to
     * @param dn dn for which we want to get the id
     * @return entry id
     * @throws LdapException
     */
    UUID getEntryId( Partition partition, Dn dn ) throws LdapException;
    
    
    /**
     * builds the Dn of the entry identified by the given id
     *
     * @param partition partition entry lives in
     * @param id the entry's id
     * @return the normalized Dn of the entry
     * @throws Exception
     */
    Dn buildEntryDn( Partition partition, UUID id ) throws Exception;
    
    
    /**
     * Gets the parent id of the given child id.
     *
     * @param partition partition childId lives in.
     * @param childId id of the entry for which we want to get the parent id.
     * @return parent id
     * @throws Exception
     */
    public UUID getParentId( Partition partition, UUID childId ) throws Exception;
    
    
    /**
     * Returns the child count of the corresponding to the id
     *
     * @param partition partition entry lives in
     * @param id id of the entry
     * @return child count of the entry
     * @throws LdapOperationErrorException
     */
    public int getChildCount( Partition partition, UUID id ) throws LdapOperationErrorException;

}
