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
package org.apache.directory.server.core.memberof;


import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An interceptor for adding virtual memberOf attributes to {code}Entry{code}s.
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MemberOfInterceptor extends BaseInterceptor
{
    /** A aggregating logger */
    private static final Logger OPERATION_STATS = LoggerFactory.getLogger( Loggers.OPERATION_STAT.getName() );

    /** An operation logger */
    private static final Logger OPERATION_TIME = LoggerFactory.getLogger( Loggers.OPERATION_TIME.getName() );

    /**
     * 
     * Creates a new instance of MemberOfInterceptor.
     *
     * @param name This interceptor's getName()
     */
    public MemberOfInterceptor( String name )
    {
        super( name );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        Entry entry = next( lookupContext );

        CoreSession adminSession = directoryService.getAdminSession();
        Value dnValue = new Value( directoryService.getAtProvider().getMember(), entry.getDn().getNormName() );
        PartitionNexus nexus = directoryService.getPartitionNexus();

        // either
        ExprNode filter = new PresenceNode( directoryService.getAtProvider().getAdministrativeRole() );
        // or
//        ExprNode filter = new PresenceNode( dnValue );

        SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, Dn.ROOT_DSE, SearchScope.SUBTREE, filter, "1.1" );
        Partition partition = nexus.getPartition( Dn.ROOT_DSE );
        searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );
        searchOperationContext.setPartition( partition );

        try ( PartitionTxn partitionTxn = partition.beginReadTransaction() )
        {
            searchOperationContext.setTransaction( partitionTxn );
            EntryFilteringCursor results = nexus.search( searchOperationContext );

            try
            {
                String memberOf = "memberOf";
                while ( results.next() )
                {
                    Entry memberEntry = results.get();

                    entry = entry.add( memberOf, memberEntry.getDn().getName() );
                    // Note: not sure f I should:
                    // if has memberOf
                    //   add to memberOf
                    // else
                    //  create memberOf with value of
                }

                results.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage(), e );
            }
        }
        catch ( Exception e )
        {
            throw new LdapOtherException( e.getMessage(), e );
        }

        return entry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        EntryFilteringCursor cursor = next( searchContext );

        return cursor;
    }
}
