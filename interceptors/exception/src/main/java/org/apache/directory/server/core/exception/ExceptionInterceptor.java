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


import org.apache.commons.collections4.map.LRUMap;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAliasException;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;


/**
 * An {@link Interceptor} that detects any operations that breaks integrity
 * of {@link Partition} and terminates the current invocation chain by
 * throwing a {@link Exception}. Those operations include when an entry
 * already exists at a Dn and is added once again to the same Dn.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExceptionInterceptor extends BaseInterceptor
{
    private PartitionNexus nexus;
    private Dn subschemSubentryDn;

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
    private final LRUMap notAliasCache = new LRUMap( DEFAULT_CACHE_SIZE );

    /** Declare a default for this cache. 100 entries seems to be enough */
    private static final int DEFAULT_CACHE_SIZE = 100;


    /**
     * Creates an interceptor that is also the exception handling service.
     */
    public ExceptionInterceptor()
    {
        super( InterceptorEnum.EXCEPTION_INTERCEPTOR );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );
        nexus = directoryService.getPartitionNexus();
        Value attr = nexus.getRootDseValue( directoryService.getAtProvider().getSubschemaSubentry() );
        subschemSubentryDn = dnFactory.create( attr.getString() );
    }


    /**
     * In the pre-invocation state this interceptor method checks to see if the entry to be added already exists.  If it
     * does an exception is raised.
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        Dn name = addContext.getDn();

        if ( subschemSubentryDn.equals( name ) )
        {
            throw new LdapEntryAlreadyExistsException( I18n.err( I18n.ERR_249 ) );
        }

        Dn suffix = nexus.getSuffixDn( name );

        // we're adding the suffix entry so just ignore stuff to mess with the parent
        if ( suffix.equals( name ) )
        {
            next( addContext );
            return;
        }

        Dn parentDn = name.getParent();

        // check if we're trying to add to a parent that is an alias
        boolean notAnAlias;

        synchronized ( notAliasCache )
        {
            notAnAlias = notAliasCache.containsKey( parentDn.getNormName() );
        }

        if ( !notAnAlias )
        {
            // We don't know if the parent is an alias or not, so we will launch a
            // lookup, and update the cache if it's not an alias
            Entry attrs;

            try
            {
                CoreSession session = addContext.getSession();
                LookupOperationContext lookupContext = new LookupOperationContext( session, parentDn,
                    SchemaConstants.ALL_ATTRIBUTES_ARRAY );
                lookupContext.setPartition( addContext.getPartition() );
                lookupContext.setTransaction( addContext.getTransaction() );

                attrs = directoryService.getPartitionNexus().lookup( lookupContext );
            }
            catch ( Exception e )
            {
                throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_251_PARENT_NOT_FOUND, parentDn.getName() ) );
            }

            Attribute objectClass = ( ( ClonedServerEntry ) attrs ).getOriginalEntry().get(
                directoryService.getAtProvider().getObjectClass() );

            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                String msg = I18n.err( I18n.ERR_252_ALIAS_WITH_CHILD_NOT_ALLOWED, name.getName(), parentDn.getName() );
                throw new LdapAliasException( msg );
            }
            else
            {
                synchronized ( notAliasCache )
                {
                    notAliasCache.put( parentDn.getNormName(), parentDn );
                }
            }
        }

        next( addContext );
    }


    /**
     * Checks to make sure the entry being deleted exists, and has no children, otherwise throws the appropriate
     * LdapException.
     */
    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();

        if ( dn.equals( subschemSubentryDn ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, I18n.err( I18n.ERR_253,
                subschemSubentryDn ) );
        }

        next( deleteContext );

        // Update the alias cache
        synchronized ( notAliasCache )
        {
            if ( notAliasCache.containsKey( dn.getNormName() ) )
            {
                notAliasCache.remove( dn.getNormName() );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        // check if entry to modify exists
        String msg = "Attempt to modify non-existant entry: ";

        // handle operations against the schema subentry in the schema service
        // and never try to look it up in the nexus below
        if ( modifyContext.getDn().equals( subschemSubentryDn ) )
        {
            next( modifyContext );
            return;
        }

        // Check that the entry we read at the beginning exists. If
        // not, we will throw an exception here
        assertHasEntry( modifyContext, msg );

        // Let's assume that the new modified entry may be an alias,
        // but we don't want to check that now...
        // We will simply remove the Dn from the NotAlias cache.
        // It would be smarter to check the modified attributes, but
        // it would also be more complex.
        synchronized ( notAliasCache )
        {
            if ( notAliasCache.containsKey( modifyContext.getDn().getNormName() ) )
            {
                notAliasCache.remove( modifyContext.getDn().getNormName() );
            }
        }

        next( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        Dn oriChildName = moveContext.getDn();

        if ( oriChildName.equals( subschemSubentryDn ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, I18n.err( I18n.ERR_258,
                subschemSubentryDn, subschemSubentryDn ) );
        }

        next( moveContext );

        // Remove the original entry from the NotAlias cache, if needed
        synchronized ( notAliasCache )
        {
            if ( notAliasCache.containsKey( oriChildName.getNormName() ) )
            {
                notAliasCache.remove( oriChildName.getNormName() );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Dn oldDn = moveAndRenameContext.getDn();

        // Don't allow M&R in the SSSE
        if ( oldDn.getNormName().equals( subschemSubentryDn.getNormName() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, I18n.err( I18n.ERR_258,
                subschemSubentryDn, subschemSubentryDn ) );
        }

        // Remove the original entry from the NotAlias cache, if needed
        synchronized ( notAliasCache )
        {
            if ( notAliasCache.containsKey( oldDn.getNormName() ) )
            {
                notAliasCache.remove( oldDn.getNormName() );
            }
        }

        next( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Dn dn = renameContext.getDn();

        if ( dn.equals( subschemSubentryDn ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, I18n.err( I18n.ERR_255,
                subschemSubentryDn, subschemSubentryDn ) );
        }

        // check to see if target entry exists
        Dn newDn = renameContext.getNewDn();

        HasEntryOperationContext hasEntryContext = new HasEntryOperationContext( renameContext.getSession(), newDn );
        hasEntryContext.setPartition( renameContext.getPartition() );
        hasEntryContext.setTransaction( renameContext.getTransaction() );

        if ( nexus.hasEntry( hasEntryContext ) )
        {
            // Ok, the target entry already exists.
            // If the target entry has the same name than the modified entry, it's a rename on itself,
            // we want to allow this.
            if ( !newDn.equals( dn ) )
            {
                throw new LdapEntryAlreadyExistsException( I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newDn.getName() ) );
            }
        }

        // Remove the previous entry from the notAnAlias cache
        synchronized ( notAliasCache )
        {
            if ( notAliasCache.containsKey( dn.getNormName() ) )
            {
                notAliasCache.remove( dn.getNormName() );
            }
        }

        next( renameContext );
    }


    /**
     * Asserts that an entry is present and as a side effect if it is not, creates a LdapNoSuchObjectException, which is
     * used to set the before exception on the invocation - eventually the exception is thrown.
     *
     * @param msg        the message to prefix to the distinguished name for explanation
     * @throws Exception if the entry does not exist
     * @param nextInterceptor the next interceptor in the chain
     */
    private void assertHasEntry( OperationContext opContext, String msg ) throws LdapException
    {
        Dn dn = opContext.getDn();

        if ( subschemSubentryDn.equals( dn ) )
        {
            return;
        }

        if ( opContext.getEntry() == null )
        {
            LdapNoSuchObjectException e;

            if ( msg != null )
            {
                e = new LdapNoSuchObjectException( msg + dn.getName() );
            }
            else
            {
                e = new LdapNoSuchObjectException( dn.getName() );
            }

            throw e;
        }
    }

    /**
     * Asserts that an entry is present and as a side effect if it is not, creates a LdapNoSuchObjectException, which is
     * used to set the before exception on the invocation - eventually the exception is thrown.
     *
     * @param msg        the message to prefix to the distinguished name for explanation
     * @param dn         the distinguished name of the entry that is asserted
     * @throws Exception if the entry does not exist
     * @param nextInterceptor the next interceptor in the chain
     *
    private void assertHasEntry( OperationContext opContext, String msg, Dn dn ) throws LdapException
    {
        if ( subschemSubentryDn.equals( dn ) )
        {
            return;
        }

        if ( !opContext.hasEntry( dn, ByPassConstants.HAS_ENTRY_BYPASS ) )
        {
            LdapNoSuchObjectException e;

            if ( msg != null )
            {
                e = new LdapNoSuchObjectException( msg + dn.getName() );
            }
            else
            {
                e = new LdapNoSuchObjectException( dn.getName() );
            }

            throw e;
        }
    }*/
}
