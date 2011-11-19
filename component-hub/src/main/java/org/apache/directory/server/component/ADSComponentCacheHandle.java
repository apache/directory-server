package org.apache.directory.server.component;


public class ADSComponentCacheHandle
{
    /*
     * Dir path for cache
     */
    private String cacheBaseDir;

    /*
     * File path containing cached schema of the component.
     */
    private String cachedSchemaFileLocation;

    /*
     * Directory path containing cached instance configurations of the component.
     */
    private String cachedInstanceConfigurationsLocation;

    /*
     * Component version of the cache. Used for validating of invalidating the cache in case of
     * component updates.
     */
    private String cachedVersion;


    public ADSComponentCacheHandle( String base, String schema, String configurations, String version )
    {
        cacheBaseDir = base;
        cachedSchemaFileLocation = schema;
        cachedInstanceConfigurationsLocation = configurations;
        cachedVersion = version;
    }


    /**
     * Getter for cache base dir
     *
     * @return String Location of the base cache path
     */
    public String getCacheBaseDir()
    {
        return cacheBaseDir;
    }


    /**
     * Getter for cached schema handle
     *
     * @return String containing file location of cached schema
     */
    public String getCachedSchemaLocation()
    {
        return cachedSchemaFileLocation;
    }


    /**
     * Getter for cached instance configurations
     *
     * @return String containing directory location of cached instance configurations
     */
    public String getCachedInstanceConfigurationsLocation()
    {
        return cachedInstanceConfigurationsLocation;
    }


    /**
     * Getter for cached version of cache
     *
     * @return version of cache.
     */
    public String getCachedVersion()
    {
        return cachedVersion;
    }


    /**
     * Setter for cached version of cache
     *
     *@param ver Version set on cache.
     */
    public void setCachedVersion( String ver )
    {
        cachedVersion = ver;
    }

}
