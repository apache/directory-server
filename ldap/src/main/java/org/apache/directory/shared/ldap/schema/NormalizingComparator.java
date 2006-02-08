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
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A comparator which normalizes a value first before using a subordinate
 * comparator to compare them.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NormalizingComparator implements Comparator
{
    private static final Logger log = LoggerFactory.getLogger( NormalizingComparator.class );

    /** the Normalizer to normalize values with before comparing */
    private Normalizer normalizer;

    /** the underlying comparator to use for comparisons */
    private Comparator comparator;


    /**
     * A comparator which normalizes a value first before comparing them.
     * 
     * @param normalizer
     *            the Normalizer to normalize values with before comparing
     * @param comparator
     *            the underlying comparator to use for comparisons
     */
    public NormalizingComparator(Normalizer normalizer, Comparator comparator)
    {
        this.normalizer = normalizer;
        this.comparator = comparator;
    }


    /**
     * If any normalization attempt fails we compare using the unnormalized
     * object.
     * 
     * @see Comparator#compare(Object, Object)
     */
    public int compare( Object o1, Object o2 )
    {
        Object n1;
        Object n2;

        try
        {
            n1 = normalizer.normalize( o1 );
        }
        catch ( NamingException e )
        {
            log.warn( "Failed to normalize: " + o1, e );
            n1 = o1;
        }

        try
        {
            n2 = normalizer.normalize( o2 );
        }
        catch ( NamingException e )
        {
            log.warn( "Failed to normalize: " + o2, e );
            n2 = o2;
        }

        return comparator.compare( n1, n2 );
    }
}
