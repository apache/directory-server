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

package org.apache.directory.server.core.shared.authn;


import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.directory.server.core.shared.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.shared.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.util.Base64;
import org.apache.directory.shared.util.DateUtils;
import org.apache.directory.shared.util.Strings;
import org.apache.directory.shared.util.UnixCrypt;


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

                String algorithm = Strings.toLowerCase( new String( credentials, 1, pos - 1 ) );

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
     * @see #createStoragePassword(byte[], LdapSecurityConstants)
     */
    public static byte[] createStoragePassword( String credentials, LdapSecurityConstants algorithm )
    {
        return createStoragePassword( Strings.getBytesUtf8(credentials), algorithm );
    }
    
    
    /**
     * create a hashed password in a format that can be stored in the server.
     * If the specified algorithm requires a salt then a random salt of 8 byte size is used
     *  
     * @param credentials the plain text password
     * @param algorithm the hashing algorithm to be applied
     * @return the password after hashing with the given algorithm 
     */
    public static byte[] createStoragePassword( byte[] credentials, LdapSecurityConstants algorithm )
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
                salt = new byte[2];
                SecureRandom sr = new SecureRandom();
                int i1 = sr.nextInt( 64 );
                int i2 = sr.nextInt( 64 );
                
                salt[0] = ( byte ) ( i1 < 12 ? ( i1 + '.' ) : i1 < 38 ? ( i1 + 'A' - 12 ) : ( i1 + 'a' - 38 ) );
                salt[1] = ( byte ) ( i2 < 12 ? ( i2 + '.' ) : i2 < 38 ? ( i2 + 'A' - 12 ) : ( i2 + 'a' - 38 ) );
                break;
                
            default:
                salt = null;
        }
        
        byte[] hashedPassword = encryptPassword( credentials, algorithm, salt );
        StringBuffer sb = new StringBuffer();

        if ( algorithm != null )
        {
            sb.append( '{' ).append( algorithm.getName().toUpperCase() ).append( '}' );

            if ( algorithm == LdapSecurityConstants.HASH_METHOD_CRYPT )
            {
                sb.append( Strings.utf8ToString(salt) );
                sb.append( Strings.utf8ToString(hashedPassword) );
            }
            else if ( salt != null )
            {
                byte[] hashedPasswordWithSaltBytes = new byte[hashedPassword.length + salt.length];
                merge( hashedPasswordWithSaltBytes, hashedPassword, salt );
                sb.append( String.valueOf( Base64.encode( hashedPasswordWithSaltBytes ) ) );
            }
            else
            {
                sb.append( String.valueOf( Base64.encode(hashedPassword) ) );
            }
        }
        else
        {
            sb.append( Strings.utf8ToString(hashedPassword) );
        }
        
        return Strings.getBytesUtf8(sb.toString());
    }
    

    /**
     * 
     * Compare the credentials.
     * We have at least 6 algorithms to encrypt the password :
     * <ul>
     * <li>- SHA</li>
     * <li>- SSHA (salted SHA)</li>
     * <li>- SHA-2(256, 384 and 512 and their salted versions)</li>
     * <li>- MD5</li>
     * <li>- SMD5 (slated MD5)</li>
     * <li>- crypt (unix crypt)</li>
     * <li>- plain text, ie no encryption.</li>
     * </ul>
     * <p>
     *  If we get an encrypted password, it is prefixed by the used algorithm, between
     *  brackets : {SSHA}password ...
     *  </p>
     *  If the password is using SSHA, SMD5 or crypt, some 'salt' is added to the password :
     *  <ul>
     *  <li>- length(password) - 20, starting at 21th position for SSHA</li>
     *  <li>- length(password) - 16, starting at 16th position for SMD5</li>
     *  <li>- length(password) - 2, starting at 3rd position for crypt</li>
     *  </ul>
     *  <p>
     *  For (S)SHA, SHA-256 and (S)MD5, we have to transform the password from Base64 encoded text
     *  to a byte[] before comparing the password with the stored one.
     *  </p>
     *  <p>
     *  For crypt, we only have to remove the salt.
     *  </p>
     *  <p>
     *  At the end, we use the digest() method for (S)SHA and (S)MD5, the crypt() method for
     *  the CRYPT algorithm and a straight comparison for PLAIN TEXT passwords.
     *  </p>
     *  <p>
     *  The stored password is always using the unsalted form, and is stored as a bytes array.
     *  </p>
     *
     * @param receivedCredentials the credentials provided by user
     * @param storedCredentials the credentials stored in the server
     * @return true if they are equal, false otherwise
     */
    public static boolean compareCredentials( byte[] receivedCredentials, byte[] storedCredentials )
    {
        LdapSecurityConstants algorithm = findAlgorithm( storedCredentials );
        
        if ( algorithm != null )
        {
            EncryptionMethod encryptionMethod = new EncryptionMethod( algorithm, null );
            
            // Let's get the encrypted part of the stored password
            // We should just keep the password, excluding the algorithm
            // and the salt, if any.
            // But we should also get the algorithm and salt to
            // be able to encrypt the submitted user password in the next step
            byte[] encryptedStored = PasswordUtil.splitCredentials( storedCredentials, encryptionMethod );
            
            // Reuse the saltedPassword informations to construct the encrypted
            // password given by the user.
            byte[] userPassword = PasswordUtil.encryptPassword( receivedCredentials, encryptionMethod.getAlgorithm(), encryptionMethod.getSalt() );
            
            // Now, compare the two passwords.
            return Arrays.equals( userPassword, encryptedStored );
        }
        else
        {
            return Arrays.equals( storedCredentials, receivedCredentials );
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
                String saltWithCrypted = UnixCrypt.crypt( Strings.utf8ToString(credentials), Strings
                    .utf8ToString(salt) );
                String crypted = saltWithCrypted.substring( 2 );

                return Strings.getBytesUtf8(crypted);

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

        Date expiryDate = DateUtils.getDate( DateUtils.getGeneralizedTime( time ) );
        Date now = DateUtils.getDate( DateUtils.getGeneralizedTime() );

        boolean expired = false;

        if ( expiryDate.equals( now ) || expiryDate.before( now ) )
        {
            expired = true;
        }

        return expired;
    }


    /**
     * purges failure timestamps which are older than the configured interval
     * (section 7.6 in the draft)
     */
    public static void purgeFailureTimes( PasswordPolicyConfiguration config, Attribute pwdFailTimeAt )
    {
        long interval = config.getPwdFailureCountInterval();

        if ( interval == 0 )
        {
            return;
        }

        interval *= 1000;

        long currentTime = DateUtils.getDate( DateUtils.getGeneralizedTime() ).getTime();
        List<Value<?>> valList = new ArrayList<Value<?>>();

        for ( Value<?> value : pwdFailTimeAt )
        {
            String failureTime = value.getString();
            long time = DateUtils.getDate( failureTime ).getTime();
            time += interval;

            if ( currentTime > time )
            {
                valList.add( value );
            }
        }

        for ( Value<?> val : valList )
        {
            pwdFailTimeAt.remove( val );
        }
    }
}
