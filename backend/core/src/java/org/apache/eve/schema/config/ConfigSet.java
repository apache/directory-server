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
package org.apache.eve.schema.config;


/**
 * A configuration of like Schema objects bundled together and identified as a
 * set.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface ConfigSet
{
    /**
     * Gets the name of the owner of the schema objects within this
     * ConfigSet.
     *
     * @return the identifier for the owner of this set's objects
     */
    String getOwner();

    /**
     * Gets the name of the logical schema the objects of this ConfigSet
     * belong to: e.g. krb5-kdc may be the logical LDAP schema name.
     *
     * @return the name of the logical schema
     */
    String getSchemaName();

    /**
     * Gets the names of other schemas that this objects within this
     * ConfigSet depends upon.  These dependent schemas are those
     * whose ConfigurationSets will be processed first.
     *
     * @return the String names of schema dependencies
     */
    String[] getDependencies();
}
