/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.kerberos.sam;

import javax.naming.directory.DirContext;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.messages.value.SamType;

/**
 * Single-use Authentication Mechanism verifier (subsystem) interface.
 * SamVerifiers are modules that can be configured and are dynamically
 * loaded as needed.  Implementations have a few requirements and things
 * implementors should know:
 *
 * <ul>
 *   <li>A public default constructor is required,</li>
 *   <li>after instantitation environment properties are supplied,</li>
 *   <li>next the KeyIntegrityChecker is set for the verifier,</li>
 *   <li>finally the verifier is started up by calling startup(),
 *       incidentally this is where all initialization work should be
 *       done using the environment properties supplied.
 *   </li>
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SamVerifier
{
    /** Starts one of many pluggable SAM type subsystem*/
    void startup() throws SamException;

    /** Shuts down one of many pluggable SAM type subsystem*/
    void shutdown();

    /**
     * SamVerifiers require a KeyIntegrityChecker to calculate the integrity of
     * a generated KerberosKey.  The Kerberos service exposes this interface
     * and supplies it to the verifier to check generated keys to conduct the
     * verification workflow.
     *
     * @param keyChecker the integrity checker that validates whether or not a
     * key can decrypt-decode preauth data (an encryped-encoded generalized
     * timestamp)
     */
    void setIntegrityChecker( KeyIntegrityChecker keyChecker );

    /**
     * Verifies the single use password supplied.
     *
     * @param principal the kerberos principal to use
     * @param sad single-use authentication data (encrypted generalized timestamp)
     */
    KerberosKey verify( KerberosPrincipal principal, byte[] sad ) throws SamException;

    /**
     * Gets the registered SAM algorithm type implemented by this SamVerifier.
     *
     * @return the type value for the SAM algorithm used to verify the SUP.
     */
    SamType getSamType();

    /** sets the user context where users are stored for the primary realm */
    void setUserContext( DirContext userContext );
}
