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


import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.*;


/**
 * A default implementation of a ServerEntry which should suite most
 * use cases.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultServerEntry implements ServerEntry
{
    private static final Logger LOG = LoggerFactory.getLogger( DefaultServerEntry.class );

    private Map<AttributeType, ServerAttribute> serverAttributeMap = new HashMap<AttributeType, ServerAttribute>();
    private ObjectClassAttribute objectClassAttribute;
    private final transient Registries registries;
    private transient AttributeType objectClassAT;
    private LdapDN dn;


    public DefaultServerEntry( LdapDN dn, Registries registries ) throws NamingException
    {
        this.dn = dn;
        this.registries = registries;

        objectClassAT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        setObjectClassAttribute( new ObjectClassAttribute( registries ) );
    }


    private ServerAttribute setObjectClassAttribute( ObjectClassAttribute objectClassAttribute ) throws NamingException
    {
        this.objectClassAttribute = objectClassAttribute;
        return serverAttributeMap.put( objectClassAT, objectClassAttribute );
    }


    public boolean addObjectClass( ObjectClass objectClass, String alias ) throws NamingException
    {
        return objectClassAttribute.addObjectClass( objectClass, alias );
    }


    public boolean addObjectClass( ObjectClass objectClass ) throws NamingException
    {
        return objectClassAttribute.addObjectClass( objectClass );
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


    public ServerAttribute get( AttributeType attributeType )
    {
        return serverAttributeMap.get( attributeType );
    }


    public ServerAttribute put( ServerAttribute serverAttribute ) throws NamingException
    {
        if ( serverAttribute.getType().equals( objectClassAT ) && serverAttribute instanceof ObjectClassAttribute )
        {
            return setObjectClassAttribute( ( ObjectClassAttribute ) serverAttribute );
        }

        if ( serverAttribute.getType().equals( objectClassAT ) )
        {
            ObjectClassAttribute objectClassAttribute = new ObjectClassAttribute( registries );
            for ( ServerValue<?> val : serverAttribute )
            {
                objectClassAttribute.add( val );
            }
            return setObjectClassAttribute( objectClassAttribute );
        }

        return serverAttributeMap.put( serverAttribute.getType(), serverAttribute );
    }


    public ServerAttribute put( String upId, AttributeType attributeType ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public ServerAttribute put( AttributeType attributeType ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public ServerAttribute remove( ServerAttribute serverAttribute ) throws NamingException
    {
        if ( serverAttribute.getType().equals( objectClassAT ) )
        {
            return setObjectClassAttribute( new ObjectClassAttribute( registries ) );
        }

        return serverAttributeMap.remove( serverAttribute.getType() );
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

        return serverAttributeMap.put( attributeType, new BasicServerAttribute( upId, attributeType, val ) );
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

        return serverAttributeMap.put( attributeType, new BasicServerAttribute( upId, attributeType, val ) );
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

        return serverAttributeMap.put( attributeType, new BasicServerAttribute( upId, attributeType, val ) );
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
}
