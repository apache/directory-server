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


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyRequest;
import org.apache.directory.api.ldap.extras.extended.pwdModify.PasswordModifyRequest;
import org.apache.directory.api.ldap.extras.extended.pwdModify.PasswordModifyResponse;
import org.apache.directory.api.ldap.extras.extended.pwdModify.PasswordModifyResponseImpl;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.shared.DefaultCoreSession;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An handler to manage PwdModifyRequest. Users can send a pwdModify request
 * for their own passwords, or for another people password. Only admin can
 * change someone else password without having to provide the original password.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PwdModifyHandler implements ExtendedOperationHandler<PasswordModifyRequest, PasswordModifyResponse>
{
    private static final Logger LOG = LoggerFactory.getLogger( PwdModifyHandler.class );
    public static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> set = new HashSet<>( 2 );
        set.add( PasswordModifyRequest.EXTENSION_OID );
        set.add( PasswordModifyResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    /**
     * {@inheritDoc}
     */
    public String getOid()
    {
        return PasswordModifyRequest.EXTENSION_OID;
    }


    /**
     * Modify the user's credentials.
     * 
     * We will need to modify the userPassword attribute, accordingly to a few rules:
     * - if the old password is present, we should verify it's valid. if not, we return an error
     * - if the old password is absent, we are modifying the password of the current used.
     * - if the new password is absent, we will return an error. The RFC says that we could
     * generate a random password, but that would be dangerous to do so.
     * - if the new password already exists, we simply return not changing anything 
     * - otherwise, we just remove the old password from the list of passwords (we may have 
     * more than one) and add the new password. This is done with a REPLACE operation (Modify)
     */
    private void modifyUserPassword( CoreSession userSession, Entry userEntry, Dn userDn, 
        byte[] oldPassword, byte[] newPassword, PasswordModifyRequest req )
    {
        IoSession ioSession = ( ( DefaultCoreSession ) userSession ).getIoSession();

        if ( newPassword == null )
        {
            // We don't support password generation on ApacheDS
            writeResult( ioSession, req, ResultCodeEnum.UNWILLING_TO_PERFORM, 
                "Cannot change a password for user " + userDn + ", exception : null new password" );

            return;
        }
        
        // Get the user password attribute
        Attribute userPassword = userEntry.get( SchemaConstants.USER_PASSWORD_AT );
        
        if ( userPassword == null )
        {
            // We can't modify the password
            writeResult( ioSession, req, ResultCodeEnum.UNWILLING_TO_PERFORM, 
                "Cannot change a password for user " + userDn + ", the user has no existing password" );

            return;
        }
        
        if ( userPassword.contains( newPassword ) )
        {
           // Ok, we are done : just return success
            PasswordModifyResponseImpl pmrl = new PasswordModifyResponseImpl(
                req.getMessageId(), ResultCodeEnum.SUCCESS );

            Control ppolicyControl = req.getControl( PasswordPolicyRequest.OID );

            if ( ppolicyControl != null )
            {
                pmrl.addControl( ppolicyControl );
            }

            ioSession.write( pmrl );

            return;
        }
        
        if ( oldPassword == null )
        {
            // We are modifying the password on behalf of another user. ACI will
            // protect such modification if it's not allowed. In any case, we just 
            // modify the existing userPassword attribute, adding the password
            ModifyRequest modifyRequest = new ModifyRequestImpl();
            modifyRequest.setName( userDn );

            Control ppolicyControl = req.getControl( PasswordPolicyRequest.OID );
            
            if ( ppolicyControl != null )
            {
                modifyRequest.addControl( ppolicyControl );
            }
            
            try
            {
                Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                    userPassword.getAttributeType(), newPassword );
    
                modifyRequest.addModification( modification );
                ResultCodeEnum errorCode = null;
                String errorMessage = null;

                try
                {
                    userSession.modify( modifyRequest );

                    if ( LOG.isDebugEnabled() )
                    {
                        LOG.debug( "Password modified for user {}", userDn );
                    }

                    // Ok, all done
                    PasswordModifyResponseImpl pmrl = new PasswordModifyResponseImpl(
                        req.getMessageId(), ResultCodeEnum.SUCCESS );

                    ppolicyControl = modifyRequest.getResultResponse().getControl( PasswordPolicyRequest.OID );

                    if ( ppolicyControl != null )
                    {
                        pmrl.addControl( ppolicyControl );
                    }

                    ioSession.write( pmrl );

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
                LOG.error( "Cannot modify the password for user {}, exception : {}", userDn, errorMessage );
                PasswordModifyResponseImpl errorPmrl = new PasswordModifyResponseImpl(
                    req.getMessageId(), errorCode, "Cannot modify the password for user "
                        + userDn + ", exception : " + errorMessage );

                ppolicyControl = modifyRequest.getResultResponse().getControl( PasswordPolicyRequest.OID );

                if ( ppolicyControl != null )
                {
                    errorPmrl.addControl( ppolicyControl );
                }

                ioSession.write( errorPmrl );
                
                return;
            }
            catch ( LdapInvalidAttributeValueException liave )
            {
                // Nothing to do, this will never be a problem
            }
        }
        else
        {
            // We are changing the password of the current user, check the password
            boolean valid = false;
            Attribute modifiedPassword = new DefaultAttribute( userPassword.getAttributeType() );
            
            for ( Value value : userPassword )
            {
                if ( !valid )
                {
                    valid = PasswordUtil.compareCredentials( oldPassword, value.getBytes() ) ;
                }
                
                try
                {
                    if ( valid )
                    {
                        modifiedPassword.add( newPassword );
                    }
                    else
                    { 
                        modifiedPassword.add( value );
                    }
                }
                catch ( LdapInvalidAttributeValueException e )
                {
                    // Nothing to do, this will never be a problem
                }
            }
            
            // At this point, we have what is needed to modify the password, if the oldPassword
            // was valid
            if ( valid )
            {
                ModifyRequest modifyRequest = new ModifyRequestImpl();
                modifyRequest.setName( userDn );

                Control ppolicyControl = req.getControl( PasswordPolicyRequest.OID );
                
                if ( ppolicyControl != null )
                {
                    modifyRequest.addControl( ppolicyControl );
                }
                
                Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                    modifiedPassword );

                modifyRequest.addModification( modification );

                ResultCodeEnum errorCode = null;
                String errorMessage = null;

                try
                {
                    userSession.modify( modifyRequest );

                    if ( LOG.isDebugEnabled() )
                    {
                        LOG.debug( "Password modified for user {}", userDn );
                    }

                    // Ok, all done
                    PasswordModifyResponseImpl pmrl = new PasswordModifyResponseImpl(
                        req.getMessageId(), ResultCodeEnum.SUCCESS );

                    ppolicyControl = modifyRequest.getResultResponse().getControl( PasswordPolicyRequest.OID );

                    if ( ppolicyControl != null )
                    {
                        pmrl.addControl( ppolicyControl );
                    }

                    ioSession.write( pmrl );

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
                LOG.error( "Cannot modify the password for user {}, exception : {}", userDn, errorMessage );
                PasswordModifyResponseImpl errorPmrl = new PasswordModifyResponseImpl(
                    req.getMessageId(), errorCode, "Cannot modify the password for user "
                        + userDn + ", exception : " + errorMessage );

                ppolicyControl = modifyRequest.getResultResponse().getControl( PasswordPolicyRequest.OID );

                if ( ppolicyControl != null )
                {
                    errorPmrl.addControl( ppolicyControl );
                }

                ioSession.write( errorPmrl );
                
                return;
            }
            else
            {
                // Too bad, the old password is invalid
                writeResult( ioSession, req, ResultCodeEnum.INVALID_CREDENTIALS, 
                    "Cannot change a password for user " + userDn + ", invalid credentials" );

                return;
            }
        }
    }

    
    private void writeResult( LdapSession requestor, PasswordModifyRequest req, ResultCodeEnum error, String errorMessage )
    {
        writeResult( requestor.getIoSession(), req, error, errorMessage );

    }

    
    private void writeResult( IoSession ioSession, PasswordModifyRequest req, ResultCodeEnum error, String errorMessage )
    {
        LOG.error( errorMessage );
        ioSession.write( new PasswordModifyResponseImpl(
            req.getMessageId(), error, errorMessage ) );

    }
    
    
    private Entry getModifiedEntry( LdapSession requestor, PasswordModifyRequest req, Dn entryDn )
    {
        try
        {
            Entry modifiedEntry = requestor.getCoreSession().lookup( entryDn, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            
            if ( modifiedEntry == null )
            {
                // The entry does not exist, we can't modify its password
                writeResult( requestor, req, ResultCodeEnum.NO_SUCH_OBJECT, 
                    "The entry does not exist, we can't modify its password" );
                return null;
            }
            else
            {
                return modifiedEntry;
            }
        }
        catch ( Exception le )
        {
            // The entry does not exist, we can't modify its password
            writeResult( requestor, req, ResultCodeEnum.NO_SUCH_OBJECT, 
                "The entry does not exist, we can't modify its password" );
            return null;
        }
    }
    
    
    private void processAuthenticatedPasswordModify( LdapSession requestor, PasswordModifyRequest req,
        Dn userDn )
    {
        byte[] oldPassword = req.getOldPassword();
        byte[] newPassword = req.getNewPassword();

        // We are already bound. Fetch the entry which we want to modify
        Entry modifiedEntry = null;
        
        Dn principalDn = requestor.getCoreSession().getEffectivePrincipal().getDn();

        LOG.debug( "User {} trying to modify password of user {}", principalDn, userDn );
        
        
        // First, check that the userDn is null : we can't change the password of someone else
        // except if we are admin
        if ( ( userDn != null ) && ( !userDn.equals( principalDn ) ) )
        {
            // Are we admin ?
            if ( requestor.getCoreSession().isAdministrator() )
            {
                modifiedEntry = getModifiedEntry( requestor, req, userDn );
                
                if ( modifiedEntry == null )
                {
                    return;
                }
                
                // We are administrator, we can try to modify the user's credentials
                modifyUserPassword( requestor.getCoreSession(), modifiedEntry, userDn, oldPassword, newPassword, req );
            }
            else
            {
                // No : error
                writeResult( requestor, req, ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS, 
                    "Non-admin user cannot access another user's password to modify it" );
            }
        }
        else
        {
            // We are trying to modify our own password
            modifiedEntry = getModifiedEntry( requestor, req, principalDn );

            if ( modifiedEntry == null )
            {
                return;
            }

            modifyUserPassword( requestor.getCoreSession(), modifiedEntry, principalDn, oldPassword, newPassword, req );
        }
    }
    

    /**
     * {@inheritDoc}
     */
    public void handleExtendedOperation( LdapSession requestor, PasswordModifyRequest req ) throws Exception
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
                // The userIdentity is not a DN : return with an error code.
                writeResult( requestor, req, ResultCodeEnum.INVALID_DN_SYNTAX, 
                    "The user DN is invalid : " + userDn );

                return;
            }
        }

        byte[] oldPassword = req.getOldPassword();
        byte[] newPassword = req.getNewPassword();

        // First check if the user is bound or not
        if ( requestor.isAuthenticated() )
        {
            processAuthenticatedPasswordModify( requestor, req, userDn );
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
                requestor.getIoSession().write( new PasswordModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.INVALID_CREDENTIALS ) );

                return;
            }

            // Ok, we were able to bind using the userIdentity and the password. Let's
            // modify the password now
            modifyUserPassword( requestor.getCoreSession(), bindContext.getEntry(), userDn, oldPassword, newPassword, req );
        }
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
