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
package org.apache.directory.shared.ldap.codec;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.InvalidNameException;

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.codec.abandon.AbandonRequestCodec;
import org.apache.directory.shared.ldap.codec.add.AddRequestCodec;
import org.apache.directory.shared.ldap.codec.add.AddResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.BindRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.BindResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.SaslCredentials;
import org.apache.directory.shared.ldap.codec.bind.SimpleAuthentication;
import org.apache.directory.shared.ldap.codec.compare.CompareRequestCodec;
import org.apache.directory.shared.ldap.codec.compare.CompareResponseCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControlCodec;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControlCodec;
import org.apache.directory.shared.ldap.codec.del.DelRequestCodec;
import org.apache.directory.shared.ldap.codec.del.DelResponseCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedRequestCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedResponseCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyResponseCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequestCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNResponseCodec;
import org.apache.directory.shared.ldap.codec.search.AndFilter;
import org.apache.directory.shared.ldap.codec.search.AttributeValueAssertionFilter;
import org.apache.directory.shared.ldap.codec.search.ConnectorFilter;
import org.apache.directory.shared.ldap.codec.search.ExtensibleMatchFilter;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.codec.search.NotFilter;
import org.apache.directory.shared.ldap.codec.search.OrFilter;
import org.apache.directory.shared.ldap.codec.search.PresentFilter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceCodec;
import org.apache.directory.shared.ldap.codec.search.SubstringFilter;
import org.apache.directory.shared.ldap.codec.search.controls.pSearch.PSearchControlCodec;
import org.apache.directory.shared.ldap.codec.search.controls.pagedSearch.PagedSearchControlCodec;
import org.apache.directory.shared.ldap.codec.search.controls.subEntry.SubEntryControlCodec;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.ApproximateNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.LessEqNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.message.AbandonRequestImpl;
import org.apache.directory.shared.ldap.message.AddRequestImpl;
import org.apache.directory.shared.ldap.message.AddResponseImpl;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.BindRequestImpl;
import org.apache.directory.shared.ldap.message.BindResponseImpl;
import org.apache.directory.shared.ldap.message.CompareRequestImpl;
import org.apache.directory.shared.ldap.message.CompareResponseImpl;
import org.apache.directory.shared.ldap.message.DeleteRequestImpl;
import org.apache.directory.shared.ldap.message.DeleteResponseImpl;
import org.apache.directory.shared.ldap.message.ExtendedRequestImpl;
import org.apache.directory.shared.ldap.message.ExtendedResponseImpl;
import org.apache.directory.shared.ldap.message.LdapResultImpl;
import org.apache.directory.shared.ldap.message.InternalMessage;
import org.apache.directory.shared.ldap.message.ModifyDnRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyDnResponseImpl;
import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyResponseImpl;
import org.apache.directory.shared.ldap.message.InternalReferral;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.message.SearchResponseDoneImpl;
import org.apache.directory.shared.ldap.message.SearchResponseEntryImpl;
import org.apache.directory.shared.ldap.message.SearchResponseReferenceImpl;
import org.apache.directory.shared.ldap.message.UnbindRequestImpl;
import org.apache.directory.shared.ldap.message.control.InternalAbstractControl;
import org.apache.directory.shared.ldap.message.control.CascadeControl;
import org.apache.directory.shared.ldap.message.control.PagedSearchControl;
import org.apache.directory.shared.ldap.message.control.PersistentSearchControl;
import org.apache.directory.shared.ldap.message.control.SubentriesControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncDoneValueControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncInfoValueNewCookieControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncInfoValueRefreshDeleteControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncInfoValueRefreshPresentControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncInfoValueSyncIdSetControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncRequestValueControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncStateValueControl;
import org.apache.directory.shared.ldap.message.extended.GracefulShutdownRequest;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Twix to Snickers Message transformer.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class TwixTransformer
{
    /** The logger */
    private static Logger LOG = LoggerFactory.getLogger( TwixTransformer.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Transform an AbandonRequest message from a TwixMessage to a
     * SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers AbandonRequestImpl
     */
    public static InternalMessage transformAbandonRequest( LdapMessageCodec twixMessage, int messageId )
    {
        AbandonRequestImpl snickersMessage = new AbandonRequestImpl( messageId );
        AbandonRequestCodec abandonRequest = twixMessage.getAbandonRequest();

        // Twix : int abandonnedMessageId -> Snickers : int abandonId
        snickersMessage.setAbandoned( abandonRequest.getAbandonedMessageId() );

        return snickersMessage;
    }


    /**
     * Transform an AddRequest message from a TwixMessage to a SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers AddRequestImpl
     */
    public static InternalMessage transformAddRequest( LdapMessageCodec twixMessage, int messageId )
    {
        AddRequestImpl snickersMessage = new AddRequestImpl( messageId );
        AddRequestCodec addRequest = twixMessage.getAddRequest();

        // Twix : LdapDN entry -> Snickers : String name
        snickersMessage.setEntry( addRequest.getEntry() );

        // Twix : Attributes attributes -> Snickers : Attributes entry
        snickersMessage.setEntry( addRequest.getEntry() );

        return snickersMessage;
    }


    /**
     * Transform a BindRequest message from a TwixMessage to a SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers BindRequestImpl
     */
    public static InternalMessage transformBindRequest( LdapMessageCodec twixMessage, int messageId )
    {
        BindRequestImpl snickersMessage = new BindRequestImpl( messageId );
        BindRequestCodec bindRequest = twixMessage.getBindRequest();

        // Twix : int version -> Snickers : boolean isVersion3
        snickersMessage.setVersion3( bindRequest.isLdapV3() );

        // Twix : LdapDN name -> Snickers : LdapDN name
        snickersMessage.setName( bindRequest.getName() );

        // Twix : Asn1Object authentication instanceOf SimpleAuthentication ->
        // Snickers : boolean isSimple
        // Twix : SimpleAuthentication OctetString simple -> Snickers : byte []
        // credentials
        Asn1Object authentication = bindRequest.getAuthentication();

        if ( authentication instanceof SimpleAuthentication )
        {
            snickersMessage.setSimple( true );
            snickersMessage.setCredentials( ( ( SimpleAuthentication ) authentication ).getSimple() );
        }
        else
        {
            snickersMessage.setSimple( false );
            snickersMessage.setCredentials( ( ( SaslCredentials ) authentication ).getCredentials() );
            snickersMessage.setSaslMechanism( ( ( SaslCredentials ) authentication ).getMechanism() );
        }

        return snickersMessage;
    }


    /**
     * Transform a BindResponse message from a TwixMessage to a 
     * SnickersMessage.  This is used by clients which are receiving a 
     * BindResponse PDU and must decode it to return the Snickers 
     * representation.
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return a Snickers BindResponseImpl
     */
    public static InternalMessage transformBindResponse( LdapMessageCodec twixMessage, int messageId )
    {
        BindResponseImpl snickersMessage = new BindResponseImpl( messageId );
        BindResponseCodec bindResponse = twixMessage.getBindResponse();

        // Twix : byte[] serverSaslcreds -> Snickers : byte[] serverSaslCreds
        snickersMessage.setServerSaslCreds( bindResponse.getServerSaslCreds() );
        transformControlsTwixToSnickers( twixMessage, snickersMessage );
        transformLdapResultTwixToSnickers( bindResponse.getLdapResult(), snickersMessage.getLdapResult() );
        
        return snickersMessage;
    }

    
    /**
     * Transforms parameters of a Twix LdapResult into a Snickers LdapResult.
     *
     * @param twixResult the Twix LdapResult representation
     * @param snickersResult the Snickers LdapResult representation
     */
    public static void transformLdapResultTwixToSnickers( LdapResultCodec twixResult, 
        org.apache.directory.shared.ldap.message.InternalLdapResult snickersResult )
    {
        snickersResult.setErrorMessage( twixResult.getErrorMessage() );
        
        try
        {
            snickersResult.setMatchedDn( new LdapDN( twixResult.getMatchedDN() ) );
        }
        catch ( InvalidNameException e )
        {
            LOG.error( "Could not parse matchedDN while transforming twix value to snickers: {}", 
                twixResult.getMatchedDN() );
            snickersResult.setMatchedDn( new LdapDN() );
        }
        
        snickersResult.setResultCode( twixResult.getResultCode() );

        if ( twixResult.getReferrals() == null )
        {
            
        }
        else
        {
            ReferralImpl referral = new ReferralImpl();
            
            for ( LdapURL url : twixResult.getReferrals() )
            {
                referral.addLdapUrl( url.toString() );
            }
            
            snickersResult.setReferral( referral );
        }
    }
    

    /**
     * Transform a CompareRequest message from a TwixMessage to a
     * SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers CompareRequestImpl
     */
    public static InternalMessage transformCompareRequest( LdapMessageCodec twixMessage, int messageId )
    {
        CompareRequestImpl snickersMessage = new CompareRequestImpl( messageId );
        CompareRequestCodec compareRequest = twixMessage.getCompareRequest();

        // Twix : LdapDN entry -> Snickers : private LdapDN
        snickersMessage.setName( compareRequest.getEntry() );

        // Twix : LdapString attributeDesc -> Snickers : String attrId
        snickersMessage.setAttributeId( compareRequest.getAttributeDesc() );

        // Twix : OctetString assertionValue -> Snickers : byte[] attrVal
        if ( compareRequest.getAssertionValue() instanceof String )
        {
            snickersMessage.setAssertionValue( ( String ) compareRequest.getAssertionValue() );
        }
        else
        {
            snickersMessage.setAssertionValue( ( byte[] ) compareRequest.getAssertionValue() );
        }

        return snickersMessage;
    }


    /**
     * Transform a DelRequest message from a TwixMessage to a SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers DeleteRequestImpl
     */
    public static InternalMessage transformDelRequest( LdapMessageCodec twixMessage, int messageId )
    {
        DeleteRequestImpl snickersMessage = new DeleteRequestImpl( messageId );
        DelRequestCodec delRequest = twixMessage.getDelRequest();

        // Twix : LdapDN entry -> Snickers : LdapDN
        snickersMessage.setName( delRequest.getEntry() );

        return snickersMessage;
    }


    /**
     * Transform an ExtendedRequest message from a TwixMessage to a
     * SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers ExtendedRequestImpl
     */
    public static InternalMessage transformExtendedRequest( LdapMessageCodec twixMessage, int messageId )
    {
        ExtendedRequestCodec extendedRequest = twixMessage.getExtendedRequest();
        ExtendedRequestImpl snickersMessage;

        if ( extendedRequest.getRequestName().equals( GracefulShutdownRequest.EXTENSION_OID ) )
        {
            snickersMessage = new GracefulShutdownRequest( messageId );
        }
        else
        {
            snickersMessage = new ExtendedRequestImpl( messageId );
        }

        // Twix : OID requestName -> Snickers : String oid
        snickersMessage.setOid( extendedRequest.getRequestName() );

        // Twix : OctetString requestValue -> Snickers : byte [] payload
        snickersMessage.setPayload( extendedRequest.getRequestValue() );

        return snickersMessage;
    }


    /**
     * Transform a ModifyDNRequest message from a TwixMessage to a
     * SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers ModifyDNRequestImpl
     */
    public static InternalMessage transformModifyDNRequest( LdapMessageCodec twixMessage, int messageId )
    {
        ModifyDnRequestImpl snickersMessage = new ModifyDnRequestImpl( messageId );
        ModifyDNRequestCodec modifyDNRequest = twixMessage.getModifyDNRequest();

        // Twix : LdapDN entry -> Snickers : LdapDN m_name
        snickersMessage.setName( modifyDNRequest.getEntry() );

        // Twix : RelativeLdapDN newRDN -> Snickers : LdapDN m_newRdn
        snickersMessage.setNewRdn( modifyDNRequest.getNewRDN() );

        // Twix : boolean deleteOldRDN -> Snickers : boolean m_deleteOldRdn
        snickersMessage.setDeleteOldRdn( modifyDNRequest.isDeleteOldRDN() );

        // Twix : LdapDN newSuperior -> Snickers : LdapDN m_newSuperior
        snickersMessage.setNewSuperior( modifyDNRequest.getNewSuperior() );

        return snickersMessage;
    }


    /**
     * Transform a ModifyRequest message from a TwixMessage to a SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers ModifyRequestImpl
     */
    public static InternalMessage transformModifyRequest( LdapMessageCodec twixMessage, int messageId )
    {
        ModifyRequestImpl snickersMessage = new ModifyRequestImpl( messageId );
        ModifyRequestCodec modifyRequest = twixMessage.getModifyRequest();

        // Twix : LdapDN object -> Snickers : String name
        snickersMessage.setName( modifyRequest.getObject() );

        // Twix : ArrayList modifications -> Snickers : ArrayList mods
        if ( modifyRequest.getModifications() != null )
        {
            // Loop through the modifications
            for ( Modification modification:modifyRequest.getModifications() )
            {
                snickersMessage.addModification( modification );
            }
        }

        return snickersMessage;
    }


    /**
     * Transform the Filter part of a SearchRequest to an ExprNode
     * 
     * @param twixFilter The filter to be transformed
     * @return An ExprNode
     */
    public static ExprNode transformFilter( Filter twixFilter )
    {
        if ( twixFilter != null )
        {
            // Transform OR, AND or NOT leaves
            if ( twixFilter instanceof ConnectorFilter )
            {
                BranchNode branch = null;

                if ( twixFilter instanceof AndFilter )
                {
                    branch = new AndNode();
                }
                else if ( twixFilter instanceof OrFilter )
                {
                    branch = new OrNode();
                }
                else if ( twixFilter instanceof NotFilter )
                {
                    branch = new NotNode();
                }

                List<Filter> filtersSet = ( ( ConnectorFilter ) twixFilter ).getFilterSet();

                // Loop on all AND/OR children
                if ( filtersSet != null )
                {
                    for ( Filter filter:filtersSet )
                    {
                        branch.addNode( transformFilter( filter ) );
                    }
                }

                return branch;
            }
            else
            {
                // Transform PRESENT or ATTRIBUTE_VALUE_ASSERTION
                LeafNode branch = null;

                if ( twixFilter instanceof PresentFilter )
                {
                    branch = new PresenceNode( ( ( PresentFilter ) twixFilter ).getAttributeDescription() );
                }
                else if ( twixFilter instanceof AttributeValueAssertionFilter )
                {
                    AttributeValueAssertion ava = ( ( AttributeValueAssertionFilter ) twixFilter ).getAssertion();

                    // Transform =, >=, <=, ~= filters
                    switch ( ( ( AttributeValueAssertionFilter ) twixFilter ).getFilterType() )
                    {
                        case LdapConstants.EQUALITY_MATCH_FILTER:
                            branch = new EqualityNode( ava.getAttributeDesc(), 
                                ava.getAssertionValue() );
                            
                            break;

                        case LdapConstants.GREATER_OR_EQUAL_FILTER:
                            branch = new GreaterEqNode( ava.getAttributeDesc(),
                                ava.getAssertionValue() );

                            break;

                        case LdapConstants.LESS_OR_EQUAL_FILTER:
                            branch = new LessEqNode( ava.getAttributeDesc(), 
                                ava.getAssertionValue() );

                            break;

                        case LdapConstants.APPROX_MATCH_FILTER:
                            branch = new ApproximateNode( ava.getAttributeDesc(), 
                                ava.getAssertionValue() );

                            break;
                    }

                }
                else if ( twixFilter instanceof SubstringFilter )
                {
                    // Transform Substring filters
                    SubstringFilter filter = ( SubstringFilter ) twixFilter;
                    String initialString = null;
                    String finalString = null;
                    List<String> anyString = null;

                    if ( filter.getInitialSubstrings() != null )
                    {
                        initialString = filter.getInitialSubstrings();
                    }

                    if ( filter.getFinalSubstrings() != null )
                    {
                        finalString = filter.getFinalSubstrings();
                    }

                    if ( filter.getAnySubstrings() != null )
                    {
                        anyString = new ArrayList<String>();

                        for ( String any:filter.getAnySubstrings() )
                        {
                            anyString.add( any );
                        }
                    }

                    branch = new SubstringNode( anyString, filter.getType(), initialString, finalString );
                }
                else if ( twixFilter instanceof ExtensibleMatchFilter )
                {
                    // Transform Extensible Match Filter
                    ExtensibleMatchFilter filter = ( ExtensibleMatchFilter ) twixFilter;
                    String attribute = null;
                    String matchingRule = null;

                    if ( filter.getType() != null )
                    {
                        attribute = filter.getType();
                    }

                    Object value = filter.getMatchValue();

                    if ( filter.getMatchingRule() != null )
                    {
                        matchingRule = filter.getMatchingRule();
                    }

                    if ( value instanceof String )
                    {
                        branch = new ExtensibleNode( attribute, (String)value, matchingRule, filter.isDnAttributes() );
                    }
                    else
                    {
                        if ( value != null )
                        {
                            branch = new ExtensibleNode( attribute, (byte[])value, matchingRule, filter.isDnAttributes() );
                        }
                        else
                        {
                            branch = new ExtensibleNode( attribute, (byte[])null, matchingRule, filter.isDnAttributes() );
                        }
                    }
                }

                return branch;
            }
        }
        else
        {
            // We have found nothing to transform. Return null then.
            return null;
        }
    }


    /**
     * Transform an ExprNode filter to a TwixFilter
     * 
     * @param exprNode The filter to be transformed
     * @return A Twix filter
     */
    public static Filter transformFilter( ExprNode exprNode )
    {
        if ( exprNode != null )
        {
            Filter filter  = null;

            // Transform OR, AND or NOT leaves
            if ( exprNode instanceof BranchNode )
            {
                if ( exprNode instanceof AndNode )
                {
                    filter = new AndFilter();
                }
                else if ( exprNode instanceof OrNode )
                {
                    filter = new OrFilter();
                }
                else if ( exprNode instanceof NotNode )
                {
                    filter = new NotFilter();
                }

                List<ExprNode> children = ((BranchNode)exprNode).getChildren();

                // Loop on all AND/OR children
                if ( children != null )
                {
                    for ( ExprNode child:children )
                    {
                        try
                        {
                            ((ConnectorFilter)filter).addFilter( transformFilter( child ) );
                        }
                        catch ( DecoderException de )
                        {
                            LOG.error( "Error while transforming a ExprNode : " + de.getMessage() );
                            return null;
                        }
                    }
                }
            }
            else
            {
                if ( exprNode instanceof PresenceNode )
                {
                    // Transform Presence Node
                    filter = new PresentFilter();
                    ((PresentFilter)filter).setAttributeDescription( ((PresenceNode)exprNode).getAttribute() );
                }
                else if ( exprNode instanceof SimpleNode<?> )
                {
                    if ( exprNode instanceof EqualityNode<?> )
                    {
                        filter = new AttributeValueAssertionFilter( LdapConstants.EQUALITY_MATCH_FILTER );
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        assertion.setAttributeDesc( ((EqualityNode<?>)exprNode).getAssertionType().name() );
                        assertion.setAssertionValue( ((EqualityNode<?>)exprNode).getValue() );
                        ((AttributeValueAssertionFilter)filter).setAssertion( assertion );
                    }
                    else if ( exprNode instanceof GreaterEqNode<?> ) 
                    {
                        filter = new AttributeValueAssertionFilter( LdapConstants.GREATER_OR_EQUAL_FILTER );
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        assertion.setAttributeDesc( ((EqualityNode<?>)exprNode).getAssertionType().name() );
                        assertion.setAssertionValue( ((EqualityNode<?>)exprNode).getValue() );
                        ((AttributeValueAssertionFilter)filter).setAssertion( assertion );
                    }
                    else if ( exprNode instanceof LessEqNode<?> ) 
                    {
                        filter = new AttributeValueAssertionFilter( LdapConstants.LESS_OR_EQUAL_FILTER );
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        assertion.setAttributeDesc( ((EqualityNode<?>)exprNode).getAssertionType().name() );
                        assertion.setAssertionValue( ((EqualityNode<?>)exprNode).getValue() );
                        ((AttributeValueAssertionFilter)filter).setAssertion( assertion );
                    }
                    else if ( exprNode instanceof ApproximateNode<?> )
                    {
                        filter = new AttributeValueAssertionFilter( LdapConstants.APPROX_MATCH_FILTER );
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        assertion.setAttributeDesc( ((EqualityNode<?>)exprNode).getAssertionType().name() );
                        assertion.setAssertionValue( ((EqualityNode<?>)exprNode).getValue() );
                        ((AttributeValueAssertionFilter)filter).setAssertion( assertion );
                    }
                }
                else if ( exprNode instanceof SubstringNode )
                {
                    // Transform Substring Nodes
                    filter = new SubstringFilter();

                    String initialString = ((SubstringNode)exprNode).getInitial();
                    String finalString = ((SubstringNode)exprNode).getFinal();
                    List<String> anyStrings = ((SubstringNode)exprNode).getAny();

                    if ( initialString != null )
                    {
                        ((SubstringFilter)filter).setInitialSubstrings( initialString );
                    }

                    if ( finalString != null )
                    {
                        ((SubstringFilter)filter).setFinalSubstrings( finalString );
                    }

                    if ( anyStrings != null )
                    {
                        for ( String any:anyStrings )
                        {
                            ((SubstringFilter)filter).addAnySubstrings( any );
                        }
                    }
                }
                else if ( exprNode instanceof ExtensibleNode )
                {
                    // Transform Extensible Node
                    filter = new ExtensibleMatchFilter();
                    
                    String attribute = ((ExtensibleNode)exprNode).getAttribute();
                    String matchingRule = ((ExtensibleNode)exprNode).getMatchingRuleId();
                    boolean dnAttributes = ((ExtensibleNode)exprNode).hasDnAttributes();
                    Object value = ((ExtensibleNode)exprNode).getValue();

                    if ( attribute != null )
                    {
                        ((ExtensibleMatchFilter)filter).setType( attribute );
                    }

                    if ( matchingRule != null )
                    {
                        ((ExtensibleMatchFilter)filter).setMatchingRule( matchingRule );
                    }

                    ((ExtensibleMatchFilter)filter).setMatchValue( value );
                    ((ExtensibleMatchFilter)filter).setDnAttributes( dnAttributes );
                }
            }

            return filter;
        }
        else
        {
            // We have found nothing to transform. Return null then.
            return null;
        }
    }


    /**
     * Transform a SearchRequest message from a TwixMessage to a SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers SearchRequestImpl
     */
    public static InternalMessage transformSearchRequest( LdapMessageCodec twixMessage, int messageId )
    {
        SearchRequestImpl snickersMessage = new SearchRequestImpl( messageId );
        SearchRequestCodec searchRequest = twixMessage.getSearchRequest();

        // Twix : LdapDN baseObject -> Snickers : String baseDn
        snickersMessage.setBase( searchRequest.getBaseObject() );

        // Twix : int scope -> Snickers : ScopeEnum scope
        snickersMessage.setScope( searchRequest.getScope() );

        // Twix : int derefAliases -> Snickers : AliasDerefMode derefAliases
        switch ( searchRequest.getDerefAliases() )
        {
            case LdapConstants.DEREF_ALWAYS:
                snickersMessage.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );
                break;

            case LdapConstants.DEREF_FINDING_BASE_OBJ:
                snickersMessage.setDerefAliases( AliasDerefMode.DEREF_FINDING_BASE_OBJ );
                break;

            case LdapConstants.DEREF_IN_SEARCHING:
                snickersMessage.setDerefAliases( AliasDerefMode.DEREF_IN_SEARCHING );
                break;

            case LdapConstants.NEVER_DEREF_ALIASES:
                snickersMessage.setDerefAliases( AliasDerefMode.NEVER_DEREF_ALIASES );
                break;
        }

        // Twix : int sizeLimit -> Snickers : int sizeLimit
        snickersMessage.setSizeLimit( searchRequest.getSizeLimit() );

        // Twix : int timeLimit -> Snickers : int timeLimit
        snickersMessage.setTimeLimit( searchRequest.getTimeLimit() );

        // Twix : boolean typesOnly -> Snickers : boolean typesOnly
        snickersMessage.setTypesOnly( searchRequest.isTypesOnly() );

        // Twix : Filter filter -> Snickers : ExprNode filter
        Filter twixFilter = searchRequest.getFilter();

        snickersMessage.setFilter( transformFilter( twixFilter ) );

        // Twix : ArrayList attributes -> Snickers : ArrayList attributes
        if ( searchRequest.getAttributes() != null )
        {
            List<EntryAttribute> attributes = searchRequest.getAttributes();

            if ( ( attributes != null ) && ( attributes.size() != 0 ) )
            {
                for ( EntryAttribute attribute:attributes )
                {
                    if ( attribute != null )
                    {
                        snickersMessage.addAttribute( attribute.getId() );
                    }
                }
            }
        }

        return snickersMessage;
    }


    /**
     * Transform an UnBindRequest message from a TwixMessage to a
     * SnickersMessage
     * 
     * @param twixMessage The message to transform
     * @param messageId The message Id
     * @return A Snickers UnBindRequestImpl
     */
    public static InternalMessage transformUnBindRequest( LdapMessageCodec twixMessage, int messageId )
    {
        return new UnbindRequestImpl( messageId );
    }


    /**
     * Transform the Twix message to a codec neutral message.
     * 
     * @param obj the object to transform
     * @return the object transformed
     */
    public static InternalMessage transform( Object obj )
    {
        LdapMessageCodec twixMessage = ( LdapMessageCodec ) obj;
        int messageId = twixMessage.getMessageId();

        if ( IS_DEBUG )
        {
            LOG.debug( "Transforming LdapMessage <" + messageId + ", " + twixMessage.getMessageTypeName()
                + "> from Twix to Snickers." );
        }

        InternalMessage snickersMessage = null;

        int messageType = twixMessage.getMessageType();

        switch ( messageType )
        {
            case ( LdapConstants.BIND_REQUEST  ):
                snickersMessage = transformBindRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.UNBIND_REQUEST  ):
                snickersMessage = transformUnBindRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.SEARCH_REQUEST  ):
                snickersMessage = transformSearchRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.MODIFY_REQUEST  ):
                snickersMessage = transformModifyRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.ADD_REQUEST  ):
                snickersMessage = transformAddRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.DEL_REQUEST  ):
                snickersMessage = transformDelRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.MODIFYDN_REQUEST  ):
                snickersMessage = transformModifyDNRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.COMPARE_REQUEST  ):
                snickersMessage = transformCompareRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.ABANDON_REQUEST  ):
                snickersMessage = transformAbandonRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.EXTENDED_REQUEST  ):
                snickersMessage = transformExtendedRequest( twixMessage, messageId );
                break;

            case ( LdapConstants.BIND_RESPONSE  ):
                snickersMessage = transformBindResponse( twixMessage, messageId );
                break;

            case ( LdapConstants.SEARCH_RESULT_ENTRY ):
            case ( LdapConstants.SEARCH_RESULT_DONE ):
            case ( LdapConstants.SEARCH_RESULT_REFERENCE ):
            case ( LdapConstants.MODIFY_RESPONSE ):
            case ( LdapConstants.ADD_RESPONSE ):
            case ( LdapConstants.DEL_RESPONSE ):
            case ( LdapConstants.MODIFYDN_RESPONSE ):
            case ( LdapConstants.COMPARE_RESPONSE ):
            case ( LdapConstants.EXTENDED_RESPONSE ):
            case ( LdapConstants.INTERMEDIATE_RESPONSE ):
                // Nothing to do !
                break;

            default:
                throw new IllegalStateException( "shouldn't happen - if it does then we have issues" );
        }

        // Transform the controls, too
        List<org.apache.directory.shared.ldap.codec.ControlCodec> twixControls = twixMessage.getControls();

        if ( twixControls != null )
        {
            for ( final ControlCodec twixControl:twixControls )
            {
                InternalAbstractControl neutralControl = null;

                if ( twixControl.getControlValue() instanceof 
                    org.apache.directory.shared.ldap.codec.controls.CascadeControlCodec )
                {
                    neutralControl = new CascadeControl();
                    neutralControl.setCritical( twixControl.getCriticality() );
                }
                else if ( twixControl.getControlValue() instanceof PSearchControlCodec )
                {
                    PersistentSearchControl neutralPsearch = new PersistentSearchControl();
                    neutralControl = neutralPsearch;
                    PSearchControlCodec twixPsearch = ( PSearchControlCodec ) twixControl.getControlValue();
                    neutralPsearch.setChangeTypes( twixPsearch.getChangeTypes() );
                    neutralPsearch.setChangesOnly( twixPsearch.isChangesOnly() );
                    neutralPsearch.setReturnECs( twixPsearch.isReturnECs() );
                    neutralPsearch.setCritical( twixControl.getCriticality() );
                }
                else if ( twixControl.getControlValue() instanceof SubEntryControlCodec )
                {
                    SubentriesControl neutralSubentriesControl = new SubentriesControl();
                    SubEntryControlCodec twixSubentriesControl = ( SubEntryControlCodec ) twixControl.getControlValue();
                    neutralControl = neutralSubentriesControl;
                    neutralSubentriesControl.setVisibility( twixSubentriesControl.isVisible() );
                    neutralSubentriesControl.setCritical( twixControl.getCriticality() );
                }
                else if ( twixControl.getControlValue() instanceof PagedSearchControlCodec )
                {
                    PagedSearchControl neutralPagedSearchControl = new PagedSearchControl();
                    neutralControl = neutralPagedSearchControl;
                    PagedSearchControlCodec twixPagedSearchControl = (PagedSearchControlCodec)twixControl.getControlValue();
                    neutralPagedSearchControl.setCookie( twixPagedSearchControl.getCookie() );
                    neutralPagedSearchControl.setSize( twixPagedSearchControl.getSize() );
                    neutralPagedSearchControl.setCritical( twixControl.getCriticality() );
                }
                else if ( twixControl.getControlValue() instanceof SyncDoneValueControlCodec )
                {
                    SyncDoneValueControl neutralSyncDoneValueControl = new SyncDoneValueControl();
                    SyncDoneValueControlCodec twixSyncDoneValueControl = (SyncDoneValueControlCodec)twixControl.getControlValue();
                    neutralControl = neutralSyncDoneValueControl;
                    neutralSyncDoneValueControl.setCritical( twixControl.getCriticality() );
                    neutralSyncDoneValueControl.setCookie( twixSyncDoneValueControl.getCookie() );
                    neutralSyncDoneValueControl.setRefreshDeletes( twixSyncDoneValueControl.isRefreshDeletes() );
                }
                else if ( twixControl.getControlValue() instanceof SyncInfoValueControlCodec )
                {
                    SyncInfoValueControlCodec twixSyncInfoValueControlCodec = (SyncInfoValueControlCodec)twixControl.getControlValue();
                    
                    switch ( twixSyncInfoValueControlCodec.getType() )
                    {
                        case NEW_COOKIE :
                            SyncInfoValueNewCookieControl neutralSyncInfoValueNewCookieControl = new SyncInfoValueNewCookieControl();
                            neutralControl = neutralSyncInfoValueNewCookieControl; 
                            neutralSyncInfoValueNewCookieControl.setCritical( twixControl.getCriticality() );
                            neutralSyncInfoValueNewCookieControl.setCookie( twixSyncInfoValueControlCodec.getCookie() );
                            
                            break;
                            
                        case REFRESH_DELETE :
                            SyncInfoValueRefreshDeleteControl neutralSyncInfoValueRefreshDeleteControl = new SyncInfoValueRefreshDeleteControl();
                            neutralControl = neutralSyncInfoValueRefreshDeleteControl; 
                            neutralSyncInfoValueRefreshDeleteControl.setCritical( twixControl.getCriticality() );
                            neutralSyncInfoValueRefreshDeleteControl.setCookie( twixSyncInfoValueControlCodec.getCookie() );
                            neutralSyncInfoValueRefreshDeleteControl.setRefreshDone( twixSyncInfoValueControlCodec.isRefreshDone() );
                            
                            break;
                            
                        case REFRESH_PRESENT :
                            SyncInfoValueRefreshPresentControl neutralSyncInfoValueRefreshPresentControl = new SyncInfoValueRefreshPresentControl();
                            neutralControl = neutralSyncInfoValueRefreshPresentControl; 
                            neutralSyncInfoValueRefreshPresentControl.setCritical( twixControl.getCriticality() );
                            neutralSyncInfoValueRefreshPresentControl.setCookie( twixSyncInfoValueControlCodec.getCookie() );
                            neutralSyncInfoValueRefreshPresentControl.setRefreshDone( twixSyncInfoValueControlCodec.isRefreshDone() );
                            
                            break;
                            
                        case SYNC_ID_SET :
                            SyncInfoValueSyncIdSetControl neutralSyncInfoValueSyncIdSetControl = new SyncInfoValueSyncIdSetControl();
                            neutralControl = neutralSyncInfoValueSyncIdSetControl; 
                            neutralSyncInfoValueSyncIdSetControl.setCritical( twixControl.getCriticality() );
                            neutralSyncInfoValueSyncIdSetControl.setCookie( twixSyncInfoValueControlCodec.getCookie() );
                            neutralSyncInfoValueSyncIdSetControl.setRefreshDeletes( twixSyncInfoValueControlCodec.isRefreshDeletes() );
                            
                            List<byte[]> uuids = twixSyncInfoValueControlCodec.getSyncUUIDs();
                            
                            if ( uuids != null )
                            {
                                for ( byte[] uuid:uuids )
                                {
                                    neutralSyncInfoValueSyncIdSetControl.addSyncUUID( uuid );
                                }
                            }
                            
                            break;
                    }
                }
                else if ( twixControl.getControlValue() instanceof SyncRequestValueControlCodec )
                {
                    SyncRequestValueControl neutralSyncRequestValueControl = new SyncRequestValueControl();
                    SyncRequestValueControlCodec twixSyncDoneValueControlCodec = (SyncRequestValueControlCodec)twixControl.getControlValue();
                    neutralControl = neutralSyncRequestValueControl;
                    neutralSyncRequestValueControl.setCritical( twixControl.getCriticality() );
                    neutralSyncRequestValueControl.setMode( twixSyncDoneValueControlCodec.getMode() );
                    neutralSyncRequestValueControl.setCookie( twixSyncDoneValueControlCodec.getCookie() );
                    neutralSyncRequestValueControl.setReloadHint( twixSyncDoneValueControlCodec.isReloadHint() );
                }
                else if ( twixControl.getControlValue() instanceof SyncStateValueControl )
                {
                    SyncStateValueControl neutralSyncStateValueControl = new SyncStateValueControl();
                    SyncStateValueControlCodec twixSyncStateValueControlCodec = (SyncStateValueControlCodec)twixControl.getControlValue();
                    neutralControl = neutralSyncStateValueControl;
                    neutralSyncStateValueControl.setCritical( twixControl.getCriticality() );
                    neutralSyncStateValueControl.setSyncStateType( twixSyncStateValueControlCodec.getSyncStateType() );
                    neutralSyncStateValueControl.setEntryUUID( twixSyncStateValueControlCodec.getEntryUUID() );
                    neutralSyncStateValueControl.setCookie( twixSyncStateValueControlCodec.getCookie() );
                }
                else if ( twixControl.getControlValue() instanceof byte[] )
                {
                    neutralControl = new InternalAbstractControl()
                    {
                        public byte[] getEncodedValue()
                        {
                            return ( byte[] ) twixControl.getControlValue();
                        }
                    };

                    // Twix : boolean criticality -> Snickers : boolean
                    // m_isCritical
                    neutralControl.setCritical( twixControl.getCriticality() );

                    // Twix : OID controlType -> Snickers : String m_oid
                    neutralControl.setID( twixControl.getControlType() );
                }
                else if ( twixControl.getControlValue() == null )
                {
                    neutralControl = new InternalAbstractControl()
                    {
                        public byte[] getEncodedValue()
                        {
                            return ( byte[] ) twixControl.getControlValue();
                        }
                    };

                    // Twix : boolean criticality -> Snickers : boolean
                    // m_isCritical
                    neutralControl.setCritical( twixControl.getCriticality() );

                    // Twix : OID controlType -> Snickers : String m_oid
                    neutralControl.setID( twixControl.getControlType() );
                }
                

                snickersMessage.add( neutralControl );
            }
        }

        return snickersMessage;
    }


    /**
     * Transform a Ldapresult part of a Snickers Response to a Twix LdapResult
     * 
     * @param snickersLdapResult the Snickers LdapResult to transform
     * @return A Twix LdapResult
     */
    public static LdapResultCodec transformLdapResult( LdapResultImpl snickersLdapResult )
    {
        LdapResultCodec twixLdapResult = new LdapResultCodec();

        // Snickers : ResultCodeEnum resultCode -> Twix : int resultCode
        twixLdapResult.setResultCode( snickersLdapResult.getResultCode() );

        // Snickers : String errorMessage -> Twix : LdapString errorMessage
        String errorMessage = snickersLdapResult.getErrorMessage();
        
        twixLdapResult.setErrorMessage( StringTools.isEmpty( errorMessage ) ? "" : errorMessage );

        // Snickers : String matchedDn -> Twix : LdapDN matchedDN
        twixLdapResult.setMatchedDN( snickersLdapResult.getMatchedDn() );

        // Snickers : Referral referral -> Twix : ArrayList referrals
        ReferralImpl snickersReferrals = ( ReferralImpl ) snickersLdapResult.getReferral();

        if ( snickersReferrals != null )
        {
            twixLdapResult.initReferrals();

            for ( String referral:snickersReferrals.getLdapUrls() )
            {
                try
                {
                    LdapURL ldapUrl = new LdapURL( referral.getBytes() );
                    twixLdapResult.addReferral( ldapUrl );
                }
                catch ( LdapURLEncodingException lude )
                {
                    LOG.warn( "The referral " + referral + " is invalid : " + lude.getMessage() );
                    twixLdapResult.addReferral( LdapURL.EMPTY_URL );
                }
            }
        }

        return twixLdapResult;
    }


    /**
     * Transform a Snickers AddResponse to a Twix AddResponse
     * 
     * @param twixMessage The Twix AddResponse to produce
     * @param snickersMessage The incoming Snickers AddResponse
     */
    public static void transformAddResponse( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        AddResponseImpl snickersAddResponse = ( AddResponseImpl ) snickersMessage;

        AddResponseCodec addResponse = new AddResponseCodec();

        // Transform the ldapResult
        addResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) snickersAddResponse.getLdapResult() ) );

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( addResponse );
    }


    /**
     * Transform a Snickers BindResponse to a Twix BindResponse
     * 
     * @param twixMessage The Twix BindResponse to produce
     * @param snickersMessage The incoming Snickers BindResponse
     */
    public static void transformBindResponse( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        BindResponseImpl snickersBindResponse = ( BindResponseImpl ) snickersMessage;

        BindResponseCodec bindResponse = new BindResponseCodec();

        // Snickers : byte [] serverSaslCreds -> Twix : OctetString
        // serverSaslCreds
        byte[] serverSaslCreds = snickersBindResponse.getServerSaslCreds();

        if ( serverSaslCreds != null )
        {
            bindResponse.setServerSaslCreds( serverSaslCreds );
        }

        // Transform the ldapResult
        bindResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) snickersBindResponse.getLdapResult() ) );

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( bindResponse );
    }


    /**
     * Transform a Snickers BindRequest to a Twix BindRequest
     * 
     * @param twixMessage The Twix BindRequest to produce
     * @param snickersMessage The incoming Snickers BindRequest
     */
    public static void transformBindRequest( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        BindRequestImpl snickersBindRequest = ( BindRequestImpl ) snickersMessage;

        BindRequestCodec bindRequest = new BindRequestCodec();
        
        if ( snickersBindRequest.isSimple() )
        {
            SimpleAuthentication simple = new SimpleAuthentication();
            simple.setSimple( snickersBindRequest.getCredentials() );
            bindRequest.setAuthentication( simple );
        }
        else
        {
            SaslCredentials sasl = new SaslCredentials();
            sasl.setCredentials( snickersBindRequest.getCredentials() );
            sasl.setMechanism( snickersBindRequest.getSaslMechanism() );
            bindRequest.setAuthentication( sasl );
        }
        
        bindRequest.setMessageId( snickersBindRequest.getMessageId() );
        bindRequest.setName( snickersBindRequest.getName() );
        bindRequest.setVersion( snickersBindRequest.isVersion3() ? 3 : 2 );
        
        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( bindRequest );
    }


    /**
     * Transform a Snickers CompareResponse to a Twix CompareResponse
     * 
     * @param twixMessage The Twix CompareResponse to produce
     * @param snickersMessage The incoming Snickers CompareResponse
     */
    public static void transformCompareResponse( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        CompareResponseImpl snickersCompareResponse = ( CompareResponseImpl ) snickersMessage;

        CompareResponseCodec compareResponse = new CompareResponseCodec();

        // Transform the ldapResult
        compareResponse
            .setLdapResult( transformLdapResult( ( LdapResultImpl ) snickersCompareResponse.getLdapResult() ) );

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( compareResponse );
    }


    /**
     * Transform a Snickers DelResponse to a Twix DelResponse
     * 
     * @param twixMessage The Twix DelResponse to produce
     * @param snickersMessage The incoming Snickers DelResponse
     */
    public static void transformDelResponse( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        DeleteResponseImpl snickersDelResponse = ( DeleteResponseImpl ) snickersMessage;

        DelResponseCodec delResponse = new DelResponseCodec();

        // Transform the ldapResult
        delResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) snickersDelResponse.getLdapResult() ) );

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( delResponse );
    }


    /**
     * Transform a Snickers ExtendedResponse to a Twix ExtendedResponse
     * 
     * @param twixMessage The Twix ExtendedResponse to produce
     * @param snickersMessage The incoming Snickers ExtendedResponse
     */
    public static void transformExtendedResponse( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        ExtendedResponseImpl snickersExtendedResponse = ( ExtendedResponseImpl ) snickersMessage;
        ExtendedResponseCodec extendedResponse = new ExtendedResponseCodec();

        // Snickers : String oid -> Twix : OID responseName
        try
        {
            extendedResponse.setResponseName( new OID( snickersExtendedResponse.getResponseName() ) );
        }
        catch ( DecoderException de )
        {
            LOG.warn( "The OID " + snickersExtendedResponse.getResponseName() + " is invalid : " + de.getMessage() );
            extendedResponse.setResponseName( null );
        }

        // Snickers : byte [] value -> Twix : Object response
        extendedResponse.setResponse( snickersExtendedResponse.getResponse() );

        // Transform the ldapResult
        extendedResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) snickersExtendedResponse
            .getLdapResult() ) );

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( extendedResponse );
    }


    /**
     * Transform a Snickers ModifyResponse to a Twix ModifyResponse
     * 
     * @param twixMessage The Twix ModifyResponse to produce
     * @param snickersMessage The incoming Snickers ModifyResponse
     */
    public static void transformModifyResponse( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        ModifyResponseImpl snickersModifyResponse = ( ModifyResponseImpl ) snickersMessage;

        ModifyResponseCodec modifyResponse = new ModifyResponseCodec();

        // Transform the ldapResult
        modifyResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) snickersModifyResponse.getLdapResult() ) );

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( modifyResponse );
    }


    /**
     * Transform a Snickers ModifyDNResponse to a Twix ModifyDNResponse
     * 
     * @param twixMessage The Twix ModifyDNResponse to produce
     * @param snickersMessage The incoming Snickers ModifyDNResponse
     */
    public static void transformModifyDNResponse( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        ModifyDnResponseImpl snickersModifyDNResponse = ( ModifyDnResponseImpl ) snickersMessage;

        ModifyDNResponseCodec modifyDNResponse = new ModifyDNResponseCodec();

        // Transform the ldapResult
        modifyDNResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) snickersModifyDNResponse
            .getLdapResult() ) );

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( modifyDNResponse );
    }


    /**
     * Transform a Snickers SearchResponseDone to a Twix SearchResultDone
     * 
     * @param twixMessage The Twix SearchResultDone to produce
     * @param snickersMessage The incoming Snickers SearchResponseDone
     */
    public static void transformSearchResultDone( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        SearchResponseDoneImpl snickersSearchResponseDone = ( SearchResponseDoneImpl ) snickersMessage;
        SearchResultDoneCodec searchResultDone = new SearchResultDoneCodec();

        // Transform the ldapResult
        searchResultDone.setLdapResult( transformLdapResult( ( LdapResultImpl ) snickersSearchResponseDone
            .getLdapResult() ) );

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( searchResultDone );
    }


    /**
     * Transform a Snickers SearchResponseEntry to a Twix SearchResultEntry
     * 
     * @param twixMessage The Twix SearchResultEntry to produce
     * @param snickersMessage The incoming Snickers SearchResponseEntry
     */
    public static void transformSearchResultEntry( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        SearchResponseEntryImpl snickersSearchResultResponse = ( SearchResponseEntryImpl ) snickersMessage;
        SearchResultEntryCodec searchResultEntry = new SearchResultEntryCodec();

        // Snickers : LdapDN dn -> Twix : LdapDN objectName
        searchResultEntry.setObjectName( snickersSearchResultResponse.getObjectName() );

        // Snickers : Attributes attributes -> Twix : ArrayList
        // partialAttributeList
        searchResultEntry.setEntry( snickersSearchResultResponse.getEntry() );

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( searchResultEntry );
    }


    /**
     * Transform a Snickers SearchResponseReference to a Twix
     * SearchResultReference
     * 
     * @param twixMessage The Twix SearchResultReference to produce
     * @param snickersMessage The incoming Snickers SearchResponseReference
     */
    public static void transformSearchResultReference( LdapMessageCodec twixMessage, InternalMessage snickersMessage )
    {
        SearchResponseReferenceImpl snickersSearchResponseReference = ( SearchResponseReferenceImpl ) snickersMessage;
        SearchResultReferenceCodec searchResultReference = new SearchResultReferenceCodec();

        // Snickers : Referral m_referral -> Twix: ArrayList
        // searchResultReferences
        InternalReferral referrals = snickersSearchResponseReference.getReferral();

        // Loop on all referals
        if ( referrals != null )
        {
            Collection<String> urls = referrals.getLdapUrls();

            if ( urls != null )
            {
                for ( String url:urls)
                {
                    try
                    {
                        searchResultReference.addSearchResultReference( new LdapURL( url ) );
                    }
                    catch ( LdapURLEncodingException luee )
                    {
                        LOG.warn( "The LdapURL " + url + " is incorrect : " + luee.getMessage() );
                    }
                }
            }
        }

        // Set the operation into the LdapMessage
        twixMessage.setProtocolOP( searchResultReference );
    }


    /**
     * Transform the Snickers message to a Twix message.
     * 
     * @param msg the message to transform
     * @return the msg transformed
     */
    public static Object transform( InternalMessage msg )
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Transforming message type " + msg.getType() );
        }

        LdapMessageCodec twixMessage = new LdapMessageCodec();

        twixMessage.setMessageId( msg.getMessageId() );

        switch ( msg.getType() )
        {
            case SEARCH_RES_ENTRY :
                transformSearchResultEntry( twixMessage, msg );
                break;
                
            case SEARCH_RES_DONE :
                transformSearchResultDone( twixMessage, msg );
                break;
                
            case SEARCH_RES_REF :
                transformSearchResultReference( twixMessage, msg );
                break;
                
            case BIND_RESPONSE :
                transformBindResponse( twixMessage, msg );
                break;
                
            case BIND_REQUEST :
                transformBindRequest( twixMessage, msg );
                break;
                
            case ADD_RESPONSE :
                transformAddResponse( twixMessage, msg );
                break;
                
            case COMPARE_RESPONSE :
                transformCompareResponse( twixMessage, msg );
                break;
                
            case DEL_RESPONSE :
                transformDelResponse( twixMessage, msg );
                break;
         
            case MODIFY_RESPONSE :
                transformModifyResponse( twixMessage, msg );
                break;

            case MOD_DN_RESPONSE :
                transformModifyDNResponse( twixMessage, msg );
                break;
                
            case EXTENDED_RESP :
                transformExtendedResponse( twixMessage, msg );
                break;
                
        }

        // We also have to transform the controls...
        if ( !msg.getControls().isEmpty() )
        {
            transformControls( twixMessage, msg );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Transformed message : " + twixMessage );
        }

        return twixMessage;
    }


    /**
     * TODO finish this implementation.  Takes Twix Controls, transforming 
     * them to Snickers Controls and populates the Snickers message with them.
     *
     * @param twixMessage the Twix message
     * @param msg the Snickers message
     */
    public static void transformControlsTwixToSnickers( LdapMessageCodec twixMessage, InternalMessage msg )
    {
        if ( twixMessage.getControls() == null )
        {
            return;
        }
        
        for ( ControlCodec control:twixMessage.getControls() )
        {
            LOG.debug( "Not decoding response control: {}", control );
        }
    }
    
    
    /**
     * Transforms the controls
     * @param twixMessage The Twix SearchResultReference to produce
     * @param msg The incoming Snickers SearchResponseReference
     */
    public static void transformControls( LdapMessageCodec twixMessage, InternalMessage msg )
    {
        for ( javax.naming.ldap.Control control:msg.getControls().values() )
        {
            org.apache.directory.shared.ldap.codec.ControlCodec twixControl = new org.apache.directory.shared.ldap.codec.ControlCodec();
            twixMessage.addControl( twixControl );
            twixControl.setCriticality( control.isCritical() );
            
            byte[] encodedValue = control.getEncodedValue();
            twixControl.setControlValue( encodedValue );
            twixControl.setEncodedValue( encodedValue );
            twixControl.setControlType( control.getID() );
            twixControl.setParent( twixMessage );
        }
    }
}
