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
package org.apache.directory.server.core.partition;


import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.interceptor.context.ModifyDNServiceContext;
import org.apache.directory.server.core.interceptor.context.ServiceContext;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * An interfaces that bridges between underlying JNDI entries and JNDI
 * {@link Context} API.  DIT (Directory Information Tree) consists one or
 * above {@link Partition}s whose parent is {@link PartitionNexus},
 * and all of them are mapped to different
 * base suffix.  Each partition contains entries whose name ends with that
 * base suffix.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Partition
{
    /** The objectClass name for aliases: 'alias' */
    String ALIAS_OBJECT = "alias";

    /**
     * The aliased Dn attribute name: aliasedObjectName for LDAP and
     * aliasedEntryName or X.500.
     */
    String ALIAS_ATTRIBUTE = "aliasedObjectName";


    /**
     * Initializes this partition.
     */
    void init( DirectoryServiceConfiguration factoryCfg, PartitionConfiguration cfg ) throws NamingException;


    /**
     * Deinitialized this partition.
     */
    void destroy();


    /**
     * Checks to see if this partition is initialized or not.
     */
    boolean isInitialized();


    /**
     * Flushes any changes made to this partition now.
     */
    void sync() throws NamingException;


    /**
     * Gets the distinguished/absolute name of the suffix for all entries
     * stored within this ContextPartition.
     *
     * @return Name representing the distinguished/absolute name of this
     * ContextPartitions root context.
     */
    LdapDN getSuffix() throws NamingException;

    /**
     * Gets the distinguished/absolute name of the suffix for all entries
     * stored within this ContextPartition.
     *
     * @return Name representing the distinguished/absolute name of this
     * ContextPartitions root context.
     */
    LdapDN getUpSuffix() throws NamingException;

    /**
     * Deletes a leaf entry from this ContextPartition: non-leaf entries cannot be 
     * deleted until this operation has been applied to their children.
     *
     * @param deleteContext the context of the entry to
     * delete from this ContextPartition.
     * @throws NamingException if there are any problems
     */
    void delete( ServiceContext deleteContext ) throws NamingException;


    /**
     * Adds an entry to this ContextPartition.
     *
     * @param addContext the context used  to add and entry to this ContextPartition
     * @throws NamingException if there are any problems
     */
    void add( ServiceContext addContext ) throws NamingException;


    /**
     * Modifies an entry by adding, removing or replacing a set of attributes.
     *
     * @param modifyContext The contetx containin the modification operation 
     * to perform on the entry which is one of constants specified by the 
     * DirContext interface:
     * <code>ADD_ATTRIBUTE, REMOVE_ATTRIBUTE, REPLACE_ATTRIBUTE</code>.
     * 
     * @throws NamingException if there are any problems
     * @see javax.naming.directory.DirContext
     * @see javax.naming.directory.DirContext#ADD_ATTRIBUTE
     * @see javax.naming.directory.DirContext#REMOVE_ATTRIBUTE
     * @see javax.naming.directory.DirContext#REPLACE_ATTRIBUTE
     */
    void modify( ServiceContext modifyContext ) throws NamingException;


    /**
     * Modifies an entry by using a combination of adds, removes or replace 
     * operations using a set of ModificationItems.
     *
     * @param name the normalized distinguished/absolute name of the entry to modify
     * @param items the ModificationItems used to affect the modification with
     * @throws NamingException if there are any problems
     * @see javax.naming.directory.ModificationItem
     */
    void modify( LdapDN name, ModificationItemImpl[] items ) throws NamingException;


    /**
     * A specialized form of one level search used to return a minimal set of 
     * information regarding child entries under a base.  Convenience method
     * used to optimize operations rather than conducting a full search with 
     * retrieval.
     *
     * @param baseName the base distinguished/absolute name for the search/listing
     * @return a NamingEnumeration containing objects of type {@link SearchResult}
     * @throws NamingException if there are any problems
     */
    NamingEnumeration list( LdapDN baseName ) throws NamingException;


    /**
     * Conducts a search against this ContextPartition.  Namespace specific
     * parameters for search are contained within the environment using
     * namespace specific keys into the hash.  For example in the LDAP namespace
     * a ContextPartition implementation may look for search Controls using a
     * namespace specific or implementation specific key for the set of LDAP
     * Controls.
     *
     * @param baseName the normalized distinguished/absolute name of the search base
     * @param environment the environment under which operation occurs
     * @param filter the root node of the filter expression tree
     * @param searchControls the search controls
     * @throws NamingException if there are any problems
     * @return a NamingEnumeration containing objects of type 
     * <a href="http://java.sun.com/j2se/1.4.2/docs/api/
     * javax/naming/directory/SearchResult.html">SearchResult</a>.
     */
    NamingEnumeration<SearchResult> search( LdapDN baseName, Map environment, ExprNode filter, SearchControls searchControls )
        throws NamingException;


    /**
     * Looks up an entry by distinguished/absolute name.  This is a simplified
     * version of the search operation used to point read an entry used for
     * convenience.
     * 
     * Depending on the context parameters, we my look for a simple entry,
     * or for a restricted set of attributes for this entry
     *
     * @param lookupContext The context containing the parameters
     * @return an Attributes object representing the entry
     * @throws NamingException if there are any problems
     */
    Attributes lookup( ServiceContext lookupContext ) throws NamingException;

    /**
     * Fast operation to check and see if a particular entry exists.
     *
     * @param ntry The entryContext used to pass informations
     * @return true if the entry exists, false if it does not
     * @throws NamingException if there are any problems
     */
    boolean hasEntry( ServiceContext entryContext ) throws NamingException;

    /**
     * Modifies an entry by changing its relative name. Optionally attributes
     * associated with the old relative name can be removed from the entry.
     * This makes sense only in certain namespaces like LDAP and will be ignored
     * if it is irrelavent.
     *
     * @param modifyDnContext the modify DN context
     * @throws NamingException if there are any problems
     */
    void modifyRn( ServiceContext modifyDnContext ) throws NamingException;


    /**
     * Transplants a child entry, to a position in the namespace under a new
     * parent entry.
     *
     * @param oldName the normalized distinguished/absolute name of the
     * original child name representing the child entry to move
     * @param newParentName the normalized distinguished/absolute name of the
     * new parent to move the target entry to
     * @throws NamingException if there are any problems
     */
    void move( LdapDN oldName, LdapDN newParentName ) throws NamingException;


    /**
     * Transplants a child entry, to a position in the namespace under a new
     * parent entry and changes the RN of the child entry which can optionally
     * have its old RN attributes removed.  The removal of old RN attributes
     * may not make sense in all namespaces.  If the concept is undefined in a
     * namespace this parameters is ignored.  An example of a namespace where
     * this parameter is significant is the LDAP namespace.
     *
     * @param oldName the normalized distinguished/absolute name of the
     * original child name representing the child entry to move
     * @param newParentName the normalized distinguished/absolute name of the
     * new parent to move the targeted entry to
     * @param newRn the new RN of the entry
     * @param deleteOldRn boolean flag which removes the old RN attribute
     * from the entry if set to true, and has no affect if set to false
     * @throws NamingException if there are any problems
     */
    void move( LdapDN oldName, LdapDN newParentName, String newRn, boolean deleteOldRn ) throws NamingException;


    /**
     * Represents a bind operation issued to authenticate a client.  Partitions
     * need not support this operation.  This operation is here to enable those
     * interested in implementing virtual directories with ApacheDS.
     * 
     * @param bindContext the bind context, containing all the needed informations to bind
     * @throws NamingException if something goes wrong
     */
    void bind( ServiceContext bindContext ) throws NamingException;

    /**
     * Represents an unbind operation issued by an authenticated client.  Partitions
     * need not support this operation.  This operation is here to enable those
     * interested in implementing virtual directories with ApacheDS.
     * 
     * @param unbindContext the context used to unbind
     * @throws NamingException if something goes wrong
     */
    void unbind( ServiceContext unbindContext ) throws NamingException;
}
