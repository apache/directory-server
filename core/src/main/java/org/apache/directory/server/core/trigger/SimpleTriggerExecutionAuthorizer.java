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
package org.apache.directory.server.core.trigger;


import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;


public class SimpleTriggerExecutionAuthorizer implements TriggerExecutionAuthorizer
{
    private static DN adminName;
    
    static
    {
        try
        {
            adminName = new DN( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
        }
        catch ( LdapInvalidDnException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public boolean hasPermission( OperationContext opContext ) throws LdapException
    {
        DN principalName = opContext.getSession().getEffectivePrincipal().getClonedName();
        return principalName.equals( adminName );
    }
}
