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

import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * An interface for managing referrals in the server
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface ReferralManager
{
    /**
     * Tells if a DN is a referral (its associated entry contains the Referral ObjectClass).
     * 
     * It does not check that the associated entry inherits from a referral.
     *
     * @param dn The entry's DN we want to check
     * @return <code>true</code> if the DN is associated with a referral
     */
    boolean isReferral( LdapDN dn );
    
    
    /**
     * Tells if this DN has a parent which is a referral.
     * <br>
     * For instance, if cn=example, dc=acme, dc=org is the DN to check,
     * and if dc=acme, dc=org is a referral, this this method will return true.
     *
     * @param dn The DN we want to check for a referral in its partents
     * @return <code>true</code> if there is a parent referral
     */
    boolean isParentReferral( LdapDN dn );
    
    
    /**
     * Add a refrral to the manager.
     *
     * @param dn The referral to add
     */
    void addReferral( LdapDN dn );
    
    
    /**
     * Remove a referral from the manager.
     * 
     * @param dn The referral to remove
     */
    void removeReferral( LdapDN dn );
    
    
    /**
     * Initialize the manager, reading all the referrals from the base.
     * The manager will search for every entries having a Referral ObjectClass.
     *
     * @param directoryService The associated LDAP service
     */
    void init( DirectoryService directoryService );
}
