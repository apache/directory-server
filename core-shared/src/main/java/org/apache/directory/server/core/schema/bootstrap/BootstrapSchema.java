/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.schema.bootstrap;


/**
 * A configuration of related Schema objects bundled together and identified as
 * a group.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface BootstrapSchema
{
    /**
     * Gets the name of the owner of the schema objects within this
     * BootstrapSchema.
     *
     * @return the identifier for the owner of this set's objects
     */
    String getOwner();


    /**
     * Gets the name of the logical schema the objects of this BootstrapSchema
     * belong to: e.g. krb5-kdc may be the logical LDAP schema name.
     *
     * @return the name of the logical schema
     */
    String getSchemaName();


    /**
     * Gets the package name of the schema's object factories.
     *
     * @return the name of the schema's package name
     */
    String getPackageName();


    /**
     * Gets the names of other schemas that this objects within this
     * BootstrapSchema depends upon.  These dependent schemas are those
     * whose ConfigurationSets will be processed first.
     *
     * @return the String names of schema dependencies
     */
    String[] getDependencies();


    /**
     * Gets the base class name for bootstrap Schema class files.  This name
     * is the schema name with the first character capitalized and qualified
     * by the package name.  So for a bootstrap schema name of 'bar' within
     * the 'foo' package would return foo.Bar as the base class name.
     *
     * @return the base of all bootstrap schema class names for this schema
     */
    String getBaseClassName();


    /**
     * Gets the default base class name for bootstrap Schema class files.  This
     * name is the schema name with the first character capitalized and qualified
     * by the default package name.  So for a bootstrap schema name of 'bar'
     * within the 'foo' package would return foo.Bar as the base class name.
     *
     * @return the default base of all bootstrap schema class names for this schema
     */
    String getDefaultBaseClassName();


    /**
     * Gets the class name for bootstrap Schema class producer type.
     *
     * @return the bootstrap schema class name for a producer type in this schema
     */
    String getFullClassName( ProducerTypeEnum type );


    /**
     * If the base class name for the target class does not resolve, we attempt
     * to load another backup class using this default base class name which
     * tries another package for the target class factory to load.
     *
     * @return the default base class name
     */
    String getFullDefaultBaseClassName( ProducerTypeEnum type );


    /**
     * Gets the unqualified class name for bootstrap Schema class producer type.
     *
     * @return the bootstrap schema class name for a producer type in this schema
     */
    String getUnqualifiedClassName( ProducerTypeEnum type );


    /**
     * Gets the unqualified class name for Schema class.
     *
     * @return the bootstrap schema class name
     */
    String getUnqualifiedClassName();
}
