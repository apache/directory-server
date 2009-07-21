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

package org.apache.directory.studio.dsmlv2;


import java.nio.ByteBuffer;
import java.util.List;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.ControlCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.apache.directory.shared.ldap.codec.abandon.AbandonRequestCodec;
import org.apache.directory.shared.ldap.codec.add.AddRequestCodec;
import org.apache.directory.shared.ldap.codec.add.AddResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.BindRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.BindResponseCodec;
import org.apache.directory.shared.ldap.codec.compare.CompareRequestCodec;
import org.apache.directory.shared.ldap.codec.compare.CompareResponseCodec;
import org.apache.directory.shared.ldap.codec.del.DelRequestCodec;
import org.apache.directory.shared.ldap.codec.del.DelResponseCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedRequestCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedResponseCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyResponseCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequestCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNResponseCodec;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceCodec;
import org.apache.directory.shared.ldap.codec.unbind.UnBindRequestCodec;


/**
 * Decorator class for LDAP Message. This is the top level class, the one 
 * that holds the instance.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class LdapMessageDecorator extends LdapMessageCodec
{
    /** The decorated instance */
    protected LdapMessageCodec instance;


    /**
     * Creates a new instance of LdapMessageDecorator.
     *
     * @param ldapMessage
     *      the message to decorate
     */
    public LdapMessageDecorator( LdapMessageCodec ldapMessage )
    {
        instance = ldapMessage;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#addControl(org.apache.directory.shared.ldap.codec.Control)
     */
    @Override
    public void addControl( ControlCodec control )
    {
        instance.addControl( control );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#computeLength()
     */
    @Override
    public int computeLength()
    {
        return instance.computeLength();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#encode(java.nio.ByteBuffer)
     */
    @Override
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        return instance.encode( buffer );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getAbandonRequest()
     */
    @Override
    public AbandonRequestCodec getAbandonRequest()
    {
        return instance.getAbandonRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getAddRequest()
     */
    @Override
    public AddRequestCodec getAddRequest()
    {
        return instance.getAddRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getAddResponse()
     */
    @Override
    public AddResponseCodec getAddResponse()
    {
        return instance.getAddResponse();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getBindRequest()
     */
    @Override
    public BindRequestCodec getBindRequest()
    {
        return instance.getBindRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getBindResponse()
     */
    @Override
    public BindResponseCodec getBindResponse()
    {
        return instance.getBindResponse();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getCompareRequest()
     */
    @Override
    public CompareRequestCodec getCompareRequest()
    {
        return instance.getCompareRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getCompareResponse()
     */
    @Override
    public CompareResponseCodec getCompareResponse()
    {
        return instance.getCompareResponse();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getControls()
     */
    @Override
    public List<ControlCodec> getControls()
    {
        return instance.getControls();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getControls(int)
     */
    @Override
    public ControlCodec getControls( int i )
    {
        return instance.getControls( i );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getCurrentControl()
     */
    @Override
    public ControlCodec getCurrentControl()
    {
        return instance.getCurrentControl();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getDelRequest()
     */
    @Override
    public DelRequestCodec getDelRequest()
    {
        return instance.getDelRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getDelResponse()
     */
    @Override
    public DelResponseCodec getDelResponse()
    {
        return instance.getDelResponse();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getExtendedRequest()
     */
    @Override
    public ExtendedRequestCodec getExtendedRequest()
    {
        return instance.getExtendedRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getExtendedResponse()
     */
    @Override
    public ExtendedResponseCodec getExtendedResponse()
    {
        return instance.getExtendedResponse();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getLdapResponse()
     */
    @Override
    public LdapResponseCodec getLdapResponse()
    {
        return instance.getLdapResponse();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getMessageId()
     */
    @Override
    public int getMessageId()
    {
        return instance.getMessageId();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getMessageType()
     */
    @Override
    public int getMessageType()
    {
        return instance.getMessageType();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getMessageTypeName()
     */
    @Override
    public String getMessageTypeName()
    {
        return instance.getMessageTypeName();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getModifyDNRequest()
     */
    @Override
    public ModifyDNRequestCodec getModifyDNRequest()
    {
        return instance.getModifyDNRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getModifyDNResponse()
     */
    @Override
    public ModifyDNResponseCodec getModifyDNResponse()
    {
        return instance.getModifyDNResponse();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getModifyRequest()
     */
    @Override
    public ModifyRequestCodec getModifyRequest()
    {
        return instance.getModifyRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getModifyResponse()
     */
    @Override
    public ModifyResponseCodec getModifyResponse()
    {
        return instance.getModifyResponse();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getSearchRequest()
     */
    @Override
    public SearchRequestCodec getSearchRequest()
    {
        return instance.getSearchRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getSearchResultDone()
     */
    @Override
    public SearchResultDoneCodec getSearchResultDone()
    {
        return instance.getSearchResultDone();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getSearchResultEntry()
     */
    @Override
    public SearchResultEntryCodec getSearchResultEntry()
    {
        return instance.getSearchResultEntry();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getSearchResultReference()
     */
    @Override
    public SearchResultReferenceCodec getSearchResultReference()
    {
        return instance.getSearchResultReference();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#getUnBindRequest()
     */
    @Override
    public UnBindRequestCodec getUnBindRequest()
    {
        return instance.getUnBindRequest();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#setMessageId(int)
     */
    @Override
    public void setMessageId( int messageId )
    {
        instance.setMessageId( messageId );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#setProtocolOP(org.apache.directory.shared.asn1.Asn1Object)
     */
    @Override
    public void setProtocolOP( Asn1Object protocolOp )
    {
        instance.setProtocolOP( protocolOp );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.codec.LdapMessageCodec#toString()
     */
    @Override
    public String toString()
    {
        return instance.toString();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.asn1.Asn1Object#addLength(int)
     */
    @Override
    public void addLength( int length ) throws DecoderException
    {
        instance.addLength( length );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.asn1.Asn1Object#getCurrentLength()
     */
    @Override
    public int getCurrentLength()
    {
        return instance.getCurrentLength();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.asn1.Asn1Object#getExpectedLength()
     */
    @Override
    public int getExpectedLength()
    {
        return instance.getExpectedLength();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.asn1.Asn1Object#getParent()
     */
    @Override
    public AbstractAsn1Object getParent()
    {
        return instance.getParent();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.asn1.Asn1Object#setCurrentLength(int)
     */
    @Override
    public void setCurrentLength( int currentLength )
    {
        instance.setCurrentLength( currentLength );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.asn1.Asn1Object#setExpectedLength(int)
     */
    @Override
    public void setExpectedLength( int expectedLength )
    {
        instance.setExpectedLength( expectedLength );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.asn1.Asn1Object#setParent(org.apache.directory.shared.asn1.Asn1Object)
     */
    public void setParent( AbstractAsn1Object parent )
    {
        instance.setParent( parent );
    }
}
