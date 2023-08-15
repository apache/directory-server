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
package org.apache.directory.server.core.ppolicy;

import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_CHANGED_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_END_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_FAILURE_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_GRACE_USE_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_HISTORY_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_LAST_SUCCESS_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_POLICY_SUBENTRY_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_RESET_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_START_TIME_AT;

import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyRequest;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyResponse;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyResponseImpl;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link Interceptor} that manage the PasswordPlicies.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PPolicyInterceptor extends BaseInterceptor
{
    private static final Logger LOG = LoggerFactory.getLogger( PPolicyInterceptor.class );
    
    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    // pwdpolicy state attribute types
    private AttributeType pwdResetAT;

    private AttributeType pwdChangedTimeAT;

    private AttributeType pwdHistoryAT;

    private AttributeType pwdFailureTimeAT;

    private AttributeType pwdAccountLockedTimeAT;

    private AttributeType pwdLastSuccessAT;

    private AttributeType pwdGraceUseTimeAT;

    private AttributeType pwdPolicySubentryAT;

    private AttributeType pwdStartTimeAT;

    private AttributeType pwdEndTimeAT;

    /** a container to hold all the ppolicies */
//    private PpolicyConfigContainer pwdPolicyContainer;


    /**
     * Creates a new instance of DefaultAuthorizationInterceptor.
     */
    public PPolicyInterceptor()
    {
        super( InterceptorEnum.PASSWORD_POLICY_INTERCEPTOR );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );
        
        // we need to check if the passwordPolicy entry is defined in teh configuration
        
        // Load the PasswordPolicy AttributeTypes
        loadPwdPolicyStateAttributeTypes();
    }


    /**
     * Initialize the PasswordPolicy attributeTypes
     * 
     * @throws LdapException If the initialization failed
     */
    public void loadPwdPolicyStateAttributeTypes() throws LdapException
    {
        pwdResetAT = schemaManager.lookupAttributeTypeRegistry( PWD_RESET_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdResetAT );

        pwdChangedTimeAT = schemaManager.lookupAttributeTypeRegistry( PWD_CHANGED_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdChangedTimeAT );

        pwdHistoryAT = schemaManager.lookupAttributeTypeRegistry( PWD_HISTORY_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdHistoryAT );

        pwdFailureTimeAT = schemaManager.lookupAttributeTypeRegistry( PWD_FAILURE_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdFailureTimeAT );

        pwdAccountLockedTimeAT = schemaManager.lookupAttributeTypeRegistry( PWD_ACCOUNT_LOCKED_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdAccountLockedTimeAT );

        pwdLastSuccessAT = schemaManager.lookupAttributeTypeRegistry( PWD_LAST_SUCCESS_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdLastSuccessAT );

        pwdGraceUseTimeAT = schemaManager.lookupAttributeTypeRegistry( PWD_GRACE_USE_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdGraceUseTimeAT );

        pwdPolicySubentryAT = schemaManager.lookupAttributeTypeRegistry( PWD_POLICY_SUBENTRY_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdPolicySubentryAT );

        pwdStartTimeAT = schemaManager.lookupAttributeTypeRegistry( PWD_START_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdStartTimeAT );

        pwdEndTimeAT = schemaManager.lookupAttributeTypeRegistry( PWD_END_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdEndTimeAT );
    }


    /**
     * set all the password policies to be used by the server.
     * This includes a default(i.e applicable to all entries) and custom(a.k.a per user) password policies
     * 
     * @param policyContainer the container holding all the password policies
     */
//    public void setPwdPolicies( PpolicyConfigContainer policyContainer )
//    {
//        this.pwdPolicyContainer = policyContainer;
//    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", addContext );
        }
        
        // No need to do anythingh if the PasswordPoloicy is not activated, 
        // or if we are in a replication context
        if ( !directoryService.isPwdPolicyEnabled() || addContext.isReplEvent() )
        {
            next( addContext );
            return;
        }

        // First check if the entry is part of a PasswordPolicy subentry
        Entry entry = addContext.getEntry();

        PasswordPolicyConfiguration policyConfig = getPwdPolicy( entry, MessageTypeEnum.ADD_REQUEST );
        
        if ( policyConfig == null )
        {
            // Simply call the next interceptor and return, there is nothing to do.
            next( addContext );
            
            return;
        }

        // At this point, the user requesting the addition has not been authenticated
        // We just check what we do depending on the user requesting the addition, either an admin or a normal user
/*        if ( addContext.getSession().isAnAdministrator() )
        {
            // TODO
        }
        else
        {
            // TODO
        }
*/        
        // First check if the user's password reset is requested 
        checkPwdReset( addContext );
        
        // We have three use cases:
        // - The entry does not belong to an administrative area associated with a passwordPolicy in a subentry: nothing to do
        // - The entry belongs to an administrative area associated with a passwordPolicy in a subentry, but has no attribute 
        //     defined in the subentry pwdAttribute: nothing to do
        // - The entry belongs to an administrative area associated with a passwordPolicy in a subentry, and has an attribute 
        //     defined in the subentry pwdAttribute: add the requested operational attribute accordingly to the applied PasswordPolicy
        //
        // We can't discard an entry simply base don the fact that it does not have a userPassword attribute, it' snot enough.
        
        
        // propagate the call to the next interceptor
        next( addContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", bindContext );
        }
        
        // Operation check
        
        
        // propagate the call to the next interceptor
        next( bindContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", compareContext );
        }

        // propagate the call to the next interceptor
        return next( compareContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", deleteContext );
        }
        
        // Check if the delete operation applies to an entry with a password

        // propagate the call to the next interceptor
        next( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry getRootDse( GetRootDseOperationContext getRootDseContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", getRootDseContext );
        }

        // propagate the call to the next interceptor
        return next( getRootDseContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", hasEntryContext );
        }

        // propagate the call to the next interceptor
        return next( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", lookupContext );
        }

        // propagate the call to the next interceptor
        return next( lookupContext );
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", modifyContext );
        }

        // propagate the call to the next interceptor
        next( modifyContext );
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", moveContext );
        }

        // propagate the call to the next interceptor
        next( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", moveAndRenameContext );
        }

        // propagate the call to the next interceptor
        next( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", renameContext );
        }

        // propagate the call to the next interceptor
        next( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", searchContext );
        }

        // propagate the call to the next interceptor
        return next( searchContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", unbindContext );
        }

        // propagate the call to the next interceptor
        next( unbindContext );
    }


    /**
     * checks to see if the user's password should be changed before performing any operations
     * other than bind, password update, unbind, abandon or StartTLS
     *
     * @param opContext the operation's context
     * @throws LdapException
     */
    private void checkPwdReset( OperationContext opContext ) throws LdapException
    {
        if ( directoryService.isPwdPolicyEnabled() )
        {
            CoreSession session = opContext.getSession();

            if ( session.isPwdMustChange() )
            {
                boolean isPPolicyReqCtrlPresent = opContext.hasRequestControl( PasswordPolicyRequest.OID );

                if ( isPPolicyReqCtrlPresent )
                {
                    PasswordPolicyResponse responseControl = new PasswordPolicyResponseImpl();
                    responseControl.setPasswordPolicyError( PasswordPolicyErrorEnum.CHANGE_AFTER_RESET );
                    opContext.addResponseControl( responseControl );
                }

                throw new LdapNoPermissionException( "password needs to be reset before performing this operation" );
            }
        }
    }
    
    


    /**
     * Gets the effective password policy of the given entry.
     * If the entry has defined a custom password policy by setting "pwdPolicySubentry" attribute
     * then the password policy associated with the Dn specified at the above attribute's value will be returned.
     * Otherwise the default password policy will be returned (if present)
     * 
     * @param userEntry the user's entry
     * @param operation The Operation. If it's a Add, then we have to find the PPolicy using the subentries
     * @return the associated password policy
     * @throws LdapException If we weren't able to fetch the password policy
     */
    private PasswordPolicyConfiguration getPwdPolicy( Entry userEntry, MessageTypeEnum operation ) throws LdapException
    {
        switch ( operation )
        {
            case ADD_REQUEST:
                // Fetch the PPolicy from the subentries
//                if ( pwdPolicyContainer == null )
//                {
//                    return null;
//                }

//                return pwdPolicyContainer.getDefaultPolicy();
                
            case BIND_REQUEST:
            case COMPARE_REQUEST:
            case DEL_REQUEST:
            case EXTENDED_REQUEST:
            case MODIFY_REQUEST:
                // The entry must have a pwdPolicySubentry AT
                if ( userEntry.get( pwdPolicySubentryAT ) == null )
                {
                    return null;
                }

                break;
                
            case MODIFYDN_REQUEST:
            case SEARCH_REQUEST:
            case UNBIND_REQUEST:
            case ABANDON_REQUEST:
            default:
        }

/*
        if ( pwdPolicyContainer == null )
        {
            return null;
        }

        if ( userEntry == null )
        {
            return pwdPolicyContainer.getDefaultPolicy();
        }

        if ( pwdPolicyContainer.hasCustomConfigs() )
        {
            Attribute pwdPolicySubentry = userEntry.get( pwdPolicySubentryAT );

            if ( pwdPolicySubentry != null )
            {
                Dn configDn = dnFactory.create( pwdPolicySubentry.getString() );

                PasswordPolicyConfiguration custom = pwdPolicyContainer.getPolicyConfig( configDn );
                
                if ( custom != null )
                {
                    return custom;
                }
                else
                {
                    LOG.warn(
                        "The custom password policy for the user entry {} is not found, returning default policy configuration",
                        userEntry.getDn() );
                }
            }
        }

        return pwdPolicyContainer.getDefaultPolicy();
*/
        return null;
    }

}
