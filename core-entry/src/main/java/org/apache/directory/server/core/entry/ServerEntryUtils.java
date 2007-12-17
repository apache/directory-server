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

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
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
    public static Attributes toAttributesImpl( ServerEntry<?> entry )
    {
        Attributes attributes = new AttributesImpl();

        for ( AttributeType attributeType:entry.getAttributeTypes() )
        {
            Attribute attribute = new AttributeImpl( attributeType.getName() );
            
            ServerAttribute<?> attr = entry.get( attributeType );
            
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
     * Convert a ServerEntry into a BasicAttributes. The DN is lost
     * during this conversion, as the Attributes object does not store
     * this element.
     *
     * @return An instance of a BasicAttributes() object
     */
    public static Attributes toBasicAttributes( ServerEntry<?> entry )
    {
        Attributes attributes = new BasicAttributes( true );

        for ( AttributeType attributeType:entry.getAttributeTypes() )
        {
            Attribute attribute = new BasicAttribute( attributeType.getName(), true );
            
            ServerAttribute<?> attr = entry.get( attributeType );
            
            for ( Iterator<ServerValue<?>> iter = attr.iterator(); iter.hasNext();)
            {
                ServerValue<?> value = iter.next();
                attribute.add( value );
            }
            
            attributes.put( attribute );
        }
        
        return attributes;
    }
}
