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
package org.apache.ldap.server.authn;


import org.apache.ldap.server.PartitionNexus;


/**
 * Defines a set of methods that an authenticator uses to communicate with its container,
 * for example, to get the partition nexus, or whether the server is configured to accept
 * anonymous connection.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 124525 $
 */
public interface AuthenticatorContext
{

    /**
     * Returns a reference to the PartitionNexus.
     */
    public PartitionNexus getPartitionNexus();

    /**
     * Returns the value of server.disable.anonymous JNDI Property.
     */
    public boolean getAllowAnonymous();
}