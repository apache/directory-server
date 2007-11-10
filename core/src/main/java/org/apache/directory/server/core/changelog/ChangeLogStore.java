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


import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.shared.ldap.ldif.Entry;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;


/**
 * A store for change events on the directory which exposes methods for 
 * managing, querying and in general performing legal operations on the log.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ChangeLogStore 
{
    /**
     * Gets the current revision of the server (a.k.a. the HEAD revision).
     *
     * @return the current revision of the server
     */
    long getCurrentRevision();
    
    /**
     * Records a change as a forward LDIF, a reverse change to revert the change and
     * the authorized principal triggering the revertable change event.
     *
     * @param principal the authorized LDAP principal triggering the change
     * @param forward LDIF of the change going to the next state
     * @param reverse LDIF (anti-operation): the change required to revert this change
     * @return the new revision reached after having applied the forward LDIF
     * @throws NamingException if there are problems logging the change
     */
    long log( LdapPrincipal principal, Entry forward, Entry reverse ) throws NamingException;

    
    /**
     * Looks up the ChangeLogEvent for a revision.
     *
     * @param revision to get a ChangeLogEvent for
     * @return the ChangeLogEvent associated with the revision
     * @throws NamingException if there are failures accessing the store
     * @throws IllegalArgumentException if the revision is out of range (less than 0
     * and greater than the current revision)
     */
    ChangeLogEvent lookup( long revision ) throws NamingException;


    /**
     * Gets a Cursor over all the ChangeLogEvents within the system since
     * revision 0.
     *
     * This method should exhibit isolation characteristics: meaning if the
     * request is initiated at revision 100, then any subsequent log entries
     * increasing the revision should not be seen.
     *
     * @return a Cursor over all the ChangeLogEvents
     * @throws NamingException if there are failures accessing the store
     */
    Cursor<ChangeLogEvent> find() throws NamingException;


    /**
     * Gets a Cursor over the ChangeLogEvents that occurred before a revision
     * exclusive.
     *
     * @param revision the revision number to get the ChangeLogEvents before
     * @return a Cursor over the ChangeLogEvents before a revision
     * @throws NamingException if there are failures accessing the store
     * @throws IllegalArgumentException if the revision is out of range (less than 0
     * and greater than the current revision)
     */
    Cursor<ChangeLogEvent> findBefore( long revision ) throws NamingException;


    /**
     * Finds the ChangeLogEvents that occurred after a revision exclusive.
     *
     * This method should exhibit isolation characteristics: meaning if the request is
     * initiated at revision 100 then any subsequent log entries increasing the revision
     * should not be seen.
     *
     * @param revision the revision number to get the ChangeLogEvents after
     * @return a Cursor of all the ChangeLogEvents after and including the revision
     * @throws NamingException if there are failures accessing the store
     * @throws IllegalArgumentException if the revision is out of range (less than 0
     * and greater than the current revision)
     */
    Cursor<ChangeLogEvent> findAfter( long revision ) throws NamingException;


    /**
     * Finds the ChangeLogEvents that occurred between a revision range inclusive.
     *
     * @param startRevision the revision number to start getting the ChangeLogEvents above
     * @param endRevision the revision number to start getting the ChangeLogEvents below
     * @return an enumeration of all the ChangeLogEvents within some revision range inclusive
     * @throws NamingException if there are failures accessing the store
     * @throws IllegalArgumentException if the start and end revisions are out of range
     * (less than 0 and greater than the current revision), or if startRevision > endRevision
     */
    Cursor<ChangeLogEvent> find( long startRevision, long endRevision ) throws NamingException;
}
