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
package org.apache.directory.server.ldap.handlers.bind;


import javax.security.sasl.SaslServer;

import org.apache.directory.server.ldap.LdapSession;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * An abstract class for all the MechanismHandlers, implementing some common methods
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractMechanismHandler implements MechanismHandler
{
    /** A logger for this class **/
    private static final Logger LOG = LoggerFactory.getLogger( AbstractMechanismHandler.class );


    /**
     * Inject a SaslFilter into the Filter chain, to deal with modified
     * PDU sent when some mechanisms have been negotiated (DIGEST-MD5, GSSAPI, 
     * for instance)
     *
     * @param ldapSession the LdapSession instance
     */
    protected void insertSaslFilter( LdapSession ldapSession )
    {
        LOG.debug( "Inserting SaslFilter to engage negotiated security layer." );
        IoSession ioSession = ldapSession.getIoSession();

        // get the Io chain
        IoFilterChain chain = ioSession.getFilterChain();

        if ( !chain.contains( SaslConstants.SASL_FILTER ) )
        {
            SaslServer saslServer = ( SaslServer ) ldapSession.getSaslProperty( SaslConstants.SASL_SERVER );
            chain.addBefore( "codec", SaslConstants.SASL_FILTER, new SaslFilter( saslServer ) );
        }

        /*
         * We disable the SASL security layer once, to write the outbound SUCCESS
         * message without SASL security layer processing.
         */
        ioSession.setAttribute( SaslFilter.DISABLE_SECURITY_LAYER_ONCE, Boolean.TRUE );
    }
}
