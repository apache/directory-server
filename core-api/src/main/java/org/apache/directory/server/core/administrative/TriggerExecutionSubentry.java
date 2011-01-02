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

import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification;


/**
 * A subentry class to manage the TriggerExecutionSubentry aspect.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TriggerExecutionSubentry extends Subentry
{
    /** The list of TriggerSpecifications */
    private List<TriggerSpecification> triggerSpecifications;
    
    /**
     * Create an instance of the TriggerExecutionSubentry class
     */
    public TriggerExecutionSubentry( EntryAttribute cn, SubtreeSpecification ss, String uuid )
    {
        super( cn, ss, uuid );
        triggerSpecifications = new ArrayList<TriggerSpecification>();
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean isTriggerExecutionAdminRole()
    {
        return true;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public AdministrativeRoleEnum getAdministrativeRole()
    {
        return AdministrativeRoleEnum.TriggerExecution;
    }


    /**
     * @return the triggerSpecifications
     */
    public List<TriggerSpecification> getTriggerSpecification()
    {
        return triggerSpecifications;
    }


    /**
     * @param triggerSpecification the TriggerSpecification to add
     */
    public void addTriggerSpecification( TriggerSpecification triggerSpecification )
    {
        triggerSpecifications.add( triggerSpecification );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "AccessControlSubentry\n" );
        
        sb.append( super.toString() );
        
        for ( TriggerSpecification triggerSpecification : triggerSpecifications )
        {
            sb.append( triggerSpecification );
        }
        
        return sb.toString();
    }
}
