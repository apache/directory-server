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
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_START_TIME_AT;
import static org.apache.directory.api.ldap.model.constants.PasswordPolicySchemaConstants.PWD_END_TIME_AT;
import static org.apache.directory.api.ldap.model.entry.ModificationOperation.ADD_ATTRIBUTE;
import static org.apache.directory.api.ldap.model.entry.ModificationOperation.REMOVE_ATTRIBUTE;
import static org.apache.directory.api.ldap.model.entry.ModificationOperation.REPLACE_ATTRIBUTE;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyRequest;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyResponse;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyResponseImpl;
import org.apache.directory.api.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
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
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.authn.ppolicy.CheckQualityEnum;
import org.apache.directory.server.core.api.authn.ppolicy.DefaultPasswordValidator;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyException;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordValidator;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.AbstractChangeOperationContext;
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
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
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

    // userAccountControl
    public static final String USER_ACCOUNT_CONTROL_AT_OID = "1.2.840.113556.1.4.8";

    // msDS-UserAccountDisabled
    public static final String MS_DS_USER_ACCOUNT_DISABLED_AT_OID = "1.2.840.113556.1.4.1853";

    // pwdLastSet
    public static final String PWD_LAST_SET_AT_OID = "1.2.840.113556.1.4.96";

    // User
    public static final String USER_OC = "user";

    // msDS-BindableObject
    public static final String MS_DS_BINDABLE_OBJECT_OC = "msDS-BindalbeObject";

    /** A Set of all the existing Authenticator to be used by the bind operation */
    private Set<Authenticator> authenticators = new HashSet<>();

    /** A map of authenticators associated with the authentication level required */
    private final EnumMap<AuthenticationLevel, Collection<Authenticator>> authenticatorsMapByType = new EnumMap<>( AuthenticationLevel.class );

    private CoreSession adminSession;

    // MS AD and AD LDS attributes
    private AttributeType userAccountControlAT;

    private AttributeType msDSUserAccountDisabledAT;

    private AttributeType pwdLastSetAT;

    private Attribute userOC;

    private Attribute msDSBindableObjectOC;

    // pwdpolicy state attribute types
    private AttributeType pwdResetAT;

    private AttributeType pwdChangedTimeAT;

    private AttributeType pwdHistoryAT;

    private AttributeType pwdFailurTimeAT;

    private AttributeType pwdAccountLockedTimeAT;

    private AttributeType pwdLastSuccessAT;

    private AttributeType pwdGraceUseTimeAT;

    private AttributeType pwdPolicySubentryAT;

    private AttributeType pwdStartTimeAT;

    private AttributeType pwdEndTimeAT;

    /** a container to hold all the ppolicies */
    private PpolicyConfigContainer pwdPolicyContainer;


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
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        adminSession = directoryService.getAdminSession();

        if ( ( authenticators == null ) || authenticators.isEmpty() )
        {
            setDefaultAuthenticators();
        }

        // Register all authenticators
        for ( Authenticator authenticator : authenticators )
        {
            register( authenticator, directoryService );
        }

        loadPwdPolicyStateAttributeTypes();

        userAccountControlAT = schemaManager.getAttributeType( USER_ACCOUNT_CONTROL_AT_OID );
        msDSUserAccountDisabledAT = schemaManager.getAttributeType( MS_DS_USER_ACCOUNT_DISABLED_AT_OID );
        pwdLastSetAT = schemaManager.getAttributeType( PWD_LAST_SET_AT_OID );
        userOC = new DefaultAttribute(
                schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT_OID ), USER_OC );
        msDSBindableObjectOC = new DefaultAttribute(
                schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT_OID ), MS_DS_BINDABLE_OBJECT_OC );
    }


    /**
     * Initialize the set of authenticators with some default values
     */
    private void setDefaultAuthenticators()
    {
        if ( authenticators == null )
        {
            authenticators = new HashSet<>();
        }

        authenticators.clear();
        authenticators.add( new AnonymousAuthenticator( Dn.ROOT_DSE ) );
        authenticators.add( new SimpleAuthenticator( Dn.ROOT_DSE ) );
        authenticators.add( new StrongAuthenticator( Dn.ROOT_DSE ) );
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
    @Override
    public void destroy()
    {
        authenticatorsMapByType.clear();
        Set<Authenticator> copy = new HashSet<>( authenticators );
        authenticators = new HashSet<>();

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
        authenticators.add( authenticator );

        Collection<Authenticator> authenticatorList = getAuthenticators( authenticator.getAuthenticatorType() );

        if ( authenticatorList == null )
        {
            authenticatorList = new ArrayList<>();
            authenticatorsMapByType.put( authenticator.getAuthenticatorType(), authenticatorList );
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

        if ( ( result != null ) && ( !result.isEmpty() ) )
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
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", addContext );
        }

        checkAuthenticated( addContext );

        updatePwdLastSet( addContext );

        Entry entry = addContext.getEntry();

        if ( !directoryService.isPwdPolicyEnabled() || addContext.isReplEvent() )
        {
            next( addContext );
            return;
        }

        PasswordPolicyConfiguration policyConfig = getPwdPolicy( entry );

        boolean isPPolicyReqCtrlPresent = addContext.hasRequestControl( PasswordPolicyRequest.OID );

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
            Value userPassword = userPasswordAttribute.get();

            try
            {
                check( addContext, entry, userPassword.getBytes(), policyConfig );
            }
            catch ( PasswordPolicyException e )
            {
                if ( isPPolicyReqCtrlPresent )
                {
                    PasswordPolicyResponse responseControl = new PasswordPolicyResponseImpl();
                    responseControl.setPasswordPolicyError(
                        PasswordPolicyErrorEnum.get( e.getErrorCode() ) );
                    addContext.addResponseControl( responseControl );
                }

                // throw exception if userPassword quality checks fail
                throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION, e.getMessage(), e );
            }

            String pwdChangedTime = DateUtils.getGeneralizedTime( directoryService.getTimeProvider() );

            if ( ( policyConfig.getPwdMinAge() > 0 ) || ( policyConfig.getPwdMaxAge() > 0 ) )
            {
                // https://issues.apache.org/jira/browse/DIRSERVER-1978
                if ( !addContext.getSession().isAnAdministrator()
                    || entry.get( pwdChangedTimeAT ) == null )
                {
                    Attribute pwdChangedTimeAt = new DefaultAttribute( pwdChangedTimeAT );
                    pwdChangedTimeAt.add( pwdChangedTime );
                    entry.add( pwdChangedTimeAt );
                }
            }

            if ( policyConfig.isPwdMustChange() && addContext.getSession().isAnAdministrator() )
            {
                Attribute pwdResetAt = new DefaultAttribute( pwdResetAT );
                pwdResetAt.add( "TRUE" );
                entry.add( pwdResetAt );
            }

            if ( policyConfig.getPwdInHistory() > 0 )
            {
                Attribute pwdHistoryAt = new DefaultAttribute( pwdHistoryAT );
                byte[] pwdHistoryVal = new PasswordHistory( pwdChangedTime, userPassword.getBytes() ).getHistoryValue();
                pwdHistoryAt.add( pwdHistoryVal );
                entry.add( pwdHistoryAt );
            }
        }

        next( addContext );
    }


    /**
     * Return the selected authenticator given the DN and the level required.
     */
    private Authenticator selectAuthenticator( Dn bindDn, AuthenticationLevel level )
        throws LdapUnwillingToPerformException, LdapAuthenticationException
    {
        Authenticator selectedAuthenticator = null;
        Collection<Authenticator> levelAuthenticators = authenticatorsMapByType.get( level );

        if ( ( levelAuthenticators == null ) || levelAuthenticators.isEmpty() )
        {
            // No authenticators associated with this level : get out
            throw new LdapAuthenticationException( "Cannot Bind for Dn "
                + bindDn.getName() + ", no authenticator for the requested level " + level );
        }

        if ( levelAuthenticators.size() == 1 )
        {
            // Just pick the existing one
            for ( Authenticator authenticator : levelAuthenticators )
            {
                // Check that the bindDN fits
                if ( authenticator.isValid( bindDn ) )
                {
                    return authenticator;
                }
                else
                {
                    throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                        "Cannot Bind for Dn " + bindDn.getName() 
                        + ", its not a descendant of the authenticator base DN '" + authenticator.getBaseDn() + "'" );
                }
            }
        }

        // We have more than one authenticator. Let's loop on all of them and
        // select the one that fits the bindDN
        Dn innerDn = Dn.ROOT_DSE;

        for ( Authenticator authenticator : levelAuthenticators )
        {
            if ( authenticator.isValid( bindDn ) )
            {
                // We have found a valid authenticator, let's check if it's the inner one
                if ( innerDn.isAncestorOf( authenticator.getBaseDn() ) )
                {
                    innerDn = authenticator.getBaseDn();
                    selectedAuthenticator = authenticator;
                }
            }
        }

        if ( selectedAuthenticator == null )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                    "Cannot Bind for Dn " + bindDn.getName() + ", there is no authenticator for it" );
        }
        
        return selectedAuthenticator;
    }
    
    
    private void internalModify( OperationContext opContext, ModifyOperationContext bindModCtx ) throws LdapException
    {
        Partition partition = opContext.getPartition();
        bindModCtx.setPartition( partition );
        PartitionTxn partitionTxn = null;

        try
        {
            partitionTxn = partition.beginWriteTransaction();
            bindModCtx.setTransaction( partitionTxn );

            directoryService.getPartitionNexus().modify( bindModCtx );

            partitionTxn.commit();
        }
        catch ( LdapException le )
        {
            try 
            {
                if ( partitionTxn != null )
                {
                    partitionTxn.abort();
                }
                
                throw le;
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
        }
        catch ( IOException ioe )
        {
            try 
            {
                partitionTxn.abort();
                
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }
            catch ( IOException ioe2 )
            {
                throw new LdapOtherException( ioe2.getMessage(), ioe2 );
            }
        }
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

        checkUserDisabled( bindContext );

        CoreSession session = bindContext.getSession();
        Dn bindDn = bindContext.getDn();

        if ( ( session != null )
            && ( session.getEffectivePrincipal() != null )
            && ( !session.isAnonymous() )
            && ( !session.isAdministrator() ) )
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
                + bindDn.getName() );
        }

        PasswordPolicyException ppe = null;
        boolean isPPolicyReqCtrlPresent = bindContext.hasRequestControl( PasswordPolicyRequest.OID );
        PasswordPolicyResponse pwdRespCtrl = new PasswordPolicyResponseImpl();
        boolean authenticated = false;

        Authenticator authenticator = selectAuthenticator( bindDn, level );

        try
        {
            // perform the authentication
            LdapPrincipal principal = authenticator.authenticate( bindContext );

            if ( principal != null )
            {
                LdapPrincipal clonedPrincipal = ( LdapPrincipal ) ( principal.clone() );

                // remove creds so there is no security risk
                bindContext.setCredentials( null );
                clonedPrincipal.setUserPassword( Strings.EMPTY_BYTES );

                // authentication was successful
                CoreSession newSession = new DefaultCoreSession( clonedPrincipal, directoryService );
                bindContext.setSession( newSession );

                authenticated = true;
            }
        }
        catch ( PasswordPolicyException e )
        {
            ppe = e;
        }
        catch ( LdapAuthenticationException e )
        {
            // authentication failed, try the next authenticator
            LOG.info( "Authenticator {} failed to authenticate: {}", authenticator, bindContext.getDn() );
        }
        catch ( Exception e )
        {
            // Log other exceptions than LdapAuthenticationException
            LOG.info( "Unexpected failure for Authenticator {} : {}", authenticator, bindContext.getDn() );
        }

        if ( ppe != null )
        {
            if ( isPPolicyReqCtrlPresent )
            {
                pwdRespCtrl.setPasswordPolicyError( PasswordPolicyErrorEnum.get( ppe.getErrorCode() ) );
                bindContext.addResponseControl( pwdRespCtrl );
            }

            throw ppe;
        }

        Entry userEntry = bindContext.getEntry();

        PasswordPolicyConfiguration policyConfig = getPwdPolicy( userEntry );

        // load the user entry again if ppolicy is enabled, cause the authenticator might have modified the entry
        if ( policyConfig != null )
        {
            LookupOperationContext lookupContext = new LookupOperationContext( adminSession, bindDn,
                SchemaConstants.ALL_ATTRIBUTES_ARRAY );
            lookupContext.setPartition( bindContext.getPartition() );
            lookupContext.setTransaction( bindContext.getTransaction() );
            
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
                Attribute pwdFailTimeAt = userEntry.get( pwdFailurTimeAT );

                if ( pwdFailTimeAt == null )
                {
                    pwdFailTimeAt = new DefaultAttribute( pwdFailurTimeAT );
                }
                else
                {
                    purgeFailureTimes( policyConfig, pwdFailTimeAt );
                }

                String failureTime = DateUtils.getGeneralizedTime( directoryService.getTimeProvider() );
                pwdFailTimeAt.add( failureTime );
                Modification pwdFailTimeMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdFailTimeAt );

                List<Modification> mods = new ArrayList<>();
                mods.add( pwdFailTimeMod );

                int numFailures = pwdFailTimeAt.size();

                if ( policyConfig.isPwdLockout() && ( numFailures >= policyConfig.getPwdMaxFailure() ) )
                {
                    // Checking that we're not locking the admin user of the system partition
                    // See DIRSERVER-1812 (The default admin account should never get locked forever)
                    if ( !userEntry.getDn().equals( new Dn( schemaManager, ServerDNConstants.ADMIN_SYSTEM_DN ) ) )
                    {
                        Attribute pwdAccountLockedTimeAt = new DefaultAttribute( pwdAccountLockedTimeAT );

                        // if zero, lockout permanently, only admin can unlock it
                        if ( policyConfig.getPwdLockoutDuration() == 0 )
                        {
                            pwdAccountLockedTimeAt.add( "000001010000Z" );
                        }
                        else
                        {
                            pwdAccountLockedTimeAt.add( failureTime );
                        }

                        Modification pwdAccountLockedMod = new DefaultModification( REPLACE_ATTRIBUTE,
                            pwdAccountLockedTimeAt );
                        mods.add( pwdAccountLockedMod );

                        pwdRespCtrl.setPasswordPolicyError( PasswordPolicyErrorEnum.ACCOUNT_LOCKED );
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
                            bindDn, e );
                    }
                }

                if ( !mods.isEmpty() )
                {
                    String csnVal = directoryService.getCSN().toString();
                    Modification csnMod = new DefaultModification( REPLACE_ATTRIBUTE, directoryService.getAtProvider()
                        .getEntryCSN(), csnVal );
                    mods.add( csnMod );
                    ModifyOperationContext bindModCtx = new ModifyOperationContext( adminSession );
                    bindModCtx.setDn( bindDn );
                    bindModCtx.setEntry( userEntry );
                    bindModCtx.setModItems( mods );
                    bindModCtx.setPushToEvtInterceptor( true );

                    internalModify( bindContext, bindModCtx );
                }
            }

            String upDn = bindDn == null ? "" : bindDn.getName();
            throw new LdapAuthenticationException( I18n.err( I18n.ERR_229, upDn ) );
        }
        else if ( policyConfig != null )
        {
            List<Modification> mods = new ArrayList<>();

            if ( policyConfig.getPwdMaxIdle() > 0 )
            {
                Attribute pwdLastSuccesTimeAt = new DefaultAttribute( pwdLastSuccessAT );
                pwdLastSuccesTimeAt.add( DateUtils.getGeneralizedTime( directoryService.getTimeProvider() ) );
                Modification pwdLastSuccesTimeMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdLastSuccesTimeAt );
                mods.add( pwdLastSuccesTimeMod );
            }

            Attribute pwdFailTimeAt = userEntry.get( pwdFailurTimeAT );

            if ( pwdFailTimeAt != null )
            {
                Modification pwdFailTimeMod = new DefaultModification( REMOVE_ATTRIBUTE, pwdFailTimeAt );
                mods.add( pwdFailTimeMod );
            }

            Attribute pwdAccLockedTimeAt = userEntry.get( pwdAccountLockedTimeAT );

            if ( pwdAccLockedTimeAt != null )
            {
                Modification pwdAccLockedTimeMod = new DefaultModification( REMOVE_ATTRIBUTE, pwdAccLockedTimeAt );
                mods.add( pwdAccLockedTimeMod );
            }

            // checking the expiration time *after* performing authentication, do we need to care about millisecond precision?
            if ( ( policyConfig.getPwdMaxAge() > 0 ) && ( policyConfig.getPwdGraceAuthNLimit() > 0 ) )
            {
                Attribute pwdChangeTimeAttr = userEntry.get( pwdChangedTimeAT );

                if ( pwdChangeTimeAttr != null )
                {
                    boolean expired = PasswordUtil.isPwdExpired( pwdChangeTimeAttr.getString(),
                        policyConfig.getPwdMaxAge(), directoryService.getTimeProvider() );

                    if ( expired )
                    {
                        Attribute pwdGraceUseAttr = userEntry.get( pwdGraceUseTimeAT );
                        int numGraceAuth;

                        if ( pwdGraceUseAttr != null )
                        {
                            numGraceAuth = policyConfig.getPwdGraceAuthNLimit() - ( pwdGraceUseAttr.size() + 1 );
                        }
                        else
                        {
                            pwdGraceUseAttr = new DefaultAttribute( pwdGraceUseTimeAT );
                            numGraceAuth = policyConfig.getPwdGraceAuthNLimit() - 1;
                        }

                        pwdRespCtrl.setGraceAuthNRemaining( numGraceAuth );

                        pwdGraceUseAttr.add( DateUtils.getGeneralizedTime( directoryService.getTimeProvider() ) );
                        Modification pwdGraceUseMod = new DefaultModification( ADD_ATTRIBUTE, pwdGraceUseAttr );
                        mods.add( pwdGraceUseMod );
                    }
                }
            }

            if ( !mods.isEmpty() )
            {
                String csnVal = directoryService.getCSN().toString();
                Modification csnMod = new DefaultModification( REPLACE_ATTRIBUTE, directoryService.getAtProvider()
                    .getEntryCSN(), csnVal );
                mods.add( csnMod );

                ModifyOperationContext bindModCtx = new ModifyOperationContext( adminSession );
                bindModCtx.setDn( bindDn );
                bindModCtx.setEntry( userEntry );
                bindModCtx.setModItems( mods );
                bindModCtx.setPushToEvtInterceptor( true );
                
                internalModify( bindContext, bindModCtx );
            }

            if ( isPPolicyReqCtrlPresent )
            {
                int expiryWarnTime = getPwdTimeBeforeExpiry( userEntry, policyConfig );

                if ( expiryWarnTime > 0 )
                {
                    pwdRespCtrl.setTimeBeforeExpiration( expiryWarnTime );
                }

                if ( isPwdMustReset( userEntry ) )
                {
                    pwdRespCtrl.setPasswordPolicyError( PasswordPolicyErrorEnum.CHANGE_AFTER_RESET );
                    bindContext.getSession().setPwdMustChange( true );
                }

                bindContext.addResponseControl( pwdRespCtrl );
            }
        }
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

        checkAuthenticated( compareContext );
        checkPwdReset( compareContext );
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

        checkAuthenticated( deleteContext );
        checkPwdReset( deleteContext );
        next( deleteContext );
        invalidateAuthenticatorCaches( deleteContext.getDn() );
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

        checkAuthenticated( getRootDseContext );
        checkPwdReset( getRootDseContext );

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

        checkAuthenticated( hasEntryContext );
        checkPwdReset( hasEntryContext );

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

        checkAuthenticated( lookupContext );
        checkPwdReset( lookupContext );

        return next( lookupContext );
    }


    private void invalidateAuthenticatorCaches( Dn principalDn )
    {
        for ( AuthenticationLevel authMech : authenticatorsMapByType.keySet() )
        {
            // try each authenticator
            for ( Authenticator authenticator : getAuthenticators( authMech ) )
            {
                authenticator.invalidateCache( principalDn );
            }
        }
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

        checkAuthenticated( modifyContext );

        updatePwdLastSet( modifyContext );

        if ( !directoryService.isPwdPolicyEnabled() || modifyContext.isReplEvent() )
        {
            processStandardModify( modifyContext );
        }
        else
        {
            processPasswordPolicydModify( modifyContext );
        }
    }

    
    /**
     * Proceed with the Modification operation when the PasswordPolicy is not activated.
     */
    private void processStandardModify( ModifyOperationContext modifyContext ) throws LdapException
    {
        next( modifyContext );

        List<Modification> modifications = modifyContext.getModItems();

        for ( Modification modification : modifications )
        {
            if ( directoryService.getAtProvider().getUserPassword()
                .equals( modification.getAttribute().getAttributeType() ) )
            {
                invalidateAuthenticatorCaches( modifyContext.getDn() );
                break;
            }
        }
    }

    
    /**
     * Proceed with the Modification operation when the PasswordPolicy is activated.
     */
    private void processPasswordPolicydModify( ModifyOperationContext modifyContext ) throws LdapException
    {
        // handle the case where pwdPolicySubentry AT is about to be deleted in this modify()
        PasswordPolicyConfiguration policyConfig = getPwdPolicy( modifyContext.getEntry() );

        PwdModDetailsHolder pwdModDetails = getPwdModDetails( modifyContext, policyConfig );

        if ( !pwdModDetails.isPwdModPresent() )
        {
            // We can going on, the password attribute is not present in the Modifications.
            next( modifyContext );
        }
        else
        {
            // The password is present in the modifications. Deal with the various use cases.
            CoreSession userSession = modifyContext.getSession();
            boolean isPPolicyReqCtrlPresent = modifyContext.hasRequestControl( PasswordPolicyRequest.OID );
            
            // First, check if the password must be changed, and if the operation allows it
            checkPwdMustChange( modifyContext, userSession, pwdModDetails, isPPolicyReqCtrlPresent );

            // Check the the old password is present if it's required by the PP config
            checkOldPwdRequired( modifyContext, policyConfig, pwdModDetails, isPPolicyReqCtrlPresent );

            // Check that we can't update the password if it's not allowed
            checkChangePwdAllowed( modifyContext, policyConfig, isPPolicyReqCtrlPresent );

            Entry entry = modifyContext.getEntry();

            boolean removePwdReset = false;

            List<Modification> mods = new ArrayList<>();

            if ( pwdModDetails.isAddOrReplace() )
            {
                if ( isPwdTooYoung( modifyContext, entry, policyConfig ) )
                {
                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyResponse responseControl = new PasswordPolicyResponseImpl();
                        responseControl.setPasswordPolicyError(
                            PasswordPolicyErrorEnum.PASSWORD_TOO_YOUNG );
                        modifyContext.addResponseControl( responseControl );
                    }

                    throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION,
                        "password is too young to update" );
                }

                byte[] newPassword = pwdModDetails.getNewPwd();

                try
                {
                    check( modifyContext, entry, newPassword, policyConfig );
                }
                catch ( PasswordPolicyException e )
                {
                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyResponse responseControl = new PasswordPolicyResponseImpl();
                        responseControl.setPasswordPolicyError(
                            PasswordPolicyErrorEnum.get( e.getErrorCode() ) );
                        modifyContext.addResponseControl( responseControl );
                    }

                    // throw exception if userPassword quality checks fail
                    throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION, e.getMessage(), e );
                }

                int histSize = policyConfig.getPwdInHistory();
                Modification pwdRemHistMod = null;
                Modification pwdAddHistMod = null;
                String pwdChangedTime = DateUtils.getGeneralizedTime( directoryService.getTimeProvider() );

                if ( histSize > 0 )
                {
                    Attribute pwdHistoryAt = entry.get( pwdHistoryAT );

                    if ( pwdHistoryAt == null )
                    {
                        pwdHistoryAt = new DefaultAttribute( pwdHistoryAT );
                    }

                    // Build the Modification containing the password history
                    pwdRemHistMod = buildPwdHistory( modifyContext, pwdHistoryAt, histSize, 
                        newPassword, isPPolicyReqCtrlPresent );

                    PasswordHistory newPwdHist = new PasswordHistory( pwdChangedTime, newPassword );
                    pwdHistoryAt.add( newPwdHist.getHistoryValue() );
                    pwdAddHistMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdHistoryAt );
                }

                next( modifyContext );

                invalidateAuthenticatorCaches( modifyContext.getDn() );

                LookupOperationContext lookupContext = new LookupOperationContext( adminSession, modifyContext.getDn(),
                    SchemaConstants.ALL_ATTRIBUTES_ARRAY );
                lookupContext.setPartition( modifyContext.getPartition() );
                lookupContext.setTransaction( modifyContext.getTransaction() );
                
                entry = directoryService.getPartitionNexus().lookup( lookupContext );

                if ( ( policyConfig.getPwdMinAge() > 0 ) || ( policyConfig.getPwdMaxAge() > 0 ) )
                {
                    Attribute pwdChangedTimeAt = new DefaultAttribute( pwdChangedTimeAT );
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
                    Attribute pwdMustChangeAt = new DefaultAttribute( pwdResetAT );
                    Modification pwdMustChangeMod;

                    if ( modifyContext.getSession().isAnAdministrator() )
                    {
                        pwdMustChangeAt.add( "TRUE" );
                        pwdMustChangeMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdMustChangeAt );
                    }
                    else
                    {
                        pwdMustChangeMod = new DefaultModification( REMOVE_ATTRIBUTE, pwdMustChangeAt );
                        removePwdReset = true;
                    }

                    mods.add( pwdMustChangeMod );
                }
            }

            // Add the attributes that have been modified following a Add/Replace password
            processModifyAddPwdAttributes( entry, mods, pwdModDetails );

            String csnVal = directoryService.getCSN().toString();
            Modification csnMod = new DefaultModification( REPLACE_ATTRIBUTE, directoryService.getAtProvider()
                .getEntryCSN(), csnVal );
            mods.add( csnMod );

            ModifyOperationContext internalModifyCtx = new ModifyOperationContext( adminSession );
            internalModifyCtx.setPushToEvtInterceptor( true );
            internalModifyCtx.setDn( modifyContext.getDn() );
            internalModifyCtx.setEntry( entry );
            internalModifyCtx.setModItems( mods );

            internalModify( modifyContext, internalModifyCtx );

            if ( removePwdReset || pwdModDetails.isDelete() )
            {
                userSession.setPwdMustChange( false );
            }
        }
    }
    
    
    /**
     * Build the list of passwordHistory
     */
    Modification buildPwdHistory( ModifyOperationContext modifyContext, Attribute pwdHistoryAt, 
        int histSize, byte[] newPassword, boolean isPPolicyReqCtrlPresent ) throws LdapOperationException
    {
        List<PasswordHistory> pwdHistLst = new ArrayList<>();

        for ( Value value : pwdHistoryAt )
        {
            PasswordHistory pwdh = new PasswordHistory( Strings.utf8ToString( value.getBytes() ) );

            // Admin user is exempt from history check
            // https://issues.apache.org/jira/browse/DIRSERVER-2084 
            if ( !modifyContext.getSession().isAnAdministrator() )
            {
                boolean matched = MessageDigest.isEqual( newPassword, pwdh.getPassword() );

                if ( matched )
                {
                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyResponse responseControl = new PasswordPolicyResponseImpl();
                        responseControl.setPasswordPolicyError(
                            PasswordPolicyErrorEnum.PASSWORD_IN_HISTORY );
                        modifyContext.addResponseControl( responseControl );
                    }

                    throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION,
                        "invalid reuse of password present in password history" );
                }
            }

            pwdHistLst.add( pwdh );
        }
 
        Modification pwdRemHistMod = null;
        
        if ( pwdHistLst.size() >= histSize )
        {
            // see the javadoc of PasswordHistory
            Collections.sort( pwdHistLst );

            // remove the oldest value
            PasswordHistory remPwdHist = ( PasswordHistory ) pwdHistLst.toArray()[histSize - 1];
            Attribute tempAt = new DefaultAttribute( pwdHistoryAT );
            tempAt.add( remPwdHist.getHistoryValue() );
            pwdRemHistMod = new DefaultModification( REMOVE_ATTRIBUTE, tempAt );
        }

        return pwdRemHistMod;
    }
    
    
    /**
     * Add the passwordPolicy related Attributes from the modified entry
     */
    private void processModifyAddPwdAttributes( Entry entry, List<Modification> mods, PwdModDetailsHolder pwdModDetails )
    {
        Attribute pwdFailureTimeAt = entry.get( pwdFailurTimeAT );
    
        if ( pwdFailureTimeAt != null )
        {
            mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdFailureTimeAt ) );
        }
    
        Attribute pwdGraceUseTimeAt = entry.get( pwdGraceUseTimeAT );
    
        if ( pwdGraceUseTimeAt != null )
        {
            mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdGraceUseTimeAt ) );
        }
    
        if ( pwdModDetails.isDelete() )
        {
            Attribute pwdHistory = entry.get( pwdHistoryAT );
            
            if ( pwdHistory != null )
            {
                mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdHistory ) );
            }
    
            Attribute pwdChangedTimeAt = entry.get( pwdChangedTimeAT );
            
            if ( pwdChangedTimeAt != null )
            {
                mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdChangedTimeAt ) );
            }
    
            Attribute pwdMustChangeAt = entry.get( pwdResetAT );
            
            if ( pwdMustChangeAt != null )
            {
                mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdMustChangeAt ) );
            }
    
            Attribute pwdAccountLockedTimeAt = entry.get( pwdAccountLockedTimeAT );
            
            if ( pwdAccountLockedTimeAt != null )
            {
                mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdAccountLockedTimeAt ) );
            }
        }
    }

    
    /**
     * Check if the password has to be changed, but can't.
     */
    private void checkPwdMustChange( ModifyOperationContext modifyContext, CoreSession userSession, 
        PwdModDetailsHolder pwdModDetails, boolean isPPolicyReqCtrlPresent ) throws LdapNoPermissionException
    {
        if ( userSession.isPwdMustChange() && !pwdModDetails.isDelete() && pwdModDetails.isOtherModExists() )
       {
           if ( isPPolicyReqCtrlPresent )
           {
               PasswordPolicyResponse responseControl = new PasswordPolicyResponseImpl();
               responseControl.setPasswordPolicyError(
                   PasswordPolicyErrorEnum.CHANGE_AFTER_RESET );
               modifyContext.addResponseControl( responseControl );
           }

           throw new LdapNoPermissionException(
               "Password should be reset before making any changes to this entry" );
       }
    }
    
    
    /**
     * If the PP config request it, the old password must be supplied in the modifications. Check that it 
     * is present.
     */
    private void checkOldPwdRequired( ModifyOperationContext modifyContext, PasswordPolicyConfiguration policyConfig,
        PwdModDetailsHolder pwdModDetails, boolean isPPolicyReqCtrlPresent ) throws LdapNoPermissionException
    {
        if ( policyConfig.isPwdSafeModify() && !pwdModDetails.isDelete() && pwdModDetails.isAddOrReplace() )
        {
            String msg = "trying to update password attribute without the supplying the old password";
            LOG.debug( msg );

            if ( isPPolicyReqCtrlPresent )
            {
                PasswordPolicyResponse responseControl = new PasswordPolicyResponseImpl();
                responseControl.setPasswordPolicyError(
                    PasswordPolicyErrorEnum.MUST_SUPPLY_OLD_PASSWORD );
                modifyContext.addResponseControl( responseControl );
            }

            throw new LdapNoPermissionException( msg );
        }
    }
    
    
    /**
     * check that if the password modification is allowed by the PP config, or if the session is 
     * the admin. 
     */
    private void checkChangePwdAllowed( ModifyOperationContext modifyContext, PasswordPolicyConfiguration policyConfig,
        boolean isPPolicyReqCtrlPresent ) throws LdapNoPermissionException
    {
        if ( !policyConfig.isPwdAllowUserChange() && !modifyContext.getSession().isAnAdministrator() )
             
        {
            if ( isPPolicyReqCtrlPresent )
            {
                PasswordPolicyResponse responseControl = new PasswordPolicyResponseImpl();
                responseControl.setPasswordPolicyError(
                    PasswordPolicyErrorEnum.PASSWORD_MOD_NOT_ALLOWED );
                modifyContext.addResponseControl( responseControl );
            }

            throw new LdapNoPermissionException();
        }
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

        checkAuthenticated( moveContext );
        checkPwdReset( moveContext );
        next( moveContext );
        invalidateAuthenticatorCaches( moveContext.getDn() );
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

        checkAuthenticated( moveAndRenameContext );
        checkPwdReset( moveAndRenameContext );
        next( moveAndRenameContext );
        invalidateAuthenticatorCaches( moveAndRenameContext.getDn() );
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

        checkAuthenticated( renameContext );
        checkPwdReset( renameContext );
        next( renameContext );
        invalidateAuthenticatorCaches( renameContext.getDn() );
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

        checkAuthenticated( searchContext );
        checkPwdReset( searchContext );

        return next( searchContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        next( unbindContext );
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
        pwdResetAT = schemaManager.lookupAttributeTypeRegistry( PWD_RESET_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdResetAT );

        pwdChangedTimeAT = schemaManager.lookupAttributeTypeRegistry( PWD_CHANGED_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdChangedTimeAT );

        pwdHistoryAT = schemaManager.lookupAttributeTypeRegistry( PWD_HISTORY_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdHistoryAT );

        pwdFailurTimeAT = schemaManager.lookupAttributeTypeRegistry( PWD_FAILURE_TIME_AT );
        PWD_POLICY_STATE_ATTRIBUTE_TYPES.add( pwdFailurTimeAT );

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


    // ---------- private methods ----------------
    private void check( OperationContext operationContext, Entry entry,
        byte[] password, PasswordPolicyConfiguration policyConfig )
        throws LdapException
    {
        // https://issues.apache.org/jira/browse/DIRSERVER-1928
        if ( operationContext.getSession().isAnAdministrator() )
        {
            return;
        }
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

        PasswordValidator passwordValidator = policyConfig.getPwdValidator();
        
        if ( passwordValidator == null )
        {
            // Use the default one
            passwordValidator = new DefaultPasswordValidator();
        }
        
        passwordValidator.validate( strPassword, entry );
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

        if ( ( maxLen > 0 ) && ( pwdLen > maxLen ) )
        {
            throw new PasswordPolicyException( "Password should not have more than " + maxLen + " characters",
                INSUFFICIENT_PASSWORD_QUALITY.getValue() );
        }

        if ( ( minLen > 0 ) && ( pwdLen < minLen ) )
        {
            throw new PasswordPolicyException( "Password should have a minimum of " + minLen + " characters",
                PASSWORD_TOO_SHORT.getValue() );
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

        Attribute pwdChangedTimeAt = userEntry.get( pwdChangedTimeAT );
        if ( pwdChangedTimeAt == null )
        {
            pwdChangedTimeAt = userEntry.get( directoryService.getAtProvider().getCreateTimestamp() );
        }
        long changedTime = DateUtils.getDate( pwdChangedTimeAt.getString() ).getTime();

        long currentTime = directoryService.getTimeProvider().currentIimeMillis();
        long pwdAge = ( currentTime - changedTime ) / 1000;

        if ( pwdAge > policyConfig.getPwdMaxAge() )
        {
            return 0;
        }

        warningAge = policyConfig.getPwdMaxAge() - warningAge;

        if ( pwdAge >= warningAge )
        {
            long timeBeforeExpiration = ( ( long ) policyConfig.getPwdMaxAge() ) - pwdAge;

            if ( timeBeforeExpiration > Integer.MAX_VALUE )
            {
                timeBeforeExpiration = Integer.MAX_VALUE;
            }

            return ( int ) timeBeforeExpiration;
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
    private boolean isPwdTooYoung( OperationContext operationContext,
        Entry userEntry, PasswordPolicyConfiguration policyConfig ) throws LdapException
    {
        // https://issues.apache.org/jira/browse/DIRSERVER-1928
        if ( operationContext.getSession().isAnAdministrator() )
        {
            return false;
        }
        if ( policyConfig.getPwdMinAge() == 0 )
        {
            return false;
        }

        CoreSession userSession = operationContext.getSession();
        
        // see sections 7.8 and 7.2 of the ppolicy draft
        if ( policyConfig.isPwdMustChange() && userSession.isPwdMustChange() )
        {
            return false;
        }

        Attribute pwdChangedTimeAt = userEntry.get( pwdChangedTimeAT );

        if ( pwdChangedTimeAt != null )
        {
            long changedTime = DateUtils.getDate( pwdChangedTimeAt.getString() ).getTime();
            changedTime += policyConfig.getPwdMinAge() * 1000L;

            long currentTime = directoryService.getTimeProvider().currentIimeMillis();

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

        Attribute pwdResetAt = userEntry.get( pwdResetAT );

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
            AttributeType passwordAttribute = schemaManager.lookupAttributeTypeRegistry( policyConfig.getPwdAttribute() );

            if ( at.getAttributeType().equals( passwordAttribute ) )
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
        if ( directoryService.isPwdPolicyEnabled() )
        {
            CoreSession session = opContext.getSession();

            if ( session.isPwdMustChange() )
            {
                boolean isPPolicyReqCtrlPresent = opContext
                    .hasRequestControl( PasswordPolicyRequest.OID );

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
     * @throws LdapException If we weren't able to ftech the password policy
     */
    public PasswordPolicyConfiguration getPwdPolicy( Entry userEntry ) throws LdapException
    {
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
        return ( pwdPolicyContainer != null )
        && ( ( pwdPolicyContainer.getDefaultPolicy() != null )
        || ( pwdPolicyContainer.hasCustomConfigs() ) );
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

        long currentTime = directoryService.getTimeProvider().currentIimeMillis();

        Iterator<Value> itr = pwdFailTimeAt.iterator();

        while ( itr.hasNext() )
        {
            Value value = itr.next();
            String failureTime = value.getString();
            long time = DateUtils.getDate( failureTime ).getTime();
            time += interval;

            if ( currentTime >= time )
            {
                itr.remove();
            }
        }
    }

    private void checkUserDisabled( BindOperationContext context ) throws LdapException
    {
        Entry principal = context.getPrincipal();

        if ( principal != null )
        {
            if ( userAccountControlAT != null )
            {
                Attribute attr = principal.get( userAccountControlAT );
                if ( attr != null && ( Long.parseUnsignedLong( attr.getString() ) & 2L ) != 0L )
                {
                    throw new LdapOperationException( ResultCodeEnum.INVALID_CREDENTIALS, "user is disabled" );
                }
            }

            if ( msDSUserAccountDisabledAT != null )
            {
                Attribute attr = principal.get( msDSUserAccountDisabledAT );
                if ( attr != null && Boolean.parseBoolean( attr.getString() ) )
                {
                    throw new LdapOperationException( ResultCodeEnum.INVALID_CREDENTIALS, "user is disabled" );
                }
            }
        }
    }

    private void updatePwdLastSet( AbstractChangeOperationContext context ) throws LdapException
    {
        Entry entry = context.getEntry();

        if ( pwdLastSetAT == null || context.isReplEvent() )
        {
            return;
        }

        if ( !entry.hasObjectClass( userOC ) && !entry.hasObjectClass( msDSBindableObjectOC ) )
        {
            return;
        }

        if ( context instanceof AddOperationContext )
        {
            Attribute pwdLastSet = entry.get( pwdLastSetAT );
            if ( pwdLastSet == null )
            {
                pwdLastSet = new DefaultAttribute( pwdLastSetAT );
                pwdLastSet.add( currentTimeInActiveDirectory() );
                entry.add( pwdLastSet );
            }
            else if ( !"0".equals( pwdLastSet.getString() ) )
            {
                pwdLastSet.clear();
                pwdLastSet.add( currentTimeInActiveDirectory() );
            }
        }
        else if ( context instanceof ModifyOperationContext )
        {
            AttributeType userPasswordAT = directoryService.getAtProvider().getUserPassword();

            if ( directoryService.isPwdPolicyEnabled() )
            {
                PasswordPolicyConfiguration policyConfig = getPwdPolicy( context.getEntry() );
                if ( context.hasRequestControl( PasswordPolicyRequest.OID ) )
                {
                    userPasswordAT = schemaManager.getAttributeType( policyConfig.getPwdAttribute() );
                }
            }

            ModifyOperationContext modifyContext = ( ModifyOperationContext ) context;
            List<Modification> modifications = modifyContext.getModItems();

            boolean needUpdatePwdLastSet = false;
            boolean nonZeroPwdLastSet = true;
            for ( Modification mod : modifications )
            {
                Attribute attr = mod.getAttribute();
                if ( nonZeroPwdLastSet && userPasswordAT.equals( attr.getAttributeType() ) )
                {
                    needUpdatePwdLastSet = true;
                }
                else if ( pwdLastSetAT.equals( attr.getAttributeType() ) )
                {
                    nonZeroPwdLastSet = !"0".equals( attr.getString() );
                    needUpdatePwdLastSet = nonZeroPwdLastSet;
                }
            }

            if ( needUpdatePwdLastSet )
            {
                Attribute pwdLastSet = new DefaultAttribute( pwdLastSetAT );
                pwdLastSet.add( currentTimeInActiveDirectory() );
                Modification pwdLastSetMod = new DefaultModification( REPLACE_ATTRIBUTE, pwdLastSet );

                List<Modification> mods = new ArrayList<>( modifications.size() + 1 );
                mods.addAll( modifications );
                mods.add( pwdLastSetMod );
                modifyContext.setModItems( mods );
            }
        }
    }

    // https://github.com/apache/directory-studio/blob/2.0.0.v20200411-M15/plugins/valueeditors/src/main/java/org/apache/directory/studio/valueeditors/adtime/ActiveDirectoryTimeUtils.java#L54
    private static String currentTimeInActiveDirectory()
    {
        return Long.toUnsignedString( System.currentTimeMillis() * 10000L + 116444736000000000L );
    }
}
