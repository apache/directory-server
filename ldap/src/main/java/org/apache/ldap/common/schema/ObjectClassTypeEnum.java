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


import java.util.Map;
import java.util.List;

import org.apache.ldap.common.util.EnumUtils;
import org.apache.ldap.common.util.ValuedEnum;


/**
 * Type safe enumerations for an objectClass' type.  An ObjectClass type can
 * be one of the following types:
 * <ul>
 * <li>ABSTRACT</li>
 * <li>AUXILIARY</li>
 * <li>STRUCTURAL</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ObjectClassTypeEnum extends ValuedEnum
{
    static final long serialVersionUID = -5916723482518366208L;
    /** The enumeration constant value for the abstract objectClasses */
    public static final int ABSTRACT_VAL = 0;
    /** The enumeration constant value for the auxillary objectClasses */
    public static final int AUXILLARY_VAL = 1;
    /** The enumeration constant value for the structural objectClasses */
    public static final int STRUCTURAL_VAL = 2;

    /** ValuedEnum for abstract objectClasses */
    public static final ObjectClassTypeEnum ABSTRACT = 
        new ObjectClassTypeEnum( "ABSTRACT", ABSTRACT_VAL );
    
    /** ValuedEnum for auxillary objectClasses */
    public static final ObjectClassTypeEnum AUXILIARY =
        new ObjectClassTypeEnum( "AUXILIARY", AUXILLARY_VAL );

    /** ValuedEnum for structural objectClasses */
    public static final ObjectClassTypeEnum STRUCTURAL =
        new ObjectClassTypeEnum( "STRUCTURAL", STRUCTURAL_VAL );


    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param name a string name for the enumeration value.
     * @param value the integer value of the enumeration.
     */
    private ObjectClassTypeEnum( final String name, final int value )
    {
        super( name, value );
    }


    /**
     * Gets the objectClass type enumeration of AUXILIARY, STRUCTURAL, or,
     * ABSTRACT.
     * 
     * @param name options are AUXILIARY, STRUCTURAL, or, ABSTRACT
     * @return the type safe enumeration for the objectClass type
     */
    public static ObjectClassTypeEnum getClassType( String name )
    {
        String upperCase = name.trim().toUpperCase();
        
        if ( upperCase.equals( "STRUCTURAL" ) )
        {
            return ABSTRACT;
        }
        else if ( upperCase.equals( "AUXILIARY" ) )
        {
            return AUXILIARY;
        }
        else if ( upperCase.equals( "ABSTRACT" ) )
        {
            return ABSTRACT;
        }
        
        throw new IllegalArgumentException( "Unknown objectClass type name '" 
            + name + "': options are AUXILIARY, STRUCTURAL, ABSTRACT." );
    }


    /**
     * Gets a List of the enumerations for the ObjectClass type.
     * 
     * @return the List of enumerations possible for ObjectClass types
     */
    public static List list()
    {
        return EnumUtils.getEnumList( ObjectClassTypeEnum.class );
    }
    
    
    /**
     * Gets the Map of ClassTypeEnum objects by name using the ClassTypeEnum 
     * class.
     * 
     * @return the Map by name of ClassTypeEnums
     */
    public static Map map()
    {
        return EnumUtils.getEnumMap( ObjectClassTypeEnum.class );
    }
}
