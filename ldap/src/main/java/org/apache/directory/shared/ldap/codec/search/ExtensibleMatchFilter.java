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
package org.apache.directory.shared.ldap.codec.search;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.Length;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.util.LdapString;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * The search request filter Matching Rule assertion
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ExtensibleMatchFilter extends Filter
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The expected lenth of the Matching Rule Assertion */
    private transient int expectedMatchingRuleLength;

    /** Matching rule */
    private LdapString matchingRule;

    /** Matching rule type */
    private LdapString type;

    /** Matching rule value */
    private Object matchValue;

    /** The dnAttributes flag */
    private boolean dnAttributes;

    /** The extensible match length */
    private transient int extensibleMatchLength;


    /** The matching Rule Assertion Length */
    // private transient int matchingRuleAssertionLength;
    // ~ Constructors
    // -------------------------------------------------------------------------------
    /**
     * Creates a new ExtensibleMatchFilter object. The dnAttributes flag
     * defaults to false.
     */
    public ExtensibleMatchFilter()
    {
        dnAttributes = false;
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the dnAttributes flag
     * 
     * @return Returns the dnAttributes.
     */
    public boolean isDnAttributes()
    {
        return dnAttributes;
    }


    /**
     * Set the dnAttributes flag
     * 
     * @param dnAttributes
     *            The dnAttributes to set.
     */
    public void setDnAttributes( boolean dnAttributes )
    {
        this.dnAttributes = dnAttributes;
    }


    /**
     * Get the matchingRule
     * 
     * @return Returns the matchingRule.
     */
    public LdapString getMatchingRule()
    {
        return matchingRule;
    }


    /**
     * Set the matchingRule
     * 
     * @param matchingRule
     *            The matchingRule to set.
     */
    public void setMatchingRule( LdapString matchingRule )
    {
        this.matchingRule = matchingRule;
    }


    /**
     * Get the matchValue
     * 
     * @return Returns the matchValue.
     */
    public Object getMatchValue()
    {
        return matchValue;
    }


    /**
     * Set the matchValue
     * 
     * @param matchValue
     *            The matchValue to set.
     */
    public void setMatchValue( Object matchValue )
    {
        this.matchValue = matchValue;
    }


    /**
     * Get the type
     * 
     * @return Returns the type.
     */
    public LdapString getType()
    {
        return type;
    }


    /**
     * Set the type
     * 
     * @param type
     *            The type to set.
     */
    public void setType( LdapString type )
    {
        this.type = type;
    }


    /**
     * get the expectedMatchingRuleLength
     * 
     * @return Returns the expectedMatchingRuleLength.
     */
    public int getExpectedMatchingRuleLength()
    {
        return expectedMatchingRuleLength;
    }


    /**
     * Set the expectedMatchingRuleLength
     * 
     * @param expectedMatchingRuleLength
     *            The expectedMatchingRuleLength to set.
     */
    public void setExpectedMatchingRuleLength( int expectedMatchingRuleLength )
    {
        this.expectedMatchingRuleLength = expectedMatchingRuleLength;
    }


    /**
     * Compute the ExtensibleMatchFilter length ExtensibleMatchFilter : 0xA9 L1 |
     * [+--> 0x81 L3 matchingRule] [+--> 0x82 L4 type] [+--> 0x83 L5 matchValue]
     * [+--> 0x01 0x01 dnAttributes]
     */
    public int computeLength()
    {
        if ( matchingRule != null )
        {
            extensibleMatchLength = 1 + Length.getNbBytes( matchingRule.getNbBytes() ) + matchingRule.getNbBytes();
        }

        if ( type != null )
        {
            extensibleMatchLength += 1 + Length.getNbBytes( type.getNbBytes() ) + type.getNbBytes();
        }

        if ( matchValue != null )
        {
            if ( matchValue instanceof String )
            {
                int matchValueLength = StringTools.getBytesUtf8( ( String ) matchValue ).length;
                extensibleMatchLength += 1 + Length.getNbBytes( matchValueLength ) + matchValueLength;
            }
            else
            {
                extensibleMatchLength += 1 + Length.getNbBytes( ( ( byte[] ) matchValue ).length )
                    + ( ( byte[] ) matchValue ).length;
            }
        }

        if ( dnAttributes )
        {
            extensibleMatchLength += 1 + 1 + 1;
        }

        return 1 + Length.getNbBytes( extensibleMatchLength ) + extensibleMatchLength;
    }


    /**
     * Encode the ExtensibleMatch Filters to a PDU. ExtensibleMatch filter :
     * 0xA9 LL | 0x81 LL matchingRule | / | 0x82 LL Type | / | /0x83 LL
     * matchValue +--+ +-+ | \ \ | \ 0x83 LL MatchValue | 0x82 LL type | 0x83 LL
     * matchValue +--[0x84 0x01 dnAttributes]
     * 
     * @param buffer
     *            The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The ExtensibleMatch Tag
            buffer.put( ( byte ) LdapConstants.EXTENSIBLE_MATCH_FILTER_TAG );
            buffer.put( Length.getBytes( extensibleMatchLength ) );

            if ( ( matchingRule == null ) && ( type == null ) )
            {
                throw new EncoderException( "Cannot have a null matching rule and a null type" );
            }

            // The matching rule
            if ( matchingRule != null )
            {
                buffer.put( ( byte ) LdapConstants.SEARCH_MATCHING_RULE_TAG );
                buffer.put( Length.getBytes( matchingRule.getNbBytes() ) );
                buffer.put( matchingRule.getBytes() );
            }

            // The type
            if ( type != null )
            {
                buffer.put( ( byte ) LdapConstants.MATCHING_RULE_ASSERTION_TYPE_TAG );
                buffer.put( Length.getBytes( type.getNbBytes() ) );
                buffer.put( type.getBytes() );
            }

            // The match value
            if ( matchValue != null )
            {
                buffer.put( ( byte ) LdapConstants.SEARCH_MATCH_VALUE_TAG );

                if ( matchValue instanceof String )
                {
                    byte[] matchValueBytes = StringTools.getBytesUtf8( ( String ) matchValue );
                    buffer.put( Length.getBytes( matchValueBytes.length ) );

                    if ( matchValueBytes.length != 0 )
                    {
                        buffer.put( matchValueBytes );
                    }
                }
                else
                {
                    buffer.put( Length.getBytes( ( ( byte[] ) matchValue ).length ) );

                    if ( ( ( byte[] ) matchValue ).length != 0 )
                    {
                        buffer.put( ( byte[] ) matchValue );
                    }
                }

            }

            // The dnAttributes flag, if true only
            if ( dnAttributes )
            {
                buffer.put( ( byte ) LdapConstants.DN_ATTRIBUTES_FILTER_TAG );
                buffer.put( ( byte ) 1 );
                buffer.put( Value.TRUE_VALUE );
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        return buffer;
    }


    /**
     * Return a String representing an extended filter as of RFC 2254
     * 
     * @return An Extened Filter String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        if ( type != null )
        {
            sb.append( type.toString() );
        }

        if ( dnAttributes )
        {
            sb.append( ":dn" );
        }

        if ( matchingRule == null )
        {

            if ( type == null )
            {
                return "Extended Filter wrong syntax";
            }
        }
        else
        {
            sb.append( ':' ).append( matchingRule.toString() );
        }

        sb.append( ":=" ).append( matchValue.toString() );

        return sb.toString();
    }
}
