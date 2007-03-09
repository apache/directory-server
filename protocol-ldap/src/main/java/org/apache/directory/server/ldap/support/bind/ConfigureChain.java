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
package org.apache.directory.server.ldap.support.bind;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.sasl.Sasl;

import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ConfigureChain implements IoHandlerCommand
{
    private static final Logger log = LoggerFactory.getLogger( ConfigureChain.class );


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        /**
         * TODO - Take intersection of supported mechanisms and mechanisms enabled in configuration.
         */
        Set<String> supportedMechanisms = new HashSet<String>();
        supportedMechanisms.add( "SIMPLE" );
        supportedMechanisms.add( "CRAM-MD5" );
        supportedMechanisms.add( "DIGEST-MD5" );
        supportedMechanisms.add( "GSSAPI" );
        session.setAttribute( "supportedMechanisms", supportedMechanisms );

        /**
         * TODO - Take host from configuration.
         */
        String saslHost = "ldap.example.com";
        session.setAttribute( "saslHost", saslHost );

        Map<String, String> saslProps = new HashMap<String, String>();

        /**
         * TODO - Take service principal name from configuration.
         * TODO - Create Subject with key material from directory.
         */
        String servicePrincipalName = "ldap/" + saslHost + "@EXAMPLE.COM";
        session.setAttribute( "saslSubject", getSubject( servicePrincipalName ) );

        /**
         * TODO - Take QoP props from configuration.
         */
        saslProps.put( Sasl.QOP, "auth,auth-int,auth-conf" );

        /**
         * TODO - Take realms from configuration.
         */
        saslProps.put( "com.sun.security.sasl.digest.realm", "example.com apache.org" );

        session.setAttribute( "saslProps", saslProps );

        /**
         * TODO - Get one or more base DN's for user lookups.
         * TODO - Make decision on base DN lookup vs. regex mapping.
         */
        session.setAttribute( "baseDn", "ou=users,dc=example,dc=com" );

        next.execute( session, message );
    }


    private Subject getSubject( String servicePrincipalName )
    {
        KerberosPrincipal servicePrincipal = new KerberosPrincipal( servicePrincipalName );
        char[] password = new String( "randall" ).toCharArray();
        KerberosKey serviceKey = new KerberosKey( servicePrincipal, password, "DES" );
        Subject subject = new Subject();
        subject.getPrivateCredentials().add( serviceKey );

        return subject;
    }
}
