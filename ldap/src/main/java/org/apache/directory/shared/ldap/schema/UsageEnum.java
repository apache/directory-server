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
 * Type safe enum for an AttributeType definition's usage string.  This can be
 * take one of the following four values: 
 * <ul>
 * <li>userApplications</li>
 * <li>directoryOperation</li>
 * <li>distributedOperation</li>
 * <li>dSAOperation</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UsageEnum extends ValuedEnum
{
    static final long serialVersionUID = -5838426429165435227L;
    /** value for attributes with userApplications usage */
    public static final int USERAPPLICATIONS_VAL = 0;
    /** value for attributes with directoryOperation usage */
    public static final int DIRECTORYOPERATION_VAL = 1;
    /** value for attributes with distributedOperation usage */
    public static final int DISTRIBUTEDOPERATION_VAL = 2;
    /** value for attributes with dSAOperation usage */
    public static final int DSAOPERATION_VAL = 3;


    /** enum for attributes with userApplications usage */
    public static final UsageEnum USERAPPLICATIONS = 
        new UsageEnum( "userApplications", USERAPPLICATIONS_VAL );
    /** enum for attributes with directoryOperation usage */
    public static final UsageEnum DIRECTORYOPERATION = 
        new UsageEnum( "directoryOperation", DIRECTORYOPERATION_VAL );
    /** enum for attributes with distributedOperation usage */
    public static final UsageEnum DISTRIBUTEDOPERATION = 
        new UsageEnum( "distributedOperation", DISTRIBUTEDOPERATION_VAL );
    /** enum for attributes with dSAOperation usage */
    public static final UsageEnum DSAOPERATION = 
        new UsageEnum( "dSAOperation", DSAOPERATION_VAL );


    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     *
     * @param name a string name for the enumeration value.
     * @param value the integer value of the enumeration.
     */
    private UsageEnum( final String name, final int value )
    {
        super( name, value );
    }
    
    
    /**
     * Gets the enumeration type for the attributeType usage string regardless 
     * of case.
     * 
     * @param usage the usage string
     * @return the usage enumeration type
     */
    public static UsageEnum getUsage( String usage )
    {
        if ( usage.equalsIgnoreCase( UsageEnum.USERAPPLICATIONS.getName() ) )
        {
            return UsageEnum.USERAPPLICATIONS;
        }
        
        if ( usage.equalsIgnoreCase(
            UsageEnum.DIRECTORYOPERATION.getName() ) )
        {
            return UsageEnum.DIRECTORYOPERATION;
        }
        
        if ( usage.equalsIgnoreCase(
            UsageEnum.DISTRIBUTEDOPERATION.getName() ) )
        {
            return UsageEnum.DISTRIBUTEDOPERATION;
        }
        
        if ( usage.equalsIgnoreCase( UsageEnum.DSAOPERATION.getName() ) )
        {
            return UsageEnum.DSAOPERATION;
        }
        
        throw new IllegalArgumentException( "Unknown attributeType usage string"
            + usage );
    }
    
    
    /**
     * Gets a List of the enumerations for attributeType usage.
     * 
     * @return the List of enumerations possible for usage
     */
    public static List list()
    {
        return EnumUtils.getEnumList( UsageEnum.class );
    }
    
    
    /**
     * Gets the Map of UsageEnum objects by name using the UsageEnum class.
     * 
     * @return the Map by name of UsageEnums
     */
    public static Map map()
    {
        return EnumUtils.getEnumMap( UsageEnum.class );
    }
}
