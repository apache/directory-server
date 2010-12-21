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
package org.apache.directory.server.core.administrative;

import java.util.Set;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.subtree.AdministrativeRole;


/**
 * An interface used to describe an AdministrativePoint. An AdministratuvePoint 
 * holds some elements useful to navigate through the administrative model :
 * <li>
 * <ul>The DN : the AP position in the DIT</ul>
 * <ul>The UUID : The AP unique identifier used when an entry point to the AP it depends on</ul>
 * <ul>The role : the AP role</ul>
 * <ul>The parent : the AP this AP is the direct descendant of</ul>
 * </li>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface AdministrativePoint
{
    /**
     * @return The AdministrativePoint DN
     */
    DN getDn();


    /**
     * @return The AdministrativePoint UUID
     */
    String getUuid();
    
    
    /**
     * @return The AdministrativePoint sequence number
     */
    long getSeqNumber();


    /**
     * Tells if the AdministrativePoint defines an autonomous area
     * 
     * @return true if the AdministrativePoint is an AutonomousArea
     */
    boolean isAutonomous();


    /**
     * Tells if the AdministrativePoint defines a inner area
     * 
     * @return true if the AdministrativePoint is an InnerArea
     */
    boolean isInner();


    /**
     * Tells if the AdministrativePoint defines a specific area
     * 
     * @return true if the AdministrativePoint is a SpecificArea
     */
    boolean isSpecific();


    /**
     * @return The parent AdministrativePoint, if any
     */
    AdministrativePoint getParent();


    /**
     * Set the AdministrativePoint parent
     *
     * @param parent the AdministrativePoint parent
     */
    void setParent( AdministrativePoint parent );


    /**
     * @return The administrativeRole
     */
    AdministrativeRole getRole();
    
    
    /**
     * Associate a new SubEntry to this AdministrationPoint
     * 
     * @param subentry The added SubEntry
     */
    void addSubentry( Subentry subentry );
    
    
    /**
     * Get the set of subentries associated with a role 
     * @param role The role we are looking the subentries for
     * @return The Set of subentries found
     */
    Set<Subentry> getSubentries( AdministrativeRole role );
    
    
    /**
     * Get the set of subentries associated with the AdministrativePoint
     * @param role The role we are looking the subentries for
     * @return The Set of subentries found
     */
    Set<Subentry> getSubentries();
}
