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
package org.apache.directory.shared.ldap.message;


import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.util.ValuedEnum;


/**
 * Type-safe scope parameter enumeration.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class ScopeEnum extends ValuedEnum
{
    static final long serialVersionUID = 6354503944147675151L;

    /** Search scope parameter value for base object search */
    public static final int BASEOBJECT_VAL = SearchControls.OBJECT_SCOPE;

    /** Search scope parameter value for single level search */
    public static final int SINGLELEVEL_VAL = SearchControls.ONELEVEL_SCOPE;

    /** Search scope parameter value for whole subtree level search */
    public static final int WHOLESUBTREE_VAL = SearchControls.SUBTREE_SCOPE;

    /** LDAP search scope parameter value for base object search */
    public static final int BASEOBJECT_LDAPVAL = 0;

    /** LDAP search scope parameter value for single level search */
    public static final int SINGLELEVEL_LDAPVAL = 1;

    /** LDAP search scope parameter value for whole subtree level search */
    public static final int WHOLESUBTREE_LDAPVAL = 2;

    /** Search scope parameter enum for base object search */
    public static final ScopeEnum BASEOBJECT = new ScopeEnum( "BASEOBJECT", BASEOBJECT_VAL );

    /** Search scope parameter enum for single level search */
    public static final ScopeEnum SINGLELEVEL = new ScopeEnum( "SINGLELEVEL", SINGLELEVEL_VAL );

    /** Search scope parameter enum for whole subtree level search */
    public static final ScopeEnum WHOLESUBTREE = new ScopeEnum( "WHOLESUBTREE", WHOLESUBTREE_VAL );


    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param a_name
     *            a string name for the enumeration value.
     * @param a_value
     *            the integer value of the enumeration.
     */
    private ScopeEnum(final String a_name, final int a_value)
    {
        super( a_name, a_value );
    }


    /**
     * Gets the type safe enumeration constant corresponding to a SearchControls
     * scope value.
     * 
     * @param a_controls
     *            the SearchControls whose scope value we convert to enum
     * @return the SopeEnum for the scope int value
     */
    public static ScopeEnum getScope( SearchControls a_controls )
    {
        switch ( a_controls.getSearchScope() )
        {
            case ( SearchControls.OBJECT_SCOPE  ):
                return BASEOBJECT;
            case ( SearchControls.ONELEVEL_SCOPE  ):
                return SINGLELEVEL;
            case ( SearchControls.SUBTREE_SCOPE  ):
                return WHOLESUBTREE;
            default:
                throw new IllegalArgumentException( "Unrecognized search scope in SearchControls: "
                    + a_controls.getSearchScope() );
        }
    }


    /**
     * Gets the LdapValue for the scope enumeration as opposed to the JNDI value
     * which is returned using getValue().
     * 
     * @return the LDAP enumeration value for the scope parameter on a search
     *         request.
     */
    public int getLdapValue()
    {
        switch ( getValue() )
        {
            case ( BASEOBJECT_VAL ):
                return BASEOBJECT_LDAPVAL;
            case ( SINGLELEVEL_VAL ):
                return SINGLELEVEL_LDAPVAL;
            case ( WHOLESUBTREE_VAL ):
                return WHOLESUBTREE_LDAPVAL;
            default:
                throw new IllegalArgumentException( "Unrecognized value: " + getValue() );
        }
    }
}
