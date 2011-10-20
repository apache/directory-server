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
package org.apache.directory.server.core.api.administrative;


import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.subtree.AdministrativeRole;


/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AutonomousAdministrativePoint extends AbstractAdministrativePoint
{
    /** A pointer to the AccessControl SAP */
    private AccessControlAdministrativePoint accessControlSap;

    /** A pointer to the CollectiveAttribute SAP */
    private CollectiveAttributeAdministrativePoint collectiveAttributeSap;

    /** A pointer to the TriggerExecution SAP */
    private TriggerExecutionAdministrativePoint triggerExecutionSap;

    /** A pointer to the Subschema SAP */
    private SubschemaAdministrativePoint subschemaSap;

    /**
     * Create an instance of AutonomousAdministrativePoint
     *
     * @param dn The AdministrativePoint Dn
     * @param uuid The AdministrativePoint UUID
     */
    public AutonomousAdministrativePoint( Dn dn, String uuid )
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
     * {@inheritDoc}
     */
    public boolean isInner()
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isSpecific()
    {
        return true;
    }


    /**
     * @return The parent AccessControl SAP if there is some, or the parent AAP
     */
    public AdministrativePoint getAccessControlSap()
    {
        return accessControlSap;
    }


    /**
     * Set the AccessControl SAP (can be an AAP)
     *
     * @param accessControlSap The AccessControl SAP
     */
    public void setAccessControlSap( AccessControlAdministrativePoint accessControlSap )
    {
        this.accessControlSap = accessControlSap;
    }


    /**
     * @return The CollectiveAttribute SAP
     */
    public AdministrativePoint getCollectiveAttribute()
    {
        return collectiveAttributeSap;
    }


    /**
     * Set the CollectiveAttribute SAP
     *
     * @param collectiveAttributeSap The CollectiveAttribute SAP
     */
    public void setCollectiveAttribute( CollectiveAttributeAdministrativePoint collectiveAttributeSap )
    {
        this.collectiveAttributeSap = collectiveAttributeSap;
    }


    /**
     * @return The TriggerExecution SAP
     */
    public AdministrativePoint getTriggerExecution()
    {
        return triggerExecutionSap;
    }


    /**
     * Set the TriggerExecution SAP
     *
     * @param triggerExecutionSap The TriggerExecution SAP
     */
    public void setTriggerExecutionParent( TriggerExecutionAdministrativePoint triggerExecutionSap )
    {
        this.triggerExecutionSap = triggerExecutionSap;
    }


    /**
     * @return The Subschema SAP
     */
    public AdministrativePoint getSubschema()
    {
        return subschemaSap;
    }


    /**
     * Set the Subschema SAP
     *
     * @param subschemaSap The Subschema SAP
     */
    public void setSubschema( SubschemaAdministrativePoint subschemaSap )
    {
        this.subschemaSap = subschemaSap;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "AAP : " + super.toString();
    }
}
