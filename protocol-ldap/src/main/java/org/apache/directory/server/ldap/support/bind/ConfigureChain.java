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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.sasl.Sasl;

import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.ldap.constants.SupportedSASLMechanisms;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ConfigureChain implements IoHandlerCommand
{
    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        LdapConfiguration config = ( LdapConfiguration ) session.getAttribute( LdapConfiguration.class.toString() );

        Map<String, String> saslProps = new HashMap<String, String>();
        saslProps.put( Sasl.QOP, getActiveQop( config ) );
        saslProps.put( "com.sun.security.sasl.digest.realm", getActiveRealms( config ) );
        session.setAttribute( "saslProps", saslProps );

        session.setAttribute( "supportedMechanisms", getActiveMechanisms( config ) );
        session.setAttribute( "saslHost", config.getSaslHost() );
        session.setAttribute( "saslSubject", getSubject( config.getSaslPrincipal() ) );
        session.setAttribute( "baseDn", config.getSaslBaseDn() );

        next.execute( session, message );
    }


    private Set getActiveMechanisms( LdapConfiguration config )
    {
        List<String> supportedMechanisms = new ArrayList<String>();
        supportedMechanisms.add( SupportedSASLMechanisms.SIMPLE );
        supportedMechanisms.add( SupportedSASLMechanisms.CRAM_MD5 );
        supportedMechanisms.add( SupportedSASLMechanisms.DIGEST_MD5 );
        supportedMechanisms.add( SupportedSASLMechanisms.GSSAPI );

        Set<String> activeMechanisms = new HashSet<String>();

        Iterator it = config.getSupportedMechanisms().iterator();
        while ( it.hasNext() )
        {
            String desiredMechanism = ( String ) it.next();
            if ( supportedMechanisms.contains( desiredMechanism ) )
            {
                activeMechanisms.add( desiredMechanism );
            }
        }

        return activeMechanisms;
    }


    private String getActiveQop( LdapConfiguration config )
    {
        List<String> supportedQop = new ArrayList<String>();
        supportedQop.add( "auth" );
        supportedQop.add( "auth-int" );
        supportedQop.add( "auth-conf" );

        StringBuilder saslQop = new StringBuilder();

        Iterator it = config.getSaslQop().iterator();
        while ( it.hasNext() )
        {
            String desiredQopLevel = ( String ) it.next();
            if ( supportedQop.contains( desiredQopLevel ) )
            {
                saslQop.append( desiredQopLevel );
            }

            if ( it.hasNext() )
            {
                // QOP is comma-delimited
                saslQop.append( "," );
            }
        }

        return saslQop.toString();
    }


    private String getActiveRealms( LdapConfiguration config )
    {
        StringBuilder realms = new StringBuilder();

        Iterator it = config.getSaslRealms().iterator();
        while ( it.hasNext() )
        {
            String realm = ( String ) it.next();
            realms.append( realm );

            if ( it.hasNext() )
            {
                // realms are space-delimited
                realms.append( " " );
            }
        }

        return realms.toString();
    }


    /**
     * TODO - Create Subject with key material from directory.
     */
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
