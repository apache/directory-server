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
package org.apache.directory.server.kerberos.kdc;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.shared.DefaultCoreSession;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswdErrorType;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswordException;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.protocol.shared.kerberos.GetPrincipal;
import org.apache.directory.server.protocol.shared.kerberos.StoreUtils;
import org.apache.directory.shared.kerberos.KerberosAttribute;


/**
 * A PrincipalStore backing entries in a DirectoryService.
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DirectoryPrincipalStore implements PrincipalStore
{
    /** The directory service backing store for this PrincipalStore. */
    private final DirectoryService directoryService;
    private final Dn searchBaseDn;

    private CoreSession adminSession;


    /**
     * Creates a new instance of DirectoryPrincipalStore.
     *
     * @param directoryService backing store for this PrincipalStore
     * @param searchBaseDn The Search Base DN
     */
    public DirectoryPrincipalStore( DirectoryService directoryService, Dn searchBaseDn )
    {
        this.directoryService = directoryService;
        this.adminSession = directoryService.getAdminSession();
        this.searchBaseDn = searchBaseDn;
    }


    /**
     * {@inheritDoc}
     */
    public void changePassword( KerberosPrincipal byPrincipal, KerberosPrincipal forPrincipal, String newPassword,
        boolean isInitialTicket ) throws ChangePasswordException
    {
        try
        {
            Entry ebyPrincipalEntry = null;

            ebyPrincipalEntry = StoreUtils.findPrincipalEntry( adminSession, searchBaseDn, byPrincipal.getName() );

            if ( ebyPrincipalEntry == null )
            {
                throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_HARDERROR,
                    ( "No such principal " + byPrincipal ).getBytes() );
            }

            SchemaManager schemaManager = directoryService.getSchemaManager();

            CoreSession bySession = null;

            boolean isAdmin = ebyPrincipalEntry.getDn()
                .equals( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );

            if ( !isInitialTicket && !isAdmin )
            {
                throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_INITIAL_FLAG_NEEDED );
            }

            // if admin assign the admin session
            if ( isAdmin )
            {
                bySession = adminSession;
            }
            // otherwise create a new session for the user with 'byPrincipal' who is trying to change the password for 'forPrincipal' 
            else
            {
                LdapPrincipal byLdapPrincipal = new LdapPrincipal( schemaManager, ebyPrincipalEntry.getDn(),
                    AuthenticationLevel.SIMPLE );

                bySession = new DefaultCoreSession( byLdapPrincipal, directoryService );
            }

            Attribute newPasswordAttribute = new DefaultAttribute(
                schemaManager.lookupAttributeTypeRegistry( SchemaConstants.USER_PASSWORD_AT ),
                Strings.getBytesUtf8( newPassword ) );
            Modification passwordMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                newPasswordAttribute );

            Attribute principalAttribute = new DefaultAttribute(
                schemaManager.lookupAttributeTypeRegistry( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ),
                forPrincipal.getName() );
            Modification principalMod = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                principalAttribute );

            Entry forPrincipalEntry = StoreUtils.findPrincipalEntry( bySession, searchBaseDn, forPrincipal.getName() );

            adminSession.modify( forPrincipalEntry.getDn(), passwordMod, principalMod );
        }
        catch ( LdapException e )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_ACCESSDENIED, e );
        }
        catch ( Exception e )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_HARDERROR, e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        return ( PrincipalStoreEntry ) new GetPrincipal( principal ).execute( adminSession, searchBaseDn );
    }
}
