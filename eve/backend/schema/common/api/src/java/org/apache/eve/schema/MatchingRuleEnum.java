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
 * Type safe enum for a matching rule's comparator and normalizer component 
 * usage string.  This can be take one of the following three values: 
 * <ul>
 * <li>ORDERING</li>
 * <li>EQUALITY</li>
 * <li>SUBSTRING</li>
 * </ul>
 * 
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Rev: 1449 $
 */
public class MatchingRuleEnum extends ValuedEnum
{
    /** value for ordering usage */
    public static final int ORDERING_VAL = 0 ;
    /** value for equality usage */
    public static final int EQUALITY_VAL = 1 ;
    /** value for substring usage */
    public static final int SUBSTRING_VAL = 2 ;

    /** enum for ordering comparator usage */
    public static final MatchingRuleEnum ORDERING = 
        new MatchingRuleEnum( "ORDERING", ORDERING_VAL ) ;
    /** enum for equality comparator usage */
    public static final MatchingRuleEnum EQUALITY = 
        new MatchingRuleEnum( "EQUALITY", EQUALITY_VAL ) ;
    /** enum for substring comparator usage */
    public static final MatchingRuleEnum SUBSTRING = 
        new MatchingRuleEnum( "SUBSTRING", SUBSTRING_VAL ) ;

    
    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param a_name a string name for the enumeration value.
     * @param a_value the integer value of the enumeration.
     */
    private MatchingRuleEnum( final String a_name, final int a_value )
    {
        super( a_name, a_value ) ;
    }
    
    
    /**
     * Gets the enumeration type for the usage string regardless of case.
     * 
     * @param a_usage the usage string
     * @return the usage enumeration type
     */
    public static MatchingRuleEnum getUsage( String a_usage )
    {
        if ( a_usage.equalsIgnoreCase( MatchingRuleEnum.EQUALITY.getName() ) )
        {
            return MatchingRuleEnum.EQUALITY ;
        }
        
        if ( a_usage.equalsIgnoreCase( 
            MatchingRuleEnum.ORDERING.getName() ) )
        {
            return MatchingRuleEnum.ORDERING ;
        }
        
        if ( a_usage.equalsIgnoreCase( MatchingRuleEnum.SUBSTRING.getName() ) )
        {
            return MatchingRuleEnum.SUBSTRING ;
        }

        throw new IllegalArgumentException( "Unknown matching rule usage string"
            + a_usage ) ;
    }
    
    
    /**
     * Gets a List of the enumerations for matching rule usage.
     * 
     * @return the List of enumerations possible for matching rule usage
     */
    public static List list()
    {
        return EnumUtils.getEnumList( MatchingRuleEnum.class ) ;
    }
    
    
    /**
     * Gets the Map of MatchingRuleEnum objects by name using the 
     * MatchingRuleEnum class.
     * 
     * @return the Map by name of MatchingRuleEnums
     */
    public static Map map()
    {
        return EnumUtils.getEnumMap( MatchingRuleEnum.class ) ;
    }
}
