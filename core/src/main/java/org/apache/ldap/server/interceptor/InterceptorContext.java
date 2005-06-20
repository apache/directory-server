package org.apache.ldap.server.interceptor;


import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.partition.ContextPartitionNexus;
import org.apache.ldap.server.partition.SystemPartition;
import org.apache.ldap.server.schema.GlobalRegistries;


/**
 * @todo doc me
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorContext
{

    /**
     * the configuration
     */
    private final StartupConfiguration configuration;

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
    private final ContextPartitionNexus rootNexus;


    public InterceptorContext( StartupConfiguration configuration,
                               SystemPartition systemPartition,
                               GlobalRegistries globalRegistries,
                               ContextPartitionNexus rootNexus )
    {
        this.configuration = configuration;

        this.systemPartition = systemPartition;

        this.globalRegistries = globalRegistries;

        this.rootNexus = rootNexus;
    }


    public StartupConfiguration getConfiguration()
    {
        return configuration;
    }


    public GlobalRegistries getGlobalRegistries()
    {
        return globalRegistries;
    }


    public ContextPartitionNexus getRootNexus()
    {
        return rootNexus;
    }


    public SystemPartition getSystemPartition()
    {
        return systemPartition;
    }
}
