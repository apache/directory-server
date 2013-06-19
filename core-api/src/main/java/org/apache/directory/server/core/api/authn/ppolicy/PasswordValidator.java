/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.api.authn.ppolicy;


/**
 * An interface for implementing password quality verifiers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface PasswordValidator
{
    /**
     * checks if the given password meets the required quality contraints.<br>
     * <p>Note: the length based validations are already done before calling this method<br>
     *       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     *       so the implementor should concentrate on the content checking.</p>
     *  
     * @param password the password value
     * @param userId user's ID (it is the value of entry's RDN e.x 'admin' if the entry's DN is {uid/cn/etc..}=admin,ou=system)
     * @throws PasswordPolicyException if the password doesn't meet the quality contraints
     */
    void validate( String password, String userId ) throws PasswordPolicyException;
}
