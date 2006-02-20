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
package org.apache.directory.server.core.schema;


import java.io.Serializable;
import java.util.Comparator;

import javax.naming.NamingException;


/**
 * A serializable wrapper around a Comparator which uses delayed initialization
 * of the underlying wrapped comparator which is JIT resolved from a static
 * global registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SerializableComparator implements Comparator, Serializable
{
    private static final long serialVersionUID = 3257566226288162870L;

    /** the system global Comparator registry */
    private static ComparatorRegistry registry = null;
    /** the OID of the matchingRule for this comparator */
    private String matchingRuleOid;
    /** the transient wrapped comparator */
    private transient Comparator wrapped;


    // ------------------------------------------------------------------------
    // S T A T I C   M E T H O D S
    // ------------------------------------------------------------------------

    /**
     * Sets the global Comparator registry for comparator lookups.
     *
     * @param registry the comparator registry to use for Comparator lookups
     */
    public static void setRegistry( ComparatorRegistry registry )
    {
        SerializableComparator.registry = registry;
    }


    // ------------------------------------------------------------------------
    // C O N T R U C T O R S
    // ------------------------------------------------------------------------

    public SerializableComparator(String matchingRuleOid)
    {
        this.matchingRuleOid = matchingRuleOid;
    }


    // ------------------------------------------------------------------------
    // C O M P A R A T O R   I M P L E M E N T A T I O N S
    // ------------------------------------------------------------------------

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Object o1, Object o2 )
    {
        if ( wrapped == null )
        {
            try
            {
                wrapped = registry.lookup( matchingRuleOid );
            }
            catch ( NamingException e )
            {
                throw new RuntimeException( "Matching rule not found: " + matchingRuleOid );
            }
        }

        return wrapped.compare( o1, o2 );
    }
}
