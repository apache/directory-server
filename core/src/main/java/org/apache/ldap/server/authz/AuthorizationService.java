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
package org.apache.ldap.server.authz;


import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.jndi.ContextFactoryConfiguration;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.partition.ContextPartitionNexus;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;


/**
 * An ACI based authorization service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AuthorizationService extends BaseInterceptor
{
    private ContextPartitionNexus nexus;
    private TupleCache cache;


    public void init( ContextFactoryConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        super.init( factoryCfg, cfg );

        nexus = factoryCfg.getPartitionNexus();
        cache = new TupleCache( factoryCfg );
    }


    public void add( NextInterceptor next, String upName, Name normName, Attributes entry ) throws NamingException
    {
        next.add( upName, normName, entry );
        cache.subentryAdded( upName, normName, entry );
    }


    public void delete( NextInterceptor next, Name name ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        next.delete( name );
        cache.subentryDeleted( name, entry );
    }


    public void modify( NextInterceptor next, Name name, int modOp, Attributes mods ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        next.modify( name, modOp, mods );
        cache.subentryModified( name, modOp, mods, entry );
    }


    public void modify( NextInterceptor next, Name name, ModificationItem[] mods ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        next.modify( name, mods );
        cache.subentryModified( name, mods, entry );
    }
}
