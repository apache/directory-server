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
package org.apache.directory.shared.ldap.schema;


import java.util.Map;
import java.util.List;

import org.apache.directory.shared.ldap.util.EnumUtils;
import org.apache.directory.shared.ldap.util.ValuedEnum;


/**
 * Type safe enum for a matching rule's comparator and normalizer component 
 * usage string.  This can be take one of the following three values: 
 * <ul>
 * <li>ORDERING</li>
 * <li>EQUALITY</li>
 * <li>SUBSTRING</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MatchingRuleEnum extends ValuedEnum
{
    static final long serialVersionUID = 5500272648097676014L;
    /** value for ordering usage */
    public static final int ORDERING_VAL = 0;
    /** value for equality usage */
    public static final int EQUALITY_VAL = 1;
    /** value for substring usage */
    public static final int SUBSTRING_VAL = 2;

    /** enum for ordering comparator usage */
    public static final MatchingRuleEnum ORDERING = 
        new MatchingRuleEnum( "ORDERING", ORDERING_VAL );
    /** enum for equality comparator usage */
    public static final MatchingRuleEnum EQUALITY = 
        new MatchingRuleEnum( "EQUALITY", EQUALITY_VAL );
    /** enum for substring comparator usage */
    public static final MatchingRuleEnum SUBSTRING = 
        new MatchingRuleEnum( "SUBSTRING", SUBSTRING_VAL );

    
    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param name a string name for the enumeration value.
     * @param value the integer value of the enumeration.
     */
    private MatchingRuleEnum( final String name, final int value )
    {
        super( name, value );
    }
    
    
    /**
     * Gets the enumeration type for the usage string regardless of case.
     * 
     * @param usage the usage string
     * @return the usage enumeration type
     */
    public static MatchingRuleEnum getUsage( String usage )
    {
        if ( usage.equalsIgnoreCase( MatchingRuleEnum.EQUALITY.getName() ) )
        {
            return MatchingRuleEnum.EQUALITY;
        }
        
        if ( usage.equalsIgnoreCase( MatchingRuleEnum.ORDERING.getName() ) )
        {
            return MatchingRuleEnum.ORDERING;
        }
        
        if ( usage.equalsIgnoreCase( MatchingRuleEnum.SUBSTRING.getName() ) )
        {
            return MatchingRuleEnum.SUBSTRING;
        }

        throw new IllegalArgumentException( "Unknown matching rule usage string"
            + usage );
    }
    
    
    /**
     * Gets a List of the enumerations for matching rule usage.
     * 
     * @return the List of enumerations possible for matching rule usage
     */
    public static List list()
    {
        return EnumUtils.getEnumList( MatchingRuleEnum.class );
    }
    
    
    /**
     * Gets the Map of MatchingRuleEnum objects by name using the 
     * MatchingRuleEnum class.
     * 
     * @return the Map by name of MatchingRuleEnums
     */
    public static Map map()
    {
        return EnumUtils.getEnumMap( MatchingRuleEnum.class );
    }
}
