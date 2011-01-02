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
package org.apache.directory.server.core.administrative;


import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * An operational view of a subentry within the system. A Subentry can have
 * many types (Collective, Schema, AccessControl or Trigger) but only one
 * Subtree Specification. This subtreeSpecification will apply to all the
 * subentry's roles.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class Subentry
{
    /** The Subtree Specification associated with this subentry */
    private SubtreeSpecification ss;

    /** The subentry UUID */
    private String uuid;
    
    /** The subentry CN */
    private String cn;
    
    /**
     * Creates a new instance of a Subentry
     */
    protected Subentry()
    {
    }
    
    
    /**
     * Creates a new instance of a Subentry
     */
    protected Subentry( String cn, SubtreeSpecification ss, String uuid )
    {
        this.cn = cn;
        this.ss = ss;
        this.uuid = uuid;
    }
    
    
    /**
     * Stores the subtreeSpecification
     *
     * @param ss The subtree specification
     */
    public void setSubtreeSpecification( SubtreeSpecification ss )
    {
        this.ss = ss;
    }


    /**
     * @return The subtree specification
     */
    public SubtreeSpecification getSubtreeSpecification()
    {
        return ss;
    }


    /**
     * Tells if the type contains the Collective attribute Administrative Role
     */
    public boolean isCollectiveAdminRole()
    {
        return false;
    }


    /**
     * Tells if the type contains the SubSchema Administrative Role
     */
    public boolean isSchemaAdminRole()
    {
        return false;
    }


    /**
     * Tells if the type contains the Access Control Administrative Role
     */
    public boolean isAccessControlAdminRole()
    {
        return false;
    }


    /**
     * Tells if the type contains the Triggers Administrative Role
     */
    public boolean isTriggersAdminRole()
    {
        return false;
    }
    
    
    /**
     * @return the subentry administrativeRole
     */
    public abstract AdministrativeRoleEnum getAdministrativeRole();


    /**
     * @return the uuid
     */
    public String getUuid()
    {
        return uuid;
    }


    /**
     * @param uuid the uuid to set
     */
    public void setUuid( String uuid )
    {
        this.uuid = uuid;
    }


    /**
     * @return the cn
     */
    public String getCn()
    {
        return cn;
    }


    /**
     * @param cn the cn to set
     */
    public void setCn( String cn )
    {
        this.cn = cn;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "Subentry name : " ).append( cn ).append( '\n' );
        sb.append( "UUID          : " ).append( uuid ).append( '\n' );
        
        return sb.toString();
    }
}
