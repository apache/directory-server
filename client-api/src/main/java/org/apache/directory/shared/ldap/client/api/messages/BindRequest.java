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
package org.apache.directory.shared.ldap.client.api.messages;

/**
 * Bind protocol operation request which authenticates and begins a client
 * session.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface BindRequest extends AbandonableRequest, RequestWithResponse
{
    /**
     * Checks to see if the authentication mechanism is simple and not SASL
     * based.
     * 
     * @return true if the mechanism is simple false if it is SASL based.
     */
    boolean isSimple();


    /**
     * Sets the authentication mechanism to SASL
     * @return The object itself, to allow chaining
     */
    BindRequest setSasl();


    /**
     * Gets the simple credentials associated with a simple authentication
     * attempt or null if this request uses SASL authentication mechanisms.
     * 
     * @return null if the mechanism is SASL or the credentials if it is simple.
     */
    byte[] getCredentials();


    /**
     * Sets the simple credentials associated with a simple authentication
     * attempt ignored if this request uses SASL authentication mechanisms.
     * 
     * @param credentials the credentials if authentication is simple, null otherwise
     * @return The object itself, to allow chaining
     */
    BindRequest setCredentials( byte[] credentials );


    /**
     * Sets the simple credentials associated with a simple authentication
     * attempt ignored if this request uses SASL authentication mechanisms.
     * 
     * @param credentials the credentials if authentication is simple, null otherwise
     * @return The object itself, to allow chaining
     */
    BindRequest setCredentials( String credentials );


    /**
     * Gets the distinguished name of the subject in this authentication
     * request. This field may take on a null value (a zero length string) for
     * the purposes of anonymous binds, when authentication has been performed
     * at a lower layer, or when using SASL credentials with a mechanism that
     * includes the LDAPDN in the credentials.
     * 
     * @return the DN of the authenticating user.
     */
    String getName();


    /**
     * Sets the distinguished name of the subject in this authentication
     * request. This field may take on a null value (or a zero length string)
     * for the purposes of anonymous binds, when authentication has been
     * performed at a lower layer, or when using SASL credentials with a
     * mechanism that includes the LDAPDN in the credentials.
     * 
     * @param name the DN of the authenticating user - leave null for annonymous
     * user.
     * @return The object itself, to allow chaining
     */
    BindRequest setName( String name );


    /**
     * Checks to see if the Ldap v3 protocol is used. Normally this would
     * extract a version number from the bind request sent by the client
     * indicating the version of the protocol to be used in this protocol
     * session. The integer is either a 2 or a 3 at the moment. We thought it
     * was better to just check if the protocol used is 3 or not rather than use
     * an type-safe enumeration type for a binary value. If an LDAPv4 comes out
     * then we shall convert the return type to a type safe enumeration.
     * 
     * @return true if client using version 3 false if it is version 2.
     */
    boolean isVersion3();


    /**
     * Gets the Ldap protocol version used. Normally this would
     * extract a version number from the bind request sent by the client
     * indicating the version of the protocol to be used in this protocol
     * session. The integer is either a 2 or a 3 at the moment. We thought it
     * was better to just check if the protocol used is 3 or not rather than use
     * an type-safe enumeration type for a binary value. If an LDAPv4 comes out
     * then we shall convert the return type to a type safe enumeration.
     * 
     * @return 3 for LdapV3, 2 for LDAPV2
     */
    int getVersion();


    /**
     * Sets whether or not the LDAP v3 or v2 protocol is used. Normally this
     * would extract a version number from the bind request sent by the client
     * indicating the version of the protocol to be used in this protocol
     * session. The integer is either a 2 or a 3 at the moment. We thought it
     * was better to just check if the protocol used is 3 or not rather than use
     * an type-safe enumeration type for a binary value. If an LDAPv4 comes out
     * then we shall convert the return type to a type safe enumeration.
     * 
     * @param version The version. Should be 3 or 2
     * @return The object itself, to allow chaining
     */
    BindRequest setVersion( int version );


    /**
     * Gets the SASL mechanism String associated with this BindRequest if the
     * bind operation is using SASL.
     * 
     * @return the SASL mechanism or null if the bind op is simple
     */
    String getSaslMechanism();


    /**
     * Sets the SASL mechanism String associated with this BindRequest if the
     * bind operation is using SASL.
     * 
     * @param saslMechanism the SASL mechanism
     * @return The object itself, to allow chaining
     */
    BindRequest setSaslMechanism( String saslMechanism );
}
