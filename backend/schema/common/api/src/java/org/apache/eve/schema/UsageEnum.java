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
 * Type safe enum for an AttributeType definition's usage string.  This can be
 * take one of the following four values: 
 * <ul>
 * <li>userApplications</li>
 * <li>directoryOperation</li>
 * <li>distributedOperation</li>
 * <li>dSAOperation</li>
 * </ul>
 * 
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class UsageEnum extends ValuedEnum
{
    /** value for attributes with userApplications usage */
    public static final int USERAPPLICATIONS_VAL = 0 ;
    /** value for attributes with directoryOperation usage */
    public static final int DIRECTORYOPERATION_VAL = 1 ;
    /** value for attributes with distributedOperation usage */
    public static final int DISTRIBUTEDOPERATION_VAL = 2 ;
    /** value for attributes with dSAOperation usage */
    public static final int DSAOPERATION_VAL = 3 ;


    /** enum for attributes with userApplications usage */
    public static final UsageEnum USERAPPLICATIONS = 
        new UsageEnum( "userApplications", USERAPPLICATIONS_VAL ) ;
    /** enum for attributes with directoryOperation usage */
    public static final UsageEnum DIRECTORYOPERATION = 
        new UsageEnum( "directoryOperation", DIRECTORYOPERATION_VAL ) ;
    /** enum for attributes with distributedOperation usage */
    public static final UsageEnum DISTRIBUTEDOPERATION = 
        new UsageEnum( "distributedOperation", DISTRIBUTEDOPERATION_VAL ) ;
    /** enum for attributes with dSAOperation usage */
    public static final UsageEnum DSAOPERATION = 
        new UsageEnum( "dSAOperation", DSAOPERATION_VAL ) ;


    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param a_name a string name for the enumeration value.
     * @param a_value the integer value of the enumeration.
     */
    private UsageEnum( final String a_name, final int a_value )
    {
        super( a_name, a_value ) ;
    }
    
    
    /**
     * Gets the enumeration type for the attributeType usage string regardless 
     * of case.
     * 
     * @param a_usage the usage string
     * @return the usage enumeration type
     */
    public static UsageEnum getUsage( String a_usage )
    {
        if ( a_usage.equalsIgnoreCase( UsageEnum.USERAPPLICATIONS.getName() ) )
        {
            return UsageEnum.USERAPPLICATIONS ;
        }
        
        if ( a_usage.equalsIgnoreCase( 
            UsageEnum.DIRECTORYOPERATION.getName() ) )
        {
            return UsageEnum.DIRECTORYOPERATION ;
        }
        
        if ( a_usage.equalsIgnoreCase( 
            UsageEnum.DISTRIBUTEDOPERATION.getName() ) )
        {
            return UsageEnum.DISTRIBUTEDOPERATION ;
        }
        
        if ( a_usage.equalsIgnoreCase( UsageEnum.DSAOPERATION.getName() ) )
        {
            return UsageEnum.DSAOPERATION ;
        }
        
        throw new IllegalArgumentException( "Unknown attributeType usage string"
            + a_usage ) ;
    }
    
    
    /**
     * Gets a List of the enumerations for attributeType usage.
     * 
     * @return the List of enumerations possible for usage
     */
    public static List list()
    {
        return EnumUtils.getEnumList( UsageEnum.class ) ;
    }
    
    
    /**
     * Gets the Map of UsageEnum objects by name using the UsageEnum class.
     * 
     * @return the Map by name of UsageEnums
     */
    public static Map map()
    {
        return EnumUtils.getEnumMap( UsageEnum.class ) ;
    }
}
