/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.authn;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.shared.ldap.constants.LdapSecurityConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.ldap.util.UnixCrypt;

/**
 * A utility class containing methods related to processing passwords.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PasswordUtil
{
    /**
     * Get the algorithm from the stored password. 
     * It can be found on the beginning of the stored password, between 
     * curly brackets.
     * @param credentials the credentials of the user
     * @return the name of the algorithm to use
     */
    public static LdapSecurityConstants findAlgorithm( byte[] credentials )
    {
        if ( ( credentials == null ) || ( credentials.length == 0 ) )
        {
            return null;
        }

        if ( credentials[0] == '{' )
        {
            // get the algorithm
            int pos = 1;

            while ( pos < credentials.length )
            {
                if ( credentials[pos] == '}' )
                {
                    break;
                }

                pos++;
            }

            if ( pos < credentials.length )
            {
                if ( pos == 1 )
                {
                    // We don't have an algorithm : return the credentials as is
                    return null;
                }

                String algorithm = new String( credentials, 1, pos - 1 ).toLowerCase();

                return LdapSecurityConstants.getAlgorithm( algorithm );
            }
            else
            {
                // We don't have an algorithm
                return null;
            }
        }
        else
        {
            // No '{algo}' part
            return null;
        }
    }

    
    /**
     * encrypts the given credentials based on the algorithm name and optional salt
     *
     * @param credentials the credentials to be encrypted
     * @param algorithm the algorithm to be used for encrypting the credentials
     * @param salt value to be used as salt (optional)
     * @return the encrypted credentials
     */
    public static byte[] encryptPassword( byte[] credentials, LdapSecurityConstants algorithm, byte[] salt )
    {
        switch ( algorithm )
        {
            case HASH_METHOD_SHA:
            case HASH_METHOD_SSHA:
                return digest( LdapSecurityConstants.HASH_METHOD_SHA, credentials, salt );

            case HASH_METHOD_SHA256:
                return digest( LdapSecurityConstants.HASH_METHOD_SHA256, credentials, salt );
                
            case HASH_METHOD_MD5:
            case HASH_METHOD_SMD5:
                return digest( LdapSecurityConstants.HASH_METHOD_MD5, credentials, salt );

            case HASH_METHOD_CRYPT:
                if ( salt == null )
                {
                    salt = new byte[2];
                    SecureRandom sr = new SecureRandom();
                    int i1 = sr.nextInt( 64 );
                    int i2 = sr.nextInt( 64 );

                    salt[0] = ( byte ) ( i1 < 12 ? ( i1 + '.' ) : i1 < 38 ? ( i1 + 'A' - 12 ) : ( i1 + 'a' - 38 ) );
                    salt[1] = ( byte ) ( i2 < 12 ? ( i2 + '.' ) : i2 < 38 ? ( i2 + 'A' - 12 ) : ( i2 + 'a' - 38 ) );
                }

                String saltWithCrypted = UnixCrypt.crypt( StringTools.utf8ToString( credentials ), StringTools
                    .utf8ToString( salt ) );
                String crypted = saltWithCrypted.substring( 2 );

                return StringTools.getBytesUtf8( crypted );

            default:
                return credentials;
        }
    }


    /**
     * Compute the hashed password given an algorithm, the credentials and 
     * an optional salt.
     *
     * @param algorithm the algorithm to use
     * @param password the credentials
     * @param salt the optional salt
     * @return the digested credentials
     */
    private static byte[] digest( LdapSecurityConstants algorithm, byte[] password, byte[] salt )
    {
        MessageDigest digest;

        try
        {
            digest = MessageDigest.getInstance( algorithm.getName() );
        }
        catch ( NoSuchAlgorithmException e1 )
        {
            return null;
        }

        if ( salt != null )
        {
            digest.update( password );
            digest.update( salt );
            return digest.digest();
        }
        else
        {
            return digest.digest( password );
        }
    }

    
    /**
     * checks if the given password's change time is older than the max age 
     *
     * @param pwdChangedZtime time when the password was last changed
     * @param pwdMaxAgeSec the max age value in seconds
     * @return true if expired, false otherwise
     */
    public static boolean isPwdExpired( String pwdChangedZtime, int pwdMaxAgeSec )
    {
        Date pwdChangeDate = DateUtils.getDate( pwdChangedZtime );

        long time = pwdMaxAgeSec * 1000;
        time += pwdChangeDate.getTime();
        
        Date expiryDate = new Date( time );
        Date now = new Date();
        
        boolean expired = false;
        
        if( expiryDate.equals( now ) || expiryDate.after( now ) )
        {
           expired = true;
        }
        
        return expired;
    }
    
    
    /**
     * purges failure timestamps which are older than the configured interval
     * (section 7.6 in the draft)
     */
    public static void purgeFailureTimes( PasswordPolicyConfiguration config, EntryAttribute pwdFailTimeAt )
    {
        long interval = config.getPwdFailureCountInterval();

        if( interval == 0 )
        {
            return;
        }
        
        Iterator<Value<?>> itr = pwdFailTimeAt.getAll();
        interval *= 1000;
        
        long currentTime = System.currentTimeMillis();
        List<Value<?>> valList = new ArrayList<Value<?>>();
        
        while( itr.hasNext() )
        {
            Value<?> val = itr.next();
            String failureTime = val.getString();
            long time = DateUtils.getDate( failureTime ).getTime();
            time += interval;
            
            if(  currentTime > time )
            {
                valList.add( val );
            }
        }
        
        for( Value<?> val : valList )
        {
            pwdFailTimeAt.remove( val );
        }
    }

}
