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
package org.apache.directory.shared.kerberos.messages;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.Asn1Object;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.components.PrincipalName;


/**
 * Change password data structure
 * 
 * ChangePasswdData ::=  SEQUENCE {
 *       newpasswd[0]   OCTET STRING,
 *       targname[1]    PrincipalName OPTIONAL,
 *       targrealm[2]   Realm OPTIONAL
 *     }
 *     
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswdData implements Asn1Object
{

    /** the new password */
    private byte[] newPasswd;

    /** principal name of the client */
    private PrincipalName targName;

    /** name of client's realm */
    private String targRealm;

    private int newPasswdLen;
    private int targNameLen;
    private int targRealmLen;
    private int seqLen;


    public ChangePasswdData()
    {
    }


    /**
     * Compute the ChangePasswdData length
     * <pre>
     * ChangePasswdData :
     *
     * 0x30 L1 ChangePasswdData sequence
     *  |
     *  +--> 0xA0 L2 newPasswd tag
     *  |     |
     *  |     +--> 0x04 L2-1 newPasswd (Octet string)
     *  |
     *  +--> 0xA1 L3 targName tag
     *  |     |
     *  |     +--> 0x30 L3-1 targName (PrincipalName)
     *  |
     *  +--> 0xA2 L4 targRealm tag
     *        |
     *        +--> 0x1B L4-1 targRealm (KerberosString)
     */
    @Override
    public int computeLength()
    {
        newPasswdLen = 1 + TLV.getNbBytes( newPasswd.length ) + newPasswd.length;

        seqLen = 1 + TLV.getNbBytes( newPasswdLen ) + newPasswdLen;

        if ( targName != null )
        {
            targNameLen = targName.computeLength();
            seqLen += 1 + TLV.getNbBytes( targNameLen ) + targNameLen;
        }

        if ( targRealm != null )
        {
            targRealmLen = Strings.getBytesUtf8( targRealm ).length;
            targRealmLen = 1 + TLV.getNbBytes( targRealmLen ) + targRealmLen;
            seqLen += 1 + TLV.getNbBytes( targRealmLen ) + targRealmLen;
        }

        return 1 + TLV.getNbBytes( seqLen ) + seqLen;
    }


    @Override
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            buffer = ByteBuffer.allocate( computeLength() );
        }

        // ChangePasswdData
        buffer.put( UniversalTag.SEQUENCE.getValue() );
        buffer.put( BerValue.getBytes( seqLen ) );

        // newpasswd
        buffer.put( ( byte ) KerberosConstants.CHNGPWD_NEWPWD_TAG );
        buffer.put( BerValue.getBytes( newPasswdLen ) );
        BerValue.encode( buffer, newPasswd );

        if ( targName != null )
        {
            buffer.put( ( byte ) KerberosConstants.CHNGPWD_TARGNAME_TAG );
            buffer.put( BerValue.getBytes( targNameLen ) );

            targName.encode( buffer );
        }

        if ( targRealm != null )
        {
            buffer.put( ( byte ) KerberosConstants.CHNGPWD_TARGREALM_TAG );
            buffer.put( BerValue.getBytes( targRealmLen ) );
            buffer.put( UniversalTag.GENERAL_STRING.getValue() );
            buffer.put( BerValue.getBytes( targRealmLen - 2 ) );
            buffer.put( Strings.getBytesUtf8( targRealm ) );
        }

        return buffer;
    }


    public byte[] getNewPasswd()
    {
        return newPasswd;
    }


    public void setNewPasswd( byte[] newPasswd )
    {
        this.newPasswd = newPasswd;
    }


    public PrincipalName getTargName()
    {
        return targName;
    }


    public void setTargName( PrincipalName targName )
    {
        this.targName = targName;
    }


    public String getTargRealm()
    {
        return targRealm;
    }


    public void setTargRealm( String targRealm )
    {
        this.targRealm = targRealm;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "ChangePasswdData : \n" );

        sb.append( "    newPasswd : " ).append( Strings.utf8ToString( newPasswd ) ).append( '\n' );
        sb.append( "    targName : " ).append( targName ).append( '\n' );
        sb.append( "    targRealm : " ).append( targRealm ).append( '\n' );

        return sb.toString();
    }
}
