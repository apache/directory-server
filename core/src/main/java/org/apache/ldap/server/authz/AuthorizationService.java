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
import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.partition.ContextPartitionNexus;
import org.apache.ldap.server.authz.support.ACDFEngine;
import org.apache.ldap.server.invocation.InvocationStack;
import org.apache.ldap.server.authn.LdapPrincipal;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.aci.AuthenticationLevel;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import java.util.Iterator;
import java.util.Map;


/**
 * An ACI based authorization service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AuthorizationService extends BaseInterceptor
{
    /** the partition nexus */
    private ContextPartitionNexus nexus;
    /** a cache that responds to add, delete, and modify attempts */
    private TupleCache cache;
    /** use and instance of the ACDF engine */
    private ACDFEngine engine;


    public void init( ContextFactoryConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        super.init( factoryCfg, cfg );

        nexus = factoryCfg.getPartitionNexus();
        cache = new TupleCache( factoryCfg );
        engine = new ACDFEngine( factoryCfg.getGlobalRegistries().getOidRegistry(),
                factoryCfg.getGlobalRegistries().getAttributeTypeRegistry() );
    }


    /*
     * Within every access controled interceptor method we must retrieve the ACITuple
     * set for all the perscriptiveACIs that apply to the candidate, the target entry
     * operated upon.  This ACITuple set is gotten from the TupleCache by looking up
     * the subentries referenced by the accessControlSubentries operational attribute
     * within the target entry.
     *
     * Then the entry is inspected for an entryACI.  If present a set of ACITuples
     * are generated for all the entryACIs within the entry.  This set is combined
     * with the ACITuples cached for the perscriptiveACI affecting the target entry.
     *
     * The union of ACITuples are fed into the engine along with other parameters
     * to decide where permission is granted or rejected for the specific operation.
     */

    public void add( NextInterceptor next, String upName, Name normName, Attributes entry ) throws NamingException
    {
        next.add( upName, normName, entry );
        cache.subentryAdded( upName, normName, entry );

        ServerContext ctx = ( ServerContext ) InvocationStack.getInstance().peek().getCaller();
        LdapPrincipal user = ctx.getPrincipal();
        Name userGroupName = null;

//        engine.checkPermission( next, userGroupName, user.getName(), user.getAuthenticationLevel(), normName, null,
//                null, ADD_OPS, aciTuples );
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


    public Attributes getRootDSE( NextInterceptor next ) throws NamingException
    {
        return super.getRootDSE( next );
    }


    public boolean hasEntry( NextInterceptor next, Name name ) throws NamingException
    {
        return next.hasEntry( name );
    }


    public NamingEnumeration list( NextInterceptor next, Name base ) throws NamingException
    {
        return super.list( next, base );
    }


    public Iterator listSuffixes( NextInterceptor next, boolean normalized ) throws NamingException
    {
        return super.listSuffixes( next, normalized );
    }


    public Attributes lookup( NextInterceptor next, Name dn, String[] attrIds ) throws NamingException
    {
        return super.lookup( next, dn, attrIds );
    }


    public Attributes lookup( NextInterceptor next, Name name ) throws NamingException
    {
        return super.lookup( next, name );
    }


    public void modifyRn( NextInterceptor next, Name name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        super.modifyRn( next, name, newRn, deleteOldRn );
    }


    public void move( NextInterceptor next, Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn )
            throws NamingException
    {
        super.move( next, oriChildName, newParentName, newRn, deleteOldRn );
    }


    public void move( NextInterceptor next, Name oriChildName, Name newParentName ) throws NamingException
    {
        super.move( next, oriChildName, newParentName );
    }


    public NamingEnumeration search( NextInterceptor next, Name base, Map env, ExprNode filter,
                                     SearchControls searchCtls ) throws NamingException
    {
        return super.search( next, base, env, filter, searchCtls );
    }
}
