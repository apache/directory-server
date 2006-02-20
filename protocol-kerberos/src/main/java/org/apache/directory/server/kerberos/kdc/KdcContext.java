/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.server.kerberos.kdc;


import java.net.InetAddress;

import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.KerberosMessage;
import org.apache.directory.server.kerberos.shared.service.LockBox;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.protocol.shared.chain.impl.ContextBase;


public class KdcContext extends ContextBase
{
    private static final long serialVersionUID = 6490030984626825108L;

    private KdcConfiguration config;
    private PrincipalStore store;
    private KdcRequest request;
    private KerberosMessage reply;
    private InetAddress clientAddress;
    private LockBox lockBox;


    /**
     * @return Returns the config.
     */
    public KdcConfiguration getConfig()
    {
        return config;
    }


    /**
     * @param config The config to set.
     */
    public void setConfig( KdcConfiguration config )
    {
        this.config = config;
    }


    /**
     * @return Returns the store.
     */
    public PrincipalStore getStore()
    {
        return store;
    }


    /**
     * @param store The store to set.
     */
    public void setStore( PrincipalStore store )
    {
        this.store = store;
    }


    /**
     * @return Returns the request.
     */
    public KdcRequest getRequest()
    {
        return request;
    }


    /**
     * @param request The request to set.
     */
    public void setRequest( KdcRequest request )
    {
        this.request = request;
    }


    /**
     * @return Returns the reply.
     */
    public KerberosMessage getReply()
    {
        return reply;
    }


    /**
     * @param reply The reply to set.
     */
    public void setReply( KerberosMessage reply )
    {
        this.reply = reply;
    }


    /**
     * @return Returns the clientAddress.
     */
    public InetAddress getClientAddress()
    {
        return clientAddress;
    }


    /**
     * @param clientAddress The clientAddress to set.
     */
    public void setClientAddress( InetAddress clientAddress )
    {
        this.clientAddress = clientAddress;
    }


    /**
     * @return Returns the lockBox.
     */
    public LockBox getLockBox()
    {
        return lockBox;
    }


    /**
     * @param lockBox The lockBox to set.
     */
    public void setLockBox( LockBox lockBox )
    {
        this.lockBox = lockBox;
    }
}
