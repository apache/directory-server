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
package org.apache.ldap.common.schema;


import javax.naming.NamingException;

import org.apache.ldap.common.util.SynchronizedLRUMap;


/**
 * Caches previously normalized values.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CachingNormalizer implements Normalizer
{
    /** Cache maximum size default */
    public static final int CACHE_MAX = 250 ;
    
    /** Least recently used cache */
    private final SynchronizedLRUMap cache ;
    /** The underlying decorated Normalizer */
    private final Normalizer normalizer;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a CachingNormalizer that decorates another normalizer using a
     * default cache size.
     * 
     * @param normalizer the underlying Normalizer being decorated
     */
    public CachingNormalizer( Normalizer normalizer )
    {
        this( normalizer, CACHE_MAX ) ;
    }


    /**
     * Creates a CachingNormalizer that decorates another normalizer using a
     * specified cache size.
     * 
     * @param normalizer the underlying Normalizer being decorated
     * @param cacheSz the maximum size of the name cache
     */
    public CachingNormalizer( Normalizer normalizer, int cacheSz )
    {
        this.normalizer = normalizer;
        cache = new SynchronizedLRUMap( cacheSz ) ;
    }


    /**
     * @see org.apache.ldap.common.schema.Normalizer#normalize(java.lang.Object)
     */
    public Object normalize( Object value ) throws NamingException
    {
    	if ( value == null )
    	{
    		return null;
    	}
    	
    	Object result = cache.get( value );
    	
        if ( result != null )
        {
            return result;
        }
        
        Object normalized = normalizer.normalize( value ) ;
        cache.put( value, normalized ) ;
        return normalized ;
    }
}
