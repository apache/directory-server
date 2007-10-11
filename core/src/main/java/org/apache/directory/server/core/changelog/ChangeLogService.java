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


import javax.naming.NamingException;


/**
 * A facade for the change log subsystem.  It exposes the functionality
 * needed to interact with the facility to query for changes, take snapshots,
 * and revert the server to an earlier revision.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ChangeLogService
{
    /**
     * Gets the current revision for the server.
     *
     * @return the current revision
     * @throws NamingException if there is a problem accessing this information
     */
    long getCurrentRevision() throws NamingException;

    /**
     * Returns whether or not this ChangeLogService supports searching for changes.
     *
     * @return true if log searching is supported, false otherwise
     */
    boolean isLogSearchSupported();

    /**
     * Returns whether or not this ChangeLogService supports searching for snapshot tags.
     *
     * @return true if snapshot tag searching is supported, false otherwise
     */
    boolean isTagSearchSupported();


    /**
     * Returns whether or not this ChangeLogService stores snapshot tags.
     *
     * @return true if this ChangeLogService supports tag storage, false otherwise
     */
    boolean isTagStorageSupported();

    /**
     * Gets the change log query engine which would be used to ask questions
     * about what changed, when, how and by whom.  It may not be supported by
     * all implementations.  Some implementations may simply log changes without
     * direct access to those changes from within the server.
     *
     * @return the change log query engine
     * @throws UnsupportedOperationException if the change log is not searchable
     */
    ChangeLogSearchEngine getChangeLogSearchEngine();

    /**
     * Gets the tag search engine used to query the snapshots taken.  If this ChangeLogService
     * does not support a taggable and searchable store then an UnsupportedOperationException
     * will result.
     *
     * @return the snapshot query engine for this store
     * @throws UnsupportedOperationException if the tag searching is not supported
     */
    TagSearchEngine getTagSearchEngine();

    /**
     * Creates a tag for a snapshot of the server in a specific state at a revision.
     * If the ChangeLog has a TaggableChangeLogStore then the tag is stored.  If it
     * does not then it's up to callers to track this tag since it will not be stored
     * by this service.
     *
     * @param revision the revision to tag the snapshot
     * @return the Tag associated with the revision
     * @throws NamingException if there is a problem taking a tag
     * @throws IllegalArgumentException if the revision is out of range (less than 0
     * and greater than the current revision)
     */
    Tag tag( long revision ) throws NamingException;

    /**
     * Creates a snapshot of the server at the current revision.
     *
     * @return the revision at which the tag is created
     * @throws NamingException if there is a problem taking a tag
     */
    Tag tag() throws NamingException;

    /**
     * Reverts the server's state to an earlier revision.  Note that the revsion number
     * still increases to revert back even though the state reverted to is the same.
     * Note that implementations may lock the server from making changes or searching
     * the directory until this operation has completed.
     *
     * @param revision the revision number to revert to
     * @return the new revision reached by applying all changes needed to revert to the
     * original state
     * @throws NamingException if there are problems reverting back to the earlier state
     * @throws IllegalArgumentException if the revision provided is greater than the current
     * revision or less than 0
     * @throws UnsupportedOperationException if this feature is not supported by the
     * change log
     */
    long revert( long revision ) throws NamingException;
}
