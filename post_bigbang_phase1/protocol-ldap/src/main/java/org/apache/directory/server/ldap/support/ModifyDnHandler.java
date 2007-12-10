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

 
import org.apache.directory.shared.ldap.message.ModifyDnRequest;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;


/**
 * A single reply handler for {@link org.apache.directory.shared.ldap.message.ModifyDnRequest}s.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class ModifyDnHandler extends AbstractLdapHandler implements MessageHandler
{
    /**
     * Deal with a ModifyDN request received from a client.
     * 
     * A ModifyDN operation has more than one semantic, depending on its parameters.
     * 
     * In any case, the first argument is the DN entry to be changed. We then
     * have the new relative DN for this entry.
     * 
     * Two other arguments can be provided :
     * - deleteOldRdn : if the old RDN attributes should be removed from the
     * new entry or not (for instance, if the old RDN was cn=acme, and the new 
     * one is sn=acme, then we may have to remove the cn: acme from the attributes
     * list)
     * - newSuperior : this is a move operation. The entry is removed from its
     * current location, and created in the new one.
     */
    public final void messageReceived( IoSession session, Object request ) throws Exception
    {
        modifyDnMessageReceived( session, ( ModifyDnRequest ) request );
    }


    protected abstract void modifyDnMessageReceived( IoSession session, ModifyDnRequest modifyDnRequest ) throws Exception;
}
