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
package org.apache.eve.jndi;


/**
 * JNDI environment property key constants.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EnvKeys
{
    // ------------------------------------------------------------------------
    // Properties for operations
    // ------------------------------------------------------------------------

    /** property used to shutdown the system */
    public static final String SHUTDOWN = "eve.operation.shutdown";
    /** property used to sync the system with disk */
    public static final String SYNC = "eve.operation.sync";


    // ------------------------------------------------------------------------
    // Properties for setting working directory, schemas and allowing anon binds
    // ------------------------------------------------------------------------

    /** bootstrap prop: path to eve's working directory - relative or absolute */
    public static final String WKDIR = "eve.wkdir";
    /** a comma separated list of schema class files to load */
    public static final String SCHEMAS = "eve.schemas";
    /** bootstrap prop: if key is present it enables anonymous users */
    public static final String DISABLE_ANONYMOUS = "eve.disable.anonymous";


    // ------------------------------------------------------------------------
    // Properties for protocol/network settings
    // ------------------------------------------------------------------------

    /** key used to disable the networking layer (wire protocol) */
    public static final String DISABLE_PROTOCOL = "eve.net.disable.protocol";
    /** key used to hold the frontend to use rather than creating one */
    public static final String PASSTHRU = "eve.net.passthru";
    /** key for port setting for ldap requests beside default 389 */
    public static final String LDAP_PORT = "eve.net.ldap.port";
    /** key for port setting for secure ldap requests besides default 636 */
    public static final String LDAPS_PORT = "eve.net.ldaps.port";

    // ------------------------------------------------------------------------
    // Properties for partition configuration
    // ------------------------------------------------------------------------

    /** a comma separated list of partition names */
    public static final String PARTITIONS = "eve.db.partitions";
    /** the envprop key base to the suffix of a partition */
    public static final String SUFFIX = "eve.db.partition.suffix.";
    /** the envprop key base to the space separated list of indices for a partition */
    public static final String INDICES = "eve.db.partition.indices.";
    /** the envprop key base to the Attributes for the context nexus entry */
    public static final String ATTRIBUTES = "eve.db.partition.attributes.";
}
