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
package org.apache.directory.server.core.authn;


import static org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum.ACCOUNT_LOCKED;
import static org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum.PASSWORD_EXPIRED;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_CHANGED_TIME_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_END_TIME_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_GRACE_USE_TIME_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_LAST_SUCCESS_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_START_TIME_AT;

import java.util.Date;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.authn.ppolicy.PasswordPolicyException;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all Authenticators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractAuthenticator implements Authenticator
{
    /** A logger for the extending classes */
    protected static final Logger LOG = LoggerFactory.getLogger( AbstractAuthenticator.class );

    /** The associated DirectoryService */
    private DirectoryService directoryService;
    
    /** authenticator type */
    private final AuthenticationLevel authenticatorType;
    
    /**
     * Creates a new instance.
     *
     * @param type the type of this authenticator (e.g. <tt>'simple'</tt>, <tt>'none'</tt>...)
     */
    protected AbstractAuthenticator( AuthenticationLevel type )
    {
        this.authenticatorType = type;
    }


    /**
     * Returns {@link DirectoryService} for this authenticator.
     * @return the directory service core
     */
    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }
    

    /**
     * {@inheritDoc}
     */
    public AuthenticationLevel getAuthenticatorType()
    {
        return authenticatorType;
    }


    /**
     * Initializes (<tt>directoryService</tt> and and calls {@link #doInit()} method.
     * Please put your initialization code into {@link #doInit()}.
     * @param directoryService the directory core for this authenticator
     * @throws LdapException if there is a problem starting up the authenticator
     */
    public final void init( DirectoryService directoryService ) throws LdapException
    {
        this.directoryService = directoryService;
        doInit();
    }


    /**
     * Implement your initialization code here.
     */
    protected void doInit()
    {
    }


    /**
     * Calls {@link #doDestroy()} method, and clears default properties
     * (<tt>factoryConfiguration</tt> and <tt>configuration</tt>).
     * Please put your deinitialization code into {@link #doDestroy()}. 
     */
    public final void destroy()
    {
        try
        {
            doDestroy();
        }
        finally
        {
            this.directoryService = null;
        }
    }


    /**
     * Implement your deinitialization code here.
     */
    protected void doDestroy()
    {
    }


    /**
     * Does nothing leaving it so subclasses can override.
     */
    public void invalidateCache( Dn bindDn )
    {
    }


    /**
     * {@inheritDoc}
     */
    public void checkPwdPolicy( Entry userEntry ) throws LdapException
    {
        if( !directoryService.isPwdPolicyEnabled() )
        {
            return;
        }

        AuthenticationInterceptor authenticationInterceptor = (AuthenticationInterceptor)directoryService.getInterceptor( AuthenticationInterceptor.class.getName() );
        PasswordPolicyConfiguration pPolicyConfig = authenticationInterceptor.getPwdPolicy( userEntry );
        
        // check for locked out account
        if( pPolicyConfig.isPwdLockout() )
        {
            LOG.debug( "checking if account with the Dn {} is locked", userEntry.getDn() );
            
            Attribute accountLockAttr = userEntry.get( PWD_ACCOUNT_LOCKED_TIME_AT );
            if( accountLockAttr != null )
            {
                String lockedTime = accountLockAttr.getString();
                if( lockedTime.equals( "000001010000Z" ) )
                {
                    throw new PasswordPolicyException( "account was permanently locked", ACCOUNT_LOCKED.getValue() );
                }
                else
                {
                    Date lockedDate = DateUtils.getDate( lockedTime );
                    long time = pPolicyConfig.getPwdLockoutDuration() * 1000;
                    time += lockedDate.getTime();
                    
                    Date unlockedDate = new Date( time );
                    if( lockedDate.before( unlockedDate ) )
                    {
                        throw new PasswordPolicyException( "account will remain locked till " + unlockedDate, ACCOUNT_LOCKED.getValue() );
                    }
                    else
                    {
                        // remove pwdAccountLockedTime attribute
                        Modification pwdAccountLockMod = new DefaultModification( ModificationOperation.REMOVE_ATTRIBUTE,  accountLockAttr );
                        
                        // DO NOT bypass the interceptor chain, otherwise the changes can't be replicated
                        directoryService.getAdminSession().modify( userEntry.getDn(), pwdAccountLockMod );
                    }
                }
            }
        }
        
        Attribute pwdStartTimeAttr = userEntry.get( PWD_START_TIME_AT );
        if( pwdStartTimeAttr != null )
        {
            Date pwdStartTime = DateUtils.getDate( pwdStartTimeAttr.getString() );
            
            if( System.currentTimeMillis() < pwdStartTime.getTime() )
            {
                throw new PasswordPolicyException( "account is locked, will be activated after " + pwdStartTime, ACCOUNT_LOCKED.getValue() ); 
            }
        }
        
        Attribute pwdEndTimeAttr = userEntry.get( PWD_END_TIME_AT );
        if( pwdEndTimeAttr != null )
        {
            Date pwdEndTime = DateUtils.getDate( pwdEndTimeAttr.getString() );
            
            if( System.currentTimeMillis() >= pwdEndTime.getTime() )
            {
                throw new PasswordPolicyException( "password end time reached, will be locked till administrator activates it", ACCOUNT_LOCKED.getValue() );
            }
        }
        
        if( pPolicyConfig.getPwdMaxIdle() > 0 )
        {
            Attribute pwdLastSuccessTimeAttr = userEntry.get( PWD_LAST_SUCCESS_AT );
            long time = pPolicyConfig.getPwdMaxIdle() * 1000;
            time += DateUtils.getDate( pwdLastSuccessTimeAttr.getString() ).getTime();
            
            if( System.currentTimeMillis() >= time )
            {
                throw new PasswordPolicyException( "account locked due to the max idle time of the password was exceeded", ACCOUNT_LOCKED.getValue() );
            }
        }
        
        if ( pPolicyConfig.getPwdMaxAge() > 0 )
        {
            if( pPolicyConfig.getPwdGraceAuthNLimit() > 0 )
            {
                Attribute pwdGraceUseAttr = userEntry.get( PWD_GRACE_USE_TIME_AT );

                // check for grace authentication count
                if( pwdGraceUseAttr != null )
                {
                    if( pwdGraceUseAttr.size() >= pPolicyConfig.getPwdGraceAuthNLimit() )
                    {
                        throw new PasswordPolicyException( "paasword expired and max grace logins were used", PASSWORD_EXPIRED.getValue() );
                    }
                }
            }
            else
            {
                Attribute pwdChangeTimeAttr = userEntry.get( PWD_CHANGED_TIME_AT );
                boolean expired = PasswordUtil.isPwdExpired( pwdChangeTimeAttr.getString(), pPolicyConfig.getPwdMaxAge() );
                
                if( expired )
                {
                    throw new PasswordPolicyException( "paasword expired", PASSWORD_EXPIRED.getValue() );
                }
            }
        }
    }
}
