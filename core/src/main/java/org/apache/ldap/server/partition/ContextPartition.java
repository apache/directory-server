/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.partition;


import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.configuration.ContextPartitionConfiguration;
import org.apache.ldap.server.jndi.ContextFactoryConfiguration;


/**
 * ContextPartitions are indivisible ContextPartitions associated with a naming
 * context as a base suffix.  All JNDI Attributes entries at and under the
 * context of this suffix are stored within this partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ContextPartition
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
    void init( ContextFactoryConfiguration factoryCfg, ContextPartitionConfiguration cfg ) throws NamingException;
    
    
    /**
     * Closes or shuts down this ContextPartition.  Operations against closed
     * ContextPartitions will fail.
     */
    void destroy();

    /**
     * Checks to see if this partition is initialized or not.
     */
    boolean isInitialized();

    /**
     * Cue to ContextPartitions with caches to flush entry and index changes to disk.
     *
     * @throws NamingException if there are problems flushing caches
     */
    void sync() throws NamingException;

    
    /**
     * Gets the distinguished/absolute name of the suffix for all entries
     * stored within this ContextPartition.
     *
     * @param normalized boolean value used to control the normalization of the
     * returned Name.  If true the normalized Name is returned, otherwise the 
     * original user provided Name without normalization is returned.
     * @return Name representing the distinguished/absolute name of this
     * ContextPartitions root context.
     */
    Name getSuffix( boolean normalized ) throws NamingException;

    
    /**
     * Deletes a leaf entry from this ContextPartition: non-leaf entries cannot be 
     * deleted until this operation has been applied to their children.
     *
     * @param name the normalized distinguished/absolute name of the entry to
     * delete from this ContextPartition.
     * @throws NamingException if there are any problems
     */ 
    void delete( Name name ) throws NamingException;

    /**
     * Adds an entry to this ContextPartition.
     *
     * @param upName the user provided distinguished/absolute name of the entry
     * @param normName the normalized distinguished/absolute name of the entry
     * @param entry the entry to add to this ContextPartition
     * @throws NamingException if there are any problems
     */
    void add( String upName, Name normName, Attributes entry ) throws NamingException;

    /**
     * Modifies an entry by adding, removing or replacing a set of attributes.
     *
     * @param name the normalized distinguished/absolute name of the entry to
     * modify
     * @param modOp the modification operation to perform on the entry which
     * is one of constants specified by the DirContext interface:
     * <code>ADD_ATTRIBUTE, REMOVE_ATTRIBUTE, REPLACE_ATTRIBUTE</code>.
     * @param mods the attributes and their values used to affect the
     * modification with.
     * @throws NamingException if there are any problems
     * @see javax.naming.directory.DirContext
     * @see javax.naming.directory.DirContext#ADD_ATTRIBUTE
     * @see javax.naming.directory.DirContext#REMOVE_ATTRIBUTE
     * @see javax.naming.directory.DirContext#REPLACE_ATTRIBUTE
     */
    void modify( Name name, int modOp, Attributes mods ) throws NamingException;

    /**
     * Modifies an entry by using a combination of adds, removes or replace 
     * operations using a set of ModificationItems.
     *
     * @param name the normalized distinguished/absolute name of the entry to modify
     * @param mods the ModificationItems used to affect the modification with
     * @throws NamingException if there are any problems
     * @see ModificationItem
     */
    void modify( Name name, ModificationItem [] mods ) throws NamingException;

    /**
     * A specialized form of one level search used to return a minimal set of 
     * information regarding child entries under a base.  Convenience method
     * used to optimize operations rather than conducting a full search with 
     * retrieval.
     *
     * @param base the base distinguished/absolute name for the search/listing
     * @return a NamingEnumeration containing objects of type {@link SearchResult}
     * @throws NamingException if there are any problems
     */
    NamingEnumeration list( Name base ) throws NamingException;
    
    /**
     * Conducts a search against this ContextPartition.  Namespace specific
     * parameters for search are contained within the environment using
     * namespace specific keys into the hash.  For example in the LDAP namespace
     * a ContextPartition implementation may look for search Controls using a
     * namespace specific or implementation specific key for the set of LDAP
     * Controls.
     *
     * @param base the normalized distinguished/absolute name of the search base
     * @param env the environment under which operation occurs
     * @param filter the root node of the filter expression tree
     * @param searchCtls the search controls
     * @throws NamingException if there are any problems
     * @return a NamingEnumeration containing objects of type 
     * <a href="http://java.sun.com/j2se/1.4.2/docs/api/
     * javax/naming/directory/SearchResult.html">SearchResult</a>.
     */
    NamingEnumeration search( Name base, Map env, ExprNode filter,
        SearchControls searchCtls ) throws NamingException;

    /**
     * Looks up an entry by distinguished/absolute name.  This is a simplified
     * version of the search operation used to point read an entry used for
     * convenience.
     *
     * @param name the normalized distinguished name of the object to lookup
     * @return an Attributes object representing the entry
     * @throws NamingException if there are any problems
     */
    Attributes lookup( Name name ) throws NamingException;

    /**
     * Looks up an entry by distinguished/absolute name.  This is a simplified
     * version of the search operation used to point read an entry used for
     * convenience with a set of attributes to return.  If the attributes is
     * null or empty, the returned entry will contain all attributes.
     *
     * @param dn the normalized distinguished name of the object to lookup
     * @param attrIds the set of attributes to return
     * @return an Attributes object representing the entry
     * @throws NamingException if there are any problems
     */
    Attributes lookup( Name dn, String [] attrIds ) throws NamingException;

    /**
     * Fast operation to check and see if a particular entry exists.
     *
     * @param name the normalized distinguished/absolute name of the object to
     * check for existance
     * @return true if the entry exists, false if it does not
     * @throws NamingException if there are any problems
     */
    boolean hasEntry( Name name ) throws NamingException;

    /**
     * Checks to see if name is a context suffix.
     *
     * @param name the normalized distinguished/absolute name of the context
     * @return true if the name is a context suffix, false if it is not.
     * @throws NamingException if there are any problems
     */
    boolean isSuffix( Name name ) throws NamingException;

    /**
     * Modifies an entry by changing its relative name. Optionally attributes
     * associated with the old relative name can be removed from the entry.
     * This makes sense only in certain namespaces like LDAP and will be ignored
     * if it is irrelavent.
     *
     * @param name the normalized distinguished/absolute name of the entry to
     * modify the RN of.
     * @param newRn the new RN of the entry specified by name
     * @param deleteOldRn boolean flag which removes the old RN attribute
     * from the entry if set to true, and has no affect if set to false
     * @throws NamingException if there are any problems
     */
    void modifyRn( Name name, String newRn, boolean deleteOldRn )
        throws NamingException;

    /**
     * Transplants a child entry, to a position in the namespace under a new
     * parent entry.
     *
     * @param newParentName the normalized distinguished/absolute name of the
     * new parent to move the target entry to
     * @param oriChildName the normalized distinguished/absolute name of the
     * original child name representing the child entry to move
     * @throws NamingException if there are any problems
     */
    void move( Name oriChildName, Name newParentName ) throws NamingException;

    /**
     * Transplants a child entry, to a position in the namespace under a new
     * parent entry and changes the RN of the child entry which can optionally
     * have its old RN attributes removed.  The removal of old RN attributes
     * may not make sense in all namespaces.  If the concept is undefined in a
     * namespace this parameters is ignored.  An example of a namespace where
     * this parameter is significant is the LDAP namespace.
     *
     * @param oriChildName the normalized distinguished/absolute name of the
     * original child name representing the child entry to move
     * @param newParentName the normalized distinguished/absolute name of the
     * new parent to move the targeted entry to
     * @param newRn the new RN of the entry
     * @param deleteOldRn boolean flag which removes the old RN attribute
     * from the entry if set to true, and has no affect if set to false
     * @throws NamingException if there are any problems
     */
    void move( Name oriChildName, Name newParentName, String newRn,
               boolean deleteOldRn ) throws NamingException;
}
