/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.protocol.shared.kerberos;


import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Commonly used store utility operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class StoreUtils
{
    /** Loggers for this class */
    private static final Logger LOG = LoggerFactory.getLogger( StoreUtils.class );
    private static final Logger LOG_KRB = LoggerFactory.getLogger( Loggers.KERBEROS_LOG.getName() );


    private StoreUtils()
    {
    }


    /**
     * Constructs a filter expression tree for the filter used to search the 
     * directory.
     * 
     * @param schemaManager The server schemaManager to use for attribute lookups
     * @param principal the principal to use for building the filter
     * @return the filter expression tree
     * @throws Exception if there are problems while looking up attributes
     */
    private static ExprNode getFilter( SchemaManager schemaManager, String principal ) throws Exception
    {
        AttributeType type = schemaManager.lookupAttributeTypeRegistry( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT );
        Value value = new Value( type, principal );

        return new EqualityNode<String>( type, value );
    }


    /**
     * Finds the Entry associated with the Kerberos principal name.
     *
     * @param session the session to use for the search
     * @param searchBaseDn the base to use while searching
     * @param principal the name of the principal to search for
     * @return the server entry for the principal or null if non-existent
     * @throws Exception if there are problems while searching the directory
     */
    public static Entry findPrincipalEntry( CoreSession session, Dn searchBaseDn, String principal )
        throws Exception
    {
        Cursor<Entry> cursor = null;

        try
        {
            SchemaManager schemaManager = session.getDirectoryService().getSchemaManager();
            cursor = session
                .search( searchBaseDn, SearchScope.SUBTREE,
                    getFilter( schemaManager, principal ), AliasDerefMode.DEREF_ALWAYS,
                    SchemaConstants.ALL_USER_ATTRIBUTES );

            cursor.beforeFirst();

            if ( cursor.next() )
            {
                Entry entry = cursor.get();
                LOG.debug( "Found entry {} for kerberos principal name {}", entry.getDn(), principal );
                LOG_KRB.debug( "Found entry {} for kerberos principal name {}", entry.getDn(), principal );

                while ( cursor.next() )
                {
                    LOG.error( I18n.err( I18n.ERR_40000_MORE_THAN_ONE_ENTRY_KERBEROS_PRINCIPAL, principal, cursor.next() ) );
                }

                return entry;
            }
            else
            {
                LOG.warn( "No server entry found for kerberos principal name {}", principal );
                LOG_KRB.warn( "No server entry found for kerberos principal name {}", principal );

                return null;
            }
        }
        finally
        {
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }
}
