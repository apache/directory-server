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


import static org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum.INSUFFICIENT_PASSWORD_QUALITY;
import static org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum.PASSWORD_TOO_SHORT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_CHANGED_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_FAILURE_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_GRACE_USE_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_HISTORY_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_LAST_SUCCESS_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_POLICY_SUBENTRY_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_RESET_AT;
import static org.apache.directory.api.ldap.model.entry.ModificationOperation.ADD_ATTRIBUTE;
import static org.apache.directory.api.ldap.model.entry.ModificationOperation.REMOVE_ATTRIBUTE;
import static org.apache.directory.api.ldap.model.entry.ModificationOperation.REPLACE_ATTRIBUTE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicy;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum;
import org.apache.directory.api.ldap.extras.controls.ppolicy_impl.PasswordPolicyDecorator;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.BinaryValue;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.api.util.StringConstants;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.authn.ppolicy.CheckQualityEnum;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyException;
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
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.apache.directory.server.core.shared.DefaultCoreSession;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} that authenticates users.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticationInterceptor extends BaseInterceptor
{
    private static final Logger LOG = LoggerFactory.getLogger( AuthenticationInterceptor.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** A Set of all the existing Authenticator to be used by the bind operation */
    private Set<Authenticator> authenticators = new HashSet<Authenticator>();

    /** A map of authenticators associated with the authentication level required */
    private final Map<AuthenticationLevel, Collection<Authenticator>> authenticatorsMapByType = new HashMap<AuthenticationLevel, Collection<Authenticator>>();

    private CoreSession adminSession;

    private Set<String> pwdResetSet = new HashSet<String>();

    // pwdpolicy state attribute types
    private AttributeType AT_PWD_RESET;

    private AttributeType AT_PWD_CHANGED_TIME;

    private AttributeType AT_PWD_HISTORY;

    private AttributeType AT_PWD_FAILURE_TIME;

    private AttributeType AT_PWD_ACCOUNT_LOCKED_TIME;

    private AttributeType AT_PWD_LAST_SUCCESS;

    private AttributeType AT_PWD_GRACE_USE_TIME;

    /** a container to hold all the ppolicies */
    private PpolicyConfigContainer pwdPolicyContainer;

    /** the pwdPolicySubentry AT */
    private AttributeType pwdPolicySubentryAT;


    /**
     * Creates an authentication service interceptor.
     */
    public AuthenticationInterceptor()
    {
        super( InterceptorEnum.AUTHENTICATION_INTERCEPTOR );
    }


    /**
     * Registers and initializes all {@link Authenticator}s to this service.
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        adminSession = directoryService.getAdminSession();
        pwdPolicySubentryAT = schemaManager
            .lookupAttributeTypeRegistry( PasswordPolicySchemaConstants.PWD_POLICY_SUBENTRY_AT );

        if ( ( authenticators == null ) || ( authenticators.size() == 0 ) )
        {
            setDefaultAuthenticators();
        }

        // Register all authenticators
        for ( Authenticator authenticator : authenticators )
        {
            register( authenticator, directoryService );
        }

        loadPwdPolicyStateAttributeTypes();
    }


    /**
     * Initialize the set of authenticators with some default values
     */
    private void setDefaultAuthenticators()
    {
        if ( authenticators == null )
        {
            authenticators = new HashSet<Authenticator>();
        }

        authenticators.clear();
        authenticators.add( new AnonymousAuthenticator() );
        authenticators.add( new SimpleAuthenticator() );
        authenticators.add( new StrongAuthenticator() );
    }


    public Set<Authenticator> getAuthenticators()
    {
        return authenticators;
    }


    /**
     * @param authenticators authenticators to be used by this AuthenticationInterceptor
     */
    public void setAuthenticators( Set<Authenticator> authenticators )
    {
        if ( authenticators == null )
        {
            this.authenticators.clear();
        }
        else
        {
            this.authenticators = authenticators;
        }
    }


    /**
     * @param authenticators authenticators to be used by this AuthenticationInterceptor
     */
    public void setAuthenticators( Authenticator[] authenticators )
    {
        if ( authenticators == null )
        {
            throw new IllegalArgumentException( "The given authenticators set is null" );
        }

        this.authenticators.clear();
        this.authenticatorsMapByType.clear();

        for ( Authenticator authenticator : authenticators )
        {
            try
            {
                register( authenticator, directoryService );
            }
            catch ( LdapException le )
            {
                LOG.error( "Cannot register authenticator {}", authenticator );
            }
        }
    }


    /**
     * Deinitializes and deregisters all {@link Authenticator}s from this service.
     */
    public void destroy()
    {
        authenticatorsMapByType.clear();
        Set<Authenticator> copy = new HashSet<Authenticator>( authenticators );
        authenticators = new HashSet<Authenticator>();

        for ( Authenticator authenticator : copy )
        {
            authenticator.destroy();
        }
    }


    /**
     * Initializes the specified {@link Authenticator} and registers it to
     * this service.
     *
     * @param authenticator Authenticator to initialize and register by type
     * @param directoryService configuration info to supply to the Authenticator during initialization
     * @throws javax.naming.Exception if initialization fails.
     */
    private void register( Authenticator authenticator, DirectoryService directoryService ) throws LdapException
    {
        authenticator.init( directoryService );

        Collection<Authenticator> authenticatorList = getAuthenticators( authenticator.getAuthenticatorType() );

        if ( authenticatorList == null )
        {
            authenticatorList = new ArrayList<Authenticator>();
            authenticatorsMapByType.put( authenticator.getAuthenticatorType(), authenticatorList );
            authenticators.add( authenticator );
        }

        if ( !authenticatorList.contains( authenticator ) )
        {
            authenticatorList.add( authenticator );
        }
    }


    /**
     * Returns the list of {@link Authenticator}s with the specified type.
     *
     * @param type type of Authenticator sought
     * @return A list of Authenticators of the requested type or <tt>null</tt> if no authenticator is found.
     */
    private Collection<Authenticator> getAuthenticators( AuthenticationLevel type )
    {
        Collection<Authenticator> result = authenticatorsMapByType.get( type );

        if ( ( result != null ) && ( result.size() > 0 ) )
        {
            return result;
        }
        else
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", addContext );
        }

        checkAuthenticated( addContext );

        Entry entry = addContext.getEntry();

        if ( !directoryService.isPwdPolicyEnabled() || addContext.isReplEvent() )
        {
            next( addContext );
            return;
        }

        PasswordPolicyConfiguration policyConfig = getPwdPolicy( entry );

        boolean isPPolicyReqCtrlPresent = addContext.hasRequestControl( PasswordPolicy.OID );

        checkPwdReset( addContext );

        // Get the password depending on the configuration
        String passwordAttribute = SchemaConstants.USER_PASSWORD_AT;

        if ( isPPolicyReqCtrlPresent )
        {
            passwordAttribute = policyConfig.getPwdAttribute();
        }

        Attribute userPasswordAttribute = entry.get( passwordAttribute );

        if ( userPasswordAttribute != null )
        {
            String username = null;

            BinaryValue userPassword = ( BinaryValue ) userPasswordAttribute.get();

            try
            {
                username = entry.getDn().getRdn().getValue().getString();
                check( username, userPassword.getValue(), policyConfig );
            }
            catch ( PasswordPolicyException e )
            {
                if ( isPPolicyReqCtrlPresent )
                {
                    PasswordPolicyDecorator responseControl =
                        new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                    responseControl.getResponse().setPasswordPolicyError(
                        PasswordPolicyErrorEnum.get( e.getErrorCode() ) );
                    addContext.addResponseControl( responseControl );
                }

                // throw exception if userPassword quality checks fail
                throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION, e.getMessage(), e );
            }

            String pwdChangedTime = DateUtils.getGeneralizedTime();

            if ( ( policyConfig.getPwdMinAge() > 0 ) || ( policyConfig.getPwdMaxAge() > 0 ) )
            {
                Attribute pwdChangedTimeAt = new DefaultAttribute( AT_PWD_CHANGED_TIME );
                pwdChangedTimeAt.add( pwdChangedTime );
                entry.add( pwdChangedTimeAt );
            }

            if ( policyConfig.isPwdMustChange() && addContext.getSession().isAnAdministrator() )
            {
                Attribute pwdResetAt = new DefaultAttribute( AT_PWD_RESET );
                pwdResetAt.add( "TRUE" );
                entry.add( pwdResetAt );
            }

            if ( policyConfig.getPwdInHistory() > 0 )
            {
                Attribute pwdHistoryAt = new DefaultAttribute( AT_PWD_HISTORY );
                byte[] pwdHistoryVal = new PasswordHistory( pwdChangedTime, userPassword.getValue() ).getHistoryValue();
                pwdHistoryAt.add( pwdHistoryVal );
                entry.add( pwdHistoryAt );
            }
        }

        next( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", bindContext );
        }

        if ( ( bindContext.getSession() != null ) &&
            ( bindContext.getSession().getEffectivePrincipal() != null ) &&
            ( !bindContext.getSession().isAnonymous() ) &&
            ( !bindContext.getSession().isAdministrator() ) )
        {
            // null out the credentials
            bindContext.setCredentials( null );
        }

        // pick the first matching authenticator type
        AuthenticationLevel level = bindContext.getAuthenticationLevel();

        if ( level == AuthenticationLevel.UNAUTHENT )
        {
            // This is a case where the Bind request contains a Dn, but no password.
            // We don't check the Dn, we just return a UnwillingToPerform error
            // Cf RFC 4513, chap. 5.1.2
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, "Cannot Bind for Dn "
                + bindContext.getDn().getName() );
        }

        Collection<Authenticator> authenticators = getAuthenticators( level );
        PasswordPolicyException ppe = null;
        boolean isPPolicyReqCtrlPresent = bindContext.hasRequestControl( PasswordPolicy.OID );
        PasswordPolicyDecorator pwdRespCtrl =
            new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
        boolean authenticated = false;

        if ( authenticators == null )
        {
            LOG.warn( "Cannot find any authenticator for level {} : {}", level );
        }
        else
        {
            // TODO : we should refactor that.
            // try each authenticator
            for ( Authenticator authenticator : authenticators )
            {
                try
                {
                    // perform the authentication
                    LdapPrincipal principal = authenticator.authenticate( bindContext );

                    LdapPrincipal clonedPrincipal = ( LdapPrincipal ) ( principal.clone() );

                    // remove creds so there is no security risk
                    bindContext.setCredentials( null );
                    clonedPrincipal.setUserPassword( StringConstants.EMPTY_BYTES );

                    // authentication was successful
                    CoreSession session = new DefaultCoreSession( clonedPrincipal, directoryService );
                    bindContext.setSession( session );

                    authenticated = true;

                    // break out of the loop if the authentication succeeded
                    break;
                }
                catch ( PasswordPolicyException e )
                {
                    ppe = e;
                    break;
                }
                catch ( LdapAuthenticationException e )
                {
                    // authentication failed, try the next authenticator
                    LOG.info( "Authenticator {} failed to authenticate: {}", authenticator, bindContext );
                }
                catch ( Exception e )
                {
                    // Log other exceptions than LdapAuthenticationException
                    LOG.info( "Unexpected failure for Authenticator {} : {}", authenticator, bindContext );
                }
            }
        }

        if ( ppe != null )
        {
            if ( isPPolicyReqCtrlPresent )
            {
                pwdRespCtrl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.get( ppe.getErrorCode() ) );
                bindContext.addResponseControl( pwdRespCtrl );
            }

            throw ppe;
        }

        Dn dn = bindContext.getDn();
        Entry userEntry = bindContext.getEntry();

        PasswordPolicyConfiguration policyConfig = getPwdPolicy( userEntry );

        // load the user entry again if ppolicy is enabled, cause the authenticator might have modified the entry
        if ( policyConfig != null )
        {
            LookupOperationContext lookupContext = new LookupOperationContext( adminSession, bindContext.getDn(),
                SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            userEntry = directoryService.getPartitionNexus().lookup( lookupContext );
        }

        // check if the user entry is null, it will be null
        // in cases of anonymous bind
        if ( authenticated && ( userEntry == null ) && directoryService.isAllowAnonymousAccess() )
        {
            return;
        }

        if ( !authenticated )
        {
            if ( LOG.isInfoEnabled() )
            {
                LOG.info( "Cannot bind to the server " );
            }

            if ( ( policyConfig != null ) && ( userEntry != null ) )
            {
                Attribute pwdFailTimeAt = userEntry.get( AT_PWD_FAILURE_TIME );

                if ( pwdFailTimeAt == null )
                {
                    pwdFailTimeAt = new DefaultAttribute( AT_PWD_FAILURE_TIME );
                }
                else
                {
                    purgeFailureTimes( policyConfig, pwdFailTimeAt );
                }

                String failureTime = DateUtils.getGeneralizedTime();
                pwdFailTimeAt.add( failureTime );
                Modification pwdFailTimeMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdFailTimeAt );

                List<Modification> mods = new ArrayList<Modification>();
                mods.add( pwdFailTimeMod );

                int numFailures = pwdFailTimeAt.size();

                if ( policyConfig.isPwdLockout() && ( numFailures >= policyConfig.getPwdMaxFailure() ) )
                {
                    // Checking that we're not locking the admin user of the system partition
                    // See DIRSERVER-1812 (The default admin account should never get locked forever)
                    if ( !userEntry.getDn().equals( new Dn( schemaManager, ServerDNConstants.ADMIN_SYSTEM_DN ) ) )
                    {
                        Attribute pwdAccountLockedTimeAt = new DefaultAttribute( AT_PWD_ACCOUNT_LOCKED_TIME );

                        // if zero, lockout permanently, only admin can unlock it
                        if ( policyConfig.getPwdLockoutDuration() == 0 )
                        {
                            pwdAccountLockedTimeAt.add( "000001010000Z" );
                        }
                        else
                        {
                            pwdAccountLockedTimeAt.add( failureTime );
                        }

                        Modification pwdAccountLockedMod = new DefaultModification( ADD_ATTRIBUTE,
                            pwdAccountLockedTimeAt );
                        mods.add( pwdAccountLockedMod );

                        pwdRespCtrl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.ACCOUNT_LOCKED );
                    }
                }
                else if ( policyConfig.getPwdMinDelay() > 0 )
                {
                    int numDelay = numFailures * policyConfig.getPwdMinDelay();
                    int maxDelay = policyConfig.getPwdMaxDelay();

                    if ( numDelay > maxDelay )
                    {
                        numDelay = maxDelay;
                    }

                    try
                    {
                        Thread.sleep( numDelay * 1000L );
                    }
                    catch ( InterruptedException e )
                    {
                        LOG.warn(
                            "Interrupted while delaying to send the failed authentication response for the user {}",
                            dn, e );
                    }
                }

                if ( !mods.isEmpty() )
                {
                    String csnVal = directoryService.getCSN().toString();
                    Modification csnMod = new DefaultModification( REPLACE_ATTRIBUTE, ENTRY_CSN_AT, csnVal );
                    mods.add( csnMod );

                    ModifyOperationContext bindModCtx = new ModifyOperationContext( adminSession );
                    bindModCtx.setDn( dn );
                    bindModCtx.setEntry( userEntry );
                    bindModCtx.setModItems( mods );
                    bindModCtx.setPushToEvtInterceptor( true );

                    directoryService.getPartitionNexus().modify( bindModCtx );
                }
            }

            String upDn = ( dn == null ? "" : dn.getName() );
            throw new LdapAuthenticationException( I18n.err( I18n.ERR_229, upDn ) );
        }
        else if ( policyConfig != null )
        {
            List<Modification> mods = new ArrayList<Modification>();

            if ( policyConfig.getPwdMaxIdle() > 0 )
            {
                Attribute pwdLastSuccesTimeAt = new DefaultAttribute( AT_PWD_LAST_SUCCESS );
                pwdLastSuccesTimeAt.add( DateUtils.getGeneralizedTime() );
                Modification pwdLastSuccesTimeMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdLastSuccesTimeAt );
                mods.add( pwdLastSuccesTimeMod );
            }

            Attribute pwdFailTimeAt = userEntry.get( AT_PWD_FAILURE_TIME );

            if ( pwdFailTimeAt != null )
            {
                Modification pwdFailTimeMod = new DefaultModification( REMOVE_ATTRIBUTE, pwdFailTimeAt );
                mods.add( pwdFailTimeMod );
            }

            Attribute pwdAccLockedTimeAt = userEntry.get( AT_PWD_ACCOUNT_LOCKED_TIME );

            if ( pwdAccLockedTimeAt != null )
            {
                Modification pwdAccLockedTimeMod = new DefaultModification( REMOVE_ATTRIBUTE, pwdAccLockedTimeAt );
                mods.add( pwdAccLockedTimeMod );
            }

            // checking the expiration time *after* performing authentication, do we need to care about millisecond precision?
            if ( ( policyConfig.getPwdMaxAge() > 0 ) && ( policyConfig.getPwdGraceAuthNLimit() > 0 ) )
            {
                Attribute pwdChangeTimeAttr = userEntry.get( AT_PWD_CHANGED_TIME );

                if ( pwdChangeTimeAttr != null )
                {
                    boolean expired = PasswordUtil.isPwdExpired( pwdChangeTimeAttr.getString(),
                        policyConfig.getPwdMaxAge() );

                    if ( expired )
                    {
                        Attribute pwdGraceUseAttr = userEntry.get( AT_PWD_GRACE_USE_TIME );
                        int numGraceAuth = 0;

                        if ( pwdGraceUseAttr != null )
                        {
                            numGraceAuth = policyConfig.getPwdGraceAuthNLimit() - ( pwdGraceUseAttr.size() + 1 );
                        }
                        else
                        {
                            pwdGraceUseAttr = new DefaultAttribute( AT_PWD_GRACE_USE_TIME );
                            numGraceAuth = policyConfig.getPwdGraceAuthNLimit() - 1;
                        }

                        pwdRespCtrl.getResponse().setGraceAuthNRemaining( numGraceAuth );

                        pwdGraceUseAttr.add( DateUtils.getGeneralizedTime() );
                        Modification pwdGraceUseMod = new DefaultModification( ADD_ATTRIBUTE, pwdGraceUseAttr );
                        mods.add( pwdGraceUseMod );
                    }
                }
            }

            if ( !mods.isEmpty() )
            {
                String csnVal = directoryService.getCSN().toString();
                Modification csnMod = new DefaultModification( REPLACE_ATTRIBUTE, ENTRY_CSN_AT, csnVal );
                mods.add( csnMod );

                ModifyOperationContext bindModCtx = new ModifyOperationContext( adminSession );
                bindModCtx.setDn( dn );
                bindModCtx.setEntry( userEntry );
                bindModCtx.setModItems( mods );
                bindModCtx.setPushToEvtInterceptor( true );

                directoryService.getPartitionNexus().modify( bindModCtx );
            }

            if ( isPPolicyReqCtrlPresent )
            {
                int expiryWarnTime = getPwdTimeBeforeExpiry( userEntry, policyConfig );

                if ( expiryWarnTime > 0 )
                {
                    pwdRespCtrl.getResponse().setTimeBeforeExpiration( expiryWarnTime );
                }

                if ( isPwdMustReset( userEntry ) )
                {
                    pwdRespCtrl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.CHANGE_AFTER_RESET );
                    pwdResetSet.add( dn.getNormName() );
                }

                bindContext.addResponseControl( pwdRespCtrl );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", compareContext );
        }

        checkAuthenticated( compareContext );
        checkPwdReset( compareContext );
        boolean result = next( compareContext );

        return result;
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", deleteContext );
        }

        checkAuthenticated( deleteContext );
        checkPwdReset( deleteContext );
        next( deleteContext );
        invalidateAuthenticatorCaches( deleteContext.getDn() );
    }


    /**
     * {@inheritDoc}
     */
    public Entry getRootDse( GetRootDseOperationContext getRootDseContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", getRootDseContext );
        }

        checkAuthenticated( getRootDseContext );
        checkPwdReset( getRootDseContext );

        return next( getRootDseContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", hasEntryContext );
        }

        checkAuthenticated( hasEntryContext );
        checkPwdReset( hasEntryContext );

        return next( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", lookupContext );
        }

        checkAuthenticated( lookupContext );
        checkPwdReset( lookupContext );

        return next( lookupContext );
    }


    private void invalidateAuthenticatorCaches( Dn principalDn )
    {
        for ( AuthenticationLevel authMech : authenticatorsMapByType.keySet() )
        {
            Collection<Authenticator> authenticators = getAuthenticators( authMech );

            // try each authenticator
            for ( Authenticator authenticator : authenticators )
            {
                authenticator.invalidateCache( principalDn );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", modifyContext );
        }

        checkAuthenticated( modifyContext );

        if ( !directoryService.isPwdPolicyEnabled() || modifyContext.isReplEvent() )
        {
            next( modifyContext );

            List<Modification> modifications = modifyContext.getModItems();

            for ( Modification modification : modifications )
            {
                if ( USER_PASSWORD_AT.equals( modification.getAttribute().getAttributeType() ) )
                {
                    invalidateAuthenticatorCaches( modifyContext.getDn() );
                    break;
                }
            }

            return;
        }

        // handle the case where pwdPolicySubentry AT is about to be deleted in thid modify()
        PasswordPolicyConfiguration policyConfig = getPwdPolicy( modifyContext.getEntry() );

        boolean isPPolicyReqCtrlPresent = modifyContext.hasRequestControl( PasswordPolicy.OID );
        Dn userDn = modifyContext.getSession().getAuthenticatedPrincipal().getDn();

        PwdModDetailsHolder pwdModDetails = null;

        pwdModDetails = getPwdModDetails( modifyContext, policyConfig );

        if ( pwdModDetails.isPwdModPresent() )
        {
            if ( pwdResetSet.contains( userDn.getNormName() ) && !pwdModDetails.isDelete() )
            {
                if ( pwdModDetails.isOtherModExists() )
                {
                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyDecorator responseControl =
                            new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                        responseControl.getResponse().setPasswordPolicyError(
                            PasswordPolicyErrorEnum.CHANGE_AFTER_RESET );
                        modifyContext.addResponseControl( responseControl );
                    }

                    throw new LdapNoPermissionException(
                        "Password should be reset before making any changes to this entry" );
                }
            }

            if ( policyConfig.isPwdSafeModify() && !pwdModDetails.isDelete() )
            {
                if ( pwdModDetails.isAddOrReplace() && !pwdModDetails.isDelete() )
                {
                    String msg = "trying to update password attribute without the supplying the old password";
                    LOG.debug( msg );

                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyDecorator responseControl =
                            new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                        responseControl.getResponse().setPasswordPolicyError(
                            PasswordPolicyErrorEnum.MUST_SUPPLY_OLD_PASSWORD );
                        modifyContext.addResponseControl( responseControl );
                    }

                    throw new LdapNoPermissionException( msg );
                }
            }

            if ( !policyConfig.isPwdAllowUserChange() && !modifyContext.getSession().isAnAdministrator() )
            {
                if ( isPPolicyReqCtrlPresent )
                {
                    PasswordPolicyDecorator responseControl =
                        new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                    responseControl.getResponse().setPasswordPolicyError(
                        PasswordPolicyErrorEnum.PASSWORD_MOD_NOT_ALLOWED );
                    modifyContext.addResponseControl( responseControl );
                }

                throw new LdapNoPermissionException();
            }

            Entry entry = modifyContext.getEntry();

            boolean removeFromPwdResetSet = false;

            List<Modification> mods = new ArrayList<Modification>();

            if ( pwdModDetails.isAddOrReplace() )
            {
                if ( isPwdTooYoung( entry, policyConfig ) )
                {
                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyDecorator responseControl =
                            new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                        responseControl.getResponse().setPasswordPolicyError(
                            PasswordPolicyErrorEnum.PASSWORD_TOO_YOUNG );
                        modifyContext.addResponseControl( responseControl );
                    }

                    throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION,
                        "password is too young to update" );
                }

                byte[] newPassword = pwdModDetails.getNewPwd();

                try
                {
                    String userName = entry.getDn().getRdn().getValue().getString();
                    check( userName, newPassword, policyConfig );
                }
                catch ( PasswordPolicyException e )
                {
                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyDecorator responseControl =
                            new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                        responseControl.getResponse().setPasswordPolicyError(
                            PasswordPolicyErrorEnum.get( e.getErrorCode() ) );
                        modifyContext.addResponseControl( responseControl );
                    }

                    // throw exception if userPassword quality checks fail
                    throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION, e.getMessage(), e );
                }

                int histSize = policyConfig.getPwdInHistory();
                Modification pwdRemHistMod = null;
                Modification pwdAddHistMod = null;
                String pwdChangedTime = DateUtils.getGeneralizedTime();

                if ( histSize > 0 )
                {
                    Attribute pwdHistoryAt = entry.get( AT_PWD_HISTORY );

                    if ( pwdHistoryAt == null )
                    {
                        pwdHistoryAt = new DefaultAttribute( AT_PWD_HISTORY );
                    }

                    List<PasswordHistory> pwdHistLst = new ArrayList<PasswordHistory>();

                    for ( Value<?> value : pwdHistoryAt )
                    {
                        PasswordHistory pwdh = new PasswordHistory( Strings.utf8ToString( value.getBytes() ) );

                        boolean matched = Arrays.equals( newPassword, pwdh.getPassword() );

                        if ( matched )
                        {
                            if ( isPPolicyReqCtrlPresent )
                            {
                                PasswordPolicyDecorator responseControl =
                                    new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                                responseControl.getResponse().setPasswordPolicyError(
                                    PasswordPolicyErrorEnum.PASSWORD_IN_HISTORY );
                                modifyContext.addResponseControl( responseControl );
                            }

                            throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION,
                                "invalid reuse of password present in password history" );
                        }

                        pwdHistLst.add( pwdh );
                    }

                    if ( pwdHistLst.size() >= histSize )
                    {
                        // see the javadoc of PasswordHistory
                        Collections.sort( pwdHistLst );

                        // remove the oldest value
                        PasswordHistory remPwdHist = ( PasswordHistory ) pwdHistLst.toArray()[histSize - 1];
                        Attribute tempAt = new DefaultAttribute( AT_PWD_HISTORY );
                        tempAt.add( remPwdHist.getHistoryValue() );
                        pwdRemHistMod = new DefaultModification( REMOVE_ATTRIBUTE, tempAt );
                    }

                    PasswordHistory newPwdHist = new PasswordHistory( pwdChangedTime, newPassword );
                    pwdHistoryAt.add( newPwdHist.getHistoryValue() );
                    pwdAddHistMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdHistoryAt );
                }

                next( modifyContext );

                invalidateAuthenticatorCaches( modifyContext.getDn() );

                LookupOperationContext lookupContext = new LookupOperationContext( adminSession, modifyContext.getDn(),
                    SchemaConstants.ALL_ATTRIBUTES_ARRAY );
                entry = directoryService.getPartitionNexus().lookup( lookupContext );

                if ( ( policyConfig.getPwdMinAge() > 0 ) || ( policyConfig.getPwdMaxAge() > 0 ) )
                {
                    Attribute pwdChangedTimeAt = new DefaultAttribute( AT_PWD_CHANGED_TIME );
                    pwdChangedTimeAt.add( pwdChangedTime );
                    Modification pwdChangedTimeMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdChangedTimeAt );
                    mods.add( pwdChangedTimeMod );
                }

                if ( pwdAddHistMod != null )
                {
                    mods.add( pwdAddHistMod );
                }

                if ( pwdRemHistMod != null )
                {
                    mods.add( pwdRemHistMod );
                }

                if ( policyConfig.isPwdMustChange() )
                {
                    Attribute pwdMustChangeAt = new DefaultAttribute( AT_PWD_RESET );
                    Modification pwdMustChangeMod = null;

                    if ( modifyContext.getSession().isAnAdministrator() )
                    {
                        pwdMustChangeAt.add( "TRUE" );
                        pwdMustChangeMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdMustChangeAt );
                    }
                    else
                    {
                        pwdMustChangeMod = new DefaultModification( REMOVE_ATTRIBUTE, pwdMustChangeAt );
                        removeFromPwdResetSet = true;
                    }

                    mods.add( pwdMustChangeMod );
                }
            }

            // these two attributes will be removed irrespective  of add or delete
            Attribute pwdFailureTimeAt = entry.get( AT_PWD_FAILURE_TIME );

            if ( pwdFailureTimeAt != null )
            {
                mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdFailureTimeAt ) );
            }

            Attribute pwdGraceUseTimeAt = entry.get( AT_PWD_GRACE_USE_TIME );

            if ( pwdGraceUseTimeAt != null )
            {
                mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdGraceUseTimeAt ) );
            }

            if ( pwdModDetails.isDelete() )
            {
                Attribute pwdHistory = entry.get( AT_PWD_HISTORY );
                if ( pwdHistory != null )
                {
                    mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdHistory ) );
                }

                Attribute pwdChangedTimeAt = entry.get( AT_PWD_CHANGED_TIME );
                if ( pwdChangedTimeAt != null )
                {
                    mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdChangedTimeAt ) );
                }

                Attribute pwdMustChangeAt = entry.get( AT_PWD_RESET );
                if ( pwdMustChangeAt != null )
                {
                    mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdMustChangeAt ) );
                }

                Attribute pwdAccountLockedTimeAt = entry.get( AT_PWD_ACCOUNT_LOCKED_TIME );
                if ( pwdAccountLockedTimeAt != null )
                {
                    mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdAccountLockedTimeAt ) );
                }
            }

            ModifyOperationContext internalModifyCtx = new ModifyOperationContext( adminSession );
            internalModifyCtx.setDn( modifyContext.getDn() );
            internalModifyCtx.setModItems( mods );

            directoryService.getPartitionNexus().modify( internalModifyCtx );

            if ( removeFromPwdResetSet || pwdModDetails.isDelete() )
            {
                pwdResetSet.remove( userDn.getNormName() );
            }
        }
        else
        {
            next( modifyContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", moveContext );
        }

        checkAuthenticated( moveContext );
        checkPwdReset( moveContext );
        next( moveContext );
        invalidateAuthenticatorCaches( moveContext.getDn() );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", moveAndRenameContext );
        }

        checkAuthenticated( moveAndRenameContext );
        checkPwdReset( moveAndRenameContext );
        next( moveAndRenameContext );
        invalidateAuthenticatorCaches( moveAndRenameContext.getDn() );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", renameContext );
        }

        checkAuthenticated( renameContext );
        checkPwdReset( renameContext );
        next( renameContext );
        invalidateAuthenticatorCaches( renameContext.getDn() );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", searchContext );
        }

        checkAuthenticated( searchContext );
        checkPwdReset( searchContext );

        return next( searchContext );
    }


    /**
     * {@inheritDoc}
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        next( unbindContext );

        // remove the Dn from the password reset Set
        // we do not perform a check to see if the reset flag in the associated ppolicy is enabled
        // cause that requires fetching the ppolicy first, which requires a lookup for user entry
        if ( !directoryService.isPwdPolicyEnabled() )
        {
            pwdResetSet.remove( unbindContext.getDn().getNormName() );
        }
    }


    /**
     * Check if the current operation has a valid PrincipalDN or not.
     *
     * @param operation the operation type
     * @throws Exception
     */
    private void checkAuthenticated( OperationContext operation ) throws LdapException
    {
        if ( operation.getSession().isAnonymous() && !directoryService.isAllowAnonymousAccess()
            && !operation.getDn().isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_5, operation.getName() );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }
    }


    /**
     * Initialize the PasswordPolicy attributeTypes
     * 
     * @throws LdapException If the initialization failed
     */
    public void loadPwdPolicyStateAttributeTypes() throws LdapException
    {
        AT_PWD_RESET = schemaManager.lookupAttributeTypeRegistry( PWD_RESET_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( AT_PWD_RESET );

        AT_PWD_CHANGED_TIME = schemaManager.lookupAttributeTypeRegistry( PWD_CHANGED_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( AT_PWD_CHANGED_TIME );

        AT_PWD_HISTORY = schemaManager.lookupAttributeTypeRegistry( PWD_HISTORY_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( AT_PWD_HISTORY );

        AT_PWD_FAILURE_TIME = schemaManager.lookupAttributeTypeRegistry( PWD_FAILURE_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( AT_PWD_FAILURE_TIME );

        AT_PWD_ACCOUNT_LOCKED_TIME = schemaManager.lookupAttributeTypeRegistry( PWD_ACCOUNT_LOCKED_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( AT_PWD_ACCOUNT_LOCKED_TIME );

        AT_PWD_LAST_SUCCESS = schemaManager.lookupAttributeTypeRegistry( PWD_LAST_SUCCESS_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( AT_PWD_LAST_SUCCESS );

        AT_PWD_GRACE_USE_TIME = schemaManager.lookupAttributeTypeRegistry( PWD_GRACE_USE_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( AT_PWD_GRACE_USE_TIME );

        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( schemaManager.lookupAttributeTypeRegistry( PWD_POLICY_SUBENTRY_AT ) );
    }


    // ---------- private methods ----------------
    private void check( String username, byte[] password, PasswordPolicyConfiguration policyConfig )
        throws LdapException
    {
        final CheckQualityEnum qualityVal = policyConfig.getPwdCheckQuality();

        if ( qualityVal == CheckQualityEnum.NO_CHECK )
        {
            return;
        }

        LdapSecurityConstants secConst = PasswordUtil.findAlgorithm( password );

        // do not perform quality check if the password is not plain text and
        // pwdCheckQuality value is set to 1
        if ( secConst != null )
        {
            if ( qualityVal == CheckQualityEnum.CHECK_ACCEPT )
            {
                return;
            }
            else
            {
                throw new PasswordPolicyException( "cannot verify the quality of the non-cleartext passwords",
                    INSUFFICIENT_PASSWORD_QUALITY.getValue() );
            }
        }

        String strPassword = Strings.utf8ToString( password );

        // perform the length validation
        validatePasswordLength( strPassword, policyConfig );

        policyConfig.getPwdValidator().validate( strPassword, username );
    }


    /**
     * validates the length of the password
     */
    private void validatePasswordLength( String password, PasswordPolicyConfiguration policyConfig )
        throws PasswordPolicyException
    {
        int maxLen = policyConfig.getPwdMaxLength();
        int minLen = policyConfig.getPwdMinLength();

        int pwdLen = password.length();

        if ( maxLen > 0 )
        {
            if ( pwdLen > maxLen )
            {
                throw new PasswordPolicyException( "Password should not have more than " + maxLen + " characters",
                    INSUFFICIENT_PASSWORD_QUALITY.getValue() );
            }
        }

        if ( minLen > 0 )
        {
            if ( pwdLen < minLen )
            {
                throw new PasswordPolicyException( "Password should have a minmum of " + minLen + " characters",
                    PASSWORD_TOO_SHORT.getValue() );
            }
        }
    }


    private int getPwdTimeBeforeExpiry( Entry userEntry, PasswordPolicyConfiguration policyConfig )
        throws LdapException
    {
        if ( policyConfig.getPwdMaxAge() == 0 )
        {
            return 0;
        }

        int warningAge = policyConfig.getPwdExpireWarning();

        if ( warningAge <= 0 )
        {
            return 0;
        }

        Attribute pwdChangedTimeAt = userEntry.get( AT_PWD_CHANGED_TIME );
        long changedTime = DateUtils.getDate( pwdChangedTimeAt.getString() ).getTime();

        long currentTime = DateUtils.getDate( DateUtils.getGeneralizedTime() ).getTime();
        int pwdAge = ( int ) ( currentTime - changedTime ) / 1000;

        if ( pwdAge > policyConfig.getPwdMaxAge() )
        {
            return 0;
        }

        warningAge = policyConfig.getPwdMaxAge() - warningAge;

        if ( pwdAge >= warningAge )
        {
            return policyConfig.getPwdMaxAge() - pwdAge;
        }

        return 0;
    }


    /**
     * checks if the password is too young
     *
     * @param userEntry the user's entry
     * @return true if the password is young, false otherwise
     * @throws LdapException
     */
    private boolean isPwdTooYoung( Entry userEntry, PasswordPolicyConfiguration policyConfig ) throws LdapException
    {
        if ( policyConfig.getPwdMinAge() == 0 )
        {
            return false;
        }

        Attribute pwdChangedTimeAt = userEntry.get( AT_PWD_CHANGED_TIME );

        if ( pwdChangedTimeAt != null )
        {
            long changedTime = DateUtils.getDate( pwdChangedTimeAt.getString() ).getTime();
            changedTime += policyConfig.getPwdMinAge() * 1000L;

            long currentTime = DateUtils.getDate( DateUtils.getGeneralizedTime() ).getTime();

            if ( changedTime > currentTime )
            {
                return true;
            }
        }

        return false;
    }


    /**
     * checks if the password must be changed after the initial bind
     *
     * @param userEntry the user's entry
     * @return true if must be changed, false otherwise
     * @throws LdapException
     */
    private boolean isPwdMustReset( Entry userEntry ) throws LdapException
    {
        boolean mustChange = false;

        Attribute pwdResetAt = userEntry.get( AT_PWD_RESET );

        if ( pwdResetAt != null )
        {
            mustChange = Boolean.parseBoolean( pwdResetAt.getString() );
        }

        return mustChange;
    }


    private PwdModDetailsHolder getPwdModDetails( ModifyOperationContext modifyContext,
        PasswordPolicyConfiguration policyConfig ) throws LdapException
    {
        PwdModDetailsHolder pwdModDetails = new PwdModDetailsHolder();

        List<Modification> mods = modifyContext.getModItems();

        for ( Modification m : mods )
        {
            Attribute at = m.getAttribute();

            if ( at.getUpId().equalsIgnoreCase( policyConfig.getPwdAttribute() ) )
            {
                pwdModDetails.setPwdModPresent( true );
                ModificationOperation op = m.getOperation();

                if ( op == REMOVE_ATTRIBUTE )
                {
                    pwdModDetails.setDelete( true );
                }
                else if ( op == REPLACE_ATTRIBUTE || op == ADD_ATTRIBUTE )
                {
                    pwdModDetails.setAddOrReplace( true );
                    pwdModDetails.setNewPwd( at.getBytes() );
                }
            }
            else
            {
                pwdModDetails.setOtherModExists( true );
            }
        }

        return pwdModDetails;
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
        if ( !directoryService.isPwdPolicyEnabled() )
        {
            CoreSession session = opContext.getSession();

            Dn userDn = session.getAuthenticatedPrincipal().getDn();

            if ( pwdResetSet.contains( userDn.getNormName() ) )
            {
                boolean isPPolicyReqCtrlPresent = opContext
                    .hasRequestControl( PasswordPolicy.OID );

                if ( isPPolicyReqCtrlPresent )
                {
                    PasswordPolicyDecorator pwdRespCtrl =
                        new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                    pwdRespCtrl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.CHANGE_AFTER_RESET );
                    opContext.addResponseControl( pwdRespCtrl );
                }

                throw new LdapNoPermissionException( "password needs to be reset before performing this operation" );
            }
        }
    }

    private static class PwdModDetailsHolder
    {
        private boolean pwdModPresent = false;

        private boolean isDelete = false;

        private boolean isAddOrReplace = false;

        private boolean otherModExists = false;

        private byte[] newPwd;


        public boolean isPwdModPresent()
        {
            return pwdModPresent;
        }


        public void setPwdModPresent( boolean pwdModPresent )
        {
            this.pwdModPresent = pwdModPresent;
        }


        public boolean isDelete()
        {
            return isDelete;
        }


        public void setDelete( boolean isDelete )
        {
            this.isDelete = isDelete;
        }


        public boolean isAddOrReplace()
        {
            return isAddOrReplace;
        }


        public void setAddOrReplace( boolean isAddOrReplace )
        {
            this.isAddOrReplace = isAddOrReplace;
        }


        public boolean isOtherModExists()
        {
            return otherModExists;
        }


        public void setOtherModExists( boolean otherModExists )
        {
            this.otherModExists = otherModExists;
        }


        public byte[] getNewPwd()
        {
            return newPwd;
        }


        public void setNewPwd( byte[] newPwd )
        {
            this.newPwd = newPwd;
        }
    }


    /**
     * Gets the effective password policy of the given entry.
     * If the entry has defined a custom password policy by setting "pwdPolicySubentry" attribute
     * then the password policy associated with the Dn specified at the above attribute's value will be returned.
     * Otherwise the default password policy will be returned (if present)
     * 
     * @param userEntry the user's entry
     * @return the associated password policy
     * @throws LdapException
     */
    public PasswordPolicyConfiguration getPwdPolicy( Entry userEntry ) throws LdapException
    {
        if ( pwdPolicyContainer == null )
        {
            return null;
        }

        if ( pwdPolicyContainer.hasCustomConfigs() )
        {
            Attribute pwdPolicySubentry = userEntry.get( pwdPolicySubentryAT );

            if ( pwdPolicySubentry != null )
            {
                Dn configDn = directoryService.getDnFactory().create( pwdPolicySubentry.getString() );

                return pwdPolicyContainer.getPolicyConfig( configDn );
            }
        }

        return pwdPolicyContainer.getDefaultPolicy();
    }


    /**
     * set all the password policies to be used by the server.
     * This includes a default(i.e applicable to all entries) and custom(a.k.a per user) password policies
     * 
     * @param policyContainer the container holding all the password policies
     */
    public void setPwdPolicies( PpolicyConfigContainer policyContainer )
    {
        this.pwdPolicyContainer = policyContainer;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isPwdPolicyEnabled()
    {
        return ( ( pwdPolicyContainer != null )
        && ( ( pwdPolicyContainer.getDefaultPolicy() != null )
        || ( pwdPolicyContainer.hasCustomConfigs() ) ) );
    }


    /**
     * @return the pwdPolicyContainer
     */
    public PpolicyConfigContainer getPwdPolicyContainer()
    {
        return pwdPolicyContainer;
    }


    /**
     * @param pwdPolicyContainer the pwdPolicyContainer to set
     */
    public void setPwdPolicyContainer( PpolicyConfigContainer pwdPolicyContainer )
    {
        this.pwdPolicyContainer = pwdPolicyContainer;
    }


    /**
     * purges failure timestamps which are older than the configured interval
     * (section 7.6 in the draft)
     */
    private void purgeFailureTimes( PasswordPolicyConfiguration config, Attribute pwdFailTimeAt )
    {
        long interval = config.getPwdFailureCountInterval();

        if ( interval == 0 )
        {
            return;
        }

        interval *= 1000;

        long currentTime = DateUtils.getDate( DateUtils.getGeneralizedTime() ).getTime();

        Iterator<Value<?>> itr = pwdFailTimeAt.iterator();

        while ( itr.hasNext() )
        {
            Value<?> value = itr.next();
            String failureTime = value.getString();
            long time = DateUtils.getDate( failureTime ).getTime();
            time += interval;

            if ( currentTime >= time )
            {
                itr.remove();
            }
        }
    }
}
