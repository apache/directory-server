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

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * A structure to hold the authoricator data.
 * 
 * <pre>
 * Authenticator   ::= [APPLICATION 2] SEQUENCE  {
 *         authenticator-vno       [0] INTEGER (5),
 *         crealm                  [1] Realm,
 *         cname                   [2] <PrincipalName>,
 *         cksum                   [3] <Checksum> OPTIONAL,
 *         cusec                   [4] Microseconds,
 *         ctime                   [5] KerberosTime,
 *         subkey                  [6] <EncryptionKey> OPTIONAL,
 *         seq-number              [7] UInt32 OPTIONAL,
 *         authorization-data      [8] <AuthorizationData> OPTIONAL
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Authenticator extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( Authenticator.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /**
     * Constant for the authenticator version number.
     */
    public static final int AUTHENTICATOR_VNO = KerberosConstants.KERBEROS_V5;

    /** The authenticator version number */
    private int versionNumber;
    
    /** The client realm */
    private String crealm;
    
    /** The client principalName */
    private PrincipalName cname;
    
    /** The checksum */
    private Checksum cksum;
    
    /** The client microseconds */
    private int cusec;
    
    /** The client time */
    private KerberosTime ctime;
    
    /** The sub-session key */
    private EncryptionKey subKey;

    /** The sequence number */
    private int seqNumber;

    /** The authorization Data */
    private AuthorizationData authorizationData;

    // Storage for computed lengths
    

    /**
     * Creates a new instance of Authenticator.
     */
    public Authenticator()
    {
    }


    /**
     * Returns the {@link AuthorizationData}.
     *
     * @return The {@link AuthorizationData}.
     */
    public AuthorizationData getAuthorizationData()
    {
        return authorizationData;
    }


    /**
     * @param authorizationData the authorizationData to set
     */
    public void setAuthorizationData( AuthorizationData authorizationData )
    {
        this.authorizationData = authorizationData;
    }


    /**
     * @return the cksum
     */
    public Checksum getCksum()
    {
        return cksum;
    }


    /**
     * @param cksum the cksum to set
     */
    public void setCksum( Checksum cksum )
    {
        this.cksum = cksum;
    }


    /**
     * @return the cname
     */
    public PrincipalName getCname()
    {
        return cname;
    }


    /**
     * @param cname the cname to set
     */
    public void setCname( PrincipalName cname )
    {
        this.cname = cname;
    }


    /**
     * @return the crealm
     */
    public String getCrealm()
    {
        return crealm;
    }


    /**
     * @param crealm the crealm to set
     */
    public void setCrealm( String crealm )
    {
        this.crealm = crealm;
    }


    /**
     * @return the ctime
     */
    public KerberosTime getCtime()
    {
        return ctime;
    }


    /**
     * @param ctime the ctime to set
     */
    public void setCtime( KerberosTime ctime )
    {
        this.ctime = ctime;
    }


    /**
     * @return the cusec
     */
    public int getCusec()
    {
        return cusec;
    }


    /**
     * @param cusec the cusec to set
     */
    public void setCusec( int cusec )
    {
        this.cusec = cusec;
    }


    /**
     * @return the seqNumber
     */
    public int getSeqNumber()
    {
        return seqNumber;
    }


    /**
     * @param seqNumber the seqNumber to set
     */
    public void setSeqNumber( int seqNumber )
    {
        this.seqNumber = seqNumber;
    }


    /**
     * @return the subKey
     */
    public EncryptionKey getSubKey()
    {
        return subKey;
    }


    /**
     * @param subKey the subKey to set
     */
    public void setSubKey( EncryptionKey subKey )
    {
        this.subKey = subKey;
    }


    /**
     * Returns the version number of the {@link Authenticator}.
     *
     * @return The version number of the {@link Authenticator}.
     */
    public int getVersionNumber()
    {
        return versionNumber;
    }


    /**
     * @param versionNumber the versionNumber to set
     */
    public void setVersionNumber( int versionNumber )
    {
        this.versionNumber = versionNumber;
    }
    
    
    public int computeLength()
    {
        return 0;
    }
    
    
    @Override
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_139, 1 + TLV.getNbBytes( 0 )
                + 0, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Authenticator encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "Authenticator initial value : {}", toString() );
        }

        return buffer;
    }



    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Authenticator : \n" );
        

        return sb.toString();
    }
}
