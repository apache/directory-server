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


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.directory.shared.ldap.subtree.AdministrativeRole;


/**
 * Abstract implementation for the AdministrativePoint
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractAdministrativePoint implements AdministrativePoint
{
    /** The AP's UUID */
    protected String uuid;
    
    /** The AP's sequence number */
    private long seqNumber;

    /** The AdmonistrativeRole */
    protected AdministrativeRole role;

    /** The parent AdministrativePoint */
    protected AdministrativePoint parent;

    /** The children AdministrativePoints */
    protected Map<String, AdministrativePoint> children;
    
    /** The associated subentries */
    private Set<Subentry> subentries; 


    /**
     * Creates a new instance of AbstractAdministrativePoint.
     * 
     * @param uuid the AdministrativePoint UUID
     * @param role the AdministrativePoint role
     * @param seqNumber the associated Sequence Number
     */
    protected AbstractAdministrativePoint( String uuid, AdministrativeRole role, long seqNumber )
    {
        this.uuid = uuid;
        this.role = role;
        this.children = new ConcurrentHashMap<String, AdministrativePoint>();
        this.seqNumber = seqNumber;
        subentries = new HashSet<Subentry>();
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
     * @return the seqNumber
     */
    public long getSeqNumber()
    {
        return seqNumber;
    }


    /**
     * @param seqNumber the seqNumber to set
     */
    public void setSeqNumber( long seqNumber )
    {
        this.seqNumber = seqNumber;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void addSubentry( Subentry subentry )
    {
        synchronized( subentries )
        {
            subentries.add( subentry );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void deleteSubentry( Subentry subentry )
    {
        synchronized( subentries )
        {
            subentries.remove( subentry );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Set<Subentry> getSubentries( AdministrativeRoleEnum role )
    {
        synchronized( subentries )
        {
            Set<Subentry> subentries = new HashSet<Subentry>();
            
            for ( Subentry subentry : this.subentries )
            {
                if ( subentry.getAdministrativeRole() == role )
                {
                    subentries.add( subentry );
                }
            }
            
            return subentries;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Set<Subentry> getSubentries()
    {
        synchronized( subentries )
        {
            return subentries;
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Role: '" ).append( role ).append( "', " );
        sb.append( "UUID: " ).append( uuid ).append( ", " );
        sb.append( "SeqNumber: " ).append( seqNumber ).append( '\n' );
        
        if ( subentries.size() > 0 )
        {
            sb.append( "Subentries: " );
            boolean isFirst = true;
            
            for ( Subentry subentry : subentries )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }
                
                sb.append( '\'' ).append( subentry.getCn() ).append( '\'' );
            }
        }

        return sb.toString();
    }
}
