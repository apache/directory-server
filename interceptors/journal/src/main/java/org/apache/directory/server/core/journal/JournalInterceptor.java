/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.journal;


import java.util.concurrent.atomic.AtomicLong;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.NextInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.journal.Journal;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.ldif.ChangeType;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An interceptor which intercepts write operations to the directory and
 * logs them into a journal.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JournalInterceptor extends BaseInterceptor
{
    /** for debugging */
    private static final Logger LOG = LoggerFactory.getLogger( JournalInterceptor.class );

    /** A flag set to true if the journal interceptor is enabled */
    private boolean journalEnabled;

    /** A shared number stored within each change */
    private AtomicLong revision;

    /** the Journal service to log changes to */
    private Journal journal;


    // -----------------------------------------------------------------------
    // Overridden init() and destroy() methods
    // -----------------------------------------------------------------------
    /**
     * The init method will initialize the local variables and load the
     * entryDeleted AttributeType.
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        if ( directoryService.getJournal().isEnabled() )
        {
            journalEnabled = true;
            journal = directoryService.getJournal();
            revision = new AtomicLong( System.currentTimeMillis() );
        }

        LOG.debug( "JournalInterceptor has been initialized" );
    }


    /**
     * Log the operation, manage the logs rotations.
     */
    private void log( OperationContext opCtx, long revision, LdifEntry ldif ) throws LdapException
    {
        journal.log( getPrincipal( opCtx ), revision, ldif );
    }


    // -----------------------------------------------------------------------
    // Overridden (only change inducing) intercepted methods
    // -----------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        long opRevision = 0;

        if ( journalEnabled )
        {
            opRevision = revision.incrementAndGet();

            // Store the added entry
            Entry addEntry = addContext.getEntry();

            LdifEntry ldif = new LdifEntry();
            ldif.setChangeType( ChangeType.Add );
            ldif.setDn( addContext.getDn() );

            for ( Attribute attribute:addEntry.getAttributes() )
            {
                AttributeType attributeType = attribute.getAttributeType();
                ldif.addAttribute( addEntry.get( attributeType).clone() );
            }

            log( addContext, opRevision, ldif );
        }

        try
        {
            next( addContext );

            if ( journalEnabled )
            {
                // log the ACK
                journal.ack( opRevision );
            }
        }
        catch( LdapException le )
        {
            if ( journalEnabled )
            {
                // log the NACK
                journal.nack( opRevision );
            }

            throw le;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        long opRevision = 0;

        if ( journalEnabled )
        {
            opRevision = revision.incrementAndGet();

            // Store the deleted entry
            LdifEntry ldif = new LdifEntry();
            ldif.setChangeType( ChangeType.Delete );
            ldif.setDn( deleteContext.getDn() );

            journal.log( getPrincipal( deleteContext ), opRevision, ldif );
        }

        try
        {
            next( deleteContext );

            if ( journalEnabled )
            {
                // log the ACK
                journal.ack( opRevision );
            }
        }
        catch( LdapException e )
        {
            if ( journalEnabled )
            {
                // log the NACK
                journal.nack( opRevision );
            }

            throw e;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        long opRevision = 0;

        if ( journalEnabled )
        {
            opRevision = revision.incrementAndGet();

            // Store the modified entry
            LdifEntry ldif = new LdifEntry();
            ldif.setChangeType( ChangeType.Modify );
            ldif.setDn( modifyContext.getDn() );

            // Store the modifications
            for ( Modification modification:modifyContext.getModItems() )
            {
                ldif.addModification( modification );
            }

            journal.log( getPrincipal( modifyContext ), opRevision, ldif );
        }

        try
        {
            next( modifyContext );

            if ( journalEnabled )
            {
                // log the ACK
                journal.ack( opRevision );
            }
        }
        catch( LdapException e )
        {
            if ( journalEnabled )
            {
                // log the NACK
                journal.nack( opRevision );
            }
            throw e;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void rename ( RenameOperationContext renameContext ) throws LdapException
    {
        long opRevision = 0;

        if ( journalEnabled )
        {
            opRevision = revision.incrementAndGet();

            // Store the renamed entry
            LdifEntry ldif = new LdifEntry();
            ldif.setChangeType( ChangeType.ModRdn );
            ldif.setDn( renameContext.getDn() );
            ldif.setNewRdn( renameContext.getNewRdn().getNormName() );
            ldif.setDeleteOldRdn( renameContext.getDeleteOldRdn() );

            journal.log( getPrincipal( renameContext ), opRevision, ldif );
        }

        try
        {
            next( renameContext );

            if ( journalEnabled )
            {
                // log the ACK
                journal.ack( opRevision );
            }
        }
        catch( LdapException e )
        {
            if ( journalEnabled )
            {
                // log the NACK
                journal.nack( opRevision );
            }

            throw e;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext )
        throws LdapException
        {
        long opRevision = 0;

        if ( journalEnabled )
        {
            opRevision = revision.incrementAndGet();

            // Store the renamed entry
            LdifEntry ldif = new LdifEntry();
            ldif.setChangeType( ChangeType.ModDn );
            ldif.setDn( moveAndRenameContext.getDn() );
            ldif.setNewRdn( moveAndRenameContext.getNewRdn().getNormName() );
            ldif.setDeleteOldRdn( moveAndRenameContext.getDeleteOldRdn() );
            ldif.setNewSuperior( moveAndRenameContext.getNewDn().getNormName() );

            journal.log( getPrincipal( moveAndRenameContext ), opRevision, ldif );
        }

        try
        {
            next.moveAndRename( moveAndRenameContext );

            if ( journalEnabled )
            {
                // log the ACK
                journal.ack( opRevision );
            }
        }
        catch( LdapException e )
        {
            if ( journalEnabled )
            {
                // log the NACK
                journal.nack( opRevision );
            }

            throw e;
        }
        }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        long opRevision = 0;

        if ( journalEnabled )
        {
            opRevision = revision.incrementAndGet();

            // Store the moved entry
            LdifEntry ldif = new LdifEntry();
            ldif.setChangeType( ChangeType.ModDn );
            ldif.setDn( moveContext.getDn() );
            ldif.setNewSuperior( moveContext.getNewSuperior().getNormName() );

            journal.log( getPrincipal( moveContext ), opRevision, ldif );
        }

        try
        {
            next( moveContext );

            if ( journalEnabled )
            {
                // log the ACK
                journal.ack( opRevision );
            }
        }
        catch( LdapException e )
        {
            if ( journalEnabled )
            {
                // log the NACK
                journal.nack( opRevision );
            }

            throw e;
        }
    }
}