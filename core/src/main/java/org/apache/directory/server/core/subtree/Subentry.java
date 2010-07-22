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
package org.apache.directory.server.core.subtree;


import java.util.Set;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * An operational view of a subentry within the system. A Subentry can have
 * many types (Collective, Schema, AccessControl or Trigger) but only one 
 * Subtree Specification.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Subentry
{
    /** The subentry DN */
    private DN subentryDn;
    
    /** The Subtree Specification associated with this subentry */
    private SubtreeSpecification ss;
    
    /** The administratives roles */
    private Set<AdministrativeRole> administrativeRoles;
    
    /** The subentry UUID */
    private String uuid;
    
    
    /**
     * Creates a new instance of Subentry.
     */
    public Subentry()
    {
    }
    
    
    /**
     * Creates a new instance of Subentry.
     *
     * @param uuid The subentry UUID
     */
    public Subentry( String uuid )
    {
        this.uuid = uuid;
    }
    
    
    /**
     * Creates a new instance of Subentry.
     *
     * @param uuid The subentry UUID
     * @param subentryDn The subentry DN
     */
    public Subentry( DN subentryDn, String uuid )
    {
        this.uuid = uuid;
        this.subentryDn = subentryDn;
    }
    
    
    /**
     * @return The subtree specification
     */
    public SubtreeSpecification getSubtreeSpecification()
    {
        return ss;
    }

    
    /**
     * Stores the subtree
     *
     * @param ss The subtree specification
     */
    public void setSubtreeSpecification( SubtreeSpecification ss )
    {
        this.ss = ss;
    }


    /**
     * @return the subentry DN
     */
    public DN getDn()
    {
        return subentryDn;
    }


    /**
     * @param subentryDn the subentry DN to set
     */
    public void setDn( DN subentryDn )
    {
        this.subentryDn = subentryDn;
    }


    /**
     * Set the administrative roles for this subentry
     *
     * @param administrativeRoles The set of administrative roles
     */
    public void setAdministrativeRoles( Set<AdministrativeRole> administrativeRoles )
    {
        this.administrativeRoles = administrativeRoles;
    }


    /**
     * @return The set of administrative roles for this subentry
     */
    public Set<AdministrativeRole> getAdministrativeRoles()
    {
        return administrativeRoles;
    }
    
    
    /**
     * Tells if the type contains the Collective attribute Administrative Role 
     */
    public boolean isCollectiveAdminRole()
    {
        return administrativeRoles.contains( AdministrativeRole.COLLECTIVE_ADMIN_ROLE );
    }
    
    
    /**
     * Tells if the type contains the SubSchema Administrative Role 
     */
    public boolean isSchemaAdminRole()
    {
        return administrativeRoles.contains( AdministrativeRole.SUB_SCHEMA_ADMIN_ROLE );
    }
    
    
    /**
     * Tells if the type contains the Access Control Administrative Role 
     */
    public boolean isAccessControlAdminRole()
    {
        return administrativeRoles.contains( AdministrativeRole.ACCESS_CONTROL_ADMIN_ROLE );
    }
    
    
    /**
     * Tells if the type contains the Triggers Administrative Role 
     */
    public boolean isTriggersAdminRole()
    {
        return administrativeRoles.contains( AdministrativeRole.TRIGGERS_ADMIN_ROLE );
    }


    /**
     * @return the uuid
     */
    public String getUuid()
    {
        return uuid;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Subentry<" + subentryDn + ", " + uuid + ", " + administrativeRoles + ", " + ss + ">";
    }
}
