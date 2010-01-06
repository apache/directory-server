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
package org.apache.directory.shared.ldap.schema.ldif.extractor;


import java.io.IOException;


/**
 * Extracts LDIF files for the schema repository.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SchemaLdifExtractor
{
    /**
     * Gets whether or not the content has been extracted previously by this
     * extractor or another into the directory file structure.
     *
     * @return true if extracted at least once, false otherwise
     */
    boolean isExtracted();

    
    /**
     * Extracts the LDIF files from a Jar file or copies exploded LDIF resources.
     *
     * @param overwrite over write extracted structure if true, false otherwise
     * @throws java.io.IOException if schema already extracted and on IO errors
     */
    void extractOrCopy( boolean overwrite ) throws IOException;

    
    /**
     * Extracts the LDIF files from a Jar file or copies exploded LDIF
     * resources without overwriting the resources if the schema has
     * already been extracted.
     *
     * @throws java.io.IOException if schema already extracted and on IO errors
     */
    void extractOrCopy() throws IOException;
}
