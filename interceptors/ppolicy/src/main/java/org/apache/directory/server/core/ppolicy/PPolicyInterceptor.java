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

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
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
}
