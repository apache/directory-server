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
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Collections;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ObjectClassAttribute implements ServerAttribute
{
    @SuppressWarnings ( { "UnusedDeclaration" } )
    private static final Logger LOG = LoggerFactory.getLogger( ObjectClassAttribute.class );

    private HashSet<ServerValue<?>> values = new HashSet<ServerValue<?>>();
    @SuppressWarnings ( { "FieldCanBeLocal", "UnusedDeclaration" } )
    private Registries registries;
    private AttributeType attributeType;
    private String upId;

    // Sets dealing with objectClass operations
    private Set<ObjectClass> allObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> abstractObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> auxiliaryObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> structuralObjectClasses = new HashSet<ObjectClass>();

    private Set<AttributeType> mayList = new HashSet<AttributeType>();
    private Set<AttributeType> mustList = new HashSet<AttributeType>();



    // maybe have some additional convenience constructors which take
    // an initial value as a string or a byte[]


    public ObjectClassAttribute( Registries registries ) throws NamingException
    {
        this( null, registries );
    }


    public ObjectClassAttribute( String upId, Registries registries ) throws NamingException
    {
        setAttributeTypeAndRegistries( registries );
        setUpId( upId, attributeType );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ServerValue which uses the specified
     * attributeType.
     */
    public ObjectClassAttribute( Registries registries, ServerValue<?> val ) throws NamingException
    {
        this( null, registries, val );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new ServerValue which uses the specified
     * attributeType.
     */
    public ObjectClassAttribute( String upId, Registries registries, ServerValue<?> val ) throws NamingException
    {
        setAttributeTypeAndRegistries( registries );

        if ( val == null )
        {
            values.add( new ServerStringValue( attributeType ) );
        }
        else if ( ! ( val instanceof ServerStringValue ) )
        {
            throw new UnsupportedOperationException( "Only String values supported for objectClass attribute" );
        }
        else
        {
            values.add( val );
        }

        setUpId( upId, attributeType );
    }


    public ObjectClassAttribute( Registries registries, String val ) throws NamingException
    {
        this( null, registries, val );
    }


    public ObjectClassAttribute( String upId, Registries registries, String val ) throws NamingException
    {
        setAttributeTypeAndRegistries( registries );
        if ( val == null )
        {
            values.add( new ServerStringValue( attributeType ) );
        }
        else
        {
            values.add( new ServerStringValue( attributeType, val ) );
        }

        setUpId( upId, attributeType );
    }


    private void setAttributeTypeAndRegistries( Registries registries ) throws NamingException
    {
        this.registries = registries;
        attributeType = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT_OID );
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



    // -----------------------------------------------------------------------


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
        if ( allObjectClasses.contains( objectClass ) )
        {
            return false;
        }

        // add the value to the set of values
        values.add( new ServerStringValue( attributeType, alias) );

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


    public boolean addObjectClass( ObjectClass objectClass ) throws NamingException
    {
        String name = objectClass.getName();

        if ( name == null )
        {
            name = objectClass.getOid();
        }

        return addObjectClass( objectClass, name );
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
        throw new UnsupportedOperationException( "Binary values are not accepted by ObjectClassAttributes" );
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
        throw new UnsupportedOperationException( "There are no binary values in an ObjectClass attribute." );
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
        throw new UnsupportedOperationException( "There are no binary values in an ObjectClass attribute." );
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
