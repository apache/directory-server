/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.eve.jndi.ibs;


import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;

import org.apache.eve.jndi.BaseInterceptor;
import org.apache.eve.jndi.Invocation;
import org.apache.eve.jndi.InvocationStateEnum;
import org.apache.eve.schema.AttributeTypeRegistry;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.util.DateUtils;


/**
 * An interceptor based service which manages the creation and modification of
 * operational attributes as operations are performed.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OperationalAttributeService extends BaseInterceptor
{
    /** the default user principal or DN */
    private final String DEFAULT_PRINCIPAL = "cn=admin,ou=system";
    /** the global attributeType registry of the schema subsystem */
    private final AttributeTypeRegistry registry;

//    /** the root nexus of the system */
//    private final RootNexus nexus;
//
//
//    /**
//     * Creates the operational attribute management service interceptor.
//     *
//     * @param nexus the root nexus of the system
//     */
//    public OperationalAttributeService( RootNexus nexus )
//    {
//        this.nexus = nexus;
//    }

    /**
     * Creates the operational attribute management service interceptor.
     *
     * @param registry the attributeType registry of the schema subsystem
     */
    public OperationalAttributeService( AttributeTypeRegistry registry)
    {
        this.registry = registry;
    }


    /**
     * Adds extra operational attributes to the entry before it is added.
     *
     * @todo add mechanism to find the identity of the caller so we can
     * properly set the owner/modifier of the entry
     *
     * @see BaseInterceptor#add(String, Name, Attributes)
     */
    protected void add( String upName, Name normName, Attributes entry ) throws NamingException
    {
        Invocation invocation = getInvocation();
        Context ctx = ( ( Context ) invocation.getContextStack().peek() );
        String principal = ( String ) ctx.getEnvironment().get(
                Context.SECURITY_PRINCIPAL );

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            BasicAttribute attribute = new BasicAttribute( "creatorsName" );
            principal = principal == null ? DEFAULT_PRINCIPAL : principal;
            attribute.add( principal );
            entry.put( attribute );

            attribute = new BasicAttribute( "createTimestamp" );
            attribute.add( DateUtils.getGeneralizedTime( System.currentTimeMillis() ) );
            entry.put( attribute );
        }
    }
}
