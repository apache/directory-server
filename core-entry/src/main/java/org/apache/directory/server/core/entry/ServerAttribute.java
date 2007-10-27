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


import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.schema.AttributeType;

import javax.naming.NamingException;
import java.util.HashSet;
import java.util.Iterator;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerAttribute implements EntryAttribute<ServerValue<?>>
{
    private HashSet<ServerValue<?>> values = new HashSet<ServerValue<?>>();
    private AttributeType attributeType;


    public ServerAttribute( AttributeType attributeType )
    {
        this.attributeType = attributeType;
    }


    /**
     * Gets the attribute type associated with this ServerAttribute.
     *
     * @return the attributeType associated with this entry attribute
     */
    public AttributeType getType()
    {
        return attributeType;
    }


    /**
     * Checks to see if this attribute is valid along with the values it contains.
     *
     * @return true if the attribute and it's values are valid, false otherwise
     * @throws NamingException if there is a failure to check syntaxes of values
     */
    public boolean isValid() throws NamingException
    {
        if ( attributeType.isSingleValue() && values.size() > 1 )
        {
            return false;
        }

        for ( ServerValue value : values )
        {
            if ( ! value.isValid() )
            {
                return false;
            }
        }

        return true;
    }


    public boolean add( ServerValue<?> val )
    {
        return values.add( val );
    }


    public boolean add( String val )
    {
        return values.add( new ServerStringValue( attributeType, val ) );
    }


    public boolean add( byte[] val )
    {
        return values.add( new ServerBinaryValue( attributeType, val ) );
    }


    public void clear()
    {
        values.clear();
    }


    public boolean contains( ServerValue<?> val )
    {
        return values.contains( val );
    }


    public boolean contains( String val )
    {
        ServerStringValue ssv = new ServerStringValue( attributeType, val );
        return values.contains( ssv );
    }


    public boolean contains( byte[] val )
    {
        ServerBinaryValue sbv = new ServerBinaryValue( attributeType, val );
        return values.contains( sbv );
    }


    public Value<?> get()
    {
        if ( values.isEmpty() )
        {
            return null;
        }
        
        return values.iterator().next();
    }


    public Iterator<? extends ServerValue<?>> getAll()
    {
        return values.iterator();
    }


    public int size()
    {
        return values.size();
    }


    public boolean remove( ServerValue<?> val )
    {
        return values.remove( val );
    }


    public boolean remove( byte[] val )
    {
        ServerBinaryValue sbv = new ServerBinaryValue( attributeType, val );
        return values.remove( sbv );
    }


    public boolean remove( String val )
    {
        ServerStringValue ssv = new ServerStringValue( attributeType, val );
        return values.remove( ssv );
    }
}
