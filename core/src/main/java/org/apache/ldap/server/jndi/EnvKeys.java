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
package org.apache.ldap.server.jndi;


/**
 * JNDI environment property key constants.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EnvKeys
{
    // ------------------------------------------------------------------------
    // Properties for operations
    // ------------------------------------------------------------------------

    /** property used to shutdown the system */
    public static final String SHUTDOWN = "server.operation.shutdown";
    /** property used to sync the system with disk */
    public static final String SYNC = "server.operation.sync";


    // ------------------------------------------------------------------------
    // Properties for setting working directory, schemas and allowing anon binds
    // ------------------------------------------------------------------------

    /** bootstrap prop: path to eve's working directory - relative or absolute */
    public static final String WKDIR = "server.wkdir";
    /** a comma separated list of schema class files to load */
    public static final String SCHEMAS = "server.schemas";
    /** bootstrap prop: if key is present it enables anonymous users */
    public static final String DISABLE_ANONYMOUS = "server.disable.anonymous";
    /** a comma separated list of authenticator names */
    public static final String AUTHENTICATORS = "server.authenticators";
    /** the envprop key base to the authenticator implementation class */
    public static final String AUTHENTICATOR_CLASS = "server.authenticator.class.";
    /** the envprop key base to the properties of an authenticator */
    public static final String AUTHENTICATOR_PROPERTIES = "server.authenticator.properties.";

    /**
     * bootstrap property: {@link Interceptor} or {@link InterceptorChain}
     * that will intercept directory operations when they are invoked.  You
     * don't need to specify this property if you want to use the default
     * interceptor chain.  If you specify this property, you might have to
     * add some default interceptors in <tt>org.apache.ldap.server.jndi.invocation.interceptor</tt>
     * package in your custom interceptor chain.
     * <p>
     * Here is an example of how to pass configuration values to interceptor:
     * <pre>
     * # Passes property 'propA=3' to the root interceptor chain.
     * server.interceptors#propA=3
     * # Passes property 'propB=7' to the interceptor whose name is 'myinterceptor'.
     * server.interceptors.myinterceptor#propB=7
     * # Passes property 'propC=9' to an interceptor 'yourinterceptor' whose
     * # parent is an interceptor chain 'childChain' which is a child of the
     * # root interceptor chain. 
     * server.interceptors.childChain.yourinterceptor#propC=9
     * </pre>
     */
    public static final String INTERCEPTORS = "server.interceptor";

    // ------------------------------------------------------------------------
    // Properties for protocol/network settings
    // ------------------------------------------------------------------------

    /** key used to disable the networking layer (wire protocol) */
    public static final String DISABLE_PROTOCOL = "server.net.disable.protocol";
    /** key used to hold the MINA registry instance to use rather than creating one */
    public static final String PASSTHRU = "server.net.passthru";
    /** key for port setting for ldap requests beside default 389 */
    public static final String LDAP_PORT = "server.net.ldap.port";
    /** key for port setting for secure ldap requests besides default 636 */
    public static final String LDAPS_PORT = "server.net.ldaps.port";

    // ------------------------------------------------------------------------
    // Properties for partition configuration
    // ------------------------------------------------------------------------

    /** a comma separated list of partition names */
    public static final String PARTITIONS = "server.db.partitions";
    /** the envprop key base to the suffix of a partition */
    public static final String SUFFIX = "server.db.partition.suffix.";
    /** the envprop key base to the implementation of a partition */
    public static final String PARTITION_CLASS = "server.db.partition.class.";
    /** the envprop key base to the properties of a partition */
    public static final String PARTITION_PROPERTIES = "server.db.partition.properties.";
    /** the envprop key base to the space separated list of indices for a partition */
    public static final String INDICES = "server.db.partition.indices.";
    /** the envprop key base to the Attributes for the context nexus entry */
    public static final String ATTRIBUTES = "server.db.partition.attributes.";
}
