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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.subtree.AdministrativeRole;

/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractAdministrativePoint implements AdministrativePoint
{
    /** The AA's DN */
    protected DN dn;

    /** The AA's UUID */
    protected String uuid;

    /** The AdmonistrativeRole */
    protected AdministrativeRole role;

    /** The parent AdministrativePoint */
    protected AdministrativePoint parent;

    /** The children AdministrativePoints */
    protected Map<String, AdministrativePoint> children;

    protected AbstractAdministrativePoint( DN dn, String uuid, AdministrativeRole role )
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
    public DN getDn()
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
        return false;
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
        return false;
    }


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
    public Map<String, AdministrativePoint> getChildren()
    {
        return children;
    }


    /**
     * {@inheritDoc}
     */
    public void setChildren( Map<String, AdministrativePoint> children )
    {
        this.children = children;
    }


    /**
     * {@inheritDoc}
     */
    public void addChild( AdministrativePoint child )
    {
        children.put( child.getUuid(), child );
    }
}
