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


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.HostAddress;


public class TgtRequest
{
    private String clientPrincipal;// cname
    private String password;
    private String realm; // realm
    private String serverPrincipal;// sname, optional

    private long startTime;// from

    private long expiryTime;// till

    private long renewTill;// rtime

    private List<HostAddress> hostAddresses = new ArrayList<>();

    private KdcOptions options = new KdcOptions();

    private boolean preAuthEnabled = false;

    /** the set of encryption types that the server replied */
    private Set<EncryptionType> eTypes;


    public TgtRequest()
    {
        startTime = System.currentTimeMillis();
        expiryTime = startTime + ( 8 * 60 * 60 * 1000 );
    }


    public void addHost( String hostNameOrIpAddress ) throws UnknownHostException
    {
        InetAddress address = InetAddress.getByName( hostNameOrIpAddress );
        hostAddresses.add( new HostAddress( address ) );
    }


    public String getPassword()
    {
        return password;
    }


    public void setPassword( String password )
    {
        this.password = password;
    }


    public String getClientPrincipal()
    {
        return clientPrincipal;
    }


    public void setClientPrincipal( String clientPrincipal )
    {
        this.clientPrincipal = clientPrincipal;
        realm = KdcClientUtil.extractRealm( clientPrincipal );
    }


    public String getRealm()
    {
        return realm;
    }


    public String getServerPrincipal()
    {
        return serverPrincipal;
    }


    public void setServerPrincipal( String serverPrincipal )
    {
        this.serverPrincipal = serverPrincipal;
    }


    public long getStartTime()
    {
        return startTime;
    }


    public void setStartTime( long startTime )
    {
        this.startTime = startTime;
    }


    public long getExpiryTime()
    {
        return expiryTime;
    }


    public void setExpiryTime( long expiryTime )
    {
        this.expiryTime = expiryTime;
    }


    public long getRenewTill()
    {
        return renewTill;
    }


    public void setRenewTill( long renewTill )
    {
        this.renewTill = renewTill;
    }


    public List<HostAddress> getHostAddresses()
    {
        return hostAddresses;
    }


    public void setForwardable( boolean forwardable )
    {
        setOrClear( KdcOptions.FORWARDABLE, forwardable );
    }


    public void setProxiable( boolean proxiable )
    {
        setOrClear( KdcOptions.PROXIABLE, proxiable );
    }


    public void setAllowPostdate( boolean allowPostdate )
    {
        setOrClear( KdcOptions.ALLOW_POSTDATE, allowPostdate );
    }


    public void setPostdated( boolean postdated )
    {
        setOrClear( KdcOptions.POSTDATED, postdated );
    }


    public void setRenewableOk( boolean renewableOk )
    {
        setOrClear( KdcOptions.RENEWABLE_OK, renewableOk );
    }


    public void setRenewable( boolean renewable )
    {
        setOrClear( KdcOptions.RENEWABLE, renewable );
    }


    public KdcOptions getOptions()
    {
        return options;
    }


    public boolean isPreAuthEnabled()
    {
        return preAuthEnabled;
    }


    public void setPreAuthEnabled( boolean preAuthEnabled )
    {
        this.preAuthEnabled = preAuthEnabled;
    }


    public String getSName()
    {
        return KdcClientUtil.extractName( serverPrincipal );
    }


    public String getCName()
    {
        return KdcClientUtil.extractName( clientPrincipal );
    }


    public Set<EncryptionType> getETypes()
    {
        return eTypes;
    }


    public void setETypes( Set<EncryptionType> eTypes )
    {
        this.eTypes = eTypes;
    }


    private void setOrClear( int pos, boolean set )
    {
        if ( set )
        {
            options.setBit( pos );
        }
        else
        {
            options.clearBit( pos );
        }
    }
}
