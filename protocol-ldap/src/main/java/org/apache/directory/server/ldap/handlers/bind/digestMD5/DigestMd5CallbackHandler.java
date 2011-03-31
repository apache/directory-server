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
package org.apache.directory.server.ldap.handlers.bind.digestMD5;


import java.util.HashSet;
import java.util.Set;

import javax.naming.Context;
import javax.security.sasl.AuthorizeCallback;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.bind.AbstractSaslCallbackHandler;
import org.apache.directory.server.ldap.handlers.bind.SaslConstants;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.FilterParser;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.BindRequest;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DigestMd5CallbackHandler extends AbstractSaslCallbackHandler
{
    private static final Logger LOG = LoggerFactory.getLogger( DigestMd5CallbackHandler.class );

    private String bindDn;
    
    /** A SchemaManager instance */
    private SchemaManager schemaManager;



    /**
     * Creates a new instance of DigestMd5CallbackHandler.
     *
     * @param ldapSession the mina IoSession
     * @param adminSession the admin session
     * @param bindRequest the bind message
     */
    public DigestMd5CallbackHandler( LdapSession ldapSession, CoreSession adminSession, BindRequest bindRequest )
    {
        super( adminSession.getDirectoryService(), bindRequest );
        this.ldapSession = ldapSession;
        this.adminSession = adminSession;
        schemaManager = adminSession.getDirectoryService().getSchemaManager();
    }


    // TODO - should return not be a byte[]
    protected Attribute lookupPassword( String username, String realm )
    {
        try
        {
            ExprNode filter = FilterParser.parse( schemaManager, "(uid=" + username + ")" );
            Set<AttributeTypeOptions> returningAttributes = new HashSet<AttributeTypeOptions>();
            
            AttributeType passwordAT = adminSession.getDirectoryService().getSchemaManager().lookupAttributeTypeRegistry( SchemaConstants.USER_PASSWORD_AT );
            returningAttributes.add( new AttributeTypeOptions( passwordAT) );
            bindDn = (String)ldapSession.getSaslProperty( SaslConstants.SASL_USER_BASE_DN );
            
            Dn baseDn = new Dn( bindDn );

            EntryFilteringCursor cursor = adminSession.search( 
                baseDn, 
                SearchScope.SUBTREE,
                filter, 
                AliasDerefMode.DEREF_ALWAYS,
                returningAttributes );
            
            cursor.beforeFirst();
            
            ClonedServerEntry entry = null;
            
            while ( cursor.next() )
            {
                entry = cursor.get();
                LdapPrincipal ldapPrincipal = new LdapPrincipal( 
                    schemaManager, 
                    entry.getDn(), 
                    AuthenticationLevel.STRONG,
                    entry.get( SchemaConstants.USER_PASSWORD_AT ).getBytes() );
                ldapSession.putSaslProperty( SaslConstants.SASL_AUTHENT_USER, ldapPrincipal );
            }

            return entry.get( passwordAT );
        }
        catch ( Exception e )
        {
            return null;
        }
    }


    protected void authorize( AuthorizeCallback authorizeCB )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "Converted username " + getUsername() + " to Dn " + bindDn );
        }

        ldapSession.putSaslProperty( Context.SECURITY_PRINCIPAL, bindDn );

        authorizeCB.setAuthorizedID( bindDn );
        authorizeCB.setAuthorized( true );
    }
}
