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
package org.apache.directory.server.core;

import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;

/**
 * An interface for managing referrals in the server
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ReferralManager
{
    /**
     * Get a read-lock on the referralManager. 
     * No read operation can be done on the referralManager if this
     * method is not called before.
     */
    void lockRead();
    
    
    /**
     * Get a write-lock on the referralManager. 
     * No write operation can be done on the referralManager if this
     * method is not called before.
     */
    void lockWrite();
    
    
    /**
     * Release the read-write lock on the referralManager. 
     * This method must be called after having read or modified the
     * ReferralManager
     */
    void unlock();
    
    
    /**
     * Tells if a DN is a referral (its associated entry contains the Referral ObjectClass).
     * 
     * It does not check that the associated entry inherits from a referral.
     *
     * @param dn The entry's DN we want to check
     * @return <code>true</code> if the DN is associated with a referral
     */
    boolean isReferral( DN dn );
    
    
    /**
     * Tells if this DN has a parent which is a referral.
     * <br>
     * For instance, if cn=example, dc=acme, dc=org is the DN to check,
     * and if dc=acme, dc=org is a referral, this this method will return true.
     *
     * @param dn The DN we want to check for a referral in its partents
     * @return <code>true</code> if there is a parent referral
     */
    boolean hasParentReferral( DN dn );
    
    
    /**
     * Get the DN of the parent referral for a specific DN
     *
     * @param dn The DN from which we want to get the parent referral
     * @return The parent referral of null if none is found
     */
    ServerEntry getParentReferral( DN dn );
    
    
    /**
     * Add a referral to the manager.
     *
     * @param dn The referral to add
     */
    void addReferral( ServerEntry entry );
    
    
    /**
     * Remove a referral from the manager.
     * 
     * @param dn The referral to remove
     */
    void removeReferral( ServerEntry entry );
    
    
    /**
     * Initialize the manager, reading all the referrals from the base.
     * The manager will search for every entries having a Referral ObjectClass.
     *
     * @param directoryService The associated LDAP service
     * @param suffixes The partition list
     * @exception If the initialization failed
     */
    void init( DirectoryService directoryService, String... suffixes ) throws Exception;

    
    /**
     * Remove a partition from the manager, reading all the referrals from the base.
     * The manager will search for every entries having a Referral ObjectClass, and
     * will remove them from the referrals table.
     *
     * @param directoryService The associated LDAP service
     * @param suffixes The partition DN to remove
     * @exception If the removal failed
     */
    void remove( DirectoryService directoryService, DN suffix ) throws Exception;
}
