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
package org.apache.directory.server.core.api.interceptor.context;


import org.apache.commons.lang.NotImplementedException;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.ReferralHandlingMode;
import org.apache.directory.server.i18n.I18n;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Bind context used for Interceptors. It contains all the informations
 * needed for the bind operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BindOperationContext extends AbstractOperationContext
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( BindOperationContext.class );

    /** The password */
    private byte[] credentials;

    /** The SASL mechanism */
    private String saslMechanism;

    /** The SASL identifier */
    private String saslAuthId;

    /** A flag to tell that this is a collateral operation */
    private boolean collateralOperation;

    private ReferralHandlingMode referralHandlingMode;

    /** The IoSession if any */
    private IoSession ioSession;


    /**
     * Creates a new instance of BindOperationContext.
     * 
     * @param session The session to use
     */
    public BindOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.BIND ) );
        }
    }


    /**
     * @return The authentication level. One of :
     * <ul>
     * <li>ANONYMOUS</li>
     * <li>SIMPLE</li>
     * <li>STRONG (sasl)</li>
     * <li>UNAUTHENT</li>
     * <li>INVALID</li>
     * </ul>
     * @throws LdapAuthenticationException If we can't get the AuthenticationLevel
     */
    public AuthenticationLevel getAuthenticationLevel() throws LdapAuthenticationException
    {
        // First check if the SASL mechanism has been set
        if ( saslMechanism == null )
        {
            // No, it's either a SIMPLE, ANONYMOUS, UNAUTHENT or an error
            //
            if ( Dn.isNullOrEmpty( dn ) )
            {
                if ( Strings.isEmpty( credentials ) )
                {
                    // Dn and Credentials are empty, this is an anonymous authent
                    return AuthenticationLevel.NONE;
                }
                else
                {
                    // If we have a password but no Dn, this is invalid
                    LOG.info( "Bad authentication for {}", dn );
                    throw new LdapAuthenticationException( "Invalid authentication" );
                }
            }
            else if ( Strings.isEmpty( credentials ) )
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
        return "BindContext for Dn '" + getDn().getName() + "', credentials <"
            + ( credentials != null ? Strings.dumpBytes( credentials ) : "" ) + ">"
            + ( saslMechanism != null ? ", saslMechanism : <" + saslMechanism + ">" : "" )
            + ( saslAuthId != null ? ", saslAuthId <" + saslAuthId + ">" : "" );
    }


    /**
     * Tells if the current operation is considered a side effect of the
     * current context
     * 
     * @return <tt>true</tt> if there is no collateral operation
     */
    public boolean isCollateralOperation()
    {
        return collateralOperation;
    }


    public void setCollateralOperation( boolean collateralOperation )
    {
        this.collateralOperation = collateralOperation;
    }


    public ReferralHandlingMode getReferralHandlingMode()
    {
        return referralHandlingMode;
    }


    public void setReferralHandlingMode( ReferralHandlingMode referralHandlingMode )
    {
        this.referralHandlingMode = referralHandlingMode;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void throwReferral()
    {
        throw new NotImplementedException( I18n.err( I18n.ERR_320 ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReferralThrown()
    {
        throw new NotImplementedException( I18n.err( I18n.ERR_321 ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void ignoreReferral()
    {
        throw new NotImplementedException( I18n.err( I18n.ERR_322 ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReferralIgnored()
    {
        throw new NotImplementedException( I18n.err( I18n.ERR_323 ) );
    }


    /**
     * @return the ioSession
     */
    public IoSession getIoSession()
    {
        return ioSession;
    }


    /**
     * @param ioSession the ioSession to set
     */
    public void setIoSession( IoSession ioSession )
    {
        this.ioSession = ioSession;
    }
}
