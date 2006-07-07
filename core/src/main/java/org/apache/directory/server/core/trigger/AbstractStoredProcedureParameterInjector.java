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

package org.apache.directory.server.core.trigger;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter;

public abstract class AbstractStoredProcedureParameterInjector implements StoredProcedureParameterInjector
{
    private Invocation invocation;
    private Map injectors;
    
    public AbstractStoredProcedureParameterInjector( Invocation invocation ) throws NamingException
    {
        this.invocation = invocation;
        injectors = new HashMap();
        injectors.put( StoredProcedureParameter.OPERATION_PRINCIPAL, $operationPrincipalInjector.inject() );
        injectors.put( StoredProcedureParameter.OPERATION_TIME, $operationTimeInjector.inject() );
        injectors.put( StoredProcedureParameter.ROOT_DSE, $rootDSE.inject() );
    }
    
    protected Name getOperationPrincipal() throws NamingException
    {
        Principal principal = ( ( ServerContext ) invocation.getCaller() ).getPrincipal();
        Name userName = new LdapDN( principal.getName() );
        return userName;
    }
    
    protected Date getOperationTime()
    {
        return new Date();
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
    
    public final List getArgumentsToInject( List parameterList )
    {
        List arguments = new ArrayList();
        
        Iterator it = parameterList.iterator();
        while ( it.hasNext() )
        {
            StoredProcedureParameter spParameter = ( StoredProcedureParameter ) it.next();
            arguments.add( injectors.get( spParameter ) );
        }
        
        return arguments;
    }
    
    MicroInjector $operationPrincipalInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            return getOperationPrincipal();
        };
    };
    
    MicroInjector $operationTimeInjector = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            return getOperationTime();
        };
    };

    MicroInjector $rootDSE = new MicroInjector()
    {
        public Object inject() throws NamingException
        {
            return ( ServerLdapContext ) ( ( ServerLdapContext ) invocation.getCaller() ).getRootContext();
        };
    };

}
