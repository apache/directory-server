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
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
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

    private Set<ObjectClass> allObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> abstractObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> auxiliaryObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> structuralObjectClasses = new HashSet<ObjectClass>();

    private Set<AttributeType> mayList = new HashSet<AttributeType>();
    private Set<AttributeType> mustList = new HashSet<AttributeType>();

    private Map<AttributeType, ServerAttribute> serverAttributeMap = new HashMap<AttributeType, ServerAttribute>();
    private final transient Registries registries;
    private transient AttributeType objectClassAT;
    private LdapDN dn;


    public DefaultServerEntry( LdapDN dn, Registries registries ) throws NamingException
    {
        this.dn = dn;
        this.registries = registries;

        objectClassAT = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        setObjectClassAttribute( new ServerAttribute( objectClassAT ) );
    }


    private ServerAttribute setObjectClassAttribute( ServerAttribute objectClassAttribute ) throws NamingException
    {
        // grab the current value before we clear all sets of schema entities
        ServerAttribute temp = serverAttributeMap.get( objectClassAT );

        mayList.clear();
        mustList.clear();
        allObjectClasses.clear();
        auxiliaryObjectClasses.clear();
        structuralObjectClasses.clear();
        abstractObjectClasses.clear();

        // if the OC has nothing in it then just setup instead of searching below
        if ( objectClassAttribute.size() == 0 )
        {
            serverAttributeMap.put( objectClassAT, objectClassAttribute );
            return temp;
        }

        // add all objectclasses as provided by the objectClassAttribute
        for ( ServerValue value : objectClassAttribute )
        {
            ObjectClass objectClass = registries.getObjectClassRegistry().lookup(
                    ( String ) value.getNormalizedValue() );

            allObjectClasses.add( objectClass );
        }

        // copy all the existing object classes so we can add ancestors while iterating
        Set<ObjectClass> copied = new HashSet<ObjectClass>();
        copied.addAll( allObjectClasses );
        for ( ObjectClass objectClass : copied )
        {
            allObjectClasses.addAll( addAncestors( objectClass, new HashSet<ObjectClass>() ) );
        }

        // now create sets of the different kinds of objectClasses
        for ( ObjectClass objectClass : allObjectClasses )
        {
            switch ( objectClass.getType().getValue() )
            {
                case( ObjectClassTypeEnum.STRUCTURAL_VAL ):
                    structuralObjectClasses.add( objectClass );
                    break;
                case( ObjectClassTypeEnum.AUXILIARY_VAL ):
                    auxiliaryObjectClasses.add( objectClass );
                    break;
                case( ObjectClassTypeEnum.ABSTRACT_VAL ):
                    abstractObjectClasses.add( objectClass );
                    break;
                default:
                    throw new IllegalStateException( "Unrecognized objectClass type value: " + objectClass.getType() );
            }

            // now go through all objectClassses to collect the must an may list attributes
            Collections.addAll( mayList, objectClass.getMayList() );
            Collections.addAll( mustList, objectClass.getMustList() );
        }

        serverAttributeMap.put( objectClassAT, objectClassAttribute );
        return temp;
    }


    private Set<ObjectClass> addAncestors( ObjectClass descendant, Set<ObjectClass> ancestors ) throws NamingException
    {
        if ( descendant == null )
        {
            return ancestors;
        }

        ObjectClass[] superClasses = descendant.getSuperClasses();
        if ( superClasses == null || superClasses.length == 0 )
        {
            return ancestors;
        }

        for ( ObjectClass ancestor : superClasses )
        {
            ancestors.add( ancestor );
            addAncestors( ancestor, ancestors );
        }

        return ancestors;
    }


    public boolean addObjectClass( ObjectClass objectClass, String alias ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public boolean addObjectClass( ObjectClass objectClass ) throws NamingException
    {
        if ( allObjectClasses.contains( objectClass ) )
        {
            return false;
        }

        ServerAttribute serverAttribute = serverAttributeMap.get( objectClassAT );
        String name = objectClass.getName();

        if ( name == null )
        {
            name = objectClass.getOid();
        }

        serverAttribute.add( name );
        Set<ObjectClass> ancestors = addAncestors( objectClass, new HashSet<ObjectClass>() );
        ancestors.add( objectClass );
        // now create sets of the different kinds of objectClasses
        for ( ObjectClass oc : ancestors )
        {
            switch ( oc.getType().getValue() )
            {
                case( ObjectClassTypeEnum.STRUCTURAL_VAL ):
                    structuralObjectClasses.add( oc );
                    break;
                case( ObjectClassTypeEnum.AUXILIARY_VAL ):
                    auxiliaryObjectClasses.add( oc );
                    break;
                case( ObjectClassTypeEnum.ABSTRACT_VAL ):
                    abstractObjectClasses.add( oc );
                    break;
                default:
                    throw new IllegalStateException( "Unrecognized objectClass type value: " + oc.getType() );
            }

            // now go through all objectClassses to collect the must an may list attributes
            Collections.addAll( mayList, oc.getMayList() );
            Collections.addAll( mustList, oc.getMustList() );
        }

        return true;
    }


    public boolean hasObjectClass( ObjectClass objectClass )
    {
        return allObjectClasses.contains( objectClass );
    }


    public Set<ObjectClass> getAbstractObjectClasses()
    {
        return Collections.unmodifiableSet( abstractObjectClasses );
    }


    public ObjectClass getStructuralObjectClass()
    {
        if ( structuralObjectClasses.isEmpty() )
        {
            return null;
        }
        return structuralObjectClasses.iterator().next();
    }


    public Set<ObjectClass> getStructuralObjectClasses()
    {
        return Collections.unmodifiableSet( structuralObjectClasses );
    }


    public Set<ObjectClass> getAuxiliaryObjectClasses()
    {
        return Collections.unmodifiableSet( auxiliaryObjectClasses );
    }


    public Set<ObjectClass> getAllObjectClasses()
    {
        return Collections.unmodifiableSet( allObjectClasses );
    }


    public Set<AttributeType> getMustList()
    {
        return Collections.unmodifiableSet( mustList );
    }


    public Set<AttributeType> getMayList()
    {
        return Collections.unmodifiableSet( mayList );
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
        if ( serverAttribute.getType().equals( objectClassAT ) )
        {
            return setObjectClassAttribute( serverAttribute );
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
            return setObjectClassAttribute( serverAttribute );
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
            String name = attributeType.getName();
            if ( name == null )
            {
                name = attributeType.getOid();
            }
            return put( name, attributeType, val );
        }
    }


    public ServerAttribute put( String upId, AttributeType attributeType, ServerValue<?> val ) throws NamingException
    {
        ServerAttribute serverAttribute = new ServerAttribute( upId, attributeType, val );

        if ( attributeType.equals( objectClassAT ) )
        {
            return setObjectClassAttribute( serverAttribute );
        }

        return serverAttributeMap.put( attributeType, serverAttribute );
    }


    public ServerAttribute put( AttributeType attributeType, String val ) throws NamingException
    {
        ServerAttribute existing = serverAttributeMap.get( attributeType );

        if ( existing != null )
        {
            return put( existing.getUpId(), attributeType, val );
        }
        else
        {
            String name = attributeType.getName();
            if ( name == null )
            {
                name = attributeType.getOid();
            }
            return put( name, attributeType, val );
        }
    }


    public ServerAttribute put( String upId, AttributeType attributeType, String val ) throws NamingException
    {
        ServerAttribute serverAttribute = new ServerAttribute( upId, attributeType, val );

        if ( attributeType.equals( objectClassAT ) )
        {
            return setObjectClassAttribute( serverAttribute );
        }

        return serverAttributeMap.put( attributeType, serverAttribute );
    }


    public ServerAttribute put( AttributeType attributeType, byte[] val ) throws NamingException
    {
        ServerAttribute existing = serverAttributeMap.get( attributeType );

        if ( existing != null )
        {
            return put( existing.getUpId(), attributeType, val );
        }
        else
        {
            String name = attributeType.getName();
            if ( name == null )
            {
                name = attributeType.getOid();
            }
            return put( name, attributeType, val );
        }
    }


    public ServerAttribute put( String upId, AttributeType attributeType, byte[] val ) throws NamingException
    {
        ServerAttribute serverAttribute = new ServerAttribute( upId, attributeType, val );

        if ( attributeType.equals( objectClassAT ) )
        {
            return setObjectClassAttribute( serverAttribute );
        }

        return serverAttributeMap.put( attributeType, serverAttribute );
    }


    public ServerAttribute remove( AttributeType attributeType ) throws NamingException
    {
        if ( attributeType.equals( objectClassAT ) )
        {
            return setObjectClassAttribute( new ServerAttribute( objectClassAT ) );
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
            setObjectClassAttribute( new ServerAttribute( objectClassAT ) );
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
