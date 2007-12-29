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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;


/**
 * A default implementation of a ServerEntry which should suite most
 * use cases.
 * 
 * This class is final, it should not be extended.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class DefaultServerEntry implements ServerEntry
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultServerEntry.class );

    /** A map containing all the attributes for this entry */
    private Map<AttributeType, ServerAttribute> serverAttributeMap = new HashMap<AttributeType, ServerAttribute>();
    
    /** The objectClass container */
    private ObjectClassAttribute objectClassAttribute;
    
    /** The global registries */
    private final transient Registries registries;
    
    /** A speedup to get the ObjectClass attribute */
    private static transient AttributeType OBJECT_CLASS_AT;
    
    /** An object used to protect the OBJECT_CLASS_AT while initializing it */
    private static final Object MUTEX = new Object();
    
    /** The DN for this entry */
    private LdapDN dn;


    public DefaultServerEntry( LdapDN dn, Registries registries ) throws NamingException
    {
        this.dn = dn;
        this.registries = registries;

        synchronized( MUTEX )
        {
            if ( OBJECT_CLASS_AT == null )
            {
                OBJECT_CLASS_AT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
            }
        }
        
        setObjectClassAttribute( new ObjectClassAttribute( registries ) );
    }


    private ServerAttribute setObjectClassAttribute( ServerAttribute objectClassAttribute ) throws NamingException
    {
        this.objectClassAttribute = (ObjectClassAttribute)objectClassAttribute;
        return serverAttributeMap.put( OBJECT_CLASS_AT, objectClassAttribute );
    }


    private ServerAttribute removeObjectClassAttribute( ServerAttribute objectClassAttribute ) throws NamingException
    {
        this.objectClassAttribute = (ObjectClassAttribute)objectClassAttribute;

        return serverAttributeMap.remove( OBJECT_CLASS_AT );
    }


    public boolean addObjectClass( ObjectClass objectClass, String alias ) throws NamingException
    {
        return objectClassAttribute.addObjectClass( objectClass, alias );
    }


    public boolean addObjectClass( ObjectClass objectClass ) throws NamingException
    {
        return objectClassAttribute.addObjectClass( objectClass );
    }


    public void addObjectClass( ObjectClassAttribute objectClassAttribute ) throws NamingException
    {
        this.objectClassAttribute = objectClassAttribute;
    }


    public boolean hasObjectClass( ObjectClass objectClass )
    {
        return objectClassAttribute.hasObjectClass( objectClass );
    }


    public boolean hasObjectClass( String objectClass )
    {
        try
        {
            ObjectClass oc = registries.getObjectClassRegistry().lookup( objectClass );
            return objectClassAttribute.hasObjectClass( oc );
        }
        catch ( NamingException ne )
        {
            return false;
        }
    }


    public Set<ObjectClass> getAbstractObjectClasses()
    {
        return objectClassAttribute.getAbstractObjectClasses();
    }


    public ObjectClass getStructuralObjectClass()
    {
        return objectClassAttribute.getStructuralObjectClass();
    }


    public Set<ObjectClass> getStructuralObjectClasses()
    {
        return objectClassAttribute.getStructuralObjectClasses();
    }


    public Set<ObjectClass> getAuxiliaryObjectClasses()
    {
        return objectClassAttribute.getAuxiliaryObjectClasses();
    }


    public Set<ObjectClass> getAllObjectClasses()
    {
        return objectClassAttribute.getAllObjectClasses();
    }


    public Set<AttributeType> getMustList()
    {
        return objectClassAttribute.getMustList();
    }


    public Set<AttributeType> getMayList()
    {
        return objectClassAttribute.getMayList();
    }


    public boolean isValid()
    {
        throw new NotImplementedException();
    }


    public boolean isValid( ObjectClass objectClass )
    {
        throw new NotImplementedException();
    }


    /**
     * Returns the attribute associated with an AttributeType
     * 
     * @param the AttributeType we are looking for
     * @return the associated attribute
     */
    public ServerAttribute get( AttributeType attributeType )
    {
        return serverAttributeMap.get( attributeType );
    }


    /**
     * Returns the attribute associated with a String
     * 
     * @param the Attribute ID we are looking for
     * @return the associated attribute
     */
    public ServerAttribute get( String attributeType ) throws NamingException
    {
        return get( registries.getAttributeTypeRegistry().lookup( attributeType ) );
    }


    public List<ServerAttribute> put( ServerAttribute... serverAttributes ) throws NamingException
    {
        List<ServerAttribute> duplicatedAttributes = new ArrayList<ServerAttribute>();
        
        for ( ServerAttribute serverAttribute:serverAttributes )
        {
            if ( serverAttribute.getType().equals( OBJECT_CLASS_AT ) )
            {
                if ( serverAttribute instanceof ObjectClassAttribute )
                {
                    setObjectClassAttribute( ( ObjectClassAttribute ) serverAttribute );
                }
                else
                {
                    ObjectClassAttribute objectClassAttribute = new ObjectClassAttribute( registries );
                    
                    for ( ServerValue<?> val : serverAttribute )
                    {
                        objectClassAttribute.add( (ServerStringValue)val );
                    }
                    
                    setObjectClassAttribute( objectClassAttribute );
                }
            }

            if ( serverAttributeMap.containsKey( serverAttribute.getType() ) )
            {
                duplicatedAttributes.add( serverAttribute );
            }
            else
            {
                serverAttributeMap.put( serverAttribute.getType(), serverAttribute );
            }
        }
        
        return duplicatedAttributes;
    }


    public ServerAttribute put( String upId, AttributeType attributeType ) throws NamingException
    {
        throw new NotImplementedException();
    }


    /**
     * Put an attribute (represented by its ID and values) into an entry. 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     *
     * @param upId The attribute ID
     * @param values The list of values to inject. It can be empty
     * @return The replaced attribute
     * @throws NamingException If the attribute does not exist
     */
    public ServerAttribute put( String upId, String... values ) throws NamingException
    {
        AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( upId );
        ServerAttribute existing = serverAttributeMap.get( attributeType );

        for ( String value:values )
        {
            put( attributeType, value );
        }
        
        return existing;
    }


    /**
     * Put an attribute (represented by its ID and values) into an entry. 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     *
     * @param upId The attribute ID
     * @param values The list of values to inject. It can be empty
     * @return The replaced attribute
     * @throws NamingException If the attribute does not exist
     */
    public ServerAttribute put( String upId, byte[]... values ) throws NamingException
    {
        AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( upId );
        ServerAttribute existing = serverAttributeMap.get( attributeType );

        for ( byte[] value:values )
        {
            put( attributeType, value );
        }
        
        return existing;
    }


    public ServerAttribute put( AttributeType attributeType ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public ServerAttribute put( String upId ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public List<ServerAttribute> remove( ServerAttribute... serverAttributes ) throws NamingException
    {
        List<ServerAttribute> removedAttributes = new ArrayList<ServerAttribute>();
        
        for ( ServerAttribute serverAttribute:serverAttributes )
        {
            if ( serverAttribute.getType().equals( OBJECT_CLASS_AT ) )
            {
                removeObjectClassAttribute( new ObjectClassAttribute( registries ) );
            }

            if ( serverAttributeMap.containsKey( serverAttribute.getType() ) )
            {
                serverAttributeMap.remove( serverAttribute.getType() );
                removedAttributes.add( serverAttribute );
            }
        }
        
        return removedAttributes;
    }


    public ServerAttribute put( AttributeType attributeType, ServerValue<?>... values ) throws NamingException
    {
        ServerAttribute existing = serverAttributeMap.get( attributeType );

        if ( existing != null )
        {
            return put( existing.getUpId(), attributeType, values );
        }
        else
        {
            return put( null, attributeType, values );
        }
    }


    public ServerAttribute put( String upId, AttributeType attributeType, ServerValue<?>... vals ) throws NamingException
    {
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            return setObjectClassAttribute( new ObjectClassAttribute( registries, upId, vals ) );
        }

        return serverAttributeMap.put( attributeType, new DefaultServerAttribute( upId, attributeType, vals ) );
    }


    public ServerAttribute put( String upId, ServerValue<?>... vals ) throws NamingException
    {
        assert registries != null : "The AttributeType registry should not be null";
        
        AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( upId );
        
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            return setObjectClassAttribute( new ObjectClassAttribute( registries, upId, vals ) );
        }

        return serverAttributeMap.put( attributeType, new DefaultServerAttribute( upId, attributeType, vals ) );
    }


    public ServerAttribute put( AttributeType attributeType, String... vals ) throws NamingException
    {
        ServerAttribute existing = serverAttributeMap.get( attributeType );

        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            if ( existing != null )
            {
                return setObjectClassAttribute( new ObjectClassAttribute( registries, existing.getUpId(), vals ) );
            }

            return setObjectClassAttribute( new ObjectClassAttribute( registries, OBJECT_CLASS_AT.getName(), vals ) );
        }

        if ( existing != null )
        {
            return put( existing.getUpId(), attributeType, vals );
        }
        else
        {
            return put( null, attributeType, vals );
        }
    }


    public ServerAttribute put( String upId, AttributeType attributeType, String... values ) throws NamingException
    {
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            return setObjectClassAttribute( new ObjectClassAttribute( registries, upId, values ) );
        }

        return serverAttributeMap.put( attributeType, new DefaultServerAttribute( upId, attributeType, values ) );
    }


    public ServerAttribute put( AttributeType attributeType, byte[]... vals ) throws NamingException
    {
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            throw new UnsupportedOperationException( "Only String values supported for objectClass attribute" );
        }

        ServerAttribute existing = serverAttributeMap.get( attributeType );

        if ( existing != null )
        {
            return put( existing.getUpId(), attributeType, vals );
        }
        else
        {
            return put( null, attributeType, vals );
        }
    }


    public ServerAttribute put( String upId, AttributeType attributeType, byte[]... vals ) throws NamingException
    {
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            throw new UnsupportedOperationException( "Only String values supported for objectClass attribute" );
        }

        return serverAttributeMap.put( attributeType, new DefaultServerAttribute( upId, attributeType, vals ) );
    }


    public List<ServerAttribute> remove( AttributeType... attributeTypes ) throws NamingException
    {
        if ( attributeTypes == null )
        {
            return null;
        }
        
        List<ServerAttribute> attributes = new ArrayList<ServerAttribute>( attributeTypes.length );
        
        for ( AttributeType attributeType:attributeTypes )
        {
            if ( attributeType.equals( OBJECT_CLASS_AT ) )
            {
                attributes.add( setObjectClassAttribute( new ObjectClassAttribute( registries ) ) );
            }
            else
            {
                attributes.add( serverAttributeMap.remove( attributeType ) );
            }
        }
        
        return attributes;
    }


    public List<ServerAttribute> remove( String... upIds ) throws NamingException
    {
        assert registries != null : "The AttributeType registry should not be null";

        if ( upIds == null )
        {
            return null;
        }
        
        List<ServerAttribute> attributes = new ArrayList<ServerAttribute>( upIds.length );
        
        for ( String upId:upIds )
        {
            AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( upId );
    
            if ( attributeType.equals( OBJECT_CLASS_AT ) )
            {
                attributes.add( setObjectClassAttribute( new ObjectClassAttribute( registries ) ) );
            }
            else
            {
                attributes.add( serverAttributeMap.remove( attributeType ) );
            }
        }
        
        return attributes;
    }


    public void clear()
    {
        serverAttributeMap.clear();

        try
        {
            setObjectClassAttribute( new ObjectClassAttribute( registries ) );
        }
        catch ( NamingException e )
        {
            String msg = "failed to properly set the objectClass attribute on clear";
            LOG.error( msg, e );
            throw new IllegalStateException( msg, e );
        }
    }


    public LdapDN getDn()
    {
        return dn;
    }


    public void setDn( LdapDN dn )
    {
        this.dn = dn;
    }


    public Iterator<ServerAttribute> iterator()
    {
        return Collections.unmodifiableMap( serverAttributeMap ).values().iterator();
    }


    public int size()
    {
        return serverAttributeMap.size();
    }
    
    
    public ServerEntry clone()
    {
        try
        {
            DefaultServerEntry clone = (DefaultServerEntry)super.clone();
            
            clone.dn = (LdapDN)dn.clone();
            //clone.objectClassAttribute = objectClassAttribute.clone();
            return clone;
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
    }
    

    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param attribute The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If there is a problem
     */
    public boolean contains( ServerAttribute attribute, Value<?> value ) throws NamingException
    {
        if ( attribute == null )
        {
            return false;
        }
        
        if ( serverAttributeMap.containsKey( attribute.getType() ) )
        {
            return serverAttributeMap.get( attribute.getType() ).contains( (ServerValue<?>)value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param id The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If the attribute does not exists
     */
    public boolean contains( String id, Value<?> value ) throws NamingException
    {
        if ( id == null )
        {
            return false;
        }
        
        AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( id );
        
        if ( attributeType == null )
        {
            return false;
        }
        else if ( serverAttributeMap.containsKey( attributeType ) )
        {
            return serverAttributeMap.get( attributeType ).contains( (ServerValue<?>)value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param id The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If the attribute does not exists
     */
    public boolean contains( String id, String value ) throws NamingException
    {
        if ( id == null )
        {
            return false;
        }
        
        AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( id );
        
        if ( attributeType == null )
        {
            return false;
        }
        else if ( serverAttributeMap.containsKey( attributeType ) )
        {
            return serverAttributeMap.get( attributeType ).contains( value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Checks if an entry contains an attribute with a given value.
     *
     * @param id The Attribute we are looking for
     * @param value The searched value
     * @return <code>true</code> if the value is found within the attribute
     * @throws NamingException If the attribute does not exists
     */
    public boolean contains( String id, byte[] value ) throws NamingException
    {
        if ( id == null )
        {
            return false;
        }
        
        AttributeType attributeType = registries.getAttributeTypeRegistry().lookup( id );
        
        if ( attributeType == null )
        {
            return false;
        }
        else if ( serverAttributeMap.containsKey( attributeType ) )
        {
            return serverAttributeMap.get( attributeType ).contains( value );
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * Gets all the attributes type (ObjectClasses, May and Must)
     *
     * @return The combined set of all the attributes, including ObjectClass.
     */
    public Set<AttributeType> getAttributeTypes()
    {
        return serverAttributeMap.keySet();
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "DefaultEntryServer\n" );
        sb.append( "    dn: " ).append( dn ).append( '\n' );
        
        if ( objectClassAttribute != null )
        {
            sb.append( "    " ).append( objectClassAttribute );
        }

        if ( serverAttributeMap.size() != 0 )
        {
            for ( ServerAttribute attribute:serverAttributeMap.values() )
            {
                if ( !attribute.getType().equals( OBJECT_CLASS_AT ) )
                {
                    sb.append( "    " ).append( attribute );
                }
            }
        }
        
        return sb.toString();
    }
}
