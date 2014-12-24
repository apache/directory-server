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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.codec.types.AuthorizationType;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.HostAddrType;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.AuthorizationDataEntry;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.flags.TicketFlags;

/**
 * Reading credentials cache according to FCC format by reference the following
 * https://www.gnu.org/software/shishi/manual/html_node/The-Credential-Cache-Binary-File-Format.html
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CacheInputStream extends DataInputStream
{	
	public CacheInputStream( InputStream in )
	{
        super( in );
    }

    public void read(CredentialsCache cache) throws IOException 
    {   
    	int version;
    	List<Tag> tags;
    	PrincipalName principal;
    	Credentials cred; 
    	
        version = readVersion();
        cache.setVersion( version );
        
        if ( version == CredentialsCacheConstants.FCC_FVNO_4 )
        {
            tags = readTag();
        } 
        else
        {
            tags = null;
        }
        cache.setTags( tags );
        
        principal = readPrincipal( version );
        cache.setPrimaryPrincipalName( principal );
        
        while ( available() > 0 )
        {
            cred = readCredentials( version );
            if (cred != null)
            {
                cache.addCredentials( cred );
            }
        }
    }
    
    private int readVersion() throws IOException
    {
        int result = readShort();
        return result;
    }
    
    private List<Tag> readTag() throws IOException
    {
    	int len;
    	int tag;
    	int taglen;
    	int time;
    	int usec;

    	len = readShort();
    	List<Tag> tags = new ArrayList<Tag>();
    	
    	while (len > 0)
    	{
    		tag = readShort();
    		taglen = readShort();
    		switch (tag)
    		{
    			case CredentialsCacheConstants.FCC_TAG_DELTATIME:
    				time = readInt();
    				usec = readInt();
    				tags.add(new Tag(tag, time, usec));
    				break;
    			default:
    				read( new byte[taglen], 0, taglen ); // ignore unknown tag
    		}
    		len = len - (4 + taglen);
    	}
    	
    	return tags;
    }
    
    private PrincipalName readPrincipal( int version ) throws IOException 
    {
        int type, length;
        PrincipalName pname;

        if (version == CredentialsCacheConstants.FCC_FVNO_1)
        {
            type = CredentialsCacheConstants.NT_UNKNOWN;
        }
        else
        {
            type = readInt();
        }
        length = readInt();
        
        if (version == CredentialsCacheConstants.FCC_FVNO_1)
        {
            length--;
        }
        
        String realm = readCountedString();
        
        String[] result = new String[length];
        for ( int i = 0; i < length; i++ )
        {
        	result[i] = readCountedString();
        }
 
        pname = new PrincipalName(result, type);
        if ( isRealm( realm ) )
        {
        	pname.setRealm( realm );
        }

        return pname;
    }

    private String readCountedString() throws IOException {
        int namelength = readInt();
        if ( namelength > CredentialsCacheConstants.MAXNAMELENGTH )
        {
            throw new IOException("Invalid name length in principal name.");
        }
        byte[] bytes = new byte[namelength];
        read( bytes, 0, bytes.length );
        
        return new String( bytes );
    }
    
    /*
	 * Domain style realm names MUST look like domain names: they consist of
     * components separated by periods (.) and they contain neither colons
     * (:) nor slashes (/). When establishing a new realm name based on an 
     * internet domain name it is recommended by convention that the characters 
     * be converted to uppercase.
     */
    private static boolean isRealm( String str )
    {
    	char chr;
    	for ( int i = 0; i < str.length(); i++ )
    	{
    		chr = str.charAt(i);
    		if ( chr != '.' && chr >= 'a' )
    		{
    			return false;
    		}
    	}
    	
    	return true;
    }
    
    private EncryptionKey readKey(int version) throws IOException
    {
        int keyType, keyLen;
        keyType = readShort();
        if ( version == CredentialsCacheConstants.FCC_FVNO_3 )
            readShort();
        // It's not correct with "uint16_t keylen", instead "uint32_t keylen" in keyblock 
        keyLen = readInt();
        byte[] bytes = new byte[keyLen];
        read( bytes, 0, bytes.length );
        
        return new EncryptionKey( EncryptionType.getTypeByValue( keyType ), bytes );
    }

    private KerberosTime[] readKerberosTimes() throws IOException
    {
    	long[] times = readTimes();
    	KerberosTime[] results = new KerberosTime[times.length];
    	KerberosTime ktime;
    	for ( int i = 0; i < times.length; ++i ) 
    	{
    		ktime = times[i] == 0 ? null : new KerberosTime( times[i] );
    		results[i] = ktime;
    	}
    	
    	return results;
    }
    
    private long[] readTimes() throws IOException
    {
        long[] times = new long[4];
        times[0] = (long)readInt() * 1000;
        times[1] = (long)readInt() * 1000;
        times[2] = (long)readInt() * 1000;
        times[3] = (long)readInt() * 1000;
        return times;
    }

    private boolean readskey() throws IOException
    {
        if ( read() == 0 )
        {
            return false;
        }
        
        return true;
    }

    private HostAddress[] readAddr() throws IOException
    {
        int numAddrs, addrType, addrLength;
        numAddrs = readInt();
        if ( numAddrs > 0 )
        {
            HostAddress[] addrs = new HostAddress[numAddrs];
            for ( int i = 0; i < numAddrs; i++ )
            {
                addrType = readShort();
                addrLength = readInt();
                if ( !( addrLength == 4 || addrLength == 16 ) )
                {
                    return null;
                }
                byte[] result = new byte[addrLength];
                for (int j = 0; j < addrLength; j++)
                {
                    result[j] = (byte)readByte();
                }
                addrs[i] = new HostAddress( HostAddrType.getTypeByOrdinal( addrType ), result );
            }
            return addrs;
        }
        
        return null;
    }

    private AuthorizationDataEntry[] readAuth() throws IOException
    {
        int num, adtype, adlength;
        num = readInt();
        if ( num > 0 )
        {
            AuthorizationDataEntry[] auData = new AuthorizationDataEntry[num];
            byte[] data = null;
            for (int i = 0; i < num; i++)
            {
                adtype = readShort();
                adlength = readInt();
                data = new byte[adlength];
                read( data, 0, data.length );
                auData[i] = new AuthorizationDataEntry( AuthorizationType.getTypeByValue( adtype ), data );
            }
            return auData;
        }
        
        return null;
    }

    private byte[] readData() throws IOException
    {
        int length;
        length = readInt();
        if ( length == 0 )
        {
            return null;
        }
        else 
        {
            byte[] bytes = new byte[length];
            read( bytes, 0, length );
            return bytes;
        }
    }

    private int readFlags() throws IOException
    {
        int ticketFlags;
        ticketFlags = readInt();
        return ticketFlags;
    }

    private Credentials readCredentials( int version ) throws IOException
    {
        PrincipalName cpname = readPrincipal(version);
        PrincipalName spname = readPrincipal(version);
        
        if ( cpname == null || spname == null )
        {
        	throw new IOException("Invalid client principal name or service principal name");
        }
        
        EncryptionKey key = readKey(version);

        KerberosTime[] times = readKerberosTimes();
        KerberosTime authtime = times[0];
        KerberosTime starttime = times[1];
        KerberosTime endtime = times[2];
        KerberosTime renewTill = times[3];
        
        boolean skey = readskey();
        
        int flags = readFlags();
        TicketFlags tFlags = new TicketFlags(flags);
        HostAddress addr[] = readAddr();
        HostAddresses addrs = null;
        if (addr != null)
        {
            addrs = new HostAddresses(addr);
        }
        
        AuthorizationDataEntry[] auDataEntries = readAuth();
        AuthorizationData auData = null;
        if (auDataEntries != null)
        {
        	auData = new AuthorizationData();
        	for (AuthorizationDataEntry ade : auDataEntries)
        	{
        		auData.addEntry(ade);
        	}
        }
        
        byte[] ticketData = readData();
        byte[] ticketData2 = readData();

        if ( version != CredentialsCacheConstants.FCC_FVNO_1 && 
        		spname.getNameType().getValue() == CredentialsCacheConstants.NT_UNKNOWN )
        {
        	// skip krb5_ccache_conf_data/fast_avail/krbtgt/REALM@REALM in MIT KRB5
        	return null; 
        }
        
        try 
        {
            return new Credentials(cpname, spname, key, authtime, starttime,
                endtime, renewTill, skey, tFlags, addrs, auData,
                ticketData != null ? KerberosDecoder.decodeTicket( ticketData ) : null,
                ticketData2 != null ? KerberosDecoder.decodeTicket( ticketData2 ) : null);
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
