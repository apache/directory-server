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
package org.apache.directory.server.operations.extended;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.schema.normalizers.DeepTrimToLowerNormalizer;
import org.apache.directory.api.ldap.model.schema.normalizers.OidNormalizer;
import org.apache.directory.api.ldap.sp.JavaStoredProcUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") }, saslHost = "localhost", saslMechanisms =
    { @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class),
        @SaslMechanism(name = SupportedSaslMechanisms.CRAM_MD5, implClass = CramMd5MechanismHandler.class),
        @SaslMechanism(name = SupportedSaslMechanisms.DIGEST_MD5, implClass = DigestMd5MechanismHandler.class),
        @SaslMechanism(name = SupportedSaslMechanisms.GSSAPI, implClass = GssapiMechanismHandler.class),
        @SaslMechanism(name = SupportedSaslMechanisms.NTLM, implClass = NtlmMechanismHandler.class),
        @SaslMechanism(name = SupportedSaslMechanisms.GSS_SPNEGO, implClass = NtlmMechanismHandler.class) }, extendedOpHandlers =
    { StoredProcedureExtendedOperationHandler.class })
public class StoredProcedureIT extends AbstractLdapTestUnit
{
    private LdapContext ctx;
    private LdapContext spCtx;
    private Map<String, OidNormalizer> oids;


    @BeforeEach
    public void setUp() throws Exception
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + getLdapServer().getPort() + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialLdapContext( env, null );

        Attributes spContainer = new BasicAttributes( "objectClass", "top", true );
        spContainer.get( "objectClass" ).add( "organizationalUnit" );
        spContainer.put( "ou", "Stored Procedures" );
        spCtx = ( LdapContext ) ctx.createSubcontext( "ou=Stored Procedures", spContainer );
        assertNotNull( spCtx );

        // Initialize OIDs maps for normalization
        oids = new HashMap<String, OidNormalizer>();

        oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
    }


    @Disabled
    @Test
    public void testExecuteProcedureWithReturnValue() throws Exception
    {
        String procedureName = HelloWorldProcedure.class.getName() + ":sayHello";
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, HelloWorldProcedure.class );
        Object response = JavaStoredProcUtils.callStoredProcedure( ctx, procedureName, new Object[]
            {} );
        assertEquals( "Hello World!", response );
    }


    @Disabled
    @Test
    public void testExecuteProcedureWithParametersAndReturnValue() throws Exception
    {
        String procedureName = HelloWorldProcedure.class.getName() + ":sayHelloTo";
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, HelloWorldProcedure.class );
        Object response = JavaStoredProcUtils.callStoredProcedure( ctx, procedureName, new Object[]
            { "Ersin" } );
        assertEquals( "Hello Ersin!", response );
    }

    /*
    @Test public void testSPDeleteSubtree() throws Exception
    {
        String ldif =
            "version: 1\n" +
            "\n" +
            "dn: ou=People,ou=system\n" +
            "ou: People\n" +
            "objectclass: organizationalUnit\n" +
            "objectclass: top\n" +
            "\n" +
            "dn: cn=John,ou=People,ou=system\n" +
            "objectclass: person\n" +
            "objectclass: top\n" +
            "sn: John\n" +
            "cn: John\n" +
            "\n" +
            "dn: cn=Jane,ou=People,ou=system\n" +
            "objectclass: person\n" +
            "objectclass: top\n" +
            "sn: Jane\n" +
            "cn: Jane\n";
        
        injectEntries( ldif );
        
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, DitUtilitiesProcedure.class );
        
        Dn people = new Dn( "ou=People" );
        people = Dn.normalize(  people, oids );
        
        String spName = DitUtilitiesProcedure.class.getName() + ":deleteSubtree";
        Object[] params = new Object[] { new LdapContextParameter( "ou=system" ),
                                         people };
        
        
        JavaStoredProcUtils.callStoredProcedure( ctx, spName, params );
        
        try
        {
            sysRoot.lookup( "cn=Jane,ou=People" );
            sysRoot.lookup( "cn=John,ou=People" );
            sysRoot.lookup( "ou=People" );
            fail( "We should not have come here." );
        }
        catch ( NameNotFoundException e )
        {
            // Expected
        }
    }
    */
}
