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

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.server.kerberos.changepwd.io.ChangePasswordDecoder;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordReply;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordRequest;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.codec.KerberosMessageContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncKrbPrivPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.messages.ApRep;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.AsRep;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.EncAsRepPart;
import org.apache.directory.shared.kerberos.messages.KrbPriv;

public abstract class KpasswdDecode
{
    private CipherTextHandler cipherTextHandler = new CipherTextHandler();

    private EncryptionKey clientKey;
    
    private EncryptionKey sessionKey;
    
    private EncryptionKey subSessionKey;
    
    public KpasswdDecode( String principal, String password, EncryptionType eType )
    {
        clientKey = KerberosKeyFactory.string2Key( principal, password, eType );
    }
    
    public void decodeAsRep( byte[] asReppkt ) throws Exception
    {
        ByteBuffer repData = ByteBuffer.wrap( asReppkt );
        
        KerberosMessageContainer kerberosMessageContainer = new KerberosMessageContainer();
        kerberosMessageContainer.setStream( repData );
        kerberosMessageContainer.setGathering( true );
        kerberosMessageContainer.setTCP( false );

        AsRep asReply = ( AsRep ) KerberosDecoder.decode( kerberosMessageContainer, new Asn1Decoder() );

        System.out.println( asReply );
        byte[] decryptedEncAsRepPart = cipherTextHandler.decrypt( clientKey, asReply.getEncPart(), KeyUsage.AS_REP_ENC_PART_WITH_CKEY );
        byte[] tmp = new byte[182];
        System.arraycopy( decryptedEncAsRepPart, 0, tmp, 0, 182 );
        EncAsRepPart encAsRepPart = KerberosDecoder.decodeEncAsRepPart( tmp );
        sessionKey = encAsRepPart.getEncKdcRepPart().getKey();
    }
    
    
    public void decodeApReq( byte[] kpasswdApReqpkt ) throws Exception
    {
        ByteBuffer chngpwdReqData = ByteBuffer.wrap( kpasswdApReqpkt );
        
        ChangePasswordRequest chngPwdReq = ( ChangePasswordRequest ) ChangePasswordDecoder.decode( chngpwdReqData, false );

        ApReq apReq = chngPwdReq.getAuthHeader();
        byte[] decryptedAuthenticator = cipherTextHandler.decrypt( sessionKey, apReq.getAuthenticator(), KeyUsage.AP_REQ_AUTHNT_SESS_KEY );
        Authenticator authenticator = KerberosDecoder.decodeAuthenticator( decryptedAuthenticator );
        subSessionKey = authenticator.getSubKey();
    }

    public void decodeApRep( byte[] kpasswdReplypkt ) throws Exception
    {
        ByteBuffer chngpwdReplyData = ByteBuffer.wrap( kpasswdReplypkt );
        
        ChangePasswordReply chngPwdReply = ( ChangePasswordReply ) ChangePasswordDecoder.decode( chngpwdReplyData, false );

        ApRep apRep = chngPwdReply.getApplicationReply();
        
        KrbPriv krbPriv = chngPwdReply.getPrivateMessage();
        byte[] decryptedKrbPrivPart = cipherTextHandler.decrypt( subSessionKey, krbPriv.getEncPart(), KeyUsage.KRB_PRIV_ENC_PART_CHOSEN_KEY );
        EncKrbPrivPart krbPrivPart = KerberosDecoder.decodeEncKrbPrivPart( decryptedKrbPrivPart );
        System.out.println( krbPrivPart );
    }
    
}
