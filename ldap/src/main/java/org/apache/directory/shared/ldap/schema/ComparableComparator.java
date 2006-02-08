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


import java.util.Comparator;
import java.io.Serializable;


/**
 * Compares two objects taking into account that one might be a Comparable.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ComparableComparator implements Comparator, Serializable
{
    static final long serialVersionUID = -5295278271807198471L;

    /**
     * Compares two objects taking into account that one may be a Comparable.
     * If the first is a comparable then its compareTo operation is called and
     * the result returned as is.  If the first is not a Comparable but the
     * second is then its compareTo method is called and the result is returned
     * after being negated.  If none are comparables the hashCode of o1 minus
     * the hashCode of o2 is returned.
     *
     * @see Comparator#compare(Object, Object)
     */
    public int compare( Object o1, Object o2 )
    {
        if ( o1 instanceof Comparable )
        {
            return ( ( Comparable ) o1 ).compareTo( o2 );
        }

        if ( o2 instanceof Comparable )
        {
            return - ( ( Comparable ) o2 ).compareTo( o1 );
        }

        return o1.hashCode() - o2.hashCode();
    }
}
