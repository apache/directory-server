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

package org.apache.directory.shared.dsmlv2.reponse;


import org.apache.directory.shared.dsmlv2.DsmlDecorator;
import org.apache.directory.shared.dsmlv2.ParserUtils;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryCodec;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;


/**
 * DSML Decorator for SearchResultEntry
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchResultEntryDsml extends LdapResponseDecorator implements DsmlDecorator
{
    /**
     * Creates a new instance of SearchResultEntryDsml.
     */
    public SearchResultEntryDsml()
    {
        super( new SearchResultEntryCodec() );
    }


    /**
     * Creates a new instance of SearchResultEntryDsml.
     *
     * @param ldapMessage
     *      the message to decorate
     */
    public SearchResultEntryDsml( SearchResultEntryCodec ldapMessage )
    {
        super( ldapMessage );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.dsmlv2.reponse.LdapMessageDecorator#getMessageType()
     */
    public int getMessageType()
    {
        return instance.getMessageType();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.dsmlv2.reponse.DsmlDecorator#toDsml(org.dom4j.Element)
     */
    public Element toDsml( Element root )
    {
        Element element = root.addElement( "searchResultEntry" );
        SearchResultEntryCodec searchResultEntry = ( SearchResultEntryCodec ) instance;
        element.addAttribute( "dn", searchResultEntry.getObjectName().getName() );

        Entry entry = searchResultEntry.getEntry();
        for ( EntryAttribute attribute : entry )
        {

            Element attributeElement = element.addElement( "attr" );
            attributeElement.addAttribute( "name", attribute.getId() );

            for ( Value<?> value : attribute )
            {
                if ( ParserUtils.needsBase64Encoding( value.get() ) )
                {
                    Namespace xsdNamespace = new Namespace( ParserUtils.XSD, ParserUtils.XML_SCHEMA_URI );
                    Namespace xsiNamespace = new Namespace( ParserUtils.XSI, ParserUtils.XML_SCHEMA_INSTANCE_URI );
                    attributeElement.getDocument().getRootElement().add( xsdNamespace );
                    attributeElement.getDocument().getRootElement().add( xsiNamespace );

                    Element valueElement = attributeElement.addElement( "value" ).addText(
                        ParserUtils.base64Encode( value.get() ) );
                    valueElement.addAttribute( new QName( "type", xsiNamespace ), ParserUtils.XSD + ":"
                        + ParserUtils.BASE64BINARY );
                }
                else
                {
                    attributeElement.addElement( "value" ).addText( value.getString() );
                }
            }
        }

        return element;
    }


    /**
     * Get the entry DN
     * 
     * @return Returns the objectName.
     */
    public LdapDN getObjectName()
    {
        return ( ( SearchResultEntryCodec ) instance ).getObjectName();
    }


    /**
     * Set the entry DN
     * 
     * @param objectName The objectName to set.
     */
    public void setObjectName( LdapDN objectName )
    {
        ( ( SearchResultEntryCodec ) instance ).setObjectName( objectName );
    }


    /**
     * Get the entry.
     * 
     * @return Returns the entry.
     */
    public Entry getEntry()
    {
        return ( ( SearchResultEntryCodec ) instance ).getEntry();
    }


    /**
     * Initialize the entry.
     * 
     * @param entry the entry
     */
    public void setEntry( Entry entry )
    {
        ( ( SearchResultEntryCodec ) instance ).setEntry( entry );
    }


    /**
     * Create a new attributeValue
     * 
     * @param type The attribute's name
     */
    public void addAttributeValues( String type )
    {
        ( ( SearchResultEntryCodec ) instance ).addAttributeValues( type );
    }


    /**
     * Add a new value to the current attribute
     * 
     * @param value
     */
    public void addAttributeValue( Object value )
    {
        ( ( SearchResultEntryCodec ) instance ).addAttributeValue( value );
    }


    /**
     * @return Returns the currentAttributeValue.
     */
    public String getCurrentAttributeValueType()
    {
        return ( ( SearchResultEntryCodec ) instance ).getCurrentAttributeValueType();
    }
}
