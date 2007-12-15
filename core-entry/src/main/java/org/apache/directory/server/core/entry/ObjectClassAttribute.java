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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ObjectClassAttribute extends AbstractServerAttribute
{
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( ObjectClassAttribute.class );

    // Sets dealing with objectClass operations
    private Set<ObjectClass> allObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> abstractObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> auxiliaryObjectClasses = new HashSet<ObjectClass>();
    private Set<ObjectClass> structuralObjectClasses = new HashSet<ObjectClass>();

    private Set<AttributeType> mayList = new HashSet<AttributeType>();
    private Set<AttributeType> mustList = new HashSet<AttributeType>();



    /**
     * Creates a new ObjectClassAttribute with a null ID
     * 
     * @param registries The server registries to use
     */
    public ObjectClassAttribute( Registries registries ) throws NamingException
    {
        this( null, registries );
    }


    /**
     * Creates a new instance of ObjectClassAttribute.
     *
     * @param upId The ObjectClass ID
     * @param registries The registries to use to initialize this object
     * @throws NamingException If something went wrong
     */
    public ObjectClassAttribute( String upId, Registries registries ) throws NamingException
    {
        attributeType = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT_OID );
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
        attributeType = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT_OID );

        if ( val == null )
        {
            values.add( new ServerStringValue( attributeType ) );
        }
        else if ( ! ( val instanceof ServerStringValue ) )
        {
            String message = "Only String values supported for objectClass attribute";
            LOG.error( message );
            throw new UnsupportedOperationException( message );
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
        attributeType = registries.getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT_OID );
        
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

    
    // -----------------------------------------------------------------------


    private Set<ObjectClass> addAncestors( ObjectClass descendant, Set<ObjectClass> ancestors ) throws NamingException
    {
        if ( descendant == null )
        {
            return ancestors;
        }

        ObjectClass[] superClasses = descendant.getSuperClasses();

        if ( ( superClasses == null ) || ( superClasses.length == 0 ) )
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
            switch ( oc.getType() )
            {
                case STRUCTURAL :
                    structuralObjectClasses.add( oc );
                    break;
                    
                case AUXILIARY :
                    auxiliaryObjectClasses.add( oc );
                    break;
                    
                case ABSTRACT :
                    abstractObjectClasses.add( oc );
                    break;
                    
                default:
                    String message = "Unrecognized objectClass type value: " + oc.getType();
                    LOG.error( message );
                    throw new UnsupportedOperationException( message );
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

    
    public boolean add( byte[] val )
    {
        String message = "Binary values are not accepted by ObjectClassAttributes";
        LOG.error( message );
        throw new UnsupportedOperationException( message );
    }


    public boolean contains( byte[] val )
    {
        String message = "There are no binary values in an ObjectClass attribute.";
        LOG.error( message );
        throw new UnsupportedOperationException( message );
    }


    public boolean remove( byte[] val )
    {
        String message = "There are no binary values in an ObjectClass attribute.";
        LOG.error( message );
        throw new UnsupportedOperationException( message );
    }
}
