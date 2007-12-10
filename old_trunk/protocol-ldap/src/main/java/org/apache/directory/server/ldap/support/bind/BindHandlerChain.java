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


import org.apache.mina.handler.chain.IoHandlerChain;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BindHandlerChain extends IoHandlerChain
{
    /**
     * Creates a new instance of BindHandlerChain.
     */
    public BindHandlerChain()
    {
        addLast( "configureChain", new ConfigureChain() );
        addLast( "chainGuard", new ChainGuard() );
        addLast( "handleSasl", new HandleSasl() );
        addLast( "handleSimple", new HandleSimple() );
        addLast( "getLdapContext", new GetLdapContext() );
        addLast( "returnSuccess", new ReturnSuccess() );
    }
}
