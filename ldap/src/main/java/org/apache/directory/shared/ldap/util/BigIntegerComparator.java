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

/*
 * $Id$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.directory.shared.ldap.util;


import java.math.BigInteger;


/**
 * Compares BigInteger keys and values within a table.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class BigIntegerComparator implements java.util.Comparator, java.io.Serializable
{
    /** A instance of this comparator */
    public static final BigIntegerComparator INSTANCE = new BigIntegerComparator();

    /**
     * Version id for serialization.
     */
    static final long serialVersionUID = 1L;


    /**
     * Compare two objects.
     * 
     * @param an_obj1
     *            First object
     * @param an_obj2
     *            Second object
     * @return 1 if obj1 > obj2, 0 if obj1 == obj2, -1 if obj1 < obj2
     */
    public int compare( Object an_obj1, Object an_obj2 )
    {
        if ( an_obj1 == null )
        {
            throw new IllegalArgumentException( "Argument 'an_obj1' is null" );
        }

        if ( an_obj2 == null )
        {
            throw new IllegalArgumentException( "Argument 'an_obj2' is null" );
        }

        BigInteger l_int1 = ( BigInteger ) an_obj1;
        BigInteger l_int2 = ( BigInteger ) an_obj2;
        return l_int1.compareTo( l_int2 );
    }
}
