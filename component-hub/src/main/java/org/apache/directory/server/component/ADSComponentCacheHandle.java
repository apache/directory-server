package org.apache.directory.server.component;


import org.apache.directory.shared.ldap.model.ldif.LdifReader;


public class ADSComponentCacheHandle
{
    /*
     * LdifReader pointing to a ldif file holding cached schema of the component.
     */
    private LdifReader cachedSchemaHandle;

    /*
     * LdifReader pointing to a ldif file holding previously created component instance entries. 
     */
    private LdifReader cachedInstancesHandle;

    /*
     * Component version of the cache. Used for validating of invalidating the cache in case of
     * component updates.
     */
    private String cachedVersion;


    public ADSComponentCacheHandle( LdifReader schema, LdifReader instances, String version )
    {
        cachedSchemaHandle = schema;
        cachedInstancesHandle = instances;
        cachedVersion = version;
    }


    /**
     * Getter for cached schema handle
     *
     * @return LdifReader pointing to a ldif file of schema.
     */
    public LdifReader getCachedSchemaHandle()
    {
        return cachedSchemaHandle;
    }


    /**
     * Getter for cached instances handle
     *
     * @return LdifReader pointing to a ldif file of instance entries.
     */
    public LdifReader getCachedInstancesHandle()
    {
        return cachedInstancesHandle;
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

}
