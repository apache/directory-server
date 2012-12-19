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


import java.util.ArrayList;
import java.util.Collection;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.event.Evaluator;
import org.apache.directory.server.core.api.event.ExpressionEvaluator;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.subtree.SubtreeEvaluator;
import org.apache.directory.server.core.api.subtree.RefinementEvaluator;
import org.apache.directory.server.core.api.subtree.RefinementLeafEvaluator;
import org.apache.directory.shared.ldap.aci.ACITuple;


/**
 * An implementation of Access Control Decision Function (18.8, X.501).
 * <br/>
 * This engine simply filters the collection of tuples using the following
 * {@link ACITupleFilter}s sequentially:
 * <ol>
 * <li>{@link RelatedUserClassFilter}</li>
 * <li>{@link RelatedProtectedItemFilter}</li>
 * <li>{@link MaxValueCountFilter}</li>
 * <li>{@link MaxImmSubFilter}</li>
 * <li>{@link RestrictedByFilter}</li>
 * <li>{@link MicroOperationFilter}</li>
 * <li>{@link HighestPrecedenceFilter}</li>
 * <li>{@link MostSpecificUserClassFilter}</li>
 * <li>{@link MostSpecificProtectedItemFilter}</li>
 * </ol>
 * <br/>
 * Operation is determined to be permitted if and only if there is at least one
 * tuple left and all of them grants the access. (18.8.4. X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ACDFEngine
{
    private final ACITupleFilter[] filters;


    /**
     * Creates a new instance.
     *
     * @param schemaManager The server schemaManager
     *
     * @throws LdapException if failed to initialize internal components
     */
    public ACDFEngine( SchemaManager schemaManager )
    {
        Evaluator entryEvaluator = new ExpressionEvaluator( schemaManager );
        SubtreeEvaluator subtreeEvaluator = new SubtreeEvaluator( schemaManager );
        RefinementEvaluator refinementEvaluator = new RefinementEvaluator( new RefinementLeafEvaluator( schemaManager ) );

        filters = new ACITupleFilter[]
            {
                new RelatedUserClassFilter( subtreeEvaluator ),
                new RelatedProtectedItemFilter( refinementEvaluator, entryEvaluator, schemaManager ),
                new MaxValueCountFilter(),
                new MaxImmSubFilter( schemaManager ),
                new RestrictedByFilter(),
                new MicroOperationFilter(),
                new HighestPrecedenceFilter(),
                new MostSpecificUserClassFilter(),
                new MostSpecificProtectedItemFilter() };
    }


    /**
     * Checks the user with the specified name can access the specified resource
     * (entry, attribute type, or attribute value) and throws {@link LdapNoPermissionException}
     * if the user doesn't have any permission to perform the specified grants.
     *
     * @param aciContext the container for ACI items
     * @throws LdapException if failed to evaluate ACI items
     */
    public void checkPermission( AciContext aciContext ) throws LdapException
    {
        if ( !hasPermission( aciContext ) )
        {
            throw new LdapNoPermissionException();
        }
    }


    /**
     * Returns <tt>true</tt> if the user with the specified name can access the specified resource
     * (entry, attribute type, or attribute value) and throws {@link org.apache.directory.api.ldap.model.exception.LdapNoPermissionException}
     * if the user doesn't have any permission to perform the specified grants.
     *
     * @param aciContext the container for ACI items
     * @throws org.apache.directory.api.ldap.model.exception.LdapException if failed to evaluate ACI items
     */
    public boolean hasPermission( AciContext aciContext ) throws LdapException
    {
        if ( aciContext.getEntryDn() == null )
        {
            throw new IllegalArgumentException( "entryName" );
        }

        CoreSession session = aciContext.getOperationContext().getSession();
        LookupOperationContext lookupContext = new LookupOperationContext( session, aciContext.getUserDn(),
            SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        Entry userEntry = session.getDirectoryService().getPartitionNexus().lookup( lookupContext );

        // Determine the scope of the requested operation.
        OperationScope scope;

        if ( aciContext.getAttributeType() == null )
        {
            scope = OperationScope.ENTRY;
        }
        else if ( aciContext.getAttrValue() == null )
        {
            scope = OperationScope.ATTRIBUTE_TYPE;
        }
        else
        {
            scope = OperationScope.ATTRIBUTE_TYPE_AND_VALUE;
        }

        // Clone aciTuples in case it is unmodifiable.
        aciContext.setAciTuples( new ArrayList<ACITuple>( aciContext.getAciTuples() ) );

        // Filter unrelated and invalid tuples
        for ( ACITupleFilter filter : filters )
        {
            if ( aciContext.getAciTuples().size() == 0 )
            {
                // No need to continue filtering
                return false;
            }

            Collection<ACITuple> aciTuples = filter.filter( aciContext, scope, userEntry );
            aciContext.setAciTuples( aciTuples );
        }

        // Deny access if no tuples left.
        if ( aciContext.getAciTuples().size() == 0 )
        {
            return false;
        }

        // Grant access if and only if one or more tuples remain and
        // all grant access. Otherwise deny access.
        for ( ACITuple tuple : aciContext.getAciTuples() )
        {
            if ( !tuple.isGrant() )
            {
                return false;
            }
        }

        return true;
    }
}
