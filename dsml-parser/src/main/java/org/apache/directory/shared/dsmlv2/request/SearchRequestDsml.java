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
package org.apache.directory.shared.dsmlv2.request;


import java.util.List;

import org.apache.directory.shared.dsmlv2.ParserUtils;
import org.apache.directory.shared.ldap.codec.AttributeValueAssertion;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.search.AndFilter;
import org.apache.directory.shared.ldap.codec.search.AttributeValueAssertionFilter;
import org.apache.directory.shared.ldap.codec.search.ExtensibleMatchFilter;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.codec.search.NotFilter;
import org.apache.directory.shared.ldap.codec.search.OrFilter;
import org.apache.directory.shared.ldap.codec.search.PresentFilter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SubstringFilter;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;


/**
 * DSML Decorator for SearchRequest
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchRequestDsml extends AbstractRequestDsml
{
    /**
     * Creates a new instance of SearchRequestDsml.
     */
    public SearchRequestDsml()
    {
        super( new SearchRequestCodec() );
    }


    /**
     * Creates a new instance of SearchRequestDsml.
     *
     * @param ldapMessage
     *      the message to decorate
     */
    public SearchRequestDsml( SearchRequestCodec ldapMessage )
    {
        super( ldapMessage );
    }


    /**
     * {@inheritDoc}
     */
    public int getMessageType()
    {
        return instance.getMessageType();
    }


    /**
     * {@inheritDoc}
     */
    public Element toDsml( Element root )
    {
        Element element = super.toDsml( root );

        SearchRequestCodec request = ( SearchRequestCodec ) instance;

        // DN
        if ( request.getBaseObject() != null )
        {
            element.addAttribute( "dn", request.getBaseObject().getName() );
        }

        // Scope
        SearchScope scope = request.getScope();
        if ( scope != null )
        {
            if ( scope == SearchScope.OBJECT )
            {
                element.addAttribute( "scope", "baseObject" );
            }
            else if ( scope == SearchScope.ONELEVEL )
            {
                element.addAttribute( "scope", "singleLevel" );
            }
            else if ( scope == SearchScope.SUBTREE )
            {
                element.addAttribute( "scope", "wholeSubtree" );
            }
        }

        // DerefAliases
        int derefAliases = request.getDerefAliases();
        if ( derefAliases == LdapConstants.NEVER_DEREF_ALIASES )
        {
            element.addAttribute( "derefAliases", "neverDerefAliases" );
        }
        else if ( derefAliases == LdapConstants.DEREF_IN_SEARCHING )
        {
            element.addAttribute( "derefAliases", "derefInSearching" );
        }
        else if ( derefAliases == LdapConstants.DEREF_FINDING_BASE_OBJ )
        {
            element.addAttribute( "derefAliases", "derefFindingBaseObj" );
        }
        else if ( derefAliases == LdapConstants.DEREF_ALWAYS )
        {
            element.addAttribute( "derefAliases", "derefAlways" );
        }

        // SizeLimit
        if ( request.getSizeLimit() != 0 )
        {
            element.addAttribute( "sizeLimit", "" + request.getSizeLimit() );
        }

        // TimeLimit
        if ( request.getTimeLimit() != 0 )
        {
            element.addAttribute( "timeLimit", "" + request.getTimeLimit() );
        }

        // TypesOnly
        if ( request.isTypesOnly() )
        {
            element.addAttribute( "typesOnly", "true" );
        }

        // Filter
        Element filterElement = element.addElement( "filter" );
        toDsml( filterElement, request.getFilter() );

        // Attributes
        List<EntryAttribute> attributes = request.getAttributes();
        if ( attributes.size() > 0 )
        {
            Element attributesElement = element.addElement( "attributes" );

            for ( EntryAttribute entryAttribute : attributes )
            {
                attributesElement.addElement( "attribute" ).addAttribute( "name", entryAttribute.getId() );
            }
        }

        return element;
    }


    /**
     * Recursively converts the filter of the Search Request into a DSML representation and adds 
     * it to the XML Element corresponding to the Search Request
     *
     * @param element
     *      the parent Element
     * @param filter
     *      the filter to convert
     */
    private void toDsml( Element element, Filter filter )
    {
        // AND FILTER
        if ( filter instanceof AndFilter )
        {
            Element newElement = element.addElement( "and" );

            List<Filter> filterList = ( ( AndFilter ) filter ).getAndFilter();
            for ( int i = 0; i < filterList.size(); i++ )
            {
                toDsml( newElement, filterList.get( i ) );
            }
        }

        // OR FILTER
        else if ( filter instanceof OrFilter )
        {
            Element newElement = element.addElement( "or" );

            List<Filter> filterList = ( ( OrFilter ) filter ).getOrFilter();
            for ( int i = 0; i < filterList.size(); i++ )
            {
                toDsml( newElement, filterList.get( i ) );
            }
        }

        // NOT FILTER
        else if ( filter instanceof NotFilter )
        {
            Element newElement = element.addElement( "not" );

            toDsml( newElement, ( ( NotFilter ) filter ).getNotFilter() );
        }

        // SUBSTRING FILTER
        else if ( filter instanceof SubstringFilter )
        {
            Element newElement = element.addElement( "substrings" );

            SubstringFilter substringFilter = ( SubstringFilter ) filter;

            newElement.addAttribute( "name", substringFilter.getType() );

            String initial = substringFilter.getInitialSubstrings();
            if ( ( initial != null ) && ( !"".equals( initial ) ) )
            {
                newElement.addElement( "initial" ).setText( initial );
            }

            List<String> anyList = substringFilter.getAnySubstrings();
            for ( int i = 0; i < anyList.size(); i++ )
            {
                newElement.addElement( "any" ).setText( anyList.get( i ) );
            }

            String finalString = substringFilter.getFinalSubstrings();
            if ( ( finalString != null ) && ( !"".equals( finalString ) ) )
            {
                newElement.addElement( "final" ).setText( finalString );
            }
        }

        // APPROXMATCH, EQUALITYMATCH, GREATEROREQUALS & LESSOREQUAL FILTERS
        else if ( filter instanceof AttributeValueAssertionFilter )
        {
            AttributeValueAssertionFilter avaFilter = ( AttributeValueAssertionFilter ) filter;

            Element newElement = null;
            int filterType = avaFilter.getFilterType();
            if ( filterType == LdapConstants.APPROX_MATCH_FILTER )
            {
                newElement = element.addElement( "approxMatch" );
            }
            else if ( filterType == LdapConstants.EQUALITY_MATCH_FILTER )
            {
                newElement = element.addElement( "equalityMatch" );
            }
            else if ( filterType == LdapConstants.GREATER_OR_EQUAL_FILTER )
            {
                newElement = element.addElement( "greaterOrEqual" );
            }
            else if ( filterType == LdapConstants.LESS_OR_EQUAL_FILTER )
            {
                newElement = element.addElement( "lessOrEqual" );
            }

            AttributeValueAssertion assertion = avaFilter.getAssertion();
            if ( assertion != null )
            {
                newElement.addAttribute( "name", assertion.getAttributeDesc() );

                Object value = assertion.getAssertionValue();
                if ( value != null )
                {
                    if ( ParserUtils.needsBase64Encoding( value ) )
                    {
                        Namespace xsdNamespace = new Namespace( "xsd", ParserUtils.XML_SCHEMA_URI );
                        Namespace xsiNamespace = new Namespace( "xsi", ParserUtils.XML_SCHEMA_INSTANCE_URI );
                        element.getDocument().getRootElement().add( xsdNamespace );
                        element.getDocument().getRootElement().add( xsiNamespace );

                        Element valueElement = newElement.addElement( "value" ).addText(
                            ParserUtils.base64Encode( value ) );
                        valueElement
                            .addAttribute( new QName( "type", xsiNamespace ), "xsd:" + ParserUtils.BASE64BINARY );
                    }
                    else
                    {
                        newElement.addElement( "value" ).setText( ( String ) value );
                    }
                }
            }
        }

        // PRESENT FILTER
        else if ( filter instanceof PresentFilter )
        {
            Element newElement = element.addElement( "present" );

            newElement.addAttribute( "name", ( ( PresentFilter ) filter ).getAttributeDescription() );
        }

        // EXTENSIBLEMATCH
        else if ( filter instanceof ExtensibleMatchFilter )
        {
            Element newElement = element.addElement( "extensibleMatch" );

            ExtensibleMatchFilter extensibleMatchFilter = ( ExtensibleMatchFilter ) filter;

            Object value = extensibleMatchFilter.getMatchValue();
            if ( value != null )
            {
                if ( ParserUtils.needsBase64Encoding( value ) )
                {
                    Namespace xsdNamespace = new Namespace( "xsd", ParserUtils.XML_SCHEMA_URI );
                    Namespace xsiNamespace = new Namespace( "xsi", ParserUtils.XML_SCHEMA_INSTANCE_URI );
                    element.getDocument().getRootElement().add( xsdNamespace );
                    element.getDocument().getRootElement().add( xsiNamespace );

                    Element valueElement = newElement.addElement( "value" ).addText( ParserUtils.base64Encode( value ) );
                    valueElement.addAttribute( new QName( "type", xsiNamespace ), "xsd:" + ParserUtils.BASE64BINARY );
                }
                else
                {
                    newElement.addElement( "value" ).setText( ( String ) value );
                }
            }

            if ( extensibleMatchFilter.isDnAttributes() )
            {
                newElement.addAttribute( "dnAttributes", "true" );
            }

            String matchingRule = extensibleMatchFilter.getMatchingRule();
            if ( ( matchingRule != null ) && ( "".equals( matchingRule ) ) )
            {
                newElement.addAttribute( "matchingRule", matchingRule );
            }
        }
    }
}
