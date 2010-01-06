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


import javax.naming.NamingException;

import org.apache.directory.shared.dsmlv2.ParserUtils;
import org.apache.directory.shared.ldap.codec.add.AddRequestCodec;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;


/**
 * DSML Decorator for AddRequest
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AddRequestDsml extends AbstractRequestDsml
{
    /**
     * Creates a new instance of AddRequestDsml.
     */
    public AddRequestDsml()
    {
        super( new AddRequestCodec() );
    }


    /**
     * Creates a new instance of AddRequestDsml.
    *
    * @param ldapMessage
    *      the message to decorate
    */
    public AddRequestDsml( AddRequestCodec ldapMessage )
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

        AddRequestCodec request = ( AddRequestCodec ) instance;

        // DN
        if ( request.getEntry() != null )
        {
            element.addAttribute( "dn", request.getEntry().getDn().getName() );
        }

        // Attributes
        Entry entry = request.getEntry();
        if ( entry != null )
        {
            for ( EntryAttribute attribute : entry )
            {
                Element attributeElement = element.addElement( "attr" );
                attributeElement.addAttribute( "name", attribute.getId() );
                // Looping on Values
                for ( Value<?> value : attribute )
                {
                    if ( ParserUtils.needsBase64Encoding( value.get() ) )
                    {
                        Namespace xsdNamespace = new Namespace( "xsd", ParserUtils.XML_SCHEMA_URI );
                        Namespace xsiNamespace = new Namespace( "xsi", ParserUtils.XML_SCHEMA_INSTANCE_URI );
                        attributeElement.getDocument().getRootElement().add( xsdNamespace );
                        attributeElement.getDocument().getRootElement().add( xsiNamespace );

                        Element valueElement = attributeElement.addElement( "value" ).addText(
                            ParserUtils.base64Encode( value.get() ) );
                        valueElement
                            .addAttribute( new QName( "type", xsiNamespace ), "xsd:" + ParserUtils.BASE64BINARY );
                    }
                    else
                    {
                        attributeElement.addElement( "value" ).addText( value.getString() );
                    }
                }
            }
        }

        return element;
    }


    /**
     * Initialize the Entry.
     */
    public void initEntry()
    {
        ( ( AddRequestCodec ) instance ).initEntry();
    }


    /**
     * Get the entry with its attributes.
     * 
     * @return Returns the entry.
     */
    public Entry getEntry()
    {
        return ( ( AddRequestCodec ) instance ).getEntry();
    }


    /**
     * Create a new attributeValue
     * 
     * @param type The attribute's name (called 'type' in the grammar)
     * @throws NamingException 
     */
    public void addAttributeType( String type ) throws NamingException
    {
        ( ( AddRequestCodec ) instance ).addAttributeType( type );
    }


    /**
     * Add a new value to the current attribute
     * 
     * @param value The value to be added
     */
    public void addAttributeValue( Object value )
    {
        if ( value instanceof Value<?> )
        {
            ( ( AddRequestCodec ) instance ).addAttributeValue( ( Value<?> ) value );
        }
        else if ( value instanceof String )
        {
            ( ( AddRequestCodec ) instance ).addAttributeValue( ( String ) value );
        }
        else if ( value instanceof byte[] )
        {
            ( ( AddRequestCodec ) instance ).addAttributeValue( ( byte[] ) value );
        }
    }


    /**
     * Get the added DN
     * 
     * @return Returns the entry DN.
     */
    public LdapDN getEntryDn()
    {
        return ( ( AddRequestCodec ) instance ).getEntryDn();
    }


    /**
     * Set the added DN.
     * 
     * @param entry The entry DN to set.
     */
    public void setEntryDn( LdapDN entryDn )
    {
        ( ( AddRequestCodec ) instance ).setEntryDn( entryDn );
    }


    /**
     * Sets the entry.
     *
     * @param entry
     *      the entry
     */
    public void setEntry( Entry entry )
    {
        ( ( AddRequestCodec ) instance ).setEntry( entry );
    }


    /**
     * @return Returns the currentAttribute type.
     */
    public String getCurrentAttributeType()
    {
        return ( ( AddRequestCodec ) instance ).getCurrentAttributeType();
    }
}
