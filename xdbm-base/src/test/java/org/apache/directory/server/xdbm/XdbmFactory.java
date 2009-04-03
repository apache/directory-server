/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.xdbm;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Factory interface used by attached tests to instantiate various Xdbm 
 * component implementations. Xdbm implementation modules would implement
 * extend this class to supply their own factories which produces their own
 * implementations. 
 * 
 * To use the tests in this module's package, just add the system property,
 * xdbmFactory, to point to the fully qualified name of the factory 
 * implementation of the new module.  This is done in the POM for the 
 * surefire plugin.  Then follow this guide to attach the jars:
 * 
 * http://maven.apache.org/guides/mini/guide-attached-tests.html
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@SuppressWarnings("unchecked")
public abstract class XdbmFactory<K,V>
{
    private static final Logger LOG = LoggerFactory.getLogger( XdbmFactory.class.getSimpleName() );

    
    /**
     * Get's an instance of XdbmFactory using a system property to find the
     * implementation specific class.
     *
     * @return implementation specific factory or null if one cannot be found
     * @throws Exception if failures occur during factory instantiation
     */
    public static XdbmFactory instance() throws Exception
    {
        String className = System.getProperty( "xdbmFactory" );
        if ( className == null )
        {
            LOG.error( "xdbmFactory does not exist in the system properties. " 
                + "Null factory instance being returned. ");
        }
        Class clazz = Class.forName( className );
        return ( XdbmFactory ) clazz.newInstance();
    }
    
    
    /**
     * Creates a new implementation specific Table instance and initializes it
     * for immediate use.
     *
     * @return a Table implementation
     * @throws Exception on implementation specific issues on Table creation
     */
    public abstract Table<K,V> createTable() throws Exception;
    
    
    /**
     * Destroy an implementation specific Table instance removing any 
     * resources it may have taken.
     *
     * @param table a Table implementation
     * @throws Exception on implementation specific issues on Table destruction
     */
    public abstract void destroy( Table<K,V> table ) throws Exception;
    
    public abstract Index createIndex() throws Exception;
    public abstract void destroy( Index index ) throws Exception;
    
    public abstract MasterTable createMasterTable() throws Exception;
    public abstract void destroy( MasterTable masterTable ) throws Exception;
    
    public abstract Store createStore() throws Exception;
    public abstract void destroy( Store store ) throws Exception;
}
