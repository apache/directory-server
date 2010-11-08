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
package org.apache.directory.shared.kerberos.components;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.messages.Ticket;

import sun.security.krb5.internal.AuthorizationData;
import sun.security.krb5.internal.KerberosTime;



/**
 * The KDC-REQ-BODY data structure. It will store the object described by the ASN.1 grammar :
 * <pre>
 * KDC-REQ-BODY    ::= SEQUENCE {
 *      kdc-options             [0] KDCOptions,
 *      cname                   [1] PrincipalName OPTIONAL
 *                                  -- Used only in AS-REQ --,
 *      realm                   [2] Realm
 *                                  -- Server's realm
 *                                  -- Also client's in AS-REQ --,
 *      sname                   [3] PrincipalName OPTIONAL,
 *      from                    [4] KerberosTime OPTIONAL,
 *      till                    [5] KerberosTime,
 *      rtime                   [6] KerberosTime OPTIONAL,
 *      nonce                   [7] UInt32,
 *      etype                   [8] SEQUENCE OF Int32 -- EncryptionType
 *                                  -- in preference order --,
 *      addresses               [9] HostAddresses OPTIONAL,
 *      enc-authorization-data  [10] EncryptedData OPTIONAL
 *                                  -- AuthorizationData --,
 *      additional-tickets      [11] SEQUENCE OF Ticket OPTIONAL
 *                                      -- NOTE: not empty
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcReqBody
{
    /** The KDC options */
    private KdcOptions kdcOptions;
    
    /** The Client Principal, if the request is an AS-REQ */
    private PrincipalName cName;
    
    /** The realm */
    private String realm;
    
    /** The Server Principal */
    private PrincipalName sName;
    
    /** The start time for the requested ticket */
    private KerberosTime from;
    
    /** The expiration date for the requested ticket */
    private KerberosTime till;
    
    /** The renew-till date for the requested ticket */
    private KerberosTime rtime;
    
    /** Random number to avoid MiM attacks */
    private int nonce;
    
    /** Set of desired encryption types */
    private Set<EncryptionType> eType;
    
    /** Addresses valid for the requested ticket */
    private HostAddresses addresses;
    
    /** Encoded authorizationData, used by the TGS-REQ only */
    private EncryptedData encAuthorizationData;
    
    /** Additional tickets */
    private List<Ticket> additionalTickets;


    /**
     * Creates a new instance of RequestBody.
     */
    public KdcReqBody()
    {
        additionalTickets = new ArrayList<Ticket>();
        eType = new HashSet<EncryptionType>();
    }


    /**
     * Returns the additional {@link Ticket}s.
     *
     * @return The additional {@link Ticket}s.
     */
    public Ticket[] getAdditionalTickets()
    {
        return additionalTickets.toArray( new Ticket[]{} );
    }


    /**
     * Set the list of additional Ticket
     * @param additionalTickets the additionalTickets to set
     */
    public void setAdditionalTickets( List<Ticket> additionalTickets )
    {
        this.additionalTickets = additionalTickets;
    }


    /**
     * Add a new Ticket to the list of additional tickets
     * @param additionalTickets the additionalTickets to set
     */
    public void addAdditionalTicket( Ticket additionalTicket )
    {
        this.additionalTickets.add( additionalTicket );
    }


    /**
     * Returns the {@link HostAddresses}.
     *
     * @return The {@link HostAddresses}.
     */
    public HostAddresses getAddresses()
    {
        return addresses;
    }
    
    
    /**
     * @param addresses the addresses to set
     */
    public void setAddresses( HostAddresses addresses )
    {
        this.addresses = addresses;
    }


    /**
     * @return the client PrincipalName
     */
    public PrincipalName getCName()
    {
        return cName;
    }


    /**
     * @param cName the cName to set
     */
    public void setCName( PrincipalName cName )
    {
        this.cName = cName;
    }


    /**
     * Returns the encrypted {@link AuthorizationData} as {@link EncryptedData}.
     *
     * @return The encrypted {@link AuthorizationData} as {@link EncryptedData}.
     */
    public EncryptedData getEncAuthorizationData()
    {
        return encAuthorizationData;
    }


    /**
     * @param encAuthorizationData the encAuthorizationData to set
     */
    public void setEncAuthorizationData( EncryptedData encAuthorizationData )
    {
        this.encAuthorizationData = encAuthorizationData;
    }


    /**
     * Returns the requested {@link EncryptionType}s.
     *
     * @return The requested {@link EncryptionType}s.
     */
    public Set<EncryptionType> getEType()
    {
        return eType;
    }


    /**
     * @param eType the eType to set
     */
    public void setEType( Set<EncryptionType> eType )
    {
        this.eType = eType;
    }


    /**
     * @param eType the eType to add
     */
    public void addEType( EncryptionType eType )
    {
        this.eType.add( eType );
    }


    /**
     * Returns the from {@link KerberosTime}.
     *
     * @return The from {@link KerberosTime}.
     */
    public KerberosTime getFrom()
    {
        return from;
    }
    
    
    /**
     * @param from the from to set
     */
    public void setFrom( KerberosTime from )
    {
        this.from = from;
    }


    /**
     * Returns the {@link KdcOptions}.
     *
     * @return The {@link KdcOptions}.
     */
    public KdcOptions getKdcOptions()
    {
        return kdcOptions;
    }


    /**
     * @param kdcOptions the kdcOptions to set
     */
    public void setKdcOptions( KdcOptions kdcOptions )
    {
        this.kdcOptions = kdcOptions;
    }


    /**
     * Returns the nonce.
     *
     * @return The nonce.
     */
    public int getNonce()
    {
        return nonce;
    }


    /**
     * @param nonce the nonce to set
     */
    public void setNonce( int nonce )
    {
        this.nonce = nonce;
    }


    /**
     * @return the realm
     */
    public String getRealm()
    {
        return realm;
    }


    /**
     * @param realm the realm to set
     */
    public void setRealm( String realm )
    {
        this.realm = realm;
    }


    /**
     * Returns the RenewTime {@link KerberosTime}.
     *
     * @return The RenewTime {@link KerberosTime}.
     */
    public KerberosTime getRTime()
    {
        return rtime;
    }


    /**
     * @param rtime the renewTime to set
     */
    public void setRtime( KerberosTime rtime )
    {
        this.rtime = rtime;
    }


    /**
     * Returns the server {@link PrincipalName}.
     *
     * @return The server {@link PrincipalName}.
     */
    public PrincipalName getSName()
    {
        return sName;
    }


    /**
     * @param sName the sName to set
     */
    public void setSName( PrincipalName sName )
    {
        this.sName = sName;
    }


    /**
     * Returns the till {@link KerberosTime}.
     *
     * @return The till {@link KerberosTime}.
     */
    public KerberosTime getTill()
    {
        return till;
    }


    /**
     * @param till the till to set
     */
    public void setTill( KerberosTime till )
    {
        this.till = till;
    }
}
