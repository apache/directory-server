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

import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.ldif.ChangeType;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;


/**
 * A custom search engine designed for optimized searching across ChangeLogEvents
 * within the ChangeLogStore.  The following lookup and search operations are 
 * provided:
 * 
 * <ul>
 *   <li>lookup change by revision</li>
 *   <li>lookup change by date</li>
 *   <li>find all changes</li>
 *   <li>find all changes before or after a revision</li>
 *   <li>find all changes in a revision range</li>
 *   <li>find changes by LDAP namespace scope on DN</li>
 *   <li>find changes by DN</li>
 *   <li>find changes by principal</li>
 *   <li>find changes by change type</li>
 *   <li>find changes by attribute</li>
 *   <li>find changes using a restricted LDAP filter on all of the above factors</li>
 * </ul>
 * 
 * Note change lookups by date can be conducted by first looking up a revision 
 * using a generalizedTime descriptor to find a revision.  Then these revisions 
 * can be plugged into the revision based find and lookup methods.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ChangeLogSearchEngine
{
    /**
     * Looks up the revision in effect at some time specified by a generalized 
     * time descriptor.
     *
     * @param generalizedTime the generalized time descriptor to find the effective revision for
     * @return the revision that was in effect at a certain time
     * @throws NamingException if there are failures accessing the store
     */
    long lookup( String generalizedTime ) throws NamingException;
    
    
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
     * Finds all the ChangeLogEvents within the system since revision 0.
     * 
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return an enumeration of all the ChangeLogEvents 
     * @throws NamingException if there are failures accessing the store
     */
    NamingEnumeration<ChangeLogEvent> find( RevisionOrder order ) throws NamingException;
    

    /**
     * Finds the ChangeLogEvents that occurred before a revision inclusive.
     * 
     * @param revision the revision number to get the ChangeLogEvents before
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return an enumeration of all the ChangeLogEvents before and including some revision
     * @throws NamingException if there are failures accessing the store
     * @throws IllegalArgumentException if the revision is out of range (less than 0
     * and greater than the current revision)
     */
    NamingEnumeration<ChangeLogEvent> findBefore( long revision, RevisionOrder order ) throws NamingException;
    
    
    /**
     * Finds the ChangeLogEvents that occurred after a revision inclusive.
     * 
     * @param revision the revision number to get the ChangeLogEvents after
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return an enumeration of all the ChangeLogEvents after and including the revision
     * @throws NamingException if there are failures accessing the store
     * @throws IllegalArgumentException if the revision is out of range (less than 0
     * and greater than the current revision)
     */
    NamingEnumeration<ChangeLogEvent> findAfter( long revision, RevisionOrder order ) throws NamingException;
    
    
    /**
     * Finds the ChangeLogEvents that occurred between a revision range inclusive.
     * 
     * @param startRevision the revision number to start getting the ChangeLogEvents above
     * @param endRevision the revision number to start getting the ChangeLogEvents below
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return an enumeration of all the ChangeLogEvents within some revision range inclusive
     * @throws NamingException if there are failures accessing the store
     * @throws IllegalArgumentException if the start and end revisions are out of range
     * (less than 0 and greater than the current revision), or if startRevision > endRevision
     */
    NamingEnumeration<ChangeLogEvent> find( long startRevision, long endRevision, RevisionOrder order ) 
        throws NamingException;
    
    
    /**
     * Finds all the ChangeLogEvents on an entry.
     *
     * @param dn the normalized DN of the entry to get ChangeLogEvents for
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return the set of changes that occurred on an entry
     * @throws NamingException if there are failures accessing the store
     */
    NamingEnumeration<ChangeLogEvent> find( LdapDN dn, RevisionOrder order ) throws NamingException;
    
    
    /**
     * Finds all the ChangeLogEvents on an entry base and/or it's children/descendants.
     *
     * @param base the normalized DN of the entry base to get ChangeLogEvents for
     * @param scope the scope of the search under the base similar to LDAP search scope
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return the set of changes that occurred on an entry and/or it's descendants depending on the scope
     * @throws NamingException if there are failures accessing the store
     */
    NamingEnumeration<ChangeLogEvent> find( LdapDN base, Scope scope, RevisionOrder order ) throws NamingException;
    

    /**
     * Finds all the ChangeLogEvents triggered by a principal in the system.
     *
     * @param principal the LDAP principal who triggered the events
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return the set of changes that were triggered by a specific LDAP user
     * @throws NamingException if there are failures accessing the store
     */
    NamingEnumeration<ChangeLogEvent> find( LdapPrincipal principal, RevisionOrder order ) throws NamingException;
    
    
    /**
     * Finds all the ChangeLogEvents of a particular change type.
     * 
     * @param changeType the change type of the ChangeLogEvents to search for
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return the set of ChangeLogEvents of a particular ChangeType
     * @throws NamingException if there are failures accessing the store
     */
    NamingEnumeration<ChangeLogEvent> find( ChangeType changeType, RevisionOrder order ) throws NamingException;
    
    
    /**
     * Finds all the ChangeLogEvents altering a particular attributeType.
     * 
     * @param attributeType the attributeType definition for the changed attribute to search changes for
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return the set of ChangeLogEvents on a particular attributeType
     * @throws NamingException if there are failures accessing the store
     */
    NamingEnumeration<ChangeLogEvent> find( AttributeType attributeType, RevisionOrder order ) throws NamingException;
    

    /**
     * Finds all the ChangeLogEvents altering a particular objectClass.
     * 
     * @param objectClass the objectClass definition for the entries to search changes for
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return the set of ChangeLogEvents on a particular attributeType
     * @throws NamingException if there are failures accessing the store
     */
    NamingEnumeration<ChangeLogEvent> find( ObjectClass objectClass, RevisionOrder order ) throws NamingException;
    
    
    /**
     * Finds all the ChangeLogEvents matched by the filter expression tree parameter.
     * 
     * The following attributes can be used in the constrained LDAP filter expression 
     * tree.  The expression must be normalized and can contain only ATA pairs with the 
     * following set of attributes:
     * 
     * <ul>
     *   <li>ndn: normalized distinguishedName syntax (defaults to matching a string)</li>
     *   <li>date: generalizedTime syntax</li>
     *   <li>revision: integer syntax</li>
     *   <li>attributeType: numeric OID</li>
     *   <li>objectClass: numeric OID</li>
     *   <li>changeType: new changeType syntax</li>
     *   <li>principal: normalized distinguishedName syntax (defaults to matching a string)</li>
     * </ul>
     * 
     * The following are the only kinds of AVA node types allowed:
     * 
     * <ul>
     *   <li>equality (=) </li>
     *   <li>greaterThanEq (>=) </li>
     *   <li>lessThanEq (<=) </li>
     *   <li>scope (specialized) </li>
     * </ul>
     * 
     * @param filter the filter to use for finding the change
     * @param order the order in which to return ChangeLogEvents (ordered by revision number)
     * @return the set of ChangeLogEvents on entries of a particular objectClass
     * @throws NamingException if there are failures accessing the store
     */
    NamingEnumeration<ChangeLogEvent> find( ExprNode filter, RevisionOrder order ) throws NamingException;
}
