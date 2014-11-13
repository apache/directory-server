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


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.subtree.AdministrativeRole;


/**
 * Abstract implementation for the AdministrativePoint
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractAdministrativePoint implements AdministrativePoint
{
    /** The AA's Dn */
    protected Dn dn;

    /** The AA's UUID */
    protected String uuid;

    /** The AdmonistrativeRole */
    protected AdministrativeRole role;

    /** The parent AdministrativePoint */
    protected AdministrativePoint parent;

    /** The children AdministrativePoints */
    protected Map<String, AdministrativePoint> children;


    /**
     * Creates a new instance of AbstractAdministrativePoint.
     */
    protected AbstractAdministrativePoint( Dn dn, String uuid, AdministrativeRole role )
    {
        this.dn = dn;
        this.uuid = uuid;
        this.role = role;
        this.children = new ConcurrentHashMap<String, AdministrativePoint>();
    }


    /**
     * {@inheritDoc}
     */
    public AdministrativeRole getRole()
    {
        return role;
    }


    /**
     * {@inheritDoc}
     */
    public Dn getDn()
    {
        return dn;
    }


    /**
     * {@inheritDoc}
     */
    public String getUuid()
    {
        return uuid;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAutonomous()
    {
        // Default to false
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public abstract boolean isInner();


    /**
     * {@inheritDoc}
     */
    public abstract boolean isSpecific();


    /**
     * {@inheritDoc}
     */
    public AdministrativePoint getParent()
    {
        return parent;
    }


    /**
     * {@inheritDoc}
     */
    public void setParent( AdministrativePoint parent )
    {
        this.parent = parent;
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Role: '" ).append( role ).append( "', " );
        sb.append( "Dn: '" ).append( dn ).append( "', " );
        sb.append( "UUID: " ).append( uuid ).append( '\n' );

        return sb.toString();
    }
}
