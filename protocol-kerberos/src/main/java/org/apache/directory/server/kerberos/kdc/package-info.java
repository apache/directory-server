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

/**
 * Provides the entry point to an instance of the {@link KdcConfiguration}
 * (KDC), as well as classes common to the KDC's two services:  the
 * Authentication Service (AS) and the Ticket-Granting Service (TGS).  The
 * AS and TGS service implementations follow the Chain of Responsibility
 * pattern, using MINA's {@link IoHandlerChain} support.  Additionally,
 * there is a third chain for pre-authentication, which is a sub-chain
 * of the Authentication Service.
 * <p/>
 * Classes common to all of the chains provide configuration
 * support, the execution context, chain monitors for logging, and chain
 * "links" ({@link IoHandlerCommand}'s) for selecting checksum and
 * encryption types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */

package org.apache.directory.server.kerberos.kdc;
