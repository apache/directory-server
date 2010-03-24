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
package org.apache.directory.server.core.interceptor.context;
 

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.ReferralHandlingMode;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A Bind context used for Interceptors. It contains all the informations
 * needed for the bind operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BindOperationContext implements OperationContext
{
    /** The password */
    private byte[] credentials;

    /** The SASL mechanism */
    private String saslMechanism;
    
    /** The SASL identifier */
    private String saslAuthId;
    
    private static final Control[] EMPTY_CONTROLS = new Control[0];

    /** The DN associated with the context */
    private DN dn;
    
    /** The associated request's controls */
    private Map<String, Control> requestControls = new HashMap<String, Control>(4);

    /** The associated response's controls */
    private Map<String, Control> responseControls = new HashMap<String, Control>(4);

    /** A flag to tell that this is a collateral operation */
    private boolean collateralOperation;
    
    /** the Interceptors bypassed by this operation */
    private Collection<String> bypassed;
    
    private CoreSession session;
    
    private LdapPrincipal authorizedPrincipal;
    
    private OperationContext next;
    
    private OperationContext previous;

    private ReferralHandlingMode referralHandlingMode;

    private ClonedServerEntry entry;

    
    /**
     * Creates a new instance of BindOperationContext.
     */
    public BindOperationContext( CoreSession session )
    {
        this.session = session;
    }

    
    /**
     * @return The authentication level. One of :
     * <li>ANONYMOUS</li>
     * <li>SIMPLE</li>
     * <li>STRONG (sasl)</li>
     * <li>UNAUTHENT</li>
     * <li>INVALID</li>
     */
    public AuthenticationLevel getAuthenticationLevel()
    {
        if ( ( saslMechanism == null ) )
        {
            if ( dn.isEmpty() )
            {
                if ( StringTools.isEmpty( credentials ) )
                {
                    // Dn and Credentials are empty, this is an anonymous authent
                    return AuthenticationLevel.NONE;
                }
                else
                {
                    // If we have a password but no DN, this is invalid 
                    return AuthenticationLevel.INVALID;
                }
            }
            else if ( StringTools.isEmpty( credentials ) )
            {
                return AuthenticationLevel.UNAUTHENT;
            }
            else
            {
                return AuthenticationLevel.SIMPLE;
            }
        }
        else
        {
            return AuthenticationLevel.STRONG;
        }
    }
    
    
    /**
     * @return the SASL mechanisms
     */
    public String getSaslMechanism()
    {
        return saslMechanism;
    }

    
    public void setSaslMechanism( String saslMechanism )
    {
        this.saslMechanism = saslMechanism;
    }

    
    /**
     * @return The principal password
     */
    public byte[] getCredentials()
    {
        return credentials;
    }

    
    public void setCredentials( byte[] credentials )
    {
        this.credentials = credentials;
    }

    
    /**
     * @return The SASL authentication ID
     */
    public String getSaslAuthId()
    {
        return saslAuthId;
    }


    public void setSaslAuthId( String saslAuthId )
    {
        this.saslAuthId = saslAuthId;
    }
    
    
    public boolean isSaslBind()
    {
        return saslMechanism != null;
    }
    
    
    /**
     * @return the operation name
     */
    public String getName()
    {
        return MessageTypeEnum.BIND_REQUEST.name();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "BindContext for DN '" + getDn().getName() + "', credentials <" +
            ( credentials != null ? StringTools.dumpBytes( credentials ) : "" ) + ">" +
            ( saslMechanism != null ? ", saslMechanism : <" + saslMechanism + ">" : "" ) +
            ( saslAuthId != null ? ", saslAuthId <" + saslAuthId + ">" : "" );
    }


    public CoreSession getSession()
    {
        return session;
    }
    
    
    public void setSession( CoreSession session )
    {
        this.session = session;
    }


    /**
     * Tells if the current operation is considered a side effect of the
     * current context
     */
    public boolean isCollateralOperation()
    {
        return collateralOperation;
    }


    public void setCollateralOperation( boolean collateralOperation )
    {
        this.collateralOperation = collateralOperation;
    }


    /**
     * @return The associated DN
     */
    public DN getDn()
    {
        return dn;
    }

    
    /**
     * Set the context DN
     *
     * @param dn The DN to set
     */
    public void setDn( DN dn )
    {
        this.dn = dn;
    }

    
    public void addRequestControl( Control requestControl )
    {
        requestControls.put( requestControl.getOid(), requestControl );
    }

    
    public Control getRequestControl( String numericOid )
    {
        return requestControls.get( numericOid );
    }

    
    public boolean hasRequestControl( String numericOid )
    {
        return requestControls.containsKey( numericOid );
    }

    
    public boolean hasRequestControls()
    {
        return ! requestControls.isEmpty();
    }


    public void addResponseControl( Control responseControl )
    {
        responseControls.put( responseControl.getOid(), responseControl );
    }


    public Control getResponseControl( String numericOid )
    {
        return responseControls.get( numericOid );
    }


    public boolean hasResponseControl( String numericOid )
    {
        return responseControls.containsKey( numericOid );
    }


    public Control[] getResponseControls()
    {
        if ( responseControls.isEmpty() )
        {
            return EMPTY_CONTROLS;
        }
        
        return responseControls.values().toArray( EMPTY_CONTROLS );
    }


    public boolean hasResponseControls()
    {
        return ! responseControls.isEmpty();
    }


    public int getResponseControlCount()
    {
        return responseControls.size();
    }


    public void addRequestControls( Control[] requestControls )
    {
        for ( Control c : requestControls )
        {
            this.requestControls.put( c.getOid(), c );
        }
    }


    /**
     * Gets the set of bypassed Interceptors.
     *
     * @return the set of bypassed Interceptors
     */
    public Collection<String> getByPassed()
    {
        if ( bypassed == null )
        {
            return Collections.emptyList();
        }
        
        return Collections.unmodifiableCollection( bypassed );
    }
    
    
    /**
     * Sets the set of bypassed Interceptors.
     * 
     * @param byPassed the set of bypassed Interceptors
     */
    public void setByPassed( Collection<String> byPassed )
    {
        this.bypassed = byPassed;
    }

    
    /**
     * Checks to see if an Interceptor is bypassed for this operation.
     *
     * @param interceptorName the interceptorName of the Interceptor to check for bypass
     * @return true if the Interceptor should be bypassed, false otherwise
     */
    public boolean isBypassed( String interceptorName )
    {
        return bypassed != null && bypassed.contains( interceptorName );
    }


    /**
     * Checks to see if any Interceptors are bypassed by this operation.
     *
     * @return true if at least one bypass exists
     */
    public boolean hasBypass()
    {
        return bypassed != null && !bypassed.isEmpty();
    }
    
    
    public LookupOperationContext newLookupContext( DN dn )
    {
        return new LookupOperationContext( session, dn );
    }


    public ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception
    {
        return session.getDirectoryService().getOperationManager().lookup( opContext );
    }


    public ClonedServerEntry lookup( DN dn, Collection<String> byPassed ) throws Exception
    {
        LookupOperationContext opContext = newLookupContext( dn );
        opContext.setByPassed( byPassed );
        return session.getDirectoryService().getOperationManager().lookup( opContext );
    }


    public LdapPrincipal getEffectivePrincipal()
    {
        if ( authorizedPrincipal != null )
        {
            return authorizedPrincipal;
        }
        
        return session.getEffectivePrincipal();
    }
    
    
    // -----------------------------------------------------------------------
    // OperationContext Linked List Methods
    // -----------------------------------------------------------------------
    
    
    public boolean isFirstOperation()
    {
        return previous == null;
    }
    
    
    public OperationContext getFirstOperation()
    {
        if ( previous == null )
        {
            return this;
        }
        
        return previous.getFirstOperation();
    }
    
    
    public OperationContext getLastOperation()
    {
        if ( next == null )
        {
            return this;
        }
        
        return next.getLastOperation();
    }
    
    
    public OperationContext getNextOperation()
    {
        return next;
    }
    
    
    public OperationContext getPreviousOperation()
    {
        return previous;
    }


    public void add( ServerEntry entry, Collection<String> bypass ) throws Exception
    {
        throw new NotImplementedException();
    }


    public void delete( DN dn, Collection<String> bypass ) throws Exception
    {
        throw new NotImplementedException();
    }


    public void modify( DN dn, List<Modification> mods, Collection<String> bypass ) throws Exception
    {
        throw new NotImplementedException();
    }


    private void setup( AbstractOperationContext opContext )
    {
        opContext.setPreviousOperation( this );
        next = opContext;
        opContext.setByPassed( opContext.getByPassed() );
        opContext.setAuthorizedPrincipal( authorizedPrincipal );
    }
    
    
    public boolean hasEntry( DN dn, Collection<String> byPassed ) throws Exception
    {
        EntryOperationContext opContext = new EntryOperationContext( session, dn );
        setup( opContext );
        opContext.setByPassed( byPassed );
        return session.getDirectoryService().getOperationManager().hasEntry( opContext );
    }


    public ReferralHandlingMode getReferralHandlingMode()
    {
        return referralHandlingMode;
    }


    public void setReferralHandlingMode( ReferralHandlingMode referralHandlingMode )
    {
        this.referralHandlingMode = referralHandlingMode;
    }


    public ClonedServerEntry getEntry()
    {
        return entry;
    }


    public void setEntry( ClonedServerEntry entry )
    {
        this.entry = entry;
    }


    /**
     * {@inheritDoc}
     */
    public void throwReferral()
    {
        throw new NotImplementedException( I18n.err( I18n.ERR_320 ) );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isReferralThrown()
    {
        throw new NotImplementedException( I18n.err( I18n.ERR_321 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void ignoreReferral()
    {
        throw new NotImplementedException( I18n.err( I18n.ERR_322 ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isReferralIgnored()
    {
        throw new NotImplementedException( I18n.err( I18n.ERR_323 ) );
    }
}
