/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.common.schema;


import java.util.List;
import java.util.ArrayList;

import java.io.Serializable;


/**
 * Objectclass specification bean used to store the schema information for an
 * objectclass definition.  
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultObjectClass extends AbstractSchemaObject implements ObjectClass, Serializable
{
    static final long serialVersionUID = -4744807759763092241L;
    /** empty array of ObjectClasses so we do not have to recreate objects */
    private static final ObjectClass[] EMPTY_OCLASS_ARR = new ObjectClass[0];
    /** empty array of AttributeTypes so we do not have to recreate objects */
    private static final AttributeType[] EMPTY_ATYPE_ARR = new AttributeType[0];

    // ------------------------------------------------------------------------
    // Specification Variables
    // ------------------------------------------------------------------------

    /** */
    private ObjectClassTypeEnum type = ObjectClassTypeEnum.ABSTRACT;
    /** */
    private ArrayList mayList;
    /** */
    private ArrayList mustList;
    /** */
    private ArrayList superClasses;

    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates an ObjectClassImpl instance with an OID.
     * 
     * @param oid the unique object identifier for this objectClass
     */
    DefaultObjectClass( String oid )
    {
        super( oid );
    }


    // ------------------------------------------------------------------------
    // ObjectClass Methods
    // ------------------------------------------------------------------------


    public ObjectClass[] getSuperClasses()
    {
        if ( superClasses == null )
        {
            return EMPTY_OCLASS_ARR;
        }

        return ( ObjectClass[] ) superClasses.toArray( EMPTY_OCLASS_ARR );
    }


    public ObjectClassTypeEnum getType()
    {
        return type;
    }


    public AttributeType[] getMustList()
    {
        if ( mustList == null )
        {
            return EMPTY_ATYPE_ARR;
        }

        return ( AttributeType[] ) mustList.toArray( EMPTY_ATYPE_ARR );
    }


    public AttributeType[] getMayList()
    {
        if ( mayList == null )
        {
            return EMPTY_ATYPE_ARR;
        }

        return ( AttributeType[] ) mayList.toArray( EMPTY_ATYPE_ARR );
    }


    // ------------------------------------------------------------------------
    // Package Friendly Mutators
    // ------------------------------------------------------------------------


    /**
     * Adds a list of AttributeTypes that may be present within this
     * ObjectClass.
     *
     * @param mayList more AttributeTypes to add to the optional attribute list
     */
    void addToMayList( List mayList )
    {
        if ( this.mayList == null )
        {
            this.mayList = new ArrayList();
        }

        this.mayList.addAll( mayList );
    }


    /**
     * Adds a list of AttributeTypes that must be present within this
     * ObjectClass.
     *
     * @param mustList more AttributeTypes to add to the mandatory list
     */
    void addToMustList( List mustList )
    {
        if ( this.mustList == null )
        {
            this.mustList = new ArrayList();
        }

        this.mustList.addAll( mustList );
    }


    /**
     * Adds ObjectClass to the list of super classes for this ObjectClass.
     *
     * @param superClasses the list of super classes to add to this ObjectClass
     */
    void addSuperClasses( List superClasses )
    {
        if ( this.superClasses == null )
        {
            this.superClasses = new ArrayList();
        }

        this.superClasses.addAll( superClasses );
    }


    /**
     * Sets the classType for this ObjectClass.
     *
     * @param type the new class type enumeration for this ObjectClass
     */
    void setType( ObjectClassTypeEnum type )
    {
        this.type = type;
    }
}
