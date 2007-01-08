/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.exception;


import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.exception.LdapAttributeInUseException;
import org.apache.directory.shared.ldap.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.exception.LdapNameAlreadyBoundException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * An {@link org.apache.directory.server.core.interceptor.Interceptor} that detects any operations that breaks integrity
 * of {@link Partition} and terminates the current invocation chain by
 * throwing a {@link NamingException}. Those operations include when an entry
 * already exists at a DN and is added once again to the same DN.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExceptionService extends BaseInterceptor
{
    private PartitionNexus nexus;
    private LdapDN subschemSubentryDn;
    
    /**
     * The OIDs normalizer map
     */
    private Map normalizerMap;

    /**
     * Creates an interceptor that is also the exception handling service.
     */
    public ExceptionService()
    {
    }


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        nexus = factoryCfg.getPartitionNexus();
        normalizerMap = factoryCfg.getRegistries().getAttributeTypeRegistry().getNormalizerMapping();
        Attribute attr = nexus.getRootDSE().get( "subschemaSubentry" );
        subschemSubentryDn = new LdapDN( ( String ) attr.get() );
        subschemSubentryDn.normalize( normalizerMap );
    }


    public void destroy()
    {
    }


    /**
     * In the pre-invocation state this interceptor method checks to see if the entry to be added already exists.  If it
     * does an exception is raised.
     */
    public void add( NextInterceptor nextInterceptor, LdapDN normName, Attributes entry )
        throws NamingException
    {
        // check if the entry already exists
        if ( nextInterceptor.hasEntry( normName ) )
        {
            NamingException ne = new LdapNameAlreadyBoundException( normName.toString() + " already exists!" );
            ne.setResolvedName( new LdapDN( normName.getUpName() ) );
            throw ne;
        }

        LdapDN parentDn = ( LdapDN ) normName.clone();
        parentDn.remove( normName.size() - 1 );

        // check if we're trying to add to a parent that is an alias
        Attributes attrs = null;
        
        try
        {
            attrs = nextInterceptor.lookup( parentDn );
        }
        catch ( Exception e )
        {
            LdapNameNotFoundException e2 = new LdapNameNotFoundException( "Parent " + parentDn.getUpName() 
                + " not found" );
            e2.setResolvedName( new LdapDN( nexus.getMatchedName( parentDn ).getUpName() ) );
            throw e2;
        }
        
        Attribute objectClass = attrs.get( "objectClass" );
        if ( objectClass.contains( "alias" ) )
        {
            String msg = "Attempt to add entry to alias '" + normName.getUpName() + "' not allowed.";
            ResultCodeEnum rc = ResultCodeEnum.ALIAS_PROBLEM;
            NamingException e = new LdapNamingException( msg, rc );
            e.setResolvedName( new LdapDN( parentDn.getUpName() ) );
            throw e;
        }

        nextInterceptor.add( normName, entry );
    }


    /**
     * Checks to make sure the entry being deleted exists, and has no children, otherwise throws the appropriate
     * LdapException.
     */
    public void delete( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        // check if entry to delete exists
        String msg = "Attempt to delete non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, name );

        // check if entry to delete has children (only leaves can be deleted)
        boolean hasChildren = false;
        NamingEnumeration list = nextInterceptor.list( name );
        if ( list.hasMore() )
        {
            hasChildren = true;
        }

        list.close();
        if ( hasChildren )
        {
            LdapContextNotEmptyException e = new LdapContextNotEmptyException();
            e.setResolvedName( new LdapDN( name.getUpName() ) );
            throw e;
        }

        nextInterceptor.delete( name );
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the appropriate LdapException.
     */
    public NamingEnumeration list( NextInterceptor nextInterceptor, LdapDN baseName ) throws NamingException
    {
        // check if entry to search exists
        String msg = "Attempt to search under non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, baseName );

        return nextInterceptor.list( baseName );
    }


    /**
     * Checks to make sure the entry being looked up exists other wise throws the appropriate LdapException.
     */
    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        String msg = "Attempt to lookup non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, name );

        return nextInterceptor.lookup( name );
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the appropriate LdapException.
     */
    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name, String[] attrIds ) throws NamingException
    {
        // check if entry to lookup exists
        String msg = "Attempt to lookup non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, name );

        return nextInterceptor.lookup( name, attrIds );
    }


    /**
     * Checks to see the entry being modified exists, otherwise throws the appropriate LdapException.
     */
    public void modify( NextInterceptor nextInterceptor, LdapDN name, int modOp, Attributes attrs )
        throws NamingException
    {
        // check if entry to modify exists
        String msg = "Attempt to modify non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, name );

        Attributes entry = nexus.lookup( name );
        NamingEnumeration attrIds = attrs.getIDs();
        while ( attrIds.hasMore() )
        {
            String attrId = ( String ) attrIds.next();
            Attribute modAttr = attrs.get( attrId );
            Attribute entryAttr = entry.get( attrId );

            if ( modOp == DirContext.ADD_ATTRIBUTE )
            {
                if ( entryAttr != null )
                {
                    for ( int ii = 0; ii < modAttr.size(); ii++ )
                    {
                        if ( entryAttr.contains( modAttr.get( ii ) ) )
                        {
                            throw new LdapAttributeInUseException( "Trying to add existing value '" + modAttr.get( ii )
                                + "' to attribute " + attrId );
                        }
                    }
                }
            }
        }
        nextInterceptor.modify( name, modOp, attrs );
    }


    /**
     * Checks to see the entry being modified exists, otherwise throws the appropriate LdapException.
     */
    public void modify( NextInterceptor nextInterceptor, LdapDN name, ModificationItemImpl[] items ) throws NamingException
    {
        // check if entry to modify exists
        String msg = "Attempt to modify non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, name );

        Attributes entry = nexus.lookup( name );
        for ( int ii = 0; ii < items.length; ii++ )
        {
            if ( items[ii].getModificationOp() == DirContext.ADD_ATTRIBUTE )
            {
                Attribute modAttr = items[ii].getAttribute();
                Attribute entryAttr = entry.get( modAttr.getID() );

                if ( entryAttr != null )
                {
                    for ( int jj = 0; jj < modAttr.size(); jj++ )
                    {
                        if ( entryAttr.contains( modAttr.get( jj ) ) )
                        {
                            throw new LdapAttributeInUseException( "Trying to add existing value '" + modAttr.get( jj )
                                + "' to attribute " + modAttr.getID() );
                        }
                    }
                }
            }
        }
        nextInterceptor.modify( name, items );
    }


    /**
     * Checks to see the entry being renamed exists, otherwise throws the appropriate LdapException.
     */
    public void modifyRn( NextInterceptor nextInterceptor, LdapDN dn, String newRn, boolean deleteOldRn )
        throws NamingException
    {
        // check if entry to rename exists
        String msg = "Attempt to rename non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, dn );

        // check to see if target entry exists
        LdapDN newDn = ( LdapDN ) dn.clone();
        newDn.remove( dn.size() - 1 );
        newDn.add( newRn );
        newDn.normalize( normalizerMap );
        if ( nextInterceptor.hasEntry( newDn ) )
        {
            LdapNameAlreadyBoundException e;
            e = new LdapNameAlreadyBoundException( "target entry " + newDn.getUpName() + " already exists!" );
            e.setResolvedName( new LdapDN( newDn.getUpName() ) );
            throw e;
        }

        nextInterceptor.modifyRn( dn, newRn, deleteOldRn );
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent, otherwise throws the appropriate
     * LdapException.
     */
    public void move( NextInterceptor nextInterceptor, LdapDN oriChildName, LdapDN newParentName ) throws NamingException
    {
        // check if child to move exists
        String msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( nextInterceptor, msg, oriChildName );

        // check if parent to move to exists
        msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( nextInterceptor, msg, newParentName );

        // check to see if target entry exists
        String rdn = oriChildName.get( oriChildName.size() - 1 );
        LdapDN target = ( LdapDN ) newParentName.clone();
        target.add( rdn );
        if ( nextInterceptor.hasEntry( target ) )
        {
            // we must calculate the resolved name using the user provided Rdn value
            String upRdn = new LdapDN( oriChildName.getUpName() ).get( oriChildName.size() - 1 );
            LdapDN upTarget = ( LdapDN ) newParentName.clone();
            upTarget.add( upRdn );

            LdapNameAlreadyBoundException e;
            e = new LdapNameAlreadyBoundException( "target entry " + upTarget.getUpName() + " already exists!" );
            e.setResolvedName( new LdapDN( upTarget.getUpName() ) );
            throw e;
        }

        nextInterceptor.move( oriChildName, newParentName );
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent, otherwise throws the appropriate
     * LdapException.
     */
    public void move( NextInterceptor nextInterceptor, LdapDN oriChildName, LdapDN newParentName, String newRn,
        boolean deleteOldRn ) throws NamingException
    {
        // check if child to move exists
        String msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( nextInterceptor, msg, oriChildName );

        // check if parent to move to exists
        msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( nextInterceptor, msg, newParentName );

        // check to see if target entry exists
        LdapDN target = ( LdapDN ) newParentName.clone();
        target.add( newRn );
        target.normalize( normalizerMap );
        if ( nextInterceptor.hasEntry( target ) )
        {
            // we must calculate the resolved name using the user provided Rdn value
            LdapDN upTarget = ( LdapDN ) newParentName.clone();
            upTarget.add( newRn );

            LdapNameAlreadyBoundException e;
            e = new LdapNameAlreadyBoundException( "target entry " + upTarget.getUpName() + " already exists!" );
            e.setResolvedName( new LdapDN( upTarget.getUpName() ) );
            throw e;
        }

        nextInterceptor.move( oriChildName, newParentName, newRn, deleteOldRn );
    }


    /**
     * Checks to see the entry being searched exists, otherwise throws the appropriate LdapException.
     */
    public NamingEnumeration search( NextInterceptor nextInterceptor, LdapDN base, Map env, ExprNode filter,
        SearchControls searchCtls ) throws NamingException
    {
        String msg = "Attempt to search under non-existant entry: ";

        if ( base.size() == 0 )
        {
            return nextInterceptor.search( base, env, filter, searchCtls );
        }

        if ( ( subschemSubentryDn.toNormName() ).equalsIgnoreCase( base.toNormName() ) )
        {
            return nextInterceptor.search( base, env, filter, searchCtls );
        }

        assertHasEntry( nextInterceptor, msg, base );

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
    private void assertHasEntry( NextInterceptor nextInterceptor, String msg, LdapDN dn ) throws NamingException
    {
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        if ( !nextInterceptor.hasEntry( dn ) )
        {
            LdapNameNotFoundException e;

            if ( msg != null )
            {
                e = new LdapNameNotFoundException( msg + dn );
            }
            else
            {
                e = new LdapNameNotFoundException( dn.toString() );
            }

            e.setResolvedName( new LdapDN( proxy.getMatchedName( dn ).getUpName() ) );
            throw e;
        }
    }
}
