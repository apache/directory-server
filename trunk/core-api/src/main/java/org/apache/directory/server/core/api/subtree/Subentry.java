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
package org.apache.directory.server.core.api.subtree;


import java.util.Set;

import org.apache.directory.shared.ldap.model.subtree.AdministrativeRole;
import org.apache.directory.shared.ldap.model.subtree.SubtreeSpecification;


/**
 * An operational view of a subentry within the system. A Subentry can have
 * many types (Collective, Schema, AccessControl or Trigger) but only one
 * Subtree Specification.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Subentry
{
    /** The Subtree Specification associated with this subentry */
    private SubtreeSpecification ss;

    /** The administratives roles */
    private Set<AdministrativeRole> administrativeRoles;


    /**
     * Stores the subtree
     *
     * @param ss The subtree specification
     */
    public final void setSubtreeSpecification( SubtreeSpecification ss )
    {
        this.ss = ss;
    }


    /**
     * @return The subtree specification
     */
    public final SubtreeSpecification getSubtreeSpecification()
    {
        return ss;
    }


    /**
     *
     * TODO setAdministrativeRoles.
     *
     * @param administrativeRoles
     */
    public final void setAdministrativeRoles( Set<AdministrativeRole> administrativeRoles )
    {
        this.administrativeRoles = administrativeRoles;
    }


    public final Set<AdministrativeRole> getAdministrativeRoles()
    {
        return administrativeRoles;
    }


    /**
     * Tells if the type contains the Collective attribute Administrative Role
     */
    public final boolean isCollectiveAdminRole()
    {
        return administrativeRoles.contains( AdministrativeRole.CollectiveAttributeInnerArea ) ||
            administrativeRoles.contains( AdministrativeRole.CollectiveAttributeSpecificArea );
    }


    /**
     * Tells if the type contains the SubSchema Administrative Role
     */
    public final boolean isSchemaAdminRole()
    {
        return administrativeRoles.contains( AdministrativeRole.SubSchemaSpecificArea );
    }


    /**
     * Tells if the type contains the Access Control Administrative Role
     */
    public final boolean isAccessControlAdminRole()
    {
        return administrativeRoles.contains( AdministrativeRole.AccessControlSpecificArea ) ||
            administrativeRoles.contains( AdministrativeRole.AccessControlInnerArea );
    }


    /**
     * Tells if the type contains the Triggers Administrative Role
     */
    public final boolean isTriggersAdminRole()
    {
        return administrativeRoles.contains( AdministrativeRole.TriggerExecutionSpecificArea ) ||
            administrativeRoles.contains( AdministrativeRole.TriggerExecutionInnerArea );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Subentry[" + administrativeRoles + ", " + ss + "]";
    }
}
