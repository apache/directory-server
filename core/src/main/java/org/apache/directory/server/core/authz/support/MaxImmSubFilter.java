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
package org.apache.directory.server.core.authz.support;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.admin.AdministrativePointInterceptor;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.protectedItem.MaxImmSubItem;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationException;
import org.apache.directory.shared.ldap.model.exception.LdapOtherException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;



/**
 * An {@link ACITupleFilter} that discards all tuples that doesn't satisfy
 * {@link org.apache.directory.shared.ldap.aci.protectedItem.MaxImmSubItem} constraint if available. (18.8.3.3, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MaxImmSubFilter implements ACITupleFilter
{
    private final ExprNode childrenFilter;
    private final SearchControls childrenSearchControls;


    public MaxImmSubFilter( SchemaManager schemaManager )
    {
        AttributeType objectClassAt = null;

        try
        {
            objectClassAt = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );
        }
        catch ( LdapException le )
        {
            // Do nothing
        }

        childrenFilter = new PresenceNode( objectClassAt );
        childrenSearchControls = new SearchControls();
        childrenSearchControls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
    }


    public Collection<ACITuple> filter( AciContext aciContext, OperationScope scope, Entry userEntry )
        throws LdapException
    {
        ACI_LOG.debug( "Filtering MaxImmSub..." );

        if ( aciContext.getEntryDn().isRootDSE() )
        {
            return aciContext.getAciTuples();
        }

        if ( aciContext.getAciTuples().size() == 0 )
        {
            return aciContext.getAciTuples();
        }

        if ( scope != OperationScope.ENTRY )
        {
            return aciContext.getAciTuples();
        }

        int immSubCount = -1;

        for ( Iterator<ACITuple> i = aciContext.getAciTuples().iterator(); i.hasNext(); )
        {
            ACITuple tuple = i.next();

            if ( !tuple.isGrant() )
            {
                continue;
            }

            for ( ProtectedItem item : tuple.getProtectedItems() )
            {
                if ( item instanceof MaxImmSubItem )
                {
                    if ( immSubCount < 0 )
                    {
                        immSubCount = getImmSubCount( aciContext.getOperationContext(), aciContext.getEntryDn() );
                    }

                    MaxImmSubItem mis = ( MaxImmSubItem ) item;

                    if ( immSubCount >= mis.getValue() )
                    {
                        i.remove();
                        break;
                    }
                }
            }
        }

        return aciContext.getAciTuples();
    }

    public static final Collection<String> SEARCH_BYPASS;
    static
    {
        Collection<String> c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( AdministrativePointInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        SEARCH_BYPASS = Collections.unmodifiableCollection( c );
    }


    private int getImmSubCount( OperationContext opContext, Dn entryName ) throws LdapException
    {
        int cnt = 0;
        EntryFilteringCursor results = null;

        try
        {
            Dn baseDn = new Dn( opContext.getSession().getDirectoryService().getSchemaManager(), entryName.getRdn( entryName.size() - 1 ) );
            SearchOperationContext searchContext = new SearchOperationContext( opContext.getSession(),
                baseDn, childrenFilter, childrenSearchControls );
            searchContext.setByPassed( SEARCH_BYPASS );
            searchContext.setAliasDerefMode( AliasDerefMode.DEREF_ALWAYS );

            results = opContext.getSession().getDirectoryService().getOperationManager().search( searchContext );

            try
            {
                while ( results.next() )
                {
                    results.get();
                    cnt++;
                }
            }
            catch ( Exception e )
            {
                throw new LdapOtherException( e.getMessage() );
            }
        }
        finally
        {
            if ( results != null )
            {
                try
                {
                    results.close();
                }
                catch ( Exception e )
                {
                    throw new LdapOperationException( e.getMessage() );
                }
            }
        }

        return cnt;
    }
}
