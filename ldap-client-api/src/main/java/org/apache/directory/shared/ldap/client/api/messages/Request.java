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
 * Ldap protocol request messages derive from this super interface.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 760984 $
 */
public interface Request extends Message
{
    /**
     * Get the client message timeout. When the timeout is reached, the 
     * request is canceled. 
     *
     * @return The timeout
     */
    long getTimeout();
    
    
    /**
     * Set a request client timeout. When this timeout is reached, the request 
     * will be canceled. If <= 0, then we wait for the response forever.  
     *
     * @param timeout The new timeout, expressed in milliseconds
     */
    Message setTimeout( long timeout );
}
