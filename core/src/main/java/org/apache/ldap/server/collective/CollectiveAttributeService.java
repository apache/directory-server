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
package org.apache.ldap.server.collective;


import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.jndi.ContextFactoryConfiguration;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.common.filter.ExprNode;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.Name;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import java.util.Map;


/**
 * An interceptor based service dealing with collective attribute
 * management.  This service intercepts read operations on entries to
 * inject collective attribute value pairs into the response based on
 * the entires inclusion within collectiveAttributeSpecificAreas and
 * collectiveAttributeInnerAreas.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CollectiveAttributeService extends BaseInterceptor
{
    public void init( ContextFactoryConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        super.init( factoryCfg, cfg );
    }


    public NamingEnumeration list( NextInterceptor next, Name base ) throws NamingException
    {
        return super.list( next, base );
    }


    public Attributes lookup( NextInterceptor next, Name dn, String[] attrIds ) throws NamingException
    {
        return super.lookup( next, dn, attrIds );
    }


    public Attributes lookup( NextInterceptor next, Name name ) throws NamingException
    {
        return super.lookup( next, name );
    }


    public NamingEnumeration search( NextInterceptor next, Name base, Map env, ExprNode filter,
                                     SearchControls searchCtls ) throws NamingException
    {
        return super.search( next, base, env, filter, searchCtls );
    }
}
