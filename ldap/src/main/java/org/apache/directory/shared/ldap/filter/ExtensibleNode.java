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
package org.apache.directory.shared.ldap.filter;


import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Filter expression tree node for extensible assertions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class ExtensibleNode extends LeafNode
{
    /** The value of the attribute to match for */
    private byte[] value;

    /** The matching rules id */
    private String matchingRuleId;

    /** The name of the dn attributes */
    private boolean dnAttributes = false;


    /**
     * Creates a new emptyExtensibleNode object.
     */
    public ExtensibleNode( String attribute )
    {
        super( attribute );
        
        dnAttributes = false;
    }

    /**
     * Creates a new ExtensibleNode object.
     * 
     * @param attribute the attribute used for the extensible assertion
     * @param value the value to match for
     * @param matchingRuleId the OID of the matching rule
     * @param dnAttributes the dn attributes
     */
    public ExtensibleNode(String attribute, String value, String matchingRuleId, boolean dnAttributes)
    {
        this( attribute, StringTools.getBytesUtf8( value ), matchingRuleId, dnAttributes );
    }


    /**
     * Creates a new ExtensibleNode object.
     * 
     * @param attribute the attribute used for the extensible assertion
     * @param value the value to match for
     * @param matchingRuleId the OID of the matching rule
     * @param dnAttributes the dn attributes
     */
    public ExtensibleNode( String attribute, byte[] value, String matchingRuleId, boolean dnAttributes )
    {
        super( attribute );

        if ( value != null ) {
            this.value = new byte[ value.length ];
            System.arraycopy( value, 0, this.value, 0, value.length );
        } else {
            this.value = null;
        }

        this.matchingRuleId = matchingRuleId;
        this.dnAttributes = dnAttributes;
    }


    /**
     * Gets the Dn attributes.
     * 
     * @return the dn attributes
     */
    public boolean hasDnAttributes()
    {
        return dnAttributes;
    }
    
    
    /**
     * Set the dnAttributes flag
     *
     * @param dnAttributes The flag to set
     */
    public void setDnAttributes( boolean dnAttributes )
    {
        this.dnAttributes = dnAttributes;
    }


    /**
     * Gets the matching rule id as an OID string.
     * 
     * @return the OID
     */
    public String getMatchingRuleId()
    {
        return matchingRuleId;
    }


    /**
     * Sets the matching rule id as an OID string.
     */
    public void setMatchingRuleId( String matchingRuleId )
    {
        this.matchingRuleId = matchingRuleId;
    }


    /**
     * Gets the value.
     * 
     * @return the value
     */
    public final byte[] getValue()
    {
        if ( value == null )
        {
            return null;
        }

        final byte[] copy = new byte[ value.length ];
        System.arraycopy( value, 0, copy, 0, value.length );
        return copy;
    }


    /**
     * Sets the value.
     */
    public final void setValue( String value)
    {
        this.value = StringTools.getBytesUtf8( value );
    }

    
    /**
     * @see ExprNode#printRefinementToBuffer(StringBuilder)
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf ) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException( "ExtensibleNode can't be part of a refinement" );
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
    	StringBuilder buf = new StringBuilder();
    	
        buf.append( '(' ).append( getAttribute() );
        buf.append( "-" );
        buf.append( dnAttributes );
        buf.append( "-EXTENSIBLE-" );
        buf.append( matchingRuleId );
        buf.append( "-" );
        buf.append( StringTools.utf8ToString( value ) );
        buf.append( "/" );
        buf.append( StringTools.dumpBytes( value ) );

        buf.append( super.toString() );
        
        buf.append( ')' );
        
        return buf.toString();
    }
}
