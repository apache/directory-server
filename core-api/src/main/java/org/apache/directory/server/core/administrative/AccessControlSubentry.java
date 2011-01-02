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

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.aci.ACIItem;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * A subentry class to manage the AccessControl aspect.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AccessControlSubentry extends Subentry
{
    /** The list of ACIItems */
    private List<ACIItem> aciItems;
    
    /**
     * Create an instance of the AccessControlSubentry class
     */
    public AccessControlSubentry( EntryAttribute cn, SubtreeSpecification ss, String uuid )
    {
        super( cn, ss, uuid );
        aciItems = new ArrayList<ACIItem>();
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isAccessControlAdminRole()
    {
        return true;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public AdministrativeRoleEnum getAdministrativeRole()
    {
        return AdministrativeRoleEnum.AccessControl;
    }


    /**
     * @return the aciItems
     */
    public List<ACIItem> getAciItems()
    {
        return aciItems;
    }


    /**
     * @param aciItem the aciItems to add
     */
    public void addAciItem( ACIItem aciItem )
    {
        aciItems.add( aciItem );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "AccessControlSubentry\n" );
        
        sb.append( super.toString() );
        
        for ( ACIItem aciItem : aciItems )
        {
            sb.append( aciItem );
        }
        
        return sb.toString();
    }
}
