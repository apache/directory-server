/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.entry;

import java.util.Iterator;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InvalidAttributeIdentifierException;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;

/**
 * A helper class used to manipulate Entries, Attributes and Values.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerEntryUtils
{
    /**
     * Convert a ServerEntry into a AttributesImpl. The DN is lost
     * during this conversion, as the Attributes object does not store
     * this element.
     *
     * @return An instance of a AttributesImpl() object
     */
    public static Attributes toAttributesImpl( ServerEntry entry )
    {
        Attributes attributes = new AttributesImpl();

        for ( AttributeType attributeType:entry.getAttributeTypes() )
        {
            Attribute attribute = new AttributeImpl( attributeType.getName() );
            
            ServerAttribute attr = entry.get( attributeType );
            
            for ( Iterator<ServerValue<?>> iter = attr.iterator(); iter.hasNext();)
            {
                ServerValue<?> value = iter.next();
                attribute.add( value.get() );
            }
            
            attributes.put( attribute );
        }
        
        return attributes;
    }

    
    /**
     * Convert a BasicAttribute or a AttributeImpl to a ServerAtribute
     *
     * @param attributes the BasicAttributes or AttributesImpl instance to convert
     * @param registries The registries, needed ro build a ServerEntry
     * @param dn The DN which is needed by the ServerEntry 
     * @return An instance of a ServerEntry object
     * 
     * @throws InvalidAttributeIdentifierException If we had an incorrect attribute
     */
    public static ServerAttribute toServerAttribute( Attribute attribute, AttributeType attributeType )
            throws InvalidAttributeIdentifierException
    {
        try 
        {
            ServerAttribute serverAttribute = new DefaultServerAttribute( attributeType );
        
            for ( NamingEnumeration<?> values = attribute.getAll(); values.hasMoreElements(); )
            {
                Object value = values.nextElement();
                
                if ( value instanceof String )
                {
                    serverAttribute.add( (String)value );
                }
                else if ( value instanceof byte[] )
                {
                    serverAttribute.add( (byte[])value );
                }
                else
                {
                    return null;
                }
            }
            
            return serverAttribute;
        }
        catch ( NamingException ne )
        {
            return null;
        }
    }
    

    /**
     * Convert a BasicAttributes or a AttributesImpl to a ServerEntry
     *
     * @param attributes the BasicAttributes or AttributesImpl instance to convert
     * @param registries The registries, needed ro build a ServerEntry
     * @param dn The DN which is needed by the ServerEntry 
     * @return An instance of a ServerEntry object
     * 
     * @throws InvalidAttributeIdentifierException If we get an invalid attribute
     */
    public static ServerEntry toServerEntry( Attributes attributes, LdapDN dn, Registries registries ) 
            throws InvalidAttributeIdentifierException
    {
        if ( ( attributes instanceof BasicAttributes ) || ( attributes instanceof AttributesImpl ) )
        {
            try 
            {
                ServerEntry entry = new DefaultServerEntry( dn, registries );
    
                for ( NamingEnumeration<? extends Attribute> attrs = attributes.getAll(); attrs.hasMoreElements(); )
                {
                    Attribute attr = attrs.nextElement();

                    AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( attr.getID() );
                    ServerAttribute serverAttribute = ServerEntryUtils.toServerAttribute( attr, attributeType );
                    
                    if ( serverAttribute != null )
                    {
                        entry.put( serverAttribute );
                    }
                }
                
                return entry;
            }
            catch ( NamingException ne )
            {
                throw new InvalidAttributeIdentifierException( ne.getMessage() );
            }
        }
        else
        {
            return null;
        }
    }


    /**
     * Convert a ServerEntry into a BasicAttributes. The DN is lost
     * during this conversion, as the Attributes object does not store
     * this element.
     *
     * @return An instance of a BasicAttributes() object
     */
    public static Attributes toBasicAttributes( ServerEntry entry )
    {
        Attributes attributes = new BasicAttributes( true );

        for ( AttributeType attributeType:entry.getAttributeTypes() )
        {
            Attribute attribute = new BasicAttribute( attributeType.getName(), true );
            
            ServerAttribute attr = entry.get( attributeType );
            
            for ( Iterator<ServerValue<?>> iter = attr.iterator(); iter.hasNext();)
            {
                ServerValue<?> value = iter.next();
                attribute.add( value );
            }
            
            attributes.put( attribute );
        }
        
        return attributes;
    }
    
    
    /**
     * Convert a ServerAttributeEntry into a BasicAttribute.
     *
     * @return An instance of a BasicAttribute() object
     */
    public static Attribute toBasicAttribute( ServerAttribute attr )
    {
        Attribute attribute = new BasicAttribute( attr.getUpId(), false );

        for ( Iterator<ServerValue<?>> iter = attr.getAll(); iter.hasNext();)
        {
            Value<?> value = iter.next();
            
            attribute.add( value.get() );
        }
        
        return attribute;
    }


    /**
     * Convert a ServerAttributeEntry into a AttributeImpl.
     *
     * @return An instance of a BasicAttribute() object
     */
    public static Attribute toAttributeImpl( ServerAttribute attr )
    {
        Attribute attribute = new AttributeImpl( attr.getUpId() );

        for ( Iterator<ServerValue<?>> iter = attr.getAll(); iter.hasNext();)
        {
            Value<?> value = iter.next();
            
            attribute.add( value.get() );
        }
        
        return attribute;
    }
}
