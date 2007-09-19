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
package org.apache.directory.server.core.changelog;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;


/**
 * An optional search interface supported by TaggableChangeLogStores.  This 
 * interface enables the:
 * 
 * <ul>
 *   <li>lookup of tags by revision</li>
 *   <li>finding all tags</li>
 *   <li>finding tags before or after a revision</li>
 *   <li>finding tags in some revision range</li>
 * </ul>
 * 
 * While investigating these interface methods keep in mind that only one 
 * tag can exist on a revision.  Unlike subversion which allows multiple 
 * tags on a revision we only allow at most one: more than one is pointless.
 * 
 * Date wise searches for tags are not present within this interface since 
 * they should be used in conjunction with the ChangeLogSearchEngine which
 * is by default supported by TaggableSearchableChangeLogStores.  The 
 * ChangeLogSearchEngine can find revisions based on time descriptors and
 * returned revisions can be checked for the presence of tags using this
 * interface.  The whole point to enabling both search engines in a single
 * interfaces is because if you can search for tags you should be able to 
 * search for revisions: however the converse may not be the case.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface TagSearchEngine
{

    // -----------------------------------------------------------------------
    // Tag Lookup Operations
    // -----------------------------------------------------------------------

    
    /**
     * Gets the tag for a specific snapshot if that snapshot exists. 
     *
     * @param revision the revision number to use to check for a snapshot
     * @return the snapshot at the revision if one exists, otherwise null
     * @throws NamingException if there is a problem accessing the store
     */
    Tag lookup( long revision ) throws NamingException;
    
    /**
     * Checks to see if a snapshot exists for a specific revision. 
     *
     * @param revision the revision number to use to check for a snapshot
     * @return true if a snapshot exists at the revision, false otherwise
     * @throws NamingException if there is a problem accessing the store
     */
    boolean has( long revision ) throws NamingException;


    // -----------------------------------------------------------------------
    // Tag Search Operations
    // -----------------------------------------------------------------------

    
    /**
     * Finds all the snapshot tags taken since revision 0 until the current 
     * revision.
     *
     * @param order the revision order in which to return snapshot tags 
     * @return an enumeration over the tags of all snapshots taken since revision 0
     * @throws NamingException if there is a problem accessing the store
     */
    NamingEnumeration<Tag> find( RevisionOrder order ) throws NamingException;
    
    /**
     * Finds all the snapshot tags taken before a specific revision.  If a tag 
     * exists at the revision parameter it will be returned as well.
     *
     * @param revision the revision number to get snapshots before 
     * @param order the revision order in which to return snapshot tags 
     * @return an enumeration over the tags of all snapshots taken before a revision inclusive
     * @throws NamingException if there is a problem accessing the store
     */
    NamingEnumeration<Tag> findBefore( long revision, RevisionOrder order ) throws NamingException;
    
    /**
     * Finds all the snapshot tags taken after a specific revision.  If a tag 
     * exists at the revision parameter it will be returned as well.
     *
     * @param revision the revision number to get snapshots after
     * @param order the revision order in which to return snapshot tags 
     * @return an enumeration over the tags of all snapshots taken after a revision inclusive
     * @throws NamingException if there is a problem accessing the store
     */
    NamingEnumeration<Tag> findAfter( long revision, RevisionOrder order ) throws NamingException;
    
    /**
     * Enumerates over the tags of all snapshots taken between a specific revision 
     * range inclusive.  The first revision parameter should be less than or equal 
     * to the second revision parameter.
     *
     * @param order the revision order in which to return snapshot tags 
     * @return enumeration over all the snapshots taken in a revision range inclusive
     * @throws NamingException if there is a problem accessing the store
     */
    NamingEnumeration<Tag> find( long startRevision, long endRevision, RevisionOrder order ) 
        throws NamingException;
}
