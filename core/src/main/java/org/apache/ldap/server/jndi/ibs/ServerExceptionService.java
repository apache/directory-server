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
package org.apache.ldap.server.jndi.ibs;


import java.util.Map;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.exception.*;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.exception.InterceptorException;
import org.apache.ldap.server.jndi.BaseInterceptor;
import org.apache.ldap.server.jndi.Invocation;
import org.apache.ldap.server.jndi.InvocationStateEnum;


/**
 * An interceptor based service used to detect, raise and handle eve exceptions
 * in one place.  This interceptor has two modes of operation.  The first mode
 * is as a before chain interceptor where it raises exceptions.  An example
 * where this interceptor raises an exception is when an entry already exists
 * at a DN and is added once again to the same DN.  The other mode is as an on
 * error chain interceptor.  In this mode the service may wrap exceptions and
 * add extra information to an exception which is to be thrown back at the
 * caller.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerExceptionService extends BaseInterceptor
{
    /** the root nexus of the system */
    private RootNexus nexus = null;


    /**
     * Creates an interceptor that is also the exception handling service.
     *
     * @param nexus the root partition nexus
     */
    public ServerExceptionService( RootNexus nexus )
    {
        this.nexus = nexus;
    }


    /**
     * Before calling super method which delegates to specific method invocation
     * analogs we make sure the exception for the before failure or after
     * failure states is one that implements LdapException interface so we
     * have something that associates an LDAP error code.
     */
    public void invoke( Invocation invocation ) throws NamingException
    {
        if ( invocation.getState() == InvocationStateEnum.FAILUREHANDLING )
        {
            Throwable t = null;

            if ( invocation.getBeforeFailure() != null )
            {
                t = invocation.getBeforeFailure();

                if ( t instanceof InterceptorException )
                {
                    InterceptorException eie = ( InterceptorException ) t;

                    if ( eie.getRootCause() != null )
                    {
                        invocation.setBeforeFailure( eie.getRootCause() );
                    }

                    else if ( eie.getCause() != null )
                    {
                        invocation.setBeforeFailure( eie.getCause() );
                    }
                }
            }
            else if ( invocation.getAfterFailure() != null )
            {
                t = invocation.getAfterFailure();

                if ( t instanceof InterceptorException )
                {
                    InterceptorException eie = ( InterceptorException ) t;

                    if ( eie.getRootCause() != null )
                    {
                        invocation.setAfterFailure( eie.getRootCause() );
                    }

                    else if ( eie.getCause() != null )
                    {
                        invocation.setAfterFailure( eie.getCause() );
                    }
                }
            }
        }

        super.invoke( invocation );
    }


    /**
     * In the pre-invocation state this interceptor method checks to see if
     * the entry to be added already exists.  If it does an exception is
     * raised.
     *
     * @see BaseInterceptor#add(String, Name, Attributes)
     */
    protected void add( String upName, Name normName, Attributes entry ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            // check if the entry already exists
            if ( nexus.hasEntry( normName ) )
            {
                NamingException ne = new LdapNameAlreadyBoundException();
                invocation.setBeforeFailure( ne );
                ne.setResolvedName( new LdapName( upName ) );
                throw ne;
            }

            Name parentDn = new LdapName( upName );
            parentDn = parentDn.getSuffix( 1 );

            // check if we don't have the parent to add to
            assertHasEntry( "Attempt to add under non-existant parent: ", parentDn, invocation );

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
                invocation.setBeforeFailure( e );
                throw e;
            }
        }
    }


    /**
     * Checks to make sure the entry being deleted exists, and has no children,
     * otherwise throws the appropriate LdapException.
     */
    protected void delete( Name name ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            // check if entry to delete exists
            String msg = "Attempt to delete non-existant entry: ";
            assertHasEntry( msg, name, invocation );

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
                invocation.setBeforeFailure( e );
                throw e;
            }
        }
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the
     * appropriate LdapException.
     */
    protected void list( Name base ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            // check if entry to search exists
            String msg = "Attempt to search under non-existant entry: ";
            assertHasEntry( msg, base, invocation );
        }
    }


    /**
     * Checks to make sure the entry being looked up exists other wise throws
     * the appropriate LdapException.
     *
     * @see org.apache.ldap.server.jndi.BaseInterceptor#lookup(javax.naming.Name)
     */
    protected void lookup( Name dn ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            String msg = "Attempt to lookup non-existant entry: ";
            assertHasEntry( msg, dn, invocation );
        }
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the
     * appropriate LdapException.
     */
    protected void lookup( Name dn, String[] attrIds ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            // check if entry to lookup exists
            String msg = "Attempt to lookup non-existant entry: ";
            assertHasEntry( msg, dn, invocation );
        }
    }


    /**
     * Checks to see the entry being modified exists, otherwise throws the
     * appropriate LdapException.
     */
    protected void modify( Name dn, int modOp, Attributes mods ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            // check if entry to modify exists
            String msg = "Attempt to modify non-existant entry: ";
            assertHasEntry( msg, dn, invocation );
        }
    }


    /**
     * Checks to see the entry being modified exists, otherwise throws the
     * appropriate LdapException.
     */
    protected void modify( Name dn, ModificationItem[] mods ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            // check if entry to modify exists
            String msg = "Attempt to modify non-existant entry: ";
            assertHasEntry( msg, dn, invocation );
        }
    }


    /**
     * Checks to see the entry being renamed exists, otherwise throws the
     * appropriate LdapException.
     */
    protected void modifyRdn( Name dn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            // check if entry to rename exists
            String msg = "Attempt to rename non-existant entry: ";
            assertHasEntry( msg, dn, invocation );

            // check to see if target entry exists
            Name target = dn.getSuffix( 1 ).add( newRdn );
            if ( nexus.hasEntry( target ) )
            {
                LdapNameAlreadyBoundException e = null;
                e = new LdapNameAlreadyBoundException( "target entry " + target
                    + " already exists!" );
                invocation.setBeforeFailure( e );
                e.setResolvedName( target );
                throw e;
            }
        }
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent,
     * otherwise throws the appropriate LdapException.
     */
    protected void move( Name oriChildName, Name newParentName ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            // check if child to move exists
            String msg = "Attempt to move to non-existant parent: ";
            assertHasEntry( msg, oriChildName, invocation );

            // check if parent to move to exists
            msg = "Attempt to move to non-existant parent: ";
            assertHasEntry( msg, newParentName, invocation );

            // check to see if target entry exists
            String rdn = oriChildName.get( oriChildName.size() - 1 );
            Name target = ( Name ) newParentName.clone();
            target.add( rdn );
            if ( nexus.hasEntry( target ) )
            {
                LdapNameAlreadyBoundException e = null;
                e = new LdapNameAlreadyBoundException( "target entry " + target
                    + " already exists!" );
                invocation.setBeforeFailure( e );
                e.setResolvedName( target );
                throw e;
            }
        }
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent,
     * otherwise throws the appropriate LdapException.
     */
    protected void move( Name oriChildName, Name newParentName, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            // check if child to move exists
            String msg = "Attempt to move to non-existant parent: ";
            assertHasEntry( msg, oriChildName, invocation );

            // check if parent to move to exists
            msg = "Attempt to move to non-existant parent: ";
            assertHasEntry( msg, newParentName, invocation );

            // check to see if target entry exists
            Name target = ( Name ) newParentName.clone();
            target.add( newRdn );
            if ( nexus.hasEntry( target ) )
            {
                LdapNameAlreadyBoundException e = null;
                e = new LdapNameAlreadyBoundException( "target entry " + target
                    + " already exists!" );
                invocation.setBeforeFailure( e );
                e.setResolvedName( target );
                throw e;
            }
        }
    }


    /**
     * Checks to see the entry being searched exists, otherwise throws the
     * appropriate LdapException.
     */
    protected void search( Name base, Map env, ExprNode filter,
                           SearchControls searchControls ) throws NamingException
    {
        Invocation invocation = getInvocation();

        if ( invocation.getState() == InvocationStateEnum.PREINVOCATION )
        {
            String msg = "Attempt to search under non-existant entry: ";

            if ( base.size() == 0 )
            {
                return;
            }
            
            Attribute attr = nexus.getRootDSE().get( "subschemaSubentry" );
            if ( ( ( String ) attr.get() ).equalsIgnoreCase( base.toString() ) )
            {
                return;
            }  

            assertHasEntry( msg, base, invocation );
        }
    }


    /**
     * Asserts that an entry is present and as a side effect if it is not,
     * creates a LdapNameNotFoundException, which is used to set the before
     * exception on the invocation - eventually the exception is thrown.
     *
     * @param msg the message to prefix to the distinguished name for explanation
     * @param dn the distinguished name of the entry that is asserted
     * @param invocation the invocation object to alter if the entry does not exist
     * @throws NamingException if the entry does not exist
     */
    private void assertHasEntry( String msg, Name dn, Invocation invocation ) throws NamingException
    {
        if ( ! nexus.hasEntry( dn ) )
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
            invocation.setBeforeFailure( e );
            throw e;
        }
    }
}
