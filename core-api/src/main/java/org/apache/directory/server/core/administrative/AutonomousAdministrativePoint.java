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


import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.subtree.AdministrativeRole;


/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AutonomousAdministrativePoint extends AbstractAdministrativePoint
{
    /** A pointer to the AccessControl SAP, if there is some */
    private AdministrativePoint accessControlSapParent;

    /** A pointer to the CollectiveAttribute SAP, if there is some */
    private AdministrativePoint collectiveAttributeSapParent;

    /** A pointer to the TriggerExecution SAP, if there is some */
    private AdministrativePoint triggerExecutionSapParent;

    /** A pointer to the Subschema SAP, if there is some */
    private AdministrativePoint subschemaSapParent;

    /**
     * Create an instance of AutonomousAdministrativePoint
     *
     * @param dn The AdministrativePoint DN
     * @param uuid The AdministrativePoint UUID
     */
    public AutonomousAdministrativePoint( DN dn, String uuid )
    {
        super( dn, uuid, AdministrativeRole.AutonomousArea );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAutonomous()
    {
        return true;
    }


    /**
     * @return The parent AccessControl SAP if there is some, or the parent AAP
     */
    public AdministrativePoint getAccessControlParent()
    {
        return accessControlSapParent;
    }


    /**
     * Set the AccessControl SAP parent (can be an AAP)
     *
     * @param accessControlSapParent The AccessControl SAP parent
     */
    public void setAccessControlParent( AdministrativePoint accessControlSapParent )
    {
        this.accessControlSapParent = accessControlSapParent;
    }


    /**
     * @return The parent CollectiveAttribute SAP if there is some, or the parent AAP
     */
    public AdministrativePoint getCollectiveAttributeParent()
    {
        return collectiveAttributeSapParent;
    }


    /**
     * Set the CollectiveAttribute SAP parent (can be an AAP)
     *
     * @param collectiveAttributeSapParent The CollectiveAttribute SAP parent
     */
    public void setCollectiveAttributeParent( AdministrativePoint collectiveAttributeSapParent )
    {
        this.collectiveAttributeSapParent = collectiveAttributeSapParent;
    }


    /**
     * @return The parent TriggerExecution SAP if there is some, or the parent AAP
     */
    public AdministrativePoint getTriggerExecutionParent()
    {
        return triggerExecutionSapParent;
    }


    /**
     * Set the TriggerExecution SAP parent (can be an AAP)
     *
     * @param triggerExecutionSapParent The TriggerExecution SAP parent
     */
    public void setTriggerExecutionParent( AdministrativePoint triggerExecutionSapParent )
    {
        this.triggerExecutionSapParent = triggerExecutionSapParent;
    }


    /**
     * @return The parent Subschema SAP if there is some, or the parent AAP
     */
    public AdministrativePoint getSubschemaParent()
    {
        return subschemaSapParent;
    }


    /**
     * Set the Subschema SAP parent (can be an AAP)
     *
     * @param subschemaSapParent The Subschema SAP parent
     */
    public void setSubschemaParent( AdministrativePoint subschemaSapParent )
    {
        this.subschemaSapParent = subschemaSapParent;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "AAP : " + super.toString();
    }
}
