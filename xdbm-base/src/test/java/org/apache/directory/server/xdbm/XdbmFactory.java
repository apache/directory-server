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
public abstract class XdbmFactory
{
    public static XdbmFactory instance() throws Exception
    {
        String className = System.getProperty( "xdbmFactory" );
        if ( className == null )
        {
            throw new RuntimeException( "xdbmFactory does not exist in the system properties." );
        }
        Class clazz = Class.forName( className );
        return ( XdbmFactory ) clazz.newInstance();
    }
    
    public abstract Table createTable();
    public abstract void destroy( Table table );
    
    public abstract Index createIndex();
    public abstract void destroy( Index index );
    
    public abstract MasterTable createMasterTable();
    public abstract void destroy( MasterTable masterTable );
    
    public abstract Store createStore();
    public abstract void destroy( Store store );
}
