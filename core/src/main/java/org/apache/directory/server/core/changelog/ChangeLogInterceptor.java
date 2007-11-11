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
package org.apache.directory.server.core.changelog;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.*;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.ldif.ChangeType;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.Rdn;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;


/**
 * An interceptor which intercepts write operations to the directory and
 * logs them with the server's ChangeLog service.
 */
public class ChangeLogInterceptor extends BaseInterceptor
{
    /** the changelog service to log changes to */
    private ChangeLog changeLog;


    // -----------------------------------------------------------------------
    // Overridden init() and destroy() methods
    // -----------------------------------------------------------------------


    public void init( DirectoryService directoryService ) throws NamingException
    {
        super.init( directoryService );
        changeLog = directoryService.getChangeLog();
    }


    // -----------------------------------------------------------------------
    // Overridden (only change inducing) intercepted methods
    // -----------------------------------------------------------------------

    public void add( NextInterceptor next, AddOperationContext opContext ) throws NamingException
    {
        next.add( opContext );

        if ( ! changeLog.isEnabled() )
        {
            return;
        }

        Entry forward = new Entry();
        forward.setChangeType( ChangeType.Add );
        forward.setDn( opContext.getDn().getUpName() );
        NamingEnumeration list = opContext.getEntry().getAll();
        while ( list.hasMore() )
        {
            forward.addAttribute( ( Attribute ) list.next() );
        }
        Entry reverse = LdifUtils.reverseAdd( opContext.getDn() );
        changeLog.log( getPrincipal(), forward, reverse );
    }


    /**
     * The delete operation has to be stored with a way to restore the deleted element.
     * There is no way to do that but reading the entry and dump it into the LOG.
     */
    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws NamingException
    {
        next.delete( opContext );

        if ( ! changeLog.isEnabled() )
        {
            return;
        }

        // @todo make sure we're not putting in operational attributes that cannot be user modified
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes attributes = proxy.lookup( new LookupOperationContext( opContext.getDn() ),
                PartitionNexusProxy.LOOKUP_BYPASS );

        Entry forward = new Entry();
        forward.setChangeType( ChangeType.Delete );
        forward.setDn( opContext.getDn().getUpName() );
        Entry reverse = LdifUtils.reverseDel( opContext.getDn(), attributes );
        changeLog.log( getPrincipal(), forward, reverse );
    }


    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws NamingException
    {
        next.modify( opContext );

        if ( ! changeLog.isEnabled() )
        {
            return;
        }

        // @todo make sure we're not putting in operational attributes that cannot be user modified
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes attributes = proxy.lookup( new LookupOperationContext( opContext.getDn() ),
                PartitionNexusProxy.LOOKUP_BYPASS );

        Entry forward = new Entry();
        forward.setChangeType( ChangeType.Modify );
        forward.setDn( opContext.getDn().getUpName() );
        for ( ModificationItemImpl modItem : opContext.getModItems() )
        {
            forward.addModificationItem( modItem );
        }

        Entry reverse = LdifUtils.reverseModify( opContext.getDn(), opContext.getModItems(), attributes );
        changeLog.log( getPrincipal(), forward, reverse );
    }


    // -----------------------------------------------------------------------
    // Though part left as an exercise (Not Any More!)
    // -----------------------------------------------------------------------


    public void rename ( NextInterceptor next, RenameOperationContext renameContext ) throws NamingException
    {
        next.rename( renameContext );

        if ( ! changeLog.isEnabled() )
        {
            return;
        }

        Entry forward = new Entry();
        forward.setChangeType( ChangeType.ModRdn );
        forward.setDn( renameContext.getDn().getUpName() );
        forward.setDeleteOldRdn( renameContext.getDelOldDn() );

        Entry reverse = LdifUtils.reverseModifyDN( null, renameContext.getDn(), new Rdn( renameContext.getNewRdn() ),
                renameContext.getDelOldDn() );
        changeLog.log( getPrincipal(), forward, reverse );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opCtx )
        throws NamingException
    {
        next.moveAndRename( opCtx );

        if ( ! changeLog.isEnabled() )
        {
            return;
        }

        Entry forward = new Entry();
        forward.setChangeType( ChangeType.ModDn );
        forward.setDn( opCtx.getDn().getUpName() );
        forward.setDeleteOldRdn( opCtx.getDelOldDn() );
        forward.setNewRdn( opCtx.getNewRdn() );
        forward.setNewSuperior( opCtx.getParent().getUpName() );

        Entry reverse = LdifUtils.reverseModifyDN( null, opCtx.getDn(), new Rdn( opCtx.getNewRdn() ),
                opCtx.getDelOldDn() );
        changeLog.log( getPrincipal(), forward, reverse );
    }


    public void move ( NextInterceptor next, MoveOperationContext opCtx ) throws NamingException
    {
        next.move( opCtx );

        if ( ! changeLog.isEnabled() )
        {
            return;
        }

        Entry forward = new Entry();
        forward.setChangeType( ChangeType.ModDn );
        forward.setDn( opCtx.getDn().getUpName() );
        forward.setNewSuperior( opCtx.getParent().getUpName() );

        Entry reverse = LdifUtils.reverseModifyDN( null, opCtx.getDn(), null, false );
        changeLog.log( getPrincipal(), forward, reverse );
    }
}