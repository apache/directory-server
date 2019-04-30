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
package org.apache.directory.kerberos.client;


import org.apache.directory.shared.kerberos.codec.options.ApOptions;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.components.EncryptionKey;


public class ServiceTicketRequest
{
    private TgTicket tgt;

    private String serverPrincipal;

    private ApOptions apOptions = new ApOptions();

    private EncryptionKey subSessionKey;

    private KdcOptions kdcOptions = new KdcOptions();


    public ServiceTicketRequest( TgTicket tgt, String serverPrincipal )
    {
        this.tgt = tgt;
        this.serverPrincipal = serverPrincipal;
    }


    public TgTicket getTgt()
    {
        return tgt;
    }


    public String getServerPrincipal()
    {
        return serverPrincipal;
    }


    public ApOptions getApOptions()
    {
        return apOptions;
    }


    public EncryptionKey getSubSessionKey()
    {
        return subSessionKey;
    }


    public void setSubSessionKey( EncryptionKey subSessionKey )
    {
        this.subSessionKey = subSessionKey;
    }


    public KdcOptions getKdcOptions()
    {
        return kdcOptions;
    }


    public void setKdcOptions( KdcOptions kdcOptions )
    {
        this.kdcOptions = kdcOptions;
    }
}
