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

import java.security.Principal;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.name.LdapDN;

public class SimpleTriggerExecutionAuthorizer implements TriggerExecutionAuthorizer
{
    private static LdapDN adminName;
    
    static
    {
        try
        {
            adminName = new LdapDN( PartitionNexusProxy.ADMIN_PRINCIPAL_NORMALIZED );
        }
        catch ( InvalidNameException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public boolean hasPermission() throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        Principal principal = ( ( ServerContext ) invocation.getCaller() ).getPrincipal();
        LdapDN principalName = new LdapDN( principal.getName() );
        
        return principalName.equals( adminName );
    }

}
