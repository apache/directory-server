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
package org.apache.directory.kerberos.credentials.cache;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.KerberosEncoder;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.AuthorizationDataEntry;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.messages.Ticket;


/**
 * Writing credentials cache according to FCC format by reference the following
 * https://www.gnu.org/software/shishi/manual/html_node/The-Credential-Cache-Binary-File-Format.html
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CacheOutputStream extends DataOutputStream
{

    public CacheOutputStream( OutputStream out )
    {
        super( out );
    }


    public void write( CredentialsCache credCache ) throws IOException
    {
        /**
         * Currently we always write using this version to limit the test effort.
         * This version seems to be the easiest to be compatible with MIT tools.
         * In future we might allow to specify the format version to write if necessary. 
         */
        int writeVersion = CredentialsCacheConstants.FCC_FVNO_3;

        writeVersion( writeVersion );

        if ( writeVersion == CredentialsCacheConstants.FCC_FVNO_4 )
        {
            writeTags( credCache.getTags() );
        }

        writePrincipal( credCache.getPrimaryPrincipalName(), writeVersion );

        List<Credentials> credentialsList = credCache.getCredsList();
        if ( credentialsList != null )
        {
            for ( Credentials cred : credentialsList )
            {
                writeCredentials( cred, writeVersion );
            }
        }
    }


    private void writeVersion( int version ) throws IOException
    {
        writeShort( version );
    }


    private void writeTags( List<Tag> tags ) throws IOException
    {
        int length = 0;
        if ( tags != null )
        {
            for ( Tag tag : tags )
            {
                if ( tag.tag != CredentialsCacheConstants.FCC_TAG_DELTATIME )
                {
                    continue;
                }
                length += tag.length;
            }
        }

        writeShort( length );

        if ( tags != null )
        {
            for ( Tag tag : tags )
            {
                if ( tag.tag != CredentialsCacheConstants.FCC_TAG_DELTATIME )
                {
                    continue;
                }
                writeTag( tag );
            }
        }
    }


    private void writeTag( Tag tag ) throws IOException
    {
        writeShort( tag.tag );
        writeShort( tag.length );
        writeInt( tag.time );
        writeInt( tag.usec );
    }


    private void writePrincipal( PrincipalName pname, int version ) throws IOException
    {
        int num = pname.getNames().size();

        if ( version != CredentialsCacheConstants.FCC_FVNO_1 )
        {
            writeInt( pname.getNameType().getValue() );
        }
        else
        {
            num++;
        }

        writeInt( num );

        if ( pname.getRealm() != null )
        {
            byte[] realmBytes = null;
            realmBytes = pname.getRealm().getBytes();
            writeInt( realmBytes.length );
            write( realmBytes );
        }
        else
        {
            writeInt( 0 );
        }

        byte[] bytes = null;
        for ( int i = 0; i < pname.getNames().size(); i++ )
        {
            bytes = pname.getNames().get( i ).getBytes();
            writeInt( bytes.length );
            write( bytes );
        }
    }


    private void writeCredentials( Credentials creds, int version ) throws IOException
    {
        writePrincipal( creds.getClientName(), version );
        writePrincipal( creds.getServerName(), version );
        writeKey( creds.getKey(), version );

        writeKerberosTime( creds.getAuthTime() );
        writeKerberosTime( creds.getStartTime() );
        writeKerberosTime( creds.getEndTime() );
        writeKerberosTime( creds.getRenewTill() );

        writeByte( creds.isEncInSKey() ? 1 : 0 );

        writeInt( creds.getFlags().getIntValue() );

        writeAddrs( creds.getClientAddresses() );
        writeAuth( creds.getAuthzData() );

        writeTicket( creds.getTicket() );
        writeTicket( creds.getSecondTicket() );
    }


    private void writeKerberosTime( KerberosTime ktime ) throws IOException
    {
        int time = 0;
        if ( ktime != null )
        {
            time = ( int ) ( ktime.getTime() / 1000 );
        }
        writeInt( time );
    }


    private void writeKey( EncryptionKey key, int version ) throws IOException
    {
        writeShort( key.getKeyType().getValue() );
        if ( version == CredentialsCacheConstants.FCC_FVNO_3 )
        {
            writeShort( key.getKeyType().getValue() );
        }
        // It's not correct with "uint16_t keylen", instead "uint32_t keylen" in keyblock    	
        writeInt( key.getKeyValue().length );
        write( key.getKeyValue() );
    }


    private void writeAddrs( HostAddresses addresses ) throws IOException
    {
        if ( addresses == null )
        {
            writeInt( 0 );
        }
        else
        {
            HostAddress[] addrs = addresses.getAddresses();
            write( addrs.length );
            for ( int i = 0; i < addrs.length; i++ )
            {
                write( addrs[i].getAddrType().getValue() );
                write( addrs[i].getAddress().length );
                write( addrs[i].getAddress(), 0,
                    addrs[i].getAddress().length );
            }
        }
    }


    private void writeAuth( AuthorizationData authData ) throws IOException
    {
        if ( authData == null )
        {
            writeInt( 0 );
        }
        else
        {
            for ( AuthorizationDataEntry ade : authData.getAuthorizationData() )
            {
                write( ade.getAdType().getValue() );
                write( ade.getAdData().length );
                write( ade.getAdData() );
            }
        }
    }


    private void writeTicket( Ticket t ) throws IOException
    {
        if ( t == null )
        {
            writeInt( 0 );
        }
        else
        {
            byte[] bytes = KerberosEncoder.encode( t, false ).array();
            writeInt( bytes.length );
            write( bytes );
        }
    }
}
