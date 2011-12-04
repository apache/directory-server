package org.apache.directory.server.component.instance;


import java.util.Properties;


public class CachedComponentInstance
{
    /*
     * Dn of the cached instance entry in DIT
     */
    private String cacheDn;

    /*
     * Configuration of the cached instance entry
     */
    private Properties cachedConfiguration;


    public CachedComponentInstance( String Dn, Properties conf )
    {
        cacheDn = Dn;
        cachedConfiguration = conf;
    }


    /**
     * Getter for cached configuration
     *
     * @return cached configuration for instance
     */
    public Properties getCachedConfiguration()
    {
        return cachedConfiguration;
    }


    /**
     * Getter for cached instance location
     *
     * @return DIT location for cached instance
     */
    public String getCachedDn()
    {
        return cacheDn;
    }
}
