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
package org.apache.eve.jndi;


import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.lang.reflect.Method;

import org.apache.ldap.common.util.ValuedEnum;


/**
 * A valued enumeration used by interceptor services for fast switching on
 * invocation method names.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class InvocationMethodEnum extends ValuedEnum
{
    /** name/signature for the getMatchedDn() method */
    public static final String GETMATCHEDDN_STR = "getMatchedDn()";
    /** name/signature for the getSuffix() method */
    public static final String GETSUFFIX_STR = "getSuffix()";
    /** name/signature for the listSuffixes() method */
    public static final String LISTSUFFIXES_STR = "listSuffixes()";
    /** name/signature for the lookup(Name, String[]) method */
    public static final String LOOKUP_NAME_STRINGARR_STR =
            "lookup(Name, String[])";
    /** name/signature for the add() method */
        public static final String ADD_STR = "add()";
    /** name/signature for the delete() method */
    public static final String DELETE_STR = "delete()";
    /** name/signature for the hasEntry() method */
    public static final String HASENTRY_STR = "hasEntry()";
    /** name/signature for the isSuffix() method */
    public static final String ISSUFFIX_STR = "isSuffix()";
    /** name/signature for the list() method */
    public static final String LIST_STR = "list()";
    /** name/signature for the lookup(Name) method */
    public static final String LOOKUP_NAME_STR = "lookup(Name)";
    /** name/signature for the modify(Name, int, Attributes) method */
    public static final String MODIFY_NAME_INT_ATTRIBUTES_STR =
            "modify(Name, int, Attributes)";
    /** name/signature for the modify(Name, ModificationItem[]) method */
    public static final String MODIFY_NAME_MODIFICATIONITEMARR_STR =
            "modify(Name, ModificationItem[])";
    /** name/signature for the modifyRdn() method */
    public static final String MODIFYRN_STR = "modifyRn()";
    /** name/signature for the move(Name, Name) method */
    public static final String MOVE_NAME_NAME_STR = "move(Name, Name)";
    /** name/signature for the move(Name, Name, String, boolean) method */
    public static final String MOVE_NAME_NAME_STRING_BOOL_STR =
            "move(Name, Name, String, boolean)";
    /** name/signature for the search() method */
    public static final String SEARCH_STR = "search()";

    /** the names in an array where the int value indexes the associated name */
    public static final String[] NAMES = {
        GETMATCHEDDN_STR, GETSUFFIX_STR, LISTSUFFIXES_STR,
        LOOKUP_NAME_STRINGARR_STR,
        ADD_STR, DELETE_STR, HASENTRY_STR, ISSUFFIX_STR, LIST_STR,
        LOOKUP_NAME_STR, MODIFY_NAME_INT_ATTRIBUTES_STR,
        MODIFY_NAME_MODIFICATIONITEMARR_STR, MODIFYRN_STR,
        MOVE_NAME_NAME_STR, MOVE_NAME_NAME_STRING_BOOL_STR, SEARCH_STR
    };

    /** the int value for the getMatchedDn() method */
    public static final int GETMATCHEDDN_VAL = 0;
    /** the int value for the getSuffix() method */
    public static final int GETSUFFIX_VAL = 1;
    /** the int value for the listSuffixes() method */
    public static final int LISTSUFFIXES_VAL = 2;
    /** the int value for the lookup(Name, String[]) method */
    public static final int LOOKUP_NAME_STRINGARR_VAL = 3;
    /** the int value for the add() method */
    public static final int ADD_VAL = 4;
    /** the int value for the delete() method */
    public static final int DELETE_VAL = 5;
    /** the int value for the hasEntry() method */
    public static final int HASENTRY_VAL = 6;
    /** the int value for the isSuffix() method */
    public static final int ISSUFFIX_VAL = 7;
    /** the int value for the list() method */
    public static final int LIST_VAL = 8;
    /** the int value for the lookup() method */
    public static final int LOOKUP_NAME_VAL = 9;
    /** the int value for the modify(Name, int, Attributes) method */
    public static final int MODIFY_NAME_INT_ATTRIBUTES_VAL = 10;
    /** the int value for the modify(Name, ModificationItem[]) method */
    public static final int MODIFY_NAME_MODIFICATIONITEMARR_VAL = 11;
    /** the int value for the modifyRdn() method */
    public static final int MODIFYRDN_VAL = 12;
    /** the int value for the move(Name, Name) method */
    public static final int MOVE_NAME_NAME_VAL = 13;
    /** the int value for the move(Name, Name, String, boolean) method */
    public static final int MOVE_NAME_NAME_STRING_BOOL_VAL = 14;
    /** the int value for the search() method */
    public static final int SEARCH_VAL = 15;

    /** the type safe enumeration for the getMatchedDn() method */
    public static final InvocationMethodEnum GETMATCHEDDN =
		new InvocationMethodEnum( NAMES[0], 0 );
    /** the type safe enumeration for the getSuffix() method */
    public static final InvocationMethodEnum GETSUFFIX =
		new InvocationMethodEnum( NAMES[1], 1 );
    /** the type safe enumeration for the listSuffixes() method */
    public static final InvocationMethodEnum LISTSUFFIXES =
		new InvocationMethodEnum( NAMES[2], 2 );
    /** the type safe enumeration for the lookup(Name, String[]) method */
    public static final InvocationMethodEnum LOOKUP_NAME_STRINGARR =
		new InvocationMethodEnum( NAMES[3], 3 );
    /** the type safe enumeration for the add() method */
    public static final InvocationMethodEnum ADD =
		new InvocationMethodEnum( NAMES[4], 4 );
    /** the type safe enumeration for the delete() method */
    public static final InvocationMethodEnum DELETE =
		new InvocationMethodEnum( NAMES[5], 5 );
    /** the type safe enumeration for the hasEntry() method */
    public static final InvocationMethodEnum HASENTRY =
		new InvocationMethodEnum( NAMES[6], 6 );
    /** the type safe enumeration for the isSuffix() method */
    public static final InvocationMethodEnum ISSUFFIX =
		new InvocationMethodEnum( NAMES[7], 7 );
    /** the type safe enumeration for the list() method */
    public static final InvocationMethodEnum LIST =
		new InvocationMethodEnum( NAMES[8], 8 );
    /** the type safe enumeration for the lookup(Name) method */
    public static final InvocationMethodEnum LOOKUP_NAME =
		new InvocationMethodEnum( NAMES[9], 9 );
    /** the type safe enumeration for the modify(Name, int, Attributes) method */
    public static final InvocationMethodEnum MODIFY_NAME_INT_ATTRIBUTES =
		new InvocationMethodEnum( NAMES[10], 10 );
    /** the type safe enumeration for the modify(Name, ModificationItem[]) method */
    public static final InvocationMethodEnum MODIFY_NAME_MODIFICATIONITEMARR =
		new InvocationMethodEnum( NAMES[11], 11 );
    /** the type safe enumeration for the modifyRdn() method */
    public static final InvocationMethodEnum MODIFYRDN =
		new InvocationMethodEnum( NAMES[12], 12 );
    /** the type safe enumeration for the move(Name, Name) method */
    public static final InvocationMethodEnum MOVE_NAME_NAME =
		new InvocationMethodEnum( NAMES[13], 13 );
    /** the type safe enumeration for the move(Name,Name,String,boolean) method */
    public static final InvocationMethodEnum MOVE_NAME_NAME_STRING_BOOL =
		new InvocationMethodEnum( NAMES[14], 14 );
    /** the type safe enumeration for the search() method */
    public static final InvocationMethodEnum SEARCH =
		new InvocationMethodEnum( NAMES[15], 15 );

    /** the type enums in an array where the int value indexes the enum */
    public static final InvocationMethodEnum[] ENUMS = {
        GETMATCHEDDN, GETSUFFIX, LISTSUFFIXES, LOOKUP_NAME_STRINGARR,
        ADD, DELETE, HASENTRY, ISSUFFIX, LIST,LOOKUP_NAME,
        MODIFY_NAME_INT_ATTRIBUTES, MODIFY_NAME_MODIFICATIONITEMARR, MODIFYRDN,
        MOVE_NAME_NAME, MOVE_NAME_NAME_STRING_BOOL, SEARCH
    };

    /** a hash that uses the method name to lookup the enumeration type */
    public static final Map MAP;


    static
    {
        HashMap map = new HashMap();

        map.put( "add", new InvocationMethodEnum[]{ ADD } );
        map.put( "list", new InvocationMethodEnum[]{ LIST } );
        map.put( "delete", new InvocationMethodEnum[]{ DELETE } );
        map.put( "search", new InvocationMethodEnum[]{ SEARCH } );
        map.put( "hasEntry", new InvocationMethodEnum[]{ HASENTRY } );
        map.put( "isSuffix", new InvocationMethodEnum[]{ ISSUFFIX } );
        map.put( "listSuffixes", new InvocationMethodEnum[]{ LISTSUFFIXES } );
        map.put( "modifyRn", new InvocationMethodEnum[]{ MODIFYRDN } );
        map.put( "getSuffix", new InvocationMethodEnum[]{ GETSUFFIX } );
        map.put( "getMatchedDn", new InvocationMethodEnum[]{ GETMATCHEDDN } );

        map.put( "lookup", new InvocationMethodEnum[]{
            LOOKUP_NAME, LOOKUP_NAME_STRINGARR } );
        map.put( "modify", new InvocationMethodEnum[]{
            MODIFY_NAME_INT_ATTRIBUTES, MODIFY_NAME_MODIFICATIONITEMARR
        } );
        map.put( "move", new InvocationMethodEnum[]{
            MOVE_NAME_NAME, MOVE_NAME_NAME_STRING_BOOL
        } );

        MAP = Collections.unmodifiableMap( map );
    }


    /**
     * Creates a type safe enumeration for a invocation method.
     *
     * @param name the name/signature of the invokation method
     * @param value the int value for the method
     */
    private InvocationMethodEnum( String name, int value )
    {
        super( name, value );
    }


    /**
     * Quickly gets the invocation method enumeration for a Method.
     *
     * @param method the method to get an InvocationMethodEnum for
     * @return the InvocationMethodEnum for the Method
     * @throws IllegalStateException if the method is not recognized to signify
     * that some invocation method has not been accounted for
     */
    public static InvocationMethodEnum getInvocationMethodEnum( Method method )
    {
        InvocationMethodEnum[] enums;
        String methodName = method.getName();

        if ( ! MAP.containsKey( methodName ) )
        {
            throw new IllegalStateException( "Looks like there's a new method "
                    + "point cut '" + methodName
                    + "()' that has not yet been properly setup." );
        }

        enums = ( InvocationMethodEnum[] ) MAP.get( methodName );

        if ( enums.length == 1 )
        {
            return enums[0];
        }

        if ( methodName.equals( "move" ) )
        {
            switch( method.getParameterTypes().length )
            {
                case(2):
                    return MOVE_NAME_NAME;
                case(4):
                    return MOVE_NAME_NAME_STRING_BOOL;
                default:
                    throw new IllegalStateException( "Looks like there's a new "
                            + "move overloaded method point cut '"
                            + getSignature( method )
                            + "()' that has not yet been properly setup." );
            }
        }
        else if ( methodName.equals( "modify" ) )
        {
            switch( method.getParameterTypes().length )
            {
                case(2):
                    return MODIFY_NAME_MODIFICATIONITEMARR;
                case(3):
                    return MODIFY_NAME_INT_ATTRIBUTES;
                default:
                    throw new IllegalStateException( "Looks like there's a new "
                            + "modify overloaded method point cut '"
                            + getSignature( method )
                            + "()' that has not yet been properly setup." );
            }
        }
        else if ( methodName.equals( "lookup" ) )
        {
            switch( method.getParameterTypes().length )
            {
                case(1):
                    return LOOKUP_NAME;
                case(2):
                    return LOOKUP_NAME_STRINGARR;
                default:
                    throw new IllegalStateException( "Looks like there's a new "
                            + "lookup overloaded method point cut '"
                            + getSignature( method )
                            + "()' that has not yet been properly setup." );
            }
        }


        throw new IllegalStateException( "Looks like there's a new method "
                + "point cut '" + getSignature( method )
                + "()' that has not yet been properly setup." );
    }


    /**
     * Gets the method signature with all parameter type class in the signature.
     *
     * @param method the method to generate a signature string for
     * @return the signature String for the Method
     */
    private static String getSignature( Method method )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( method.getName() );
        buf.append( '(' );

        Class[] params = method.getParameterTypes();
        for ( int ii = 0; ii < params.length; ii++ )
        {
            buf.append( params[ii].getName() );

            if ( ii < params.length - 1 )
            {
                buf.append( ',' );
            }
        }

        buf.append( ')' );
        return buf.toString();
    }
}
