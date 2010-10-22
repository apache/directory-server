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


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.shared.ldap.constants.LdapSecurityConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.util.Base64;
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

    /** The SHA1 hash length */
    public static final int SHA1_LENGTH = 20;

    /** The SHA256 hash length */
    public static final int SHA256_LENGTH = 32;

    /** The SHA384 hash length */
    public static final int SHA384_LENGTH = 48;

    /** The SHA512 hash length */
    public static final int SHA512_LENGTH = 64;

    /** The MD5 hash length */
    public static final int MD5_LENGTH = 16;


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


    public static byte[] createStoragePassword( String credentials, LdapSecurityConstants algorithm )
    {
        byte[] salt;
        
        switch( algorithm )
        {
            case HASH_METHOD_SSHA:
            case HASH_METHOD_SSHA256:
            case HASH_METHOD_SSHA384:
            case HASH_METHOD_SSHA512:
            case HASH_METHOD_SMD5:
                salt = new byte[8]; // we use 8 byte salt always except for "crypt" which needs 2 byte salt
                new SecureRandom().nextBytes( salt );
                break;
                
            case HASH_METHOD_CRYPT:
                salt = null; // we calculate this salt in encryptPassword() method
                
            default:
                salt = null;
        }
        
        byte[] hashedPassword = encryptPassword( StringTools.getBytesUtf8( credentials ), algorithm, salt );
        StringBuffer sb = new StringBuffer();

        if ( algorithm != null )
        {
            sb.append( '{' ).append( algorithm.getName().toUpperCase() ).append( '}' );

            if ( algorithm == LdapSecurityConstants.HASH_METHOD_CRYPT )
            {
                sb.append( StringTools.utf8ToString( salt ) );
                sb.append( StringTools.utf8ToString( hashedPassword ) );
            }
            else if ( salt != null )
            {
                byte[] hashedPasswordWithSaltBytes = new byte[hashedPassword.length + salt.length];
                merge( hashedPasswordWithSaltBytes, hashedPassword, salt );
                sb.append( String.valueOf( Base64.encode( hashedPasswordWithSaltBytes ) ) );
            }
            else
            {
                sb.append( String.valueOf( Base64.encode( hashedPassword ) ) );
            }
        }
        else
        {
            sb.append( StringTools.utf8ToString( hashedPassword ) );
        }
        
        return StringTools.getBytesUtf8( sb.toString() );
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
            case HASH_METHOD_SSHA256:
                return digest( LdapSecurityConstants.HASH_METHOD_SHA256, credentials, salt );

            case HASH_METHOD_SHA384:
            case HASH_METHOD_SSHA384:
                return digest( LdapSecurityConstants.HASH_METHOD_SHA384, credentials, salt );

            case HASH_METHOD_SHA512:
            case HASH_METHOD_SSHA512:
                return digest( LdapSecurityConstants.HASH_METHOD_SHA512, credentials, salt );

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
     * Decompose the stored password in an algorithm, an eventual salt
     * and the password itself.
     *
     * If the algorithm is SHA, SSHA, MD5 or SMD5, the part following the algorithm
     * is base64 encoded
     *
     * @param encryptionMethod The structure to feed
     * @return The password
     * @param credentials the credentials to split
     */
    public static byte[] splitCredentials( byte[] credentials, EncryptionMethod encryptionMethod )
    {
        int algoLength = encryptionMethod.getAlgorithm().getName().length() + 2;

        int hashLen = 0;
        
        switch ( encryptionMethod.getAlgorithm() )
        {
            case HASH_METHOD_MD5:
            case HASH_METHOD_SHA:
                try
                {
                    // We just have the password just after the algorithm, base64 encoded.
                    // Just decode the password and return it.
                    return Base64
                        .decode( new String( credentials, algoLength, credentials.length - algoLength, "UTF-8" )
                            .toCharArray() );
                }
                catch ( UnsupportedEncodingException uee )
                {
                    // do nothing
                    return credentials;
                }

            case HASH_METHOD_SMD5:
                try
                {
                    // The password is associated with a salt. Decompose it
                    // in two parts, after having decoded the password.
                    // The salt will be stored into the EncryptionMethod structure
                    // The salt is at the end of the credentials, and is 8 bytes long
                    byte[] passwordAndSalt = Base64.decode( new String( credentials, algoLength, credentials.length
                        - algoLength, "UTF-8" ).toCharArray() );

                    int saltLength = passwordAndSalt.length - MD5_LENGTH;
                    encryptionMethod.setSalt( new byte[saltLength] );
                    byte[] password = new byte[MD5_LENGTH];
                    split( passwordAndSalt, 0, password, encryptionMethod.getSalt() );

                    return password;
                }
                catch ( UnsupportedEncodingException uee )
                {
                    // do nothing
                    return credentials;
                }

            case HASH_METHOD_SSHA:
                hashLen = SHA1_LENGTH;
            
            case HASH_METHOD_SHA256:
            case HASH_METHOD_SSHA256:
                if ( hashLen == 0 )
                {
                    hashLen = SHA256_LENGTH;
                }
            
            case HASH_METHOD_SHA384:
            case HASH_METHOD_SSHA384:
                if ( hashLen == 0 )
                {
                    hashLen = SHA384_LENGTH;
                }
                
            case HASH_METHOD_SHA512:
            case HASH_METHOD_SSHA512:
                if ( hashLen == 0 )
                {
                    hashLen = SHA512_LENGTH;
                }
                
                try
                {
                    // The password is associated with a salt. Decompose it
                    // in two parts, after having decoded the password.
                    // The salt will be stored into the EncryptionMethod structure
                    // The salt is at the end of the credentials, and is 8 bytes long
                    byte[] passwordAndSalt = Base64.decode( new String( credentials, algoLength, credentials.length
                        - algoLength, "UTF-8" ).toCharArray() );

                    int saltLength = passwordAndSalt.length - hashLen;
                    encryptionMethod.setSalt( new byte[saltLength] );
                    byte[] password = new byte[hashLen];
                    split( passwordAndSalt, 0, password, encryptionMethod.getSalt() );

                    return password;
                }
                catch ( UnsupportedEncodingException uee )
                {
                    // do nothing
                    return credentials;
                }

            case HASH_METHOD_CRYPT:
                // The password is associated with a salt. Decompose it
                // in two parts, storing the salt into the EncryptionMethod structure.
                // The salt comes first, not like for SSHA and SMD5, and is 2 bytes long
                encryptionMethod.setSalt( new byte[2] );
                byte[] password = new byte[credentials.length - encryptionMethod.getSalt().length - algoLength];
                split( credentials, algoLength, encryptionMethod.getSalt(), password );

                return password;

            default:
                // unknown method
                return credentials;

        }
    }


    private static void split( byte[] all, int offset, byte[] left, byte[] right )
    {
        System.arraycopy( all, offset, left, 0, left.length );
        System.arraycopy( all, offset + left.length, right, 0, right.length );
    }


    private static void merge( byte[] all, byte[] left, byte[] right )
    {
        System.arraycopy( left, 0, all, 0, left.length );
        System.arraycopy( right, 0, all, left.length, right.length );
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

        if ( expiryDate.equals( now ) || expiryDate.after( now ) )
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

        if ( interval == 0 )
        {
            return;
        }

        Iterator<Value<?>> itr = pwdFailTimeAt.getAll();
        interval *= 1000;

        long currentTime = System.currentTimeMillis();
        List<Value<?>> valList = new ArrayList<Value<?>>();

        while ( itr.hasNext() )
        {
            Value<?> val = itr.next();
            String failureTime = val.getString();
            long time = DateUtils.getDate( failureTime ).getTime();
            time += interval;

            if ( currentTime > time )
            {
                valList.add( val );
            }
        }

        for ( Value<?> val : valList )
        {
            pwdFailTimeAt.remove( val );
        }
    }
}
