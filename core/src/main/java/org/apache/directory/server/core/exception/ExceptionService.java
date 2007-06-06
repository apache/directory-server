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
import javax.naming.directory.SearchResult;

import org.apache.commons.collections.map.LRUMap;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapAttributeInUseException;
import org.apache.directory.shared.ldap.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.exception.LdapNameAlreadyBoundException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.EmptyEnumeration;


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
    /** The service name */
    public static final String NAME = "exceptionService";

    private PartitionNexus nexus;
    private LdapDN subschemSubentryDn;
    
    /**
     * The OIDs normalizer map
     */
    private Map<String, OidNormalizer> normalizerMap;
    
    /**
     * A cache to store entries which are not aliases. 
     * It's a speedup, we will be able to avoid backend lookups.
     * 
     * Note that the backend also use a cache mechanism, but for performance gain, it's good 
     * to manage a cache here. The main problem is that when a user modify the parent, we will
     * have to update it at three different places :
     * - in the backend,
     * - in the partition cache,
     * - in this cache.
     * 
     * The update of the backend and partition cache is already correctly handled, so we will
     * just have to offer an access to refresh the local cache. This should be done in 
     * delete, modify and move operations.
     * 
     * We need to be sure that frequently used DNs are always in cache, and not discarded.
     * We will use a LRU cache for this purpose. 
     */ 
    private LRUMap notAliasCache;

    /** Declare a default for this cache. 100 entries seems to be enough */
    private static final int DEFAULT_CACHE_SIZE = 100;

    
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
        Attribute attr = nexus.getRootDSE( null ).get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
        subschemSubentryDn = new LdapDN( ( String ) attr.get() );
        subschemSubentryDn.normalize( normalizerMap );
        notAliasCache = new LRUMap( DEFAULT_CACHE_SIZE );
    }


    public void destroy()
    {
    }

    /**
     * In the pre-invocation state this interceptor method checks to see if the entry to be added already exists.  If it
     * does an exception is raised.
     */
    public void add( NextInterceptor nextInterceptor, OperationContext opContext )
        throws NamingException
    {
    	LdapDN name = opContext.getDn();
    	
        if ( subschemSubentryDn.getNormName().equals( name.getNormName() ) )
        {
            throw new LdapNameAlreadyBoundException( 
                "The global schema subentry cannot be added since it exists by default." );
        }
        
        // check if the entry already exists
        if ( nextInterceptor.hasEntry( new EntryOperationContext( name ) ) )
        {
            NamingException ne = new LdapNameAlreadyBoundException( name.getUpName() + " already exists!" );
            ne.setResolvedName( new LdapDN( name.getUpName() ) );
            throw ne;
        }

        LdapDN parentDn = ( LdapDN ) name.clone();
        parentDn.remove( name.size() - 1 );

        // check if we're trying to add to a parent that is an alias
        boolean notAnAlias = false;
        
        synchronized( notAliasCache )
        {
        	notAnAlias = notAliasCache.containsKey( parentDn.getNormName() );
        }
        
        if ( notAnAlias == false )
        {
        	// We don't know if the parent is an alias or not, so we will launch a 
        	// lookup, and update the cache if it's not an alias
            Attributes attrs = null;
            
            try
            {
                attrs = nextInterceptor.lookup( new LookupOperationContext( parentDn ) );
            }
            catch ( Exception e )
            {
                LdapNameNotFoundException e2 = new LdapNameNotFoundException( "Parent " + parentDn.getUpName() 
                    + " not found" );
                e2.setResolvedName( new LdapDN( nexus.getMatchedName( new GetMatchedNameOperationContext( parentDn ) ).getUpName() ) );
                throw e2;
            }
            
            Attribute objectClass = attrs.get( SchemaConstants.OBJECT_CLASS_AT );
            
            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                String msg = "Attempt to add entry to alias '" + name.getUpName() + "' not allowed.";
                ResultCodeEnum rc = ResultCodeEnum.ALIAS_PROBLEM;
                NamingException e = new LdapNamingException( msg, rc );
                e.setResolvedName( new LdapDN( parentDn.getUpName() ) );
                throw e;
            }
            else
            {
            	synchronized ( notAliasCache )
            	{
            		notAliasCache.put( parentDn.getNormName(), parentDn );
            	}
            }
        }

        nextInterceptor.add( opContext );
    }


    /**
     * Checks to make sure the entry being deleted exists, and has no children, otherwise throws the appropriate
     * LdapException.
     */
    public void delete( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
    	LdapDN name = opContext.getDn();
    	
        if ( name.getNormName().equalsIgnoreCase( subschemSubentryDn.getNormName() ) )
        {
            throw new LdapOperationNotSupportedException( 
                "Can not allow the deletion of the subschemaSubentry (" + 
                subschemSubentryDn + ") for the global schema.",
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        // check if entry to delete exists
        String msg = "Attempt to delete non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, name );

        // check if entry to delete has children (only leaves can be deleted)
        boolean hasChildren = false;
        NamingEnumeration list = nextInterceptor.list( new ListOperationContext( name ) );
        
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

        synchronized( notAliasCache )
        {
        	if ( notAliasCache.containsKey( name.getNormName() ) )
        	{
        		notAliasCache.remove( name.getNormName() );
        	}
        }
        
        nextInterceptor.delete( opContext );
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the appropriate LdapException.
     */
    public NamingEnumeration list( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        if ( opContext.getDn().getNormName().equals( subschemSubentryDn.getNormName() ) )
        {
            // there is nothing under the schema subentry
            return new EmptyEnumeration();
        }
        
        // check if entry to search exists
        String msg = "Attempt to search under non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, opContext.getDn() );

        return nextInterceptor.list( opContext );
    }


    /**
     * Checks to see the base being searched exists, otherwise throws the appropriate LdapException.
     */
    public Attributes lookup( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LookupOperationContext ctx = (LookupOperationContext)opContext;
        
        if ( ctx.getDn().getNormName().equals( subschemSubentryDn.getNormName() ) )
        {
            return nexus.getRootDSE( null );
        }
        
        // check if entry to lookup exists
        String msg = "Attempt to lookup non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, ctx.getDn() );

        return nextInterceptor.lookup( opContext );
    }


    /**
     * Checks to see the entry being modified exists, otherwise throws the appropriate LdapException.
     */
    public void modify( NextInterceptor nextInterceptor, OperationContext opContext )
        throws NamingException
    {
    	ModifyOperationContext ctx = (ModifyOperationContext)opContext;

        // check if entry to modify exists
        String msg = "Attempt to modify non-existant entry: ";

        // handle operations against the schema subentry in the schema service
        // and never try to look it up in the nexus below
        if ( ctx.getDn().getNormName().equalsIgnoreCase( subschemSubentryDn.getNormName() ) )
        {
            nextInterceptor.modify( opContext );
            return;
        }
        
        assertHasEntry( nextInterceptor, msg, ctx.getDn() );

        Attributes entry = nexus.lookup( new LookupOperationContext( ctx.getDn() ) );
        ModificationItemImpl[] items = ctx.getModItems();
        
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

        // Let's assume that the new modified entry may be an alias,
        // but we don't want to check that now...
        // We will simply remove the DN from the NotAlias cache.
        // It would be smarter to check the modified attributes, but
        // it would also be more complex.
        synchronized( notAliasCache )
        {
        	if ( notAliasCache.containsKey( ctx.getDn().getNormName() ) )
        	{
        		notAliasCache.remove( ctx.getDn().getNormName() );
        	}
        }

        nextInterceptor.modify( opContext );
    }

    /**
     * Checks to see the entry being renamed exists, otherwise throws the appropriate LdapException.
     */
    public void rename( NextInterceptor nextInterceptor, OperationContext opContext )
        throws NamingException
    {
        LdapDN dn = opContext.getDn();
        
        if ( dn.getNormName().equalsIgnoreCase( subschemSubentryDn.getNormName() ) )
        {
            throw new LdapOperationNotSupportedException( 
                "Can not allow the renaming of the subschemaSubentry (" + 
                subschemSubentryDn + ") for the global schema: it is fixed at " + subschemSubentryDn,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        // check if entry to rename exists
        String msg = "Attempt to rename non-existant entry: ";
        assertHasEntry( nextInterceptor, msg, dn );

        // check to see if target entry exists
        LdapDN newDn = ( LdapDN ) dn.clone();
        newDn.remove( dn.size() - 1 );
        newDn.add( ((RenameOperationContext)opContext).getNewRdn() );
        newDn.normalize( normalizerMap );
        
        if ( nextInterceptor.hasEntry( new EntryOperationContext( newDn ) ) )
        {
            LdapNameAlreadyBoundException e;
            e = new LdapNameAlreadyBoundException( "target entry " + newDn.getUpName() + " already exists!" );
            e.setResolvedName( new LdapDN( newDn.getUpName() ) );
            throw e;
        }

        // Remove the previous entry from the notAnAlias cache
        synchronized( notAliasCache )
        {
        	if ( notAliasCache.containsKey( dn.getNormName() ) )
        	{
        		notAliasCache.remove( dn.getNormName() );
        	}
        }

        nextInterceptor.rename( opContext );
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent, otherwise throws the appropriate
     * LdapException.
     */
    public void move( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN oriChildName = opContext.getDn();
        LdapDN newParentName = ((MoveOperationContext)opContext).getParent();
        
        if ( oriChildName.getNormName().equalsIgnoreCase( subschemSubentryDn.getNormName() ) )
        {
            throw new LdapOperationNotSupportedException( 
                "Can not allow the move of the subschemaSubentry (" + 
                subschemSubentryDn + ") for the global schema: it is fixed at " + subschemSubentryDn,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
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
        
        if ( nextInterceptor.hasEntry( new EntryOperationContext( target ) ) )
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

        // Remove the original entry from the NotAlias cache, if needed
        synchronized( notAliasCache )
        {
        	if ( notAliasCache.containsKey( oriChildName.getNormName() ) )
        	{
        		notAliasCache.remove( oriChildName.getNormName() );
        	}
        }
                
        nextInterceptor.move( opContext );
    }


    /**
     * Checks to see the entry being moved exists, and so does its parent, otherwise throws the appropriate
     * LdapException.
     */
    public void moveAndRename( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN oriChildName = opContext.getDn();
        LdapDN parent = ((MoveAndRenameOperationContext)opContext).getParent();
        String newRn = ((MoveAndRenameOperationContext)opContext).getNewRdn();
        
        if ( oriChildName.getNormName().equalsIgnoreCase( subschemSubentryDn.getNormName() ) )
        {
            throw new LdapOperationNotSupportedException( 
                "Can not allow the move of the subschemaSubentry (" + 
                subschemSubentryDn + ") for the global schema: it is fixed at " + subschemSubentryDn,
                ResultCodeEnum.UNWILLING_TO_PERFORM );
        }
        
        // check if child to move exists
        String msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( nextInterceptor, msg, oriChildName );

        // check if parent to move to exists
        msg = "Attempt to move to non-existant parent: ";
        assertHasEntry( nextInterceptor, msg, parent );

        // check to see if target entry exists
        LdapDN target = ( LdapDN ) parent.clone();
        target.add( newRn );
        target.normalize( normalizerMap );
        
        if ( nextInterceptor.hasEntry( new EntryOperationContext( target ) ) )
        {
            // we must calculate the resolved name using the user provided Rdn value
            LdapDN upTarget = ( LdapDN ) parent.clone();
            upTarget.add( newRn );

            LdapNameAlreadyBoundException e;
            e = new LdapNameAlreadyBoundException( "target entry " + upTarget.getUpName() + " already exists!" );
            e.setResolvedName( new LdapDN( upTarget.getUpName() ) );
            throw e;
        }

        // Remove the original entry from the NotAlias cache, if needed
        synchronized( notAliasCache )
        {
        	if ( notAliasCache.containsKey( oriChildName.getNormName() ) )
        	{
        		notAliasCache.remove( oriChildName.getNormName() );
        	}
        }
        
        nextInterceptor.moveAndRename( opContext );
    }


    /**
     * Checks to see the entry being searched exists, otherwise throws the appropriate LdapException.
     */
    public NamingEnumeration<SearchResult> search( NextInterceptor nextInterceptor, OperationContext opContext ) throws NamingException
    {
        LdapDN base = opContext.getDn();

        try
        {
	        NamingEnumeration<SearchResult> result =  nextInterceptor.search( opContext );
	        
	        if ( result.hasMoreElements() == false )
	        {
	            if ( !base.isEmpty() && !( subschemSubentryDn.toNormName() ).equalsIgnoreCase( base.toNormName() ) )
	            {
	                // We just check that the entry exists only if we didn't found any entry
	                assertHasEntry( nextInterceptor, "Attempt to search under non-existant entry:" , base );
	            }
	        }

	        return result;
        }
        catch ( NamingException ne )
        {
            String msg = "Attempt to search under non-existant entry: ";
            assertHasEntry( nextInterceptor, msg, base );
            throw ne;
        }
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
        if ( subschemSubentryDn.getNormName().equals( dn.getNormName() ) )
        {
            return;
        }
        
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        
        if ( !nextInterceptor.hasEntry( new EntryOperationContext( dn ) ) )
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

            e.setResolvedName( 
                new LdapDN( 
                    proxy.getMatchedName( 
                        new GetMatchedNameOperationContext( dn ) ).getUpName() ) );
            throw e;
        }
    }
}
