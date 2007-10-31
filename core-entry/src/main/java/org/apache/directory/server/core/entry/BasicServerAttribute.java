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


import org.apache.directory.shared.ldap.schema.AttributeType;

import javax.naming.NamingException;
import java.util.HashSet;
import java.util.Iterator;


/**
 * A server side entry attribute aware of schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BasicServerAttribute implements ServerAttribute
{
    private HashSet<ServerValue<?>> values = new HashSet<ServerValue<?>>();
    private AttributeType attributeType;
    private String upId;

    // maybe have some additional convenience constructors which take
    // an initial value as a string or a byte[]


    public BasicServerAttribute( AttributeType attributeType )
    {
        this.attributeType = attributeType;
        setUpId( null, attributeType );
    }


    public BasicServerAttribute( String upId, AttributeType attributeType )
    {
        this.attributeType = attributeType;
        setUpId( upId, attributeType );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ServerValue which uses the specified
     * attributeType.
     *
     * @param attributeType
     * @param val
     * @throws NamingException
     */
    public BasicServerAttribute( AttributeType attributeType, ServerValue<?> val ) throws NamingException
    {
        this( null, attributeType, val );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ServerValue which uses the specified
     * attributeType.
     *
     * @param upId
     * @param attributeType
     * @param val
     * @throws NamingException
     */
    public BasicServerAttribute( String upId, AttributeType attributeType, ServerValue<?> val ) throws NamingException
    {
        this.attributeType = attributeType;
        if ( val == null )
        {
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                values.add( new ServerStringValue( attributeType ) );
            }
            else
            {
                values.add( new ServerBinaryValue( attributeType ) );
            }
        }
        else
        {
            if ( attributeType.equals( val.getAttributeType() ) )
            {
                values.add( val );
            }
            else if ( val instanceof ServerStringValue )
            {
                values.add( new ServerStringValue( attributeType, ( String ) val.get() ) );
            }
            else if ( val instanceof ServerBinaryValue )
            {
                values.add( new ServerBinaryValue( attributeType, ( byte[] ) val.get() ) );
            }
            else
            {
                throw new IllegalStateException( "Unknown value type: " + val.getClass().getName() );
            }

            values.add( val );
        }
        setUpId( upId, attributeType );
    }


    public BasicServerAttribute( AttributeType attributeType, String val ) throws NamingException
    {
        this( null, attributeType, val );
    }


    public BasicServerAttribute( String upId, AttributeType attributeType, String val ) throws NamingException
    {
        this.attributeType = attributeType;
        if ( val == null )
        {
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                values.add( new ServerStringValue( attributeType ) );
            }
            else
            {
                values.add( new ServerBinaryValue( attributeType ) );
            }
        }
        else
        {
            values.add( new ServerStringValue( attributeType, val ) );
        }
        setUpId( upId, attributeType );
    }


    public BasicServerAttribute( AttributeType attributeType, byte[] val ) throws NamingException
    {
        this( null, attributeType, val );
    }


    public BasicServerAttribute( String upId, AttributeType attributeType, byte[] val ) throws NamingException
    {
        this.attributeType = attributeType;
        if ( val == null )
        {
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                values.add( new ServerStringValue( attributeType ) );
            }
            else
            {
                values.add( new ServerBinaryValue( attributeType ) );
            }
        }
        else
        {
            values.add( new ServerBinaryValue( attributeType, val ) );
        }
        setUpId( upId, attributeType );
    }


    private void setUpId( String upId, AttributeType attributeType )
    {
        if ( upId == null )
        {
            String name = attributeType.getName();
            if ( name == null )
            {
                this.upId = attributeType.getOid();
            }
            else
            {
                this.upId = name;
            }
        }
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
     * Get's the user provided identifier for this entry.  This is the value
     * that will be used as the identifier for the attribute within the
     * entry.  If this is a commonName attribute for example and the user
     * provides "COMMONname" instead when adding the entry then this is
     * the format the user will have that entry returned by the directory
     * server.  To do so we store this value as it was given and track it
     * in the attribute using this property.
     *
     * @return the user provided identifier for this attribute
     */
    public String getUpId()
    {
        return upId;
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


    public ServerValue<?> get()
    {
        if ( values.isEmpty() )
        {
            return null;
        }
        
        return values.iterator().next();
    }


    public Iterator<? extends ServerValue<?>> getAll()
    {
        return iterator();
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


    public Iterator<ServerValue<?>> iterator()
    {
        return values.iterator();
    }
}
