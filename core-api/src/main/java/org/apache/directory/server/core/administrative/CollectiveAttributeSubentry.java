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

import java.util.List;

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;

/**
 * A subentry class to manage the CollectiveAttribute aspect.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CollectiveAttributeSubentry extends Subentry
{
    private List<EntryAttribute> collectiveAttributes;
    
    /**
     * Create an instance of the CollectiveAttributeSubentry class
     */
    public CollectiveAttributeSubentry( List<EntryAttribute> collectiveAttributes )
    {
        super();
        this.collectiveAttributes = collectiveAttributes;
    }

    
    /**
     * Create an instance of the CollectiveAttributeSubentry class
     */
    public CollectiveAttributeSubentry( EntryAttribute cn, SubtreeSpecification ss, String uuid, List<EntryAttribute> collectiveAttributes )
    {
        super( cn, ss, uuid );
        this.collectiveAttributes = collectiveAttributes;
    }

    
    /**
     * @return the collective Attributes
     */
    public List<EntryAttribute> getCollectiveAttributes()
    {
        return collectiveAttributes;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isCollectiveAdminRole()
    {
        return true;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public AdministrativeRoleEnum getAdministrativeRole()
    {
        return AdministrativeRoleEnum.CollectiveAttribute;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "CollectiveAttributeSubentry\n" );
        
        sb.append( super.toString() );
        
        for ( EntryAttribute attribute : collectiveAttributes )
        {
            sb.append( attribute );
        }
        
        return sb.toString();
    }
}
