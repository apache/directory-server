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


import org.apache.ldap.common.exception.LdapContextNotEmptyException;
import org.apache.ldap.common.exception.LdapNameAlreadyBoundException;
import org.apache.ldap.common.exception.LdapNameNotFoundException;
import org.apache.ldap.common.exception.LdapNamingException;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.BackingStore;
import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.interceptor.BaseInterceptor;
import org.apache.ldap.server.interceptor.InterceptorContext;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.invocation.*;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;


/**
 * An {@link org.apache.ldap.server.interceptor.Interceptor} that detects any operations that breaks integrity
 * of {@link BackingStore} and terminates the current invocation chain by
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
    private RootNexus nexus;


    /**
     * Creates an interceptor that is also the exception handling service.
     */
    public ExceptionService()
    {
    }


    public void init( InterceptorContext ctx )
    {
        this.nexus = ctx.getRootNexus();
    }


    public void destroy()
    {
    }


    /**
     * In the pre-invocation state this interceptor method checks to see if the entry to be added already exists.  If it
     * does an exception is raised.
     */
    protected void process( NextInterceptor nextInterceptor, Add call ) throws NamingException
    {
        // check if the entry already exists
        Name normName = call.getNormalizedName();
        String upName = call.getUserProvidedName();
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

        nextInterceptor.process( call );
    }


    /**
     * Checks to make sure the entry being deleted exists, and has no children, otherwise throws the appropriate
     * LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, Delete call ) throws NamingException
    {
        Name name = call.getName();
        
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

        nextInterceptor.process( call );
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the appropriate LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, List call ) throws NamingException
    {
        // check if entry to search exists
        String msg = "Attempt to search under non-existant entry: ";
        assertHasEntry( msg, call.getBaseName() );

        nextInterceptor.process( call );
    }


    /**
     * Checks to make sure the entry being looked up exists other wise throws the appropriate LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, Lookup call ) throws NamingException
    {
        String msg = "Attempt to lookup non-existant entry: ";
        assertHasEntry( msg, call.getName() );

        nextInterceptor.process( call );
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the appropriate LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, LookupWithAttrIds call ) throws NamingException
    {
        // check if entry to lookup exists
        String msg = "Attempt to lookup non-existant entry: ";
        assertHasEntry( msg, call.getName() );

        nextInterceptor.process( call );
    }


    /**
     * Checks to see the entry being modified exists, otherwise throws the appropriate LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, Modify call ) throws NamingException
    {
        // check if entry to modify exists
        String msg = "Attempt to modify non-existant entry: ";
        assertHasEntry( msg, call.getName() );

        nextInterceptor.process( call );
    }


    /**
     * Checks to see the entry being modified exists, otherwise throws the appropriate LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, ModifyMany call ) throws NamingException
    {
        // check if entry to modify exists
        String msg = "Attempt to modify non-existant entry: ";
        assertHasEntry( msg, call.getName() );

        nextInterceptor.process( call );
    }


    /**
     * Checks to see the entry being renamed exists, otherwise throws the appropriate LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, ModifyRN call ) throws NamingException
    {
        Name dn = call.getName();
        String newRdn = call.getNewRelativeName();
        
        // check if entry to rename exists
        String msg = "Attempt to rename non-existant entry: ";
        assertHasEntry( msg, dn );

        // check to see if target entry exists
        Name target = dn.getSuffix( 1 ).add( newRdn );
        if ( nexus.hasEntry( target ) )
        {
            LdapNameAlreadyBoundException e = null;
            e = new LdapNameAlreadyBoundException( "target entry " + target
                    + " already exists!" );
            e.setResolvedName( target );
            throw e;
        }

        nextInterceptor.process( call );
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent, otherwise throws the appropriate
     * LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, Move call ) throws NamingException
    {
        // check if child to move exists
        String msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( msg, call.getName() );

        // check if parent to move to exists
        msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( msg, call.getNewParentName() );

        // check to see if target entry exists
        String rdn = call.getName().get( call.getName().size() - 1 );
        Name target = ( Name ) call.getNewParentName().clone();
        target.add( rdn );
        if ( nexus.hasEntry( target ) )
        {
            LdapNameAlreadyBoundException e = null;
            e = new LdapNameAlreadyBoundException( "target entry " + target
                    + " already exists!" );
            e.setResolvedName( target );
            throw e;
        }

        nextInterceptor.process( call );
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent, otherwise throws the appropriate
     * LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, MoveAndModifyRN call ) throws NamingException
    {
        // check if child to move exists
        String msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( msg, call.getName() );

        // check if parent to move to exists
        msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( msg, call.getNewParentName() );

        // check to see if target entry exists
        Name target = ( Name ) call.getNewParentName().clone();
        target.add( call.getNewRelativeName() );
        if ( nexus.hasEntry( target ) )
        {
            LdapNameAlreadyBoundException e = null;
            e = new LdapNameAlreadyBoundException( "target entry " + target
                    + " already exists!" );
            e.setResolvedName( target );
            throw e;
        }

        nextInterceptor.process( call );
    }


    /**
     * Checks to see the entry being searched exists, otherwise throws the appropriate LdapException.
     */
    protected void process( NextInterceptor nextInterceptor, Search call ) throws NamingException
    {
        String msg = "Attempt to search under non-existant entry: ";

        Name base = call.getBaseName();
        if ( base.size() == 0 )
        {
            nextInterceptor.process( call );
            return;
        }

        Attribute attr = nexus.getRootDSE().get( "subschemaSubentry" );
        if ( ( ( String ) attr.get() ).equalsIgnoreCase( base.toString() ) )
        {
            nextInterceptor.process( call );
            return;
        }

        assertHasEntry( msg, base );

        nextInterceptor.process( call );
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

            e.setResolvedName( nexus.getMatchedDn( dn, false ) );
            throw e;
        }
    }
}
