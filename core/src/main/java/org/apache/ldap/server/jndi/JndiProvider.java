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
package org.apache.ldap.server.jndi;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.BackendSubsystem;
import org.apache.ldap.server.ContextPartition;
import org.apache.ldap.server.PartitionNexus;
import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.interceptor.Interceptor;
import org.apache.ldap.server.invocation.Delete;
import org.apache.ldap.server.invocation.GetMatchedDN;
import org.apache.ldap.server.invocation.*;
import org.apache.ldap.server.invocation.GetSuffix;
import org.apache.ldap.server.invocation.HasEntry;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.invocation.IsSuffix;
import org.apache.ldap.server.invocation.List;
import org.apache.ldap.server.invocation.ListSuffixes;
import org.apache.ldap.server.invocation.Lookup;
import org.apache.ldap.server.invocation.LookupWithAttrIds;
import org.apache.ldap.server.invocation.Modify;
import org.apache.ldap.server.invocation.ModifyMany;
import org.apache.ldap.server.invocation.ModifyRN;
import org.apache.ldap.server.invocation.Move;
import org.apache.ldap.server.invocation.MoveAndModifyRN;
import org.apache.ldap.server.invocation.Search;
import org.apache.ldap.server.interceptor.Interceptor;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.interceptor.InterceptorChain;


/**
 * The BackendSubsystem service implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JndiProvider implements BackendSubsystem
{
    /** Singleton instance of this class */
    private static JndiProvider s_singleton;
    
    /** The interceptor (or interceptor chain) for this provider */
    private Interceptor interceptor;
    /** RootNexus as it was given to us by the ServiceManager */
    private RootNexus nexus;
    /** PartitionNexus proxy wrapping nexus to inject services */
    private PartitionNexus proxy;

    /** whether or not this instance has been shutdown */
    private boolean isShutdown = false;


    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------


    /**
     * Creates a singlton instance of the BackendSubsystem.  In the words of
     * the Highlander, "there can only be one."
     *
     * @throws IllegalStateException if another JndiProvider has already
     * been instantiated.
     */
    public JndiProvider( RootNexus nexus )
    {
        if ( s_singleton != null )
        {
            throw new IllegalStateException(
                "Cannot instantiate more than one JndiProvider!" );
        }

        s_singleton = this;
        this.nexus = nexus;
        this.interceptor = new InterceptorChain();
        this.proxy = new PartitionNexusImpl();
    }


    // ------------------------------------------------------------------------
    // Static Package Friendly Methods
    // ------------------------------------------------------------------------


    /**
     * Enables a ServerContextFactory with a handle to the system wide
     * JndiProvider instance.
     *
     * @param factory the ServerContextFactory to enable
     */
    static void setProviderOn( CoreContextFactory factory )
    {
        factory.setProvider( s_singleton );
    }


    // ------------------------------------------------------------------------
    // BackendSubsystem Interface Method Implemetations
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.ldap.server.BackendSubsystem#getLdapContext(Hashtable)
     */
    public LdapContext getLdapContext( Hashtable env ) throws NamingException
    {
        if ( this.isShutdown )
        {
            throw new IllegalStateException( "Eve has been shutdown!" );
        }

        return new ServerLdapContext( proxy, env );
    }


    public void sync() throws NamingException
    {
        if ( this.isShutdown )
        {
            throw new IllegalStateException( "Eve has been shutdown!" );
        }

        this.nexus.sync();
    }


    public void shutdown() throws NamingException
    {
        if ( this.isShutdown )
        {
            throw new IllegalStateException( "Eve has been shutdown!" );
        }

        this.nexus.sync();
        this.nexus.close();
        this.nexus = null;
        this.proxy = null;
        this.interceptor.destroy();
        this.interceptor = null;
        this.isShutdown = true;
        s_singleton = null;
    }
    
    public Interceptor getInterceptor()
    {
        return interceptor;
    }

    public void setInterceptor( Interceptor interceptor )
    {
        if( interceptor == null )
        {
            throw new NullPointerException( "interceptor" );
        }
        this.interceptor = interceptor;
    }


    public Object invoke( Invocation call ) throws NamingException
    {
        interceptor.process( null, call );
        return call.getReturnValue();
    }

    /**
     * A dead context is requested and returned when we shutdown the system. It
     * prevents a {@link javax.naming.NoInitialContextException} from being
     * thrown by InitialContext or one of its subclasses.
     *
     * @return a unusable dead context
     */
    public Context getDeadContext()
    {
        return new DeadContext();
    }
    
    private class PartitionNexusImpl implements PartitionNexus
    {

        public LdapContext getLdapContext() {
            return nexus.getLdapContext();
        }

        public Name getMatchedDn(Name dn, boolean normalized) throws NamingException {
            return ( Name ) JndiProvider.this.invoke( new GetMatchedDN( dn, normalized ) );
        }

        public Name getSuffix(Name dn, boolean normalized) throws NamingException {
            return ( Name ) JndiProvider.this.invoke( new GetSuffix( dn, normalized ) );
        }

        public Iterator listSuffixes(boolean normalized) throws NamingException {
            return ( Iterator ) JndiProvider.this.invoke( new ListSuffixes( normalized ) );
        }

        public void register(ContextPartition partition) {
            nexus.register( partition );
        }

        public void unregister(ContextPartition partition) {
            nexus.unregister( partition );
        }

        public void delete(Name name) throws NamingException {
            JndiProvider.this.invoke( new Delete( name ) );
        }

        public void add(String upName, Name normName, Attributes entry) throws NamingException {
            JndiProvider.this.invoke( new Add( upName, normName, entry ) );
        }

        public void modify(Name name, int modOp, Attributes mods) throws NamingException {
            JndiProvider.this.invoke( new Modify( name, modOp, mods ) );
        }

        public void modify(Name name, ModificationItem[] mods) throws NamingException {
            JndiProvider.this.invoke( new ModifyMany( name, mods ) );
        }

        public NamingEnumeration list(Name base) throws NamingException {
            return ( NamingEnumeration ) JndiProvider.this.invoke( new List( base ) );
        }

        public NamingEnumeration search(Name base, Map env, ExprNode filter, SearchControls searchCtls) throws NamingException {
            return ( NamingEnumeration ) JndiProvider.this.invoke( new Search( base, env, filter, searchCtls ) );
        }

        public Attributes lookup(Name name) throws NamingException {
            return ( Attributes ) JndiProvider.this.invoke( new Lookup( name ) );
        }

        public Attributes lookup(Name dn, String[] attrIds) throws NamingException {
            return ( Attributes ) JndiProvider.this.invoke( new LookupWithAttrIds( dn, attrIds ) );
        }

        public boolean hasEntry(Name name) throws NamingException {
            return Boolean.TRUE.equals( JndiProvider.this.invoke( new HasEntry( name ) ) );
        }

        public boolean isSuffix(Name name) throws NamingException {
            return Boolean.TRUE.equals( JndiProvider.this.invoke( new IsSuffix( name ) ) );
        }

        public void modifyRn(Name name, String newRn, boolean deleteOldRn) throws NamingException {
            JndiProvider.this.invoke( new ModifyRN( name, newRn, deleteOldRn ) );
        }

        public void move(Name oriChildName, Name newParentName) throws NamingException {
            JndiProvider.this.invoke( new Move( oriChildName, newParentName ) );
        }

        public void move(Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn) throws NamingException {
            JndiProvider.this.invoke( new MoveAndModifyRN( oriChildName, newParentName, newRn, deleteOldRn ) );
        }

        public void sync() throws NamingException {
            nexus.sync();
        }

        public void close() throws NamingException {
            nexus.close();
        }

        public boolean isClosed() {
            return nexus.isClosed();
        }
        
    }
}
