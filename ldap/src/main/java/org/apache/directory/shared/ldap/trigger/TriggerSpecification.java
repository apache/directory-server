/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.directory.shared.ldap.trigger;

import java.util.List;

/**
 * The Trigger Specification Bean.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class TriggerSpecification
{
    
    private LdapOperation ldapOperation;
    
    private ActionTime actionTime;
    
    private String storedProcedureName;
    
    private List storedProcedureOptions;
    
    private List storedProcedureParameters;
    
    public TriggerSpecification( LdapOperation ldapOperation, ActionTime actionTime, String storedProcedureName, List storedProcedureOptions, List storedProcedureParameters )
    {
        super();
        this.ldapOperation = ldapOperation;
        this.actionTime = actionTime;
        this.storedProcedureName = storedProcedureName;
        this.storedProcedureOptions = storedProcedureOptions;
        this.storedProcedureParameters = storedProcedureParameters;
    }

    public ActionTime getActionTime()
    {
        return actionTime;
    }

    public LdapOperation getLdapOperation()
    {
        return ldapOperation;
    }

    public String getStoredProcedureName()
    {
        return storedProcedureName;
    }

    public List getStoredProcedureOptions()
    {
        return storedProcedureOptions;
    }

    public List getStoredProcedureParameters()
    {
        return storedProcedureParameters;
    }
    
}
