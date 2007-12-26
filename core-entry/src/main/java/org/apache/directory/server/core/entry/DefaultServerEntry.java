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

import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
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
    
    /** The registries */
    private final transient Registries registries;
    
    /** The AttributeType registry */
    private final transient AttributeTypeRegistry atRegistry;
    
    /** A speedup to get the ObjectClass attribute */
    private transient AttributeType objectClassAT;
    
    /** The DN for this entry */
    private LdapDN dn;


    public DefaultServerEntry( LdapDN dn, Registries registries ) throws NamingException
    {
        this.dn = dn;
        this.registries = registries;
        atRegistry = registries.getAttributeTypeRegistry();

        objectClassAT = atRegistry.lookup( SchemaConstants.OBJECT_CLASS_AT );
        setObjectClassAttribute( new ObjectClassAttribute( registries ) );
    }


    private ServerAttribute setObjectClassAttribute( ServerAttribute objectClassAttribute ) throws NamingException
    {
        this.objectClassAttribute = (ObjectClassAttribute)objectClassAttribute;
        return serverAttributeMap.put( objectClassAT, objectClassAttribute );
    }


    private ServerAttribute removeObjectClassAttribute( ServerAttribute objectClassAttribute ) throws NamingException
    {
        this.objectClassAttribute = (ObjectClassAttribute)objectClassAttribute;

        return serverAttributeMap.remove( objectClassAT );
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
        return get( atRegistry.lookup( attributeType ) );
    }


    public List<ServerAttribute> put( ServerAttribute... serverAttributes ) throws NamingException
    {
        List<ServerAttribute> duplicatedAttributes = new ArrayList<ServerAttribute>();
        
        for ( ServerAttribute serverAttribute:serverAttributes )
        {
            if ( serverAttribute.getType().equals( objectClassAT ) )
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
                        objectClassAttribute.add( val );
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
        AttributeType attributeType = atRegistry.lookup( upId );
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
        AttributeType attributeType = atRegistry.lookup( upId );
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


    public List<ServerAttribute> remove( ServerAttribute... serverAttributes ) throws NamingException
    {
        List<ServerAttribute> removedAttributes = new ArrayList<ServerAttribute>();
        
        for ( ServerAttribute serverAttribute:serverAttributes )
        {
            if ( serverAttribute.getType().equals( objectClassAT ) )
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


    public ServerAttribute put( AttributeType attributeType, ServerValue<?> val ) throws NamingException
    {
        ServerAttribute existing = serverAttributeMap.get( attributeType );

        if ( existing != null )
        {
            return put( existing.getUpId(), attributeType, val );
        }
        else
        {
            return put( null, attributeType, val );
        }
    }


    public ServerAttribute put( String upId, AttributeType attributeType, ServerValue<?> val ) throws NamingException
    {
        if ( attributeType.equals( objectClassAT ) )
        {
            return setObjectClassAttribute( new ObjectClassAttribute( upId, registries, val ) );
        }

        return serverAttributeMap.put( attributeType, new DefaultServerAttribute( upId, attributeType, val ) );
    }


    public ServerAttribute put( AttributeType attributeType, String val ) throws NamingException
    {
        ServerAttribute existing = serverAttributeMap.get( attributeType );

        if ( attributeType.equals( objectClassAT ) )
        {
            if ( existing != null )
            {
                return setObjectClassAttribute( new ObjectClassAttribute( existing.getUpId(), registries, val ) );
            }

            return setObjectClassAttribute( new ObjectClassAttribute( registries, val ) );
        }

        if ( existing != null )
        {
            return put( existing.getUpId(), attributeType, val );
        }
        else
        {
            return put( null, attributeType, val );
        }
    }


    public ServerAttribute put( String upId, AttributeType attributeType, String val ) throws NamingException
    {
        if ( attributeType.equals( objectClassAT ) )
        {
            return setObjectClassAttribute( new ObjectClassAttribute( upId, registries, val ) );
        }

        return serverAttributeMap.put( attributeType, new DefaultServerAttribute( upId, attributeType, val ) );
    }


    public ServerAttribute put( AttributeType attributeType, byte[] val ) throws NamingException
    {
        if ( attributeType.equals( objectClassAT ) )
        {
            throw new UnsupportedOperationException( "Only String values supported for objectClass attribute" );
        }

        ServerAttribute existing = serverAttributeMap.get( attributeType );

        if ( existing != null )
        {
            return put( existing.getUpId(), attributeType, val );
        }
        else
        {
            return put( null, attributeType, val );
        }
    }


    public ServerAttribute put( String upId, AttributeType attributeType, byte[] val ) throws NamingException
    {
        if ( attributeType.equals( objectClassAT ) )
        {
            throw new UnsupportedOperationException( "Only String values supported for objectClass attribute" );
        }

        return serverAttributeMap.put( attributeType, new DefaultServerAttribute( upId, attributeType, val ) );
    }


    public ServerAttribute remove( AttributeType attributeType ) throws NamingException
    {
        if ( attributeType.equals( objectClassAT ) )
        {
            return setObjectClassAttribute( new ObjectClassAttribute( registries ) );
        }
        else
        {
            return serverAttributeMap.remove( attributeType );
        }
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
    
    
    /**
     * Gest all the attributes type (ObjectClasses, May and Must)
     *
     * @return The combined set of all the attributes, including ObjectClass.
     */
    public Set<AttributeType> getAttributeTypes()
    {
        return serverAttributeMap.keySet();
    }
}
