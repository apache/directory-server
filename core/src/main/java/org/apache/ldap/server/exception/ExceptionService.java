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
package org.apache.ldap.server.exception;


import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.exception.LdapContextNotEmptyException;
import org.apache.ldap.common.exception.LdapNameAlreadyBoundException;
import org.apache.ldap.common.exception.LdapNameNotFoundException;
import org.apache.ldap.common.exception.LdapNamingException;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.jndi.ContextFactoryConfiguration;
import org.apache.ldap.server.partition.ContextPartition;
import org.apache.ldap.server.partition.ContextPartitionNexus;


/**
 * An {@link org.apache.ldap.server.interceptor.Interceptor} that detects any operations that breaks integrity
 * of {@link ContextPartition} and terminates the current invocation chain by
 * throwing a {@link NamingException}. Those operations include when an entry
 * already exists at a DN and is added once again to the same DN.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExceptionService extends BaseInterceptor
{
    /**
     * the root nexus of the system
     */
    private ContextPartitionNexus nexus;


    /**
     * Creates an interceptor that is also the exception handling service.
     */
    public ExceptionService()
    {
    }


    public void init( ContextFactoryConfiguration factoryCfg, InterceptorConfiguration cfg )
    {
        this.nexus = factoryCfg.getPartitionNexus();
    }


    public void destroy()
    {
    }


    /**
     * In the pre-invocation state this interceptor method checks to see if the entry to be added already exists.  If it
     * does an exception is raised.
     */
    public void add( NextInterceptor nextInterceptor, String upName, Name normName, Attributes entry ) throws NamingException
    {
        // check if the entry already exists
        if ( nexus.hasEntry( normName ) )
        {
            NamingException ne = new LdapNameAlreadyBoundException();
            ne.setResolvedName( new LdapName( upName ) );
            throw ne;
        }

        Name parentDn = new LdapName( upName );
        parentDn = parentDn.getSuffix( 1 );

        // check if we don't have the parent to add to
        assertHasEntry( "Attempt to add under non-existant parent: ", parentDn );

        // check if we're trying to add to a parent that is an alias
        Attributes attrs = nexus.lookup( normName.getSuffix( 1 ) );
        Attribute objectClass = attrs.get( "objectClass" );
        if ( objectClass.contains( "alias" ) )
        {
            String msg = "Attempt to add entry to alias '" + upName
                    + "' not allowed.";
            ResultCodeEnum rc = ResultCodeEnum.ALIASPROBLEM;
            NamingException e = new LdapNamingException( msg, rc );
            e.setResolvedName( parentDn );
            throw e;
        }

        nextInterceptor.add( upName, normName, entry );
    }


    /**
     * Checks to make sure the entry being deleted exists, and has no children, otherwise throws the appropriate
     * LdapException.
     */
    public void delete( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        // check if entry to delete exists
        String msg = "Attempt to delete non-existant entry: ";
        assertHasEntry( msg, name );

        // check if entry to delete has children (only leaves can be deleted)
        boolean hasChildren = false;
        NamingEnumeration list = nexus.list( name );
        if ( list.hasMore() )
        {
            hasChildren = true;
        }

        list.close();
        if ( hasChildren )
        {
            LdapContextNotEmptyException e = new LdapContextNotEmptyException();
            e.setResolvedName( name );
            throw e;
        }

        nextInterceptor.delete( name );
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the appropriate LdapException.
     */
    public NamingEnumeration list( NextInterceptor nextInterceptor, Name baseName ) throws NamingException
    {
        // check if entry to search exists
        String msg = "Attempt to search under non-existant entry: ";
        assertHasEntry( msg, baseName );

        return nextInterceptor.list( baseName );
    }


    /**
     * Checks to make sure the entry being looked up exists other wise throws the appropriate LdapException.
     */
    public Attributes lookup( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        String msg = "Attempt to lookup non-existant entry: ";
        assertHasEntry( msg, name );

        return nextInterceptor.lookup( name );
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the appropriate LdapException.
     */
    public Attributes lookup( NextInterceptor nextInterceptor, Name name, String[] attrIds ) throws NamingException
    {
        // check if entry to lookup exists
        String msg = "Attempt to lookup non-existant entry: ";
        assertHasEntry( msg, name );

        return nextInterceptor.lookup( name, attrIds );
    }


    /**
     * Checks to see the entry being modified exists, otherwise throws the appropriate LdapException.
     */
    public void modify( NextInterceptor nextInterceptor, Name name, int modOp, Attributes attrs ) throws NamingException
    {
        // check if entry to modify exists
        String msg = "Attempt to modify non-existant entry: ";
        assertHasEntry( msg, name );

        nextInterceptor.modify( name, modOp, attrs );
    }


    /**
     * Checks to see the entry being modified exists, otherwise throws the appropriate LdapException.
     */
    public void modify( NextInterceptor nextInterceptor, Name name, ModificationItem[] items ) throws NamingException
    {
        // check if entry to modify exists
        String msg = "Attempt to modify non-existant entry: ";
        assertHasEntry( msg, name );

        nextInterceptor.modify( name, items );
    }


    /**
     * Checks to see the entry being renamed exists, otherwise throws the appropriate LdapException.
     */
    public void modifyRn( NextInterceptor nextInterceptor, Name dn, String newRn, boolean deleteOldRn ) throws NamingException
    {
        // check if entry to rename exists
        String msg = "Attempt to rename non-existant entry: ";
        assertHasEntry( msg, dn );

        // check to see if target entry exists
        Name target = dn.getSuffix( 1 ).add( newRn );
        if ( nexus.hasEntry( target ) )
        {
            LdapNameAlreadyBoundException e = null;
            e = new LdapNameAlreadyBoundException( "target entry " + target
                    + " already exists!" );
            e.setResolvedName( target );
            throw e;
        }

        nextInterceptor.modifyRn( dn, newRn, deleteOldRn );
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent, otherwise throws the appropriate
     * LdapException.
     */
    public void move( NextInterceptor nextInterceptor, Name oriChildName, Name newParentName ) throws NamingException
    {
        // check if child to move exists
        String msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( msg, oriChildName );

        // check if parent to move to exists
        msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( msg, newParentName );

        // check to see if target entry exists
        String rdn = oriChildName.get( oriChildName.size() - 1 );
        Name target = ( Name ) newParentName.clone();
        target.add( rdn );
        if ( nexus.hasEntry( target ) )
        {
            LdapNameAlreadyBoundException e = null;
            e = new LdapNameAlreadyBoundException( "target entry " + target
                    + " already exists!" );
            e.setResolvedName( target );
            throw e;
        }

        nextInterceptor.move( oriChildName, newParentName );
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent, otherwise throws the appropriate
     * LdapException.
     */
    public void move( NextInterceptor nextInterceptor,
            Name oriChildName, Name newParentName, String newRn,
            boolean deleteOldRn ) throws NamingException
    {
        // check if child to move exists
        String msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( msg, oriChildName );

        // check if parent to move to exists
        msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( msg, newParentName );

        // check to see if target entry exists
        Name target = ( Name ) newParentName.clone();
        target.add( newRn );
        if ( nexus.hasEntry( target ) )
        {
            LdapNameAlreadyBoundException e = null;
            e = new LdapNameAlreadyBoundException( "target entry " + target
                    + " already exists!" );
            e.setResolvedName( target );
            throw e;
        }

        nextInterceptor.move( oriChildName, newParentName, newRn, deleteOldRn );
    }


    /**
     * Checks to see the entry being searched exists, otherwise throws the appropriate LdapException.
     */
    public NamingEnumeration search( NextInterceptor nextInterceptor, 
            Name base, Map env, ExprNode filter,
            SearchControls searchCtls ) throws NamingException
    {
        String msg = "Attempt to search under non-existant entry: ";

        if ( base.size() == 0 )
        {
            return nextInterceptor.search( base, env, filter, searchCtls );
        }

        Attribute attr = nexus.getRootDSE().get( "subschemaSubentry" );
        if ( ( ( String ) attr.get() ).equalsIgnoreCase( base.toString() ) )
        {
            return nextInterceptor.search( base, env, filter, searchCtls );
        }

        assertHasEntry( msg, base );

        return nextInterceptor.search( base, env, filter, searchCtls );
    }


    /**
     * Asserts that an entry is present and as a side effect if it is not, creates a LdapNameNotFoundException, which is
     * used to set the before exception on the invocation - eventually the exception is thrown.
     *
     * @param msg        the message to prefix to the distinguished name for explanation
     * @param dn         the distinguished name of the entry that is asserted
     * @throws NamingException if the entry does not exist
     */
    private void assertHasEntry( String msg, Name dn ) throws NamingException
    {
        if ( !nexus.hasEntry( dn ) )
        {
            LdapNameNotFoundException e = null;

            if ( msg != null )
            {
                e = new LdapNameNotFoundException( msg + dn );
            }
            else
            {
                e = new LdapNameNotFoundException( dn.toString() );
            }

            e.setResolvedName( nexus.getMatchedName( dn, false ) );
            throw e;
        }
    }
}
