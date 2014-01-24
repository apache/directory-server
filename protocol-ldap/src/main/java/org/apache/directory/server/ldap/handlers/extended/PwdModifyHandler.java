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
package org.apache.directory.server.ldap.handlers.extended;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicy;
import org.apache.directory.api.ldap.extras.extended.PwdModifyRequest;
import org.apache.directory.api.ldap.extras.extended.PwdModifyResponse;
import org.apache.directory.api.ldap.extras.extended.PwdModifyResponseImpl;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An handler to manage PwdModifyRequest. Users can send a pwdModify request
 * for their own passwords, or for another people password. Only admin can
 * change someone else password without having to provide the original password.
 * Here rae the different use cases : <br/>
 * <ul>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PwdModifyHandler implements ExtendedOperationHandler<PwdModifyRequest, PwdModifyResponse>
{
    private static final Logger LOG = LoggerFactory.getLogger( PwdModifyHandler.class );
    public static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> set = new HashSet<String>( 2 );
        set.add( PwdModifyRequest.EXTENSION_OID );
        set.add( PwdModifyResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    /**
     * {@inheritDoc}
     */
    public String getOid()
    {
        return PwdModifyRequest.EXTENSION_OID;
    }


    /**
     * Modify the user's credentials.
     */
    private void modifyUserPassword( LdapSession requestor, Dn userDn, byte[] oldPassword, byte[] newPassword,
        PwdModifyRequest req )
    {
        DirectoryService service = requestor.getLdapServer().getDirectoryService();
        CoreSession adminSession = service.getAdminSession();

        // First, check that the user exists
        try
        {
            HasEntryOperationContext hasEntryContext = new HasEntryOperationContext( adminSession );
            hasEntryContext.setDn( userDn );

            if ( !service.getOperationManager().hasEntry( hasEntryContext ) )
            {
                LOG.error( "Cannot find an entry for DN " + userDn );
                // We can't find the entry in the DIT
                requestor.getIoSession().write( new PwdModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.NO_SUCH_OBJECT, "Cannot find an entry for DN " + userDn ) );

                return;
            }
        }
        catch ( LdapException le )
        {
            LOG.error( "Cannot find an entry for DN " + userDn + ", exception : " + le.getMessage() );
            // We can't find the entry in the DIT
            requestor.getIoSession().write(
                new PwdModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.NO_SUCH_OBJECT, "Cannot find an entry for DN " + userDn
                        + ", exception : " + le.getMessage() ) );

            return;
        }

        // We can try to update the userPassword now
        ModifyOperationContext modifyContext = new ModifyOperationContext( adminSession );
        modifyContext.setDn( userDn );

        Control ppolicyControl = req.getControl( PasswordPolicy.OID );
        if( ppolicyControl != null )
        {
            modifyContext.addRequestControl( ppolicyControl );
        }

        List<Modification> modifications = new ArrayList<Modification>();
        Modification modification = null;

        if ( oldPassword != null )
        {
            modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
                SchemaConstants.USER_PASSWORD_AT, oldPassword );

            modifications.add( modification );
        }

        if ( newPassword != null )
        {
            if ( oldPassword == null )
            {
                modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                    SchemaConstants.USER_PASSWORD_AT, newPassword );
            }
            else
            {
                modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
                    SchemaConstants.USER_PASSWORD_AT, newPassword );
            }

            modifications.add( modification );
        }
        else
        {
            // In this case, we could either generate a new password, or return an error
            // Atm, we will return an unwillingToPerform error
            LOG.error( "Cannot create a new password for user " + userDn + ", exception : " + userDn );

            // We can't modify the password
            requestor.getIoSession().write( new PwdModifyResponseImpl(
                req.getMessageId(), ResultCodeEnum.UNWILLING_TO_PERFORM, "Cannot generate a new password for user "
                    + userDn ) );

            return;
        }

        modifyContext.setModItems( modifications );

        ResultCodeEnum errorCode = null;
        String errorMessage = null;

        try
        {
            service.getOperationManager().modify( modifyContext );

            LOG.debug( "Password modified for user " + userDn );

            // Ok, all done
            PwdModifyResponseImpl pmrl = new PwdModifyResponseImpl(
                req.getMessageId(), ResultCodeEnum.SUCCESS );

            ppolicyControl = modifyContext.getResponseControl( PasswordPolicy.OID );

            if( ppolicyControl != null )
            {
                pmrl.addControl( ppolicyControl );
            }

            requestor.getIoSession().write( pmrl );

            return;
        }
        catch ( LdapOperationException loe )
        {
            errorCode = loe.getResultCode();
            errorMessage = loe.getMessage();
        }
        catch ( LdapException le )
        {
            // this exception means something else must be wrong
            errorCode = ResultCodeEnum.OTHER;
            errorMessage = le.getMessage();
        }

        // We can't modify the password
        LOG.error( "Cannot modify the password for user " + userDn + ", exception : " + errorMessage );
        PwdModifyResponseImpl errorPmrl = new PwdModifyResponseImpl(
            req.getMessageId(), errorCode, "Cannot modify the password for user "
                + userDn + ", exception : " + errorMessage );

        ppolicyControl = modifyContext.getResponseControl( PasswordPolicy.OID );

        if( ppolicyControl != null )
        {
            errorPmrl.addControl( ppolicyControl );
        }

        requestor.getIoSession().write( errorPmrl );
    }


    /**
     * Modify his password
     */
    private void modifyOwnPassword( LdapSession requestor, Dn principalDn, byte[] oldPassword, byte[] newPassword,
        PwdModifyRequest req )
    {
        DirectoryService service = requestor.getLdapServer().getDirectoryService();

        // Try to update the userPassword
        ModifyOperationContext modifyContext = new ModifyOperationContext( requestor.getCoreSession() );
        modifyContext.setDn( principalDn );

        Control ppolicyControl = req.getControl( PasswordPolicy.OID );
        if( ppolicyControl != null )
        {
            modifyContext.addRequestControl( ppolicyControl );
        }

        List<Modification> modifications = new ArrayList<Modification>();
        Modification modification = null;

        if ( oldPassword != null )
        {
            modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
                SchemaConstants.USER_PASSWORD_AT, oldPassword );

            modifications.add( modification );
        }
        else
        {
            modification = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,
                SchemaConstants.USER_PASSWORD_AT );

            modifications.add( modification );
        }

        if ( newPassword != null )
        {
            modification = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
                SchemaConstants.USER_PASSWORD_AT, newPassword );

            modifications.add( modification );
        }

        modifyContext.setModItems( modifications );

        ResultCodeEnum errorCode = null;
        String errorMessage = null;

        try
        {
            service.getOperationManager().modify( modifyContext );

            LOG.debug( "Password modified for user " + principalDn );

            // Ok, all done
            PwdModifyResponseImpl pmrl = new PwdModifyResponseImpl(
                req.getMessageId(), ResultCodeEnum.SUCCESS );

            ppolicyControl = modifyContext.getResponseControl( PasswordPolicy.OID );

            if( ppolicyControl != null )
            {
                pmrl.addControl( ppolicyControl );
            }

            requestor.getIoSession().write( pmrl );
            return;
        }
        catch ( LdapOperationException loe )
        {
            errorCode = loe.getResultCode();
            errorMessage = loe.getMessage();
        }
        catch ( LdapException le )
        {
            // this exception means something else must be wrong
            errorCode = ResultCodeEnum.OTHER;
            errorMessage = le.getMessage();
        }

        // We can't modify the password
        LOG.error( "Cannot modify the password for user " + principalDn + ", exception : " + errorMessage );

        PwdModifyResponseImpl errorPmrl = new PwdModifyResponseImpl( req.getMessageId(), errorCode,
            "Cannot modify the password for user "
                + principalDn + ", exception : " + errorMessage );

        ppolicyControl = modifyContext.getResponseControl( PasswordPolicy.OID );

        if( ppolicyControl != null )
        {
            errorPmrl.addControl( ppolicyControl );
        }

        requestor.getIoSession().write( errorPmrl );
    }


    /**
     * {@inheritDoc}
     */
    public void handleExtendedOperation( LdapSession requestor, PwdModifyRequest req ) throws Exception
    {
        LOG.debug( "Password modification requested" );

        // Grab the adminSession, we might need it later
        DirectoryService service = requestor.getLdapServer().getDirectoryService();
        CoreSession adminSession = service.getAdminSession();
        String userIdentity = Strings.utf8ToString( req.getUserIdentity() );
        Dn userDn = null;

        if ( !Strings.isEmpty( userIdentity ) )
        {
            try
            {
                userDn = service.getDnFactory().create( userIdentity );
            }
            catch ( LdapInvalidDnException lide )
            {
                LOG.error( "The user DN is invalid : " + userDn );
                // The userIdentity is not a DN : return with an error code.
                requestor.getIoSession().write( new PwdModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.INVALID_DN_SYNTAX, "The user DN is invalid : " + userDn ) );
                return;
            }
        }

        byte[] oldPassword = req.getOldPassword();
        byte[] newPassword = req.getNewPassword();

        // First check if the user is bound or not
        if ( requestor.isAuthenticated() )
        {
            Dn principalDn = requestor.getCoreSession().getEffectivePrincipal().getDn();

            LOG.debug( "User {} trying to modify password of user {}", principalDn, userDn );

            // First, check that the userDn is null : we can't change the password of someone else
            // except if we are admin
            if ( ( userDn != null ) && ( !userDn.equals( principalDn ) ) )
            {
                // Are we admin ?
                if ( !requestor.getCoreSession().isAdministrator() )
                {
                    // No : error
                    LOG.error( "Non-admin user cannot access another user's password to modify it" );
                    requestor.getIoSession().write( new PwdModifyResponseImpl(
                        req.getMessageId(), ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS,
                        "Non-admin user cannot access another user's password to modify it" ) );
                }
                else
                {
                    // We are administrator, we can try to modify the user's credentials
                    modifyUserPassword( requestor, userDn, oldPassword, newPassword, req );
                }
            }
            else
            {
                // We are trying to modify our own password
                modifyOwnPassword( requestor, principalDn, oldPassword, newPassword, req );
            }
        }
        else
        {
            // The user is not authenticated : we have to use the provided userIdentity
            // and the oldPassword to check if the user is present
            BindOperationContext bindContext = new BindOperationContext( adminSession );
            bindContext.setDn( userDn );
            bindContext.setCredentials( oldPassword );

            try
            {
                service.getOperationManager().bind( bindContext );
            }
            catch ( LdapException le )
            {
                // We can't bind with the provided information : we thus can't
                // change the password...
                requestor.getIoSession().write( new PwdModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.INVALID_CREDENTIALS ) );

                return;
            }

            // Ok, we were able to bind using the userIdentity and the password. Let's
            // modify the password now
            ModifyOperationContext modifyContext = new ModifyOperationContext( 
                service.getSession( userDn, oldPassword ) );
            modifyContext.setDn( userDn );
            List<Modification> modifications = new ArrayList<Modification>();
            Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                SchemaConstants.USER_PASSWORD_AT, newPassword );
            modifications.add( modification );
            modifyContext.setModItems( modifications );

            try
            {
                service.getOperationManager().modify( modifyContext );

                // Ok, all done
                requestor.getIoSession().write( new PwdModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.SUCCESS ) );
            }
            catch ( LdapException le )
            {
                // We can't modify the password
                requestor.getIoSession().write(
                    new PwdModifyResponseImpl(
                        req.getMessageId(), ResultCodeEnum.UNWILLING_TO_PERFORM,
                        "Cannot modify the password, exception : " + le.getMessage() ) );

                return;
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public static PwdModifyResponse getPwdModifyResponse()
    {
        // build the PwdModifyResponse message with replicationContexts
        return new PwdModifyResponseImpl();
    }


    /**
     * {@inheritDoc}
     */
    public Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    /**
     * {@inheritDoc}
     */
    public void setLdapServer( LdapServer ldapServer )
    {
    }
}
