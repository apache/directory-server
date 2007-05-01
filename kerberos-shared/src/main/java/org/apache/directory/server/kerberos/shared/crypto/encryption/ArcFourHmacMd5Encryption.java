package org.apache.directory.server.kerberos.shared.crypto.encryption;


import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ArcFourHmacMd5Encryption extends EncryptionEngine
{
    public EncryptionType getEncryptionType()
    {
        return EncryptionType.RC4_HMAC;
    }


    public int getChecksumLength()
    {
        return 16;
    }


    public int getConfounderLength()
    {
        return 8;
    }


    public byte[] getDecryptedData( EncryptionKey key, EncryptedData data ) throws KerberosException
    {
        return data.getCipherText();
    }


    public EncryptedData getEncryptedData( EncryptionKey key, byte[] plainText )
    {
        return new EncryptedData( getEncryptionType(), key.getKeyVersion(), plainText );
    }


    public byte[] encrypt( byte[] plainText, byte[] keyBytes )
    {
        return processCipher( true, plainText, keyBytes );
    }


    public byte[] decrypt( byte[] cipherText, byte[] keyBytes )
    {
        return processCipher( false, cipherText, keyBytes );
    }


    public byte[] calculateChecksum( byte[] data, byte[] key )
    {
        try
        {
            Mac digester = Mac.getInstance( "HmacMD5" );
            return digester.doFinal( data );
        }
        catch ( NoSuchAlgorithmException nsae )
        {
            return null;
        }
    }


    private byte[] processCipher( boolean isEncrypt, byte[] data, byte[] keyBytes )
    {
        try
        {
            Cipher cipher = Cipher.getInstance( "ARCFOUR" );
            SecretKey key = new SecretKeySpec( keyBytes, "ARCFOUR" );

            if ( isEncrypt )
            {
                cipher.init( Cipher.ENCRYPT_MODE, key );
            }
            else
            {
                cipher.init( Cipher.DECRYPT_MODE, key );
            }

            return cipher.doFinal( data );
        }
        catch ( GeneralSecurityException nsae )
        {
            nsae.printStackTrace();
            return null;
        }
    }
}
