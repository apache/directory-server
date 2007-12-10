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
package org.apache.directory.server.ldap.support;


import org.apache.directory.shared.ldap.message.CompareRequest;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;


/**
 * A single reply handler for {@link org.apache.directory.shared.ldap.message.CompareRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class CompareHandler extends AbstractLdapHandler implements MessageHandler
{
    public final void messageReceived( IoSession session, Object request ) throws Exception
    {
        compareMessageReceived( session, ( CompareRequest ) request );
    }


    protected abstract void compareMessageReceived( IoSession session, CompareRequest compareRequest ) throws Exception;
}
