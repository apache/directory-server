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


import java.util.Iterator;
import java.util.List;

import org.apache.directory.shared.dsmlv2.ParserUtils;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestCodec;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;


/**
 * DSML Decorator for ModifyRequest
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ModifyRequestDsml extends AbstractRequestDsml
{
    /**
     * Creates a new instance of ModifyRequestDsml.
     */
    public ModifyRequestDsml()
    {
        super( new ModifyRequestCodec() );
    }


    /**
     * Creates a new instance of ModifyRequestDsml.
     *
     * @param ldapMessage
     *      the message to decorate
     */
    public ModifyRequestDsml( ModifyRequestCodec ldapMessage )
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

        ModifyRequestCodec request = ( ModifyRequestCodec ) instance;

        // DN
        if ( request.getObject() != null )
        {
            element.addAttribute( "dn", request.getObject().getName() );
        }

        // Modifications
        List<Modification> modifications = request.getModifications();

        for ( int i = 0; i < modifications.size(); i++ )
        {
            Modification modificationItem = modifications.get( i );

            Element modElement = element.addElement( "modification" );
            if ( modificationItem.getAttribute() != null )
            {
                modElement.addAttribute( "name", modificationItem.getAttribute().getId() );

                Iterator<Value<?>> iterator = modificationItem.getAttribute().getAll();
                while ( iterator.hasNext() )
                {
                    Value<?> value = iterator.next();

                    if ( value.get() != null )
                    {
                        if ( ParserUtils.needsBase64Encoding( value.get() ) )
                        {
                            Namespace xsdNamespace = new Namespace( "xsd", ParserUtils.XML_SCHEMA_URI );
                            Namespace xsiNamespace = new Namespace( "xsi", ParserUtils.XML_SCHEMA_INSTANCE_URI );
                            element.getDocument().getRootElement().add( xsdNamespace );
                            element.getDocument().getRootElement().add( xsiNamespace );

                            Element valueElement = modElement.addElement( "value" ).addText(
                                ParserUtils.base64Encode( value.get() ) );
                            valueElement.addAttribute( new QName( "type", xsiNamespace ), "xsd:"
                                + ParserUtils.BASE64BINARY );
                        }
                        else
                        {
                            modElement.addElement( "value" ).setText( value.getString() );
                        }
                    }
                }
            }

            ModificationOperation operation = modificationItem.getOperation();
            if ( operation == ModificationOperation.ADD_ATTRIBUTE )
            {
                modElement.addAttribute( "operation", "add" );
            }
            else if ( operation == ModificationOperation.REPLACE_ATTRIBUTE )
            {
                modElement.addAttribute( "operation", "replace" );
            }
            else if ( operation == ModificationOperation.REMOVE_ATTRIBUTE )
            {
                modElement.addAttribute( "operation", "delete" );
            }
        }

        return element;
    }
}
