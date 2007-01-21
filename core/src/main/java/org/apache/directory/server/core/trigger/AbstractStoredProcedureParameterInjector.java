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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter.Generic_LDAP_CONTEXT;

public abstract class AbstractStoredProcedureParameterInjector implements StoredProcedureParameterInjector
{
    private Invocation invocation;
    private Map injectors;
    
    public AbstractStoredProcedureParameterInjector( Invocation invocation ) throws NamingException
    {
        this.invocation = invocation;
        injectors = new HashMap();
        injectors.put( StoredProcedureParameter.Generic_OPERATION_PRINCIPAL.class, $operationPrincipalInjector );
        injectors.put( StoredProcedureParameter.Generic_LDAP_CONTEXT.class, $ldapContextInjector );
    }
    
    protected Name getOperationPrincipal() throws NamingException
    {
        Principal principal = ( ( ServerContext ) invocation.getCaller() ).getPrincipal();
        Name userName = new LdapDN( principal.getName() );
        return userName;
    }
    
    protected Map getInjectors()
    {
        return injectors;
    }
    
    public Invocation getInvocation()
    {
        return invocation;
    }
    
    public void setInvocation( Invocation invocation )
    {
        this.invocation = invocation;
    }
    
    public final List getArgumentsToInject( List parameterList ) throws NamingException
    {
        List arguments = new ArrayList();
        
        Iterator it = parameterList.iterator();
        while ( it.hasNext() )
        {
            StoredProcedureParameter spParameter = ( StoredProcedureParameter ) it.next();
            MicroInjector injector = ( MicroInjector ) injectors.get( spParameter.getClass() );
            arguments.add( injector.inject( spParameter ) );
        }
        
        return arguments;
    }
    
    MicroInjector $operationPrincipalInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            return getOperationPrincipal();
        };
    };
    
    MicroInjector $ldapContextInjector = new MicroInjector()
    {
        public Object inject( StoredProcedureParameter param ) throws NamingException
        {
            Generic_LDAP_CONTEXT ldapCtxParam = ( Generic_LDAP_CONTEXT ) param;
            LdapDN ldapCtxName = ldapCtxParam.getCtxName();
            return (( ServerLdapContext ) ( ( ServerLdapContext ) invocation.getCaller() ).getRootContext()).lookup( ldapCtxName );
        };
    };

}
