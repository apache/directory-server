package org.apache.ldap.server.jndi.invocation.interceptor;


import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.SystemPartition;
import org.apache.ldap.server.schema.GlobalRegistries;

import java.util.Map;


/**
 * @todo doc me
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorContext
{

    /**
     * the initial context environment that fired up the backend subsystem
     */
    private final Map environment;

    /**
     * Configuration for the interceptor.
     */
    private final Map config;

    /**
     * the system partition used by the context factory
     */
    private final SystemPartition systemPartition;

    /**
     * the registries for system schema objects
     */
    private final GlobalRegistries globalRegistries;

    /**
     * the root nexus
     */
    private final RootNexus rootNexus;


    public InterceptorContext( Map environment,
                               SystemPartition systemPartition,
                               GlobalRegistries globalRegistries,
                               RootNexus rootNexus,
                               Map config )
    {
        this.environment = environment;
        this.systemPartition = systemPartition;
        this.globalRegistries = globalRegistries;
        this.rootNexus = rootNexus;
        this.config = config;
    }


    public Map getEnvironment()
    {
        return environment;
    }


    public Map getConfig()
    {
        return config;
    }


    public GlobalRegistries getGlobalRegistries()
    {
        return globalRegistries;
    }


    public RootNexus getRootNexus()
    {
        return rootNexus;
    }


    public SystemPartition getSystemPartition()
    {
        return systemPartition;
    }
}
