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
package org.apache.directory.server.core.partition.impl.btree;


import org.apache.directory.server.core.schema.SerializableComparator;
import org.apache.directory.shared.ldap.util.BigIntegerComparator;


/**
 * TupleComparator for index records.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class IndexComparator implements TupleComparator
{
    private static final long serialVersionUID = 3257283621751633459L;

    private static final SerializableComparator BIG_INTEGER_COMPARATOR = new SerializableComparator(
        "1.2.6.1.4.1.18060.1.1.1.2.2" )
    {
        private static final long serialVersionUID = 3690478030414165816L;


        public int compare( Object o1, Object o2 )
        {
            return BigIntegerComparator.INSTANCE.compare( o1, o2 );
        }
    };
    /** Whether or not the key/value is swapped */
    private final boolean isForwardMap;
    /** The key comparison to use */
    private final SerializableComparator keyComp;


    /**
     * Creates an IndexComparator.
     *
     * @param keyComp the table comparator to use for keys
     * @param isForwardMap whether or not the comparator should swap the 
     * key value pair while conducting comparisons.
     */
    public IndexComparator(SerializableComparator keyComp, boolean isForwardMap)
    {
        this.keyComp = keyComp;
        this.isForwardMap = isForwardMap;
    }


    /**
     * Gets the comparator used to compare keys.  May be null in which
     * case the compareKey method will throw an UnsupportedOperationException.
     *
     * @return the comparator for comparing keys.
     */
    public SerializableComparator getKeyComparator()
    {
        if ( isForwardMap )
        {
            return keyComp;
        }

        return BIG_INTEGER_COMPARATOR;
    }


    /**
     * Gets the binary comparator used to compare valuess.  May be null in which
     * case the compareValue method will throw an UnsupportedOperationException.
     *
     * @return the binary comparator for comparing values.
     */
    public SerializableComparator getValueComparator()
    {
        if ( isForwardMap )
        {
            return BIG_INTEGER_COMPARATOR;
        }

        return keyComp;
    }


    /**
     * Compares key Object to determine their sorting order returning a
     * value = to, < or > than 0.
     *
     * @param key1 the first key to compare
     * @param key2 the other key to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second byte array.
     */
    public int compareKey( Object key1, Object key2 )
    {
        return getKeyComparator().compare( key1, key2 );
    }


    /**
     * Comparse value Objects to determine their sorting order returning a
     * value = to, < or > than 0.
     *
     * @param value1 the first value to compare
     * @param value2 the other value to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second Object.
     */
    public int compareValue( Object value1, Object value2 )
    {
        return getValueComparator().compare( value1, value2 );
    }
}
