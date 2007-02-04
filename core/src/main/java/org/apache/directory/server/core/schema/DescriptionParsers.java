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
package org.apache.directory.server.core.schema;

import java.text.ParseException;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.syntax.AttributeTypeDescription;
import org.apache.directory.shared.ldap.schema.syntax.parser.AttributeTypeDescriptionSchemaParser;

/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DescriptionParsers
{
    private static final String OTHER_SCHEMA = "other";
    private static final String X_SCHEMA = "X-SCHEMA";
    private final AttributeTypeDescriptionSchemaParser attributeTypeParser = new AttributeTypeDescriptionSchemaParser();
    private final Registries globalRegistries;
    
    
    public DescriptionParsers( Registries globalRegistries )
    {
        this.globalRegistries = globalRegistries;
    }
    
    private static final String[] EMPTY = new String[0];
    public AttributeType parseAttributeType( Attribute attr ) throws NamingException
    {
        AttributeTypeDescription desc = null;
        
        try
        {
            desc = attributeTypeParser.parseAttributeTypeDescription( ( String ) attr.get() );
        }
        catch ( ParseException e )
        {
            throw new LdapInvalidAttributeValueException( 
                "The following does not conform to the attributeTypeDescription syntax: " + attr.get(), 
                ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
        }
        
        AttributeTypeImpl at = new AttributeTypeImpl( desc.getNumericOid(), globalRegistries );
        at.setCanUserModify( desc.isUserModifiable() );
        at.setCollective( desc.isCollective() );
        at.setDescription( desc.getDescription() );
        at.setEqualityOid( desc.getEqualityMatchingRule() );
        at.setNames( ( String [] ) desc.getNames().toArray( EMPTY ) );
        at.setObsolete( desc.isObsolete() );
        at.setOrderingOid( desc.getOrderingMatchingRule() );
        at.setSingleValue( desc.isSingleValued() );
        at.setSubstrOid( desc.getSubstringsMatchingRule() );
        at.setSuperiorOid( desc.getSuperType() );
        at.setSyntaxOid( desc.getSyntax() );
        at.setUsage( desc.getUsage() );
        
        if ( desc.getExtensions().get( X_SCHEMA ) != null )
        {
            at.setSchema( desc.getExtensions().get( X_SCHEMA ).get( 0 ) );
        }
        else
        {
            at.setSchema( OTHER_SCHEMA );
        }
        
        return at;
    }
}
