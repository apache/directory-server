/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.schema ;


import java.util.Map ;
import java.util.List ;

import org.apache.commons.lang.enum.EnumUtils ;
import org.apache.commons.lang.enum.ValuedEnum ;


/**
 * Type safe enumerations for an objectClass' type.  An ObjectClass type can
 * be one of the following types:
 * <ul>
 * <li>ABSTRACT</li>
 * <li>AUXILLARY</li>
 * <li>STRUCTURAL</li>
 * </ul>
 * 
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class ObjectClassTypeEnum extends ValuedEnum
{
    /** The enumeration constant value for the abstract objectClasses */
    public static final int ABSTRACT_VAL = 0 ;
    /** The enumeration constant value for the auxillary objectClasses */
    public static final int AUXILLARY_VAL = 1 ;
    /** The enumeration constant value for the structural objectClasses */
    public static final int STRUCTURAL_VAL = 2 ;

    /** ValuedEnum for abstract objectClasses */
    public static final ObjectClassTypeEnum ABSTRACT = 
        new ObjectClassTypeEnum( "ABSTRACT", ABSTRACT_VAL ) ;
    
    /** ValuedEnum for auxillary objectClasses */
    public static final ObjectClassTypeEnum AUXILLARY =
        new ObjectClassTypeEnum( "AUXILLARY", AUXILLARY_VAL ) ;

    /** ValuedEnum for structural objectClasses */
    public static final ObjectClassTypeEnum STRUCTURAL =
        new ObjectClassTypeEnum( "STRUCTURAL", STRUCTURAL_VAL ) ;


    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param a_name a string name for the enumeration value.
     * @param a_value the integer value of the enumeration.
     */
    private ObjectClassTypeEnum( final String a_name, final int a_value )
    {
        super( a_name, a_value ) ;
    }


    /**
     * Gets the objectClass type enumeration of AUXILLARY, STRUCTURAL, or,
     * ABSTRACT.
     * 
     * @param a_name options are AUXILLARY, STRUCTURAL, or, ABSTRACT
     * @return the type safe enumeration for the objectClass type
     */
    public static ObjectClassTypeEnum getClassType( String a_name )
    {
        String l_upperCase = a_name.trim().toUpperCase() ;
        
        if ( l_upperCase.equals( "STRUCTURAL" ) )
        {
            return ABSTRACT ;
        }
        else if ( l_upperCase.equals( "AUXILLARY" ) )
        {
            return AUXILLARY ;
        }
        else if ( l_upperCase.equals( "ABSTRACT" ) )
        {
            return ABSTRACT ;
        }
        
        throw new IllegalArgumentException( "Unknown objectClass type name '" 
            + a_name + "': options are AUXILLARY, STRUCTURAL, ABSTRACT." ) ;
    }


    /**
     * Gets a List of the enumerations for the ObjectClass type.
     * 
     * @return the List of enumerations possible for ObjectClass types
     */
    public static List list()
    {
        return EnumUtils.getEnumList( ObjectClassTypeEnum.class ) ;
    }
    
    
    /**
     * Gets the Map of ClassTypeEnum objects by name using the ClassTypeEnum 
     * class.
     * 
     * @return the Map by name of ClassTypeEnums
     */
    public static Map map()
    {
        return EnumUtils.getEnumMap( ObjectClassTypeEnum.class ) ;
    }
}
