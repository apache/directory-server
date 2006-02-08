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


/**
 * A key/value tuple for simple two column Tables.  Implemented to provide 
 * independence from the Jdbm Tuple class.  Key and value copying should be 
 * performed to transpose jdbm.helper.Tuple data into our generic Tuple.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class Tuple
{
    /** the key for this Tuple */
    private Object key;
    /** the value for this Tuple */
    private Object value;

    
    /**
     * Do nothing default that has a null key and null value.
     */
    public Tuple()
    {
        // does nothing!
    }
    

    /**
     * Creates a Tuple using a key and a value.
     * 
     * @param key the key to set
     * @param value the value to set
     */    
    public Tuple( Object key, Object value )
    {
        this.key = key;
        this.value = value;
    }
    
    
    /**
     * Gets the key for this Tuple.
     *
     * @return the Tuple's key
     */
    public Object getKey()
    {
        return key;
    }
    
    
    /**
     * Sets the key for this Tuple.
     *
     * @param key the new key to set
     */
    public void setKey( Object key )
    {
        this.key = key;
    }
    
    
    /**
     * Gets the value for this Tuple.
     *
     * @return the Tuple's value
     */
    public Object getValue()
    {
        return value;
    }
    
    
    /**
     * Sets the value for this Tuple.
     *
     * @param value the new value to set
     */
    public void setValue( Object value )
    {
        this.value = value;
    }
}
