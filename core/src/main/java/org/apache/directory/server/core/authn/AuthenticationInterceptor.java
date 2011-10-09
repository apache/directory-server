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


import static org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum.INSUFFICIENT_PASSWORD_QUALITY;
import static org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum.PASSWORD_TOO_SHORT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_ACCOUNT_LOCKED_TIME_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_CHANGED_TIME_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_EXPIRE_WARNING_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_FAILURE_TIME_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_GRACE_USE_TIME_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_HISTORY_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_LAST_SUCCESS_AT;
import static org.apache.directory.shared.ldap.model.constants.PasswordPolicySchemaConstants.PWD_RESET_AT;
import static org.apache.directory.shared.ldap.model.entry.ModificationOperation.ADD_ATTRIBUTE;
import static org.apache.directory.shared.ldap.model.entry.ModificationOperation.REMOVE_ATTRIBUTE;
import static org.apache.directory.shared.ldap.model.entry.ModificationOperation.REPLACE_ATTRIBUTE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.admin.AdministrativePointInterceptor;
import org.apache.directory.server.core.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.authn.ppolicy.PasswordPolicyException;
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.collective.CollectiveAttributeInterceptor;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.core.trigger.TriggerInterceptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicy;
import org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyDecorator;
import org.apache.directory.shared.ldap.extras.controls.ppolicy.PasswordPolicyErrorEnum;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.util.DateUtils;
import org.apache.directory.shared.util.StringConstants;
import org.apache.directory.shared.util.Strings;
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

    private Set<Dn> pwdResetSet = new HashSet<Dn>();

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
     * the set of interceptors we should *not* go through when pwdpolicy state information is being updated
     */
    private static final Collection<String> BYPASS_INTERCEPTORS;

    static
    {
        Set<String> c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( AdministrativePointInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( AdministrativePointInterceptor.class.getName() );
        c.add( ExceptionInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( CollectiveAttributeInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        c.add( TriggerInterceptor.class.getName() );
        BYPASS_INTERCEPTORS = Collections.unmodifiableCollection( c );
    }


    /**
     * Creates an authentication service interceptor.
     */
    public AuthenticationInterceptor()
    {
    }


    /**
     * Registers and initializes all {@link Authenticator}s to this service.
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        adminSession = directoryService.getAdminSession();
        pwdPolicySubentryAT = schemaManager.lookupAttributeTypeRegistry( "pwdPolicySubentry" );

        if ( ( authenticators == null ) || ( authenticators.size() == 0 ) )
        {
            setDefaultAuthenticators();
        }
        
        // Register all authenticators
        for ( Authenticator authenticator : authenticators )
        {
            register( authenticator, directoryService );
        }

        loadPwdPolicyStateAtributeTypes();
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

        for (Authenticator authenticator : authenticators) 
        {
            this.authenticators.add( authenticator );
        }
    }
    
    
    /**
     * Deinitializes and deregisters all {@link Authenticator}s from this service.
     */
    public void destroy()
    {
        authenticatorsMapByType.clear();
        Set<Authenticator> copy = new HashSet<Authenticator>( authenticators );
        authenticators = null;
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
        }

        authenticatorList.add( authenticator );
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


    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", addContext );
        }

        checkAuthenticated( addContext );

        Entry entry = addContext.getEntry();
        
        
        if ( !directoryService.isPwdPolicyEnabled() )
        {
            next.add( addContext );
            return;
        }
        
        PasswordPolicyConfiguration policyConfig = getPwdPolicy( entry );

        boolean isPPolicyReqCtrlPresent = addContext.hasRequestControl( PasswordPolicy.OID );

        checkPwdReset( addContext );

        if ( entry.get( SchemaConstants.USER_PASSWORD_AT ) != null )
        {
            String username = null;

            BinaryValue userPassword = ( BinaryValue ) entry.get( SchemaConstants.USER_PASSWORD_AT ).get();

            try
            {
                username = entry.getDn().getRdn().getUpValue().getString();
                check( username, userPassword.getValue(), policyConfig );
            }
            catch ( PasswordPolicyException e )
            {
                if ( isPPolicyReqCtrlPresent )
                {
                    PasswordPolicyDecorator responseControl = 
                        new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                    responseControl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.get( e.getErrorCode() ) );
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

        next.add( addContext );
    }


    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", deleteContext );
        }

        checkAuthenticated( deleteContext );
        checkPwdReset( deleteContext );
        next.delete( deleteContext );
        invalidateAuthenticatorCaches( deleteContext.getDn() );
    }


    public Entry getRootDSE( NextInterceptor next, GetRootDSEOperationContext getRootDseContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", getRootDseContext );
        }

        checkAuthenticated( getRootDseContext );
        checkPwdReset( getRootDseContext );
        return next.getRootDSE( getRootDseContext );
    }


    public boolean hasEntry( NextInterceptor next, EntryOperationContext hasEntryContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", hasEntryContext );
        }

        checkAuthenticated( hasEntryContext );
        checkPwdReset( hasEntryContext );
        return next.hasEntry( hasEntryContext );
    }


    public EntryFilteringCursor list( NextInterceptor next, ListOperationContext listContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", listContext );
        }

        checkAuthenticated( listContext );
        checkPwdReset( listContext );
        return next.list( listContext );
    }


    public Entry lookup( NextInterceptor next, LookupOperationContext lookupContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", lookupContext );
        }

        checkAuthenticated( lookupContext );
        checkPwdReset( lookupContext );
        
        return next.lookup( lookupContext );
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


    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", modifyContext );
        }

        checkAuthenticated( modifyContext );

        
        if ( ! directoryService.isPwdPolicyEnabled() )
        {
            next.modify( modifyContext );
            invalidateAuthenticatorCaches( modifyContext.getDn() );
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
            if ( pwdResetSet.contains( userDn ) )
            {
                if ( pwdModDetails.isOtherModExists() )
                {
                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyDecorator responseControl = 
                            new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                        responseControl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.CHANGE_AFTER_RESET );
                        modifyContext.addResponseControl( responseControl );
                    }

                    throw new LdapNoPermissionException();
                }
            }

            if ( policyConfig.isPwdSafeModify() )
            {
                if ( pwdModDetails.isAddOrReplace() && !pwdModDetails.isDelete() )
                {
                    LOG.debug( "trying to update password attribute without the supplying the old password" );
                    
                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyDecorator responseControl = 
                            new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                        responseControl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.MUST_SUPPLY_OLD_PASSWORD );
                        modifyContext.addResponseControl( responseControl );
                    }

                    throw new LdapNoPermissionException();
                }
            }

            if ( !policyConfig.isPwdAllowUserChange() && !modifyContext.getSession().isAnAdministrator() )
            {
                if ( isPPolicyReqCtrlPresent )
                {
                    PasswordPolicyDecorator responseControl = 
                        new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                    responseControl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.PASSWORD_MOD_NOT_ALLOWED );
                    modifyContext.addResponseControl( responseControl );
                }

                throw new LdapNoPermissionException();
            }

            Entry entry = modifyContext.getEntry();

            if ( isPwdTooYoung( entry, policyConfig ) )
            {
                if ( isPPolicyReqCtrlPresent )
                {
                    PasswordPolicyDecorator responseControl = 
                        new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                    responseControl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.PASSWORD_TOO_YOUNG );
                    modifyContext.addResponseControl( responseControl );
                }

                throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION,
                    "password is too young to update" );
            }

            byte[] newPassword = null;
            
            if ( ( pwdModDetails != null ) )
            {
                newPassword = pwdModDetails.getNewPwd();
                
                try
                {
                    String userName = entry.getDn().getRdn().getUpValue().getString();
                    check( userName, newPassword, policyConfig );
                }
                catch ( PasswordPolicyException e )
                {
                    if ( isPPolicyReqCtrlPresent )
                    {
                        PasswordPolicyDecorator responseControl = 
                            new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                        responseControl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.get( e.getErrorCode() ) );
                        modifyContext.addResponseControl( responseControl );
                    }

                    // throw exception if userPassword quality checks fail
                    throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION, e.getMessage(), e );
                }
            }

            int histSize = policyConfig.getPwdInHistory();
            Modification pwdRemHistMod = null;
            Modification pwdAddHistMod = null;
            String pwdChangedTime = DateUtils.getGeneralizedTime();

            if ( histSize > 0 )
            {
                Attribute pwdHistoryAt = entry.get( PWD_HISTORY_AT );
                if ( pwdHistoryAt == null )
                {
                	pwdHistoryAt = new DefaultAttribute( AT_PWD_HISTORY );
                }
                
                Set<PasswordHistory> pwdHistSet = new TreeSet<PasswordHistory>();

                for ( Value<?> value : pwdHistoryAt  )
                {
                    PasswordHistory pwdh = new PasswordHistory( Strings.utf8ToString( value.getBytes() ) );

                    boolean matched = Arrays.equals( newPassword, pwdh.getPassword() );

                    if ( matched )
                    {
                        if ( isPPolicyReqCtrlPresent )
                        {
                            PasswordPolicyDecorator responseControl = 
                                new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );
                            responseControl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.PASSWORD_IN_HISTORY );
                            modifyContext.addResponseControl( responseControl );
                        }

                        throw new LdapOperationException( ResultCodeEnum.CONSTRAINT_VIOLATION,
                            "invalid reuse of password present in password history" );
                    }

                    pwdHistSet.add( pwdh );
                }

                PasswordHistory newPwdHist = new PasswordHistory( pwdChangedTime, newPassword );
                pwdHistSet.add( newPwdHist );

                pwdHistoryAt.clear();
                pwdHistoryAt.add( newPwdHist.getHistoryValue() );
                pwdAddHistMod = new DefaultModification( ADD_ATTRIBUTE, pwdHistoryAt );

                if ( pwdHistSet.size() > histSize )
                {
                    PasswordHistory remPwdHist = ( PasswordHistory ) pwdHistSet.toArray()[histSize - 1];
                    pwdHistoryAt.add( remPwdHist.getHistoryValue() );
                    pwdRemHistMod = new DefaultModification( REMOVE_ATTRIBUTE, pwdHistoryAt );
                }
            }

            next.modify( modifyContext );
            
            invalidateAuthenticatorCaches( modifyContext.getDn() );

            List<Modification> mods = new ArrayList<Modification>();
            
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

            boolean removeFromPwdResetSet = false;
            
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

            Attribute pwdFailureTimeAt = entry.get( PWD_FAILURE_TIME_AT );
            
            if ( pwdFailureTimeAt != null )
            {
                mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdFailureTimeAt ) );
            }

            Attribute pwdGraceUseTimeAt = entry.get( PWD_GRACE_USE_TIME_AT );
            
            if ( pwdGraceUseTimeAt != null )
            {
                mods.add( new DefaultModification( REMOVE_ATTRIBUTE, pwdGraceUseTimeAt ) );
            }

            directoryService.getAdminSession().modify( modifyContext.getDn(), mods );

            if ( removeFromPwdResetSet )
            {
                pwdResetSet.remove( userDn );
            }
        }
        else
        {
            next.modify( modifyContext );
            invalidateAuthenticatorCaches( modifyContext.getDn() );
        }
    }


    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", renameContext );
        }

        checkAuthenticated( renameContext );
        checkPwdReset( renameContext );
        next.rename( renameContext );
        invalidateAuthenticatorCaches( renameContext.getDn() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean compare( NextInterceptor next, CompareOperationContext compareContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", compareContext );
        }

        checkAuthenticated( compareContext );
        checkPwdReset( compareContext );
        boolean result = next.compare( compareContext );
        invalidateAuthenticatorCaches( compareContext.getDn() );

        return result;
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext )
        throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", moveAndRenameContext );
        }

        checkAuthenticated( moveAndRenameContext );
        checkPwdReset( moveAndRenameContext );
        next.moveAndRename( moveAndRenameContext );
        invalidateAuthenticatorCaches( moveAndRenameContext.getDn() );
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", moveContext );
        }

        checkAuthenticated( moveContext );
        checkPwdReset( moveContext );
        next.move( moveContext );
        invalidateAuthenticatorCaches( moveContext.getDn() );
    }


    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext searchContext )
        throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", searchContext );
        }

        checkAuthenticated( searchContext );
        checkPwdReset( searchContext );
        return next.search( searchContext );
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


    public void bind( NextInterceptor next, BindOperationContext bindContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", bindContext );
        }

        if ( ( bindContext.getSession() != null ) && ( bindContext.getSession().getEffectivePrincipal() != null ) )
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

        if ( authenticators == null )
        {
            LOG.debug( "No authenticators found, delegating bind to the nexus." );

            // as a last resort try binding via the nexus
            next.bind( bindContext );

            LOG.debug( "Nexus succeeded on bind operation." );

            // bind succeeded if we got this far
            // TODO - authentication level not being set
            LdapPrincipal principal = new LdapPrincipal( schemaManager, bindContext.getDn(), AuthenticationLevel.SIMPLE );
            CoreSession session = new DefaultCoreSession( principal, directoryService );
            bindContext.setSession( session );

            // remove creds so there is no security risk
            bindContext.setCredentials( null );
            return;
        }

        boolean isPPolicyReqCtrlPresent = bindContext.hasRequestControl( PasswordPolicy.OID );
        PasswordPolicyDecorator pwdRespCtrl = 
            new PasswordPolicyDecorator( directoryService.getLdapCodecService(), true );

        boolean authenticated = false;
        PasswordPolicyException ppe = null;

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
                if ( LOG.isInfoEnabled() )
                {
                    LOG.info( "Authenticator {} failed to authenticate: {}", authenticator, bindContext );
                }
            }
            catch ( Exception e )
            {
                // Log other exceptions than LdapAuthenticationException
                if ( LOG.isWarnEnabled() )
                {
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
                Attribute pwdFailTimeAt = userEntry.get( PWD_FAILURE_TIME_AT );
                if ( pwdFailTimeAt == null )
                {
                    pwdFailTimeAt = new DefaultAttribute( AT_PWD_FAILURE_TIME );
                }
                else
                {
                    PasswordUtil.purgeFailureTimes( policyConfig, pwdFailTimeAt );
                }

                String failureTime = DateUtils.getGeneralizedTime();
                pwdFailTimeAt.add( failureTime );
                Modification pwdFailTimeMod = new DefaultModification( ADD_ATTRIBUTE, pwdFailTimeAt );

                List<Modification> mods = new ArrayList<Modification>();
                mods.add( pwdFailTimeMod );

                int numFailures = pwdFailTimeAt.size();

                if ( policyConfig.isPwdLockout() && ( numFailures >= policyConfig.getPwdMaxFailure() ) )
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
                    
                    Modification pwdAccountLockedMod = new DefaultModification( ADD_ATTRIBUTE, pwdAccountLockedTimeAt );
                    mods.add( pwdAccountLockedMod );

                    pwdRespCtrl.getResponse().setPasswordPolicyError( PasswordPolicyErrorEnum.ACCOUNT_LOCKED );
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
                        Thread.sleep( numDelay * 1000 );
                    }
                    catch ( InterruptedException e )
                    {
                        LOG.warn(
                            "Interrupted while delaying to send the failed authentication response for the user {}",
                            dn, e );
                    }
                }

                //adminSession.modify( dn, Collections.singletonList( pwdFailTimeMod ) );
                ModifyOperationContext bindModCtx = new ModifyOperationContext( adminSession );
                bindModCtx.setByPassed( BYPASS_INTERCEPTORS );
                bindModCtx.setDn( dn );
                bindModCtx.setModItems( mods );
                directoryService.getOperationManager().modify( bindModCtx );
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
                Attribute pwdChangeTimeAttr = userEntry.get( PWD_CHANGED_TIME_AT );
                if ( pwdChangeTimeAttr != null )
                {
                    boolean expired = PasswordUtil.isPwdExpired( pwdChangeTimeAttr.getString(),
                        policyConfig.getPwdMaxAge() );
                    if ( expired )
                    {
                        Attribute pwdGraceUseAttr = userEntry.get( PWD_GRACE_USE_TIME_AT );
                        if ( pwdGraceUseAttr != null )
                        {
                            pwdRespCtrl.getResponse().setGraceAuthNsRemaining( policyConfig.getPwdGraceAuthNLimit()
                                - ( pwdGraceUseAttr.size() + 1 ) );
                        }
                        else
                        {
                            pwdGraceUseAttr = new DefaultAttribute( AT_PWD_GRACE_USE_TIME );
                        }

                        pwdGraceUseAttr.add( DateUtils.getGeneralizedTime() );
                        Modification pwdGraceUseMod = new DefaultModification( ADD_ATTRIBUTE, pwdGraceUseAttr );
                        mods.add( pwdGraceUseMod );
                    }
                }
            }

            if ( !mods.isEmpty() )
            {
                //adminSession.modify( dn, mods );
                ModifyOperationContext bindModCtx = new ModifyOperationContext( adminSession );
                bindModCtx.setByPassed( BYPASS_INTERCEPTORS );
                bindModCtx.setDn( dn );
                bindModCtx.setModItems( mods );
                directoryService.getOperationManager().modify( bindModCtx );
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
                    pwdResetSet.add( dn );
                }

                bindContext.addResponseControl( pwdRespCtrl );
            }
        }
    }


    @Override
    public void unbind( NextInterceptor next, UnbindOperationContext unbindContext ) throws LdapException
    {
        super.unbind( next, unbindContext );

        // remove the Dn from the password reset Set
        // we do not perform a check to see if the reset flag in the associated ppolicy is enabled
        // cause that requires fetching the ppolicy first, which requires a lookup for user entry
        if ( !directoryService.isPwdPolicyEnabled() )
        {
            pwdResetSet.remove( unbindContext.getDn() );
        }
    }


    /**
     * Initialize the PasswordPolicy attributeTypes
     * 
     * @throws LdapException If the initialization failed
     */
    public void loadPwdPolicyStateAtributeTypes() throws LdapException
    {
        if ( directoryService.isPwdPolicyEnabled() )
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
        }
    }


    // ---------- private methods ----------------

    private void check( String username, byte[] password, PasswordPolicyConfiguration policyConfig ) throws LdapException
    {
        final int qualityVal = policyConfig.getPwdCheckQuality();

        if ( qualityVal == 0 )
        {
            return;
        }

        LdapSecurityConstants secConst = PasswordUtil.findAlgorithm( password );

        // do not perform quality check if the password is not plain text and
        // pwdCheckQuality value is set to 1
        if ( secConst != null )
        {
            if ( qualityVal == 1 )
            {
                return;
            }
            else
            {
                throw new PasswordPolicyException( "cannot verify the quality of the non-cleartext passwords",
                    INSUFFICIENT_PASSWORD_QUALITY.getValue() );
            }
        }

        String strPassword = Strings.utf8ToString(password);
        
        // perform the length validation
        validatePasswordLength( strPassword, policyConfig );
        
        policyConfig.getPwdValidator().validate( strPassword, username );
    }


    /**
     * validates the length of the password
     */
    private void validatePasswordLength( String password, PasswordPolicyConfiguration policyConfig ) throws PasswordPolicyException
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


    private int getPwdTimeBeforeExpiry( Entry userEntry, PasswordPolicyConfiguration policyConfig ) throws LdapException
    {
        if ( policyConfig.getPwdMaxAge() == 0 )
        {
            return 0;
        }

        Attribute pwdExpireWarningAt = userEntry.get( PWD_EXPIRE_WARNING_AT );
        if ( pwdExpireWarningAt == null )
        {
            return 0;
        }

        Attribute pwdChangedTimeAt = userEntry.get( PWD_CHANGED_TIME_AT );
        long changedTime = DateUtils.getDate(pwdChangedTimeAt.getString()).getTime();

        int pwdAge = ( int ) ( System.currentTimeMillis() - changedTime ) / 1000;

        if ( pwdAge > policyConfig.getPwdMaxAge() )
        {
            return 0;
        }

        int warningAge = ( int ) ( DateUtils.getDate( pwdExpireWarningAt.getString() ).getTime() ) / 1000;

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

        Attribute pwdChangedTimeAt = userEntry.get( PWD_CHANGED_TIME_AT );
        if ( pwdChangedTimeAt != null )
        {
        	long changedTime = DateUtils.getDate( pwdChangedTimeAt.getString() ).getTime();
        	changedTime += policyConfig.getPwdMinAge() * 1000;
        	
        	if ( changedTime > System.currentTimeMillis() )
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

        Attribute pwdResetAt = userEntry.get( PWD_RESET_AT );
        if ( pwdResetAt != null )
        {
            mustChange = Boolean.parseBoolean( pwdResetAt.getString() );
        }

        return mustChange;
    }


    private PwdModDetailsHolder getPwdModDetails( ModifyOperationContext modifyContext, PasswordPolicyConfiguration policyConfig ) throws LdapException
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
        if ( ! directoryService.isPwdPolicyEnabled() )
        {
            CoreSession session = opContext.getSession();

            Dn userDn = session.getAuthenticatedPrincipal().getDn();

            if ( pwdResetSet.contains( userDn ) )
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
                Dn configDn = adminSession.getDirectoryService().getDnFactory().create( pwdPolicySubentry.getString() );
                
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
}
