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

import java.util.Iterator;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.configuration.ContextPartitionConfiguration;
import org.apache.ldap.server.invocation.Add;
import org.apache.ldap.server.invocation.Delete;
import org.apache.ldap.server.invocation.GetMatchedDN;
import org.apache.ldap.server.invocation.GetSuffix;
import org.apache.ldap.server.invocation.HasEntry;
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
import org.apache.ldap.server.partition.ContextPartitionNexus;

class RootNexusProxy implements ContextPartitionNexus
{
    private final ContextFactoryConfiguration provider;

    RootNexusProxy( ContextFactoryConfiguration provider )
    {
        this.provider = provider;
    }

    public LdapContext getLdapContext() {
        return this.provider.getRootNexus().getLdapContext();
    }

    public Name getMatchedDn(Name dn, boolean normalized) throws NamingException {
        return ( Name ) this.provider.invoke( new GetMatchedDN( dn, normalized ) );
    }

    public Name getSuffix(Name dn, boolean normalized) throws NamingException {
        return ( Name ) this.provider.invoke( new GetSuffix( dn, normalized ) );
    }

    public Iterator listSuffixes(boolean normalized) throws NamingException {
        return ( Iterator ) this.provider.invoke( new ListSuffixes( normalized ) );
    }

    public void delete(Name name) throws NamingException {
        this.provider.invoke( new Delete( name ) );
    }

    public void add(String upName, Name normName, Attributes entry) throws NamingException {
        this.provider.invoke( new Add( upName, normName, entry ) );
    }

    public void modify(Name name, int modOp, Attributes mods) throws NamingException {
        this.provider.invoke( new Modify( name, modOp, mods ) );
    }

    public void modify(Name name, ModificationItem[] mods) throws NamingException {
        this.provider.invoke( new ModifyMany( name, mods ) );
    }

    public NamingEnumeration list(Name base) throws NamingException {
        return ( NamingEnumeration ) this.provider.invoke( new List( base ) );
    }

    public NamingEnumeration search(Name base, Map env, ExprNode filter, SearchControls searchCtls) throws NamingException {
        return ( NamingEnumeration ) this.provider.invoke( new Search( base, env, filter, searchCtls ) );
    }

    public Attributes lookup(Name name) throws NamingException {
        return ( Attributes ) this.provider.invoke( new Lookup( name ) );
    }

    public Attributes lookup(Name dn, String[] attrIds) throws NamingException {
        return ( Attributes ) this.provider.invoke( new LookupWithAttrIds( dn, attrIds ) );
    }

    public boolean hasEntry(Name name) throws NamingException {
        return Boolean.TRUE.equals( this.provider.invoke( new HasEntry( name ) ) );
    }

    public boolean isSuffix(Name name) throws NamingException {
        return Boolean.TRUE.equals( this.provider.invoke( new IsSuffix( name ) ) );
    }

    public void modifyRn(Name name, String newRn, boolean deleteOldRn) throws NamingException {
        this.provider.invoke( new ModifyRN( name, newRn, deleteOldRn ) );
    }

    public void move(Name oriChildName, Name newParentName) throws NamingException {
        this.provider.invoke( new Move( oriChildName, newParentName ) );
    }

    public void move(Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn) throws NamingException {
        this.provider.invoke( new MoveAndModifyRN( oriChildName, newParentName, newRn, deleteOldRn ) );
    }

    public void sync() throws NamingException {
        this.provider.sync();
    }

    public void close() throws NamingException {
        this.provider.shutdown();
    }

    public boolean isInitialized() {
        return this.provider.getRootNexus().isInitialized();
    }

    public void init( ContextFactoryConfiguration factoryCfg, ContextPartitionConfiguration cfg ) throws NamingException
    {
        throw new IllegalStateException();
    }

    public void destroy() throws NamingException
    {
        throw new IllegalStateException();
    }

    public Name getSuffix( boolean normalized )
    {
        return this.provider.getRootNexus().getSuffix( normalized );
    }
}