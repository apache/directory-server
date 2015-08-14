package org.apache.directory.server.core.api;


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

import java.io.File;


/**
 * Convenience class to encapsulate paths to various directories and files within
 * an instance.
 * <br>
 * The default layout is :
 * <pre>
 *  &lt;instance directory&gt;
 *    |
 *    +-- conf/
 *    |    |
 *    |    +-- config.ldif
 *    |    |
 *    |    +-- wrapper.conf
 *    |    |
 *    |    +-- log4j.properties
 *    |
 *    +-- partitions/
 *    |    |
 *    |    +-- system/
 *    |    |    |
 *    |    |    +-- master.db
 *    |    |    |
 *    |    |    +-- objectclass.db
 *    |    |    |
 *    |    |    +-- objectclass.lg
 *    |    |    |
 *    |    |    +-- &lt;index XXX lg and db files&gt;
 *    |    |
 *    |    +-- schema/
 *    |    |    |
 *    |    |    :
 *    |    |
 *    |    +-- &lt;partition XXX&gt;/
 *    |    |    |
 *    |    :    :
 *    |
 *    +-- log/
 *    |    |
 *    |   [+-- journal.ldif]
 *    |    |
 *    |    +-- &lt;log file&gt;
 *    |
 *    +-- run/
 *    |
 *    +-- cache/
 *    |
 *    +-- syncrepl-data/
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InstanceLayout extends AbstractLayout
{
    // Static final fields for system property names
    private static final String LOG_DIR = "apacheds.log.dir";
    private static final String RUN_DIR = "apacheds.run.dir";

    /** Static directory names */
    public static final String LOG_NAME = "log";
    public static final String RUN_NAME = "run";
    public static final String CONF_NAME = "conf";
    public static final String PARTITIONS_NAME = "partitions";
    private static final String REPL_NAME = "syncrepl-data";
    private static final String CACHE_NAME = "cache";

    /** Static file names */
    private static final String LOG4J_PROPERTIES = "log4j.properties";
    private static final String WRAPPER_CONF = "wrapper.conf";
    private static final String CONFIG_LDIF = "config.ldif";

    /** The Log directory */
    private File logDir;

    /** The Partitions directory */
    private File partitionsDir;

    /** The Run directory */
    private File runDir;

    /** The Conf directory */
    private File confDir;

    /** The replication data directory */
    private File replDir;

    /** The cache directory */
    private File cacheDir;


    /**
     * Creates a new instance of InstanceLayout.
     *
     * @param instanceDirectory the instance directory
     */
    public InstanceLayout( File instanceDirectory )
    {
        super( instanceDirectory );
        init();
    }


    /**
     * Creates a new instance of InstanceLayout.
     *
     * @param instanceDirectoryPath the path to the instance directory
     */
    public InstanceLayout( String instanceDirectoryPath )
    {
        super( instanceDirectoryPath );
        init();
    }


    /**
     * Initializes the InstanceLayout.
     */
    private void init()
    {
        // The required directories
        File[] requiredDirectories = new File[]
            {
                getInstanceDirectory(),
                getConfDirectory(),
                getLogDirectory(),
                getPartitionsDirectory(),
                getRunDirectory(),
                getCacheDirectory()
        };

        setRequiredDirectories( requiredDirectories );

        // The required files
        File[] requiredFiles = new File[]
            {
                getWrapperConfigurationFile(),
                getLogConfigurationFile() /*,
                                          getApacheDsConfigurationLdifFile() */// TODO re-activate this when possible.
        };

        setRequiredFiles( requiredFiles );
    }


    /**
     * Gets the 'conf' directory ('&lt;instance&gt;/conf').
     *
     * @return the 'conf' directory
     */
    public File getConfDirectory()
    {
        if ( confDir == null )
        {
            confDir = new File( getInstanceDirectory(), CONF_NAME );
        }

        return confDir;
    }


    /**
     * @param confDir the confDir to set
     */
    public void setConfDir( File confDir )
    {
        this.confDir = confDir;
    }


    /**
     * Gets the 'cache' directory ('&lt;instance&gt;/cache').
     *
     * @return the 'cache' directory
     */
    public File getCacheDirectory()
    {
        if ( cacheDir == null )
        {
            cacheDir = new File( getInstanceDirectory(), CACHE_NAME );
        }

        return cacheDir;
    }


    /**
     * @param cacheDir the confDir to set
     */
    public void setCacheDir( File cacheDir )
    {
        this.cacheDir = cacheDir;
    }


    /**
     * Gets the 'log' directory ('&lt;instance&gt;/log').
     *
     * @return the 'log' directory
     */
    public File getLogDirectory()
    {
        if ( logDir == null )
        {
            String systemLogDir = System.getProperty( LOG_DIR );

            if ( systemLogDir != null )
            {
                logDir = new File( systemLogDir );
            }
            else
            {
                logDir = new File( getInstanceDirectory(), LOG_NAME );
            }
        }

        return logDir;
    }


    /**
     * @param logDir the logDir to set
     */
    public void setLogDir( File logDir )
    {
        this.logDir = logDir;
    }


    /**
     * Gets the 'partitions' directory ('&lt;instance&gt;/partitions')
     *
     * @return the 'partitions' directory
     */
    public File getPartitionsDirectory()
    {
        if ( partitionsDir == null )
        {
            partitionsDir = new File( getInstanceDirectory(), PARTITIONS_NAME );
        }

        return partitionsDir;
    }


    /**
     * @param partitionsDir the partitionsDir to set
     */
    public void setPartitionsDir( File partitionsDir )
    {
        this.partitionsDir = partitionsDir;
    }


    /**
     * Gets the 'run' directory in the installation directory ('&lt;instance&gt;/run').
     *
     * @return the 'run' directory
     */
    public File getRunDirectory()
    {
        if ( runDir == null )
        {
            String systemRunDir = System.getProperty( RUN_DIR );

            if ( systemRunDir != null )
            {
                runDir = new File( systemRunDir );
            }
            else
            {
                runDir = new File( getInstanceDirectory(), RUN_NAME );
            }
        }

        return runDir;
    }


    /**
     * @param runDir the runDir to set
     */
    public void setRunDir( File runDir )
    {
        this.runDir = runDir;
    }


    /**
     * Gets the instance directory.
     *
     * @return the instance directory
     */
    public File getInstanceDirectory()
    {
        return getDirectory();
    }


    /**
     * Gets the log configuration file (<em>'&lt;instance&gt;/conf/log4j.properties'</em>).
     *
     * @return the log configuration file
     */
    public File getLogConfigurationFile()
    {
        return new File( getConfDirectory(), LOG4J_PROPERTIES );
    }


    /**
     * Gets the wrapper configuration file (<em>'&lt;instance&gt;/conf/wrapper.conf'</em>).
     *
     * @return the wrapper configuration file
     */
    public File getWrapperConfigurationFile()
    {
        return new File( getConfDirectory(), WRAPPER_CONF );
    }


    /**
     * Gets the apacheds configuration ldif file (<em>'&lt;instance&gt;/conf/config.ldif'</em>).
     *
     * @return the apacheds configuration ldif file
     */
    public File getApacheDsConfigurationLdifFile()
    {
        return new File( getConfDirectory(), CONFIG_LDIF );
    }


    /**
     * Gets the 'replication' directory where replication journals are stored
     * (<em>'&lt;instance&gt;/syncrepl-data'</em>).
     *
     * @return the 'replication' directory
     */
    public File getReplDirectory()
    {
        if ( replDir == null )
        {
            replDir = new File( getInstanceDirectory(), REPL_NAME );
        }

        return replDir;
    }


    /**
     * Sets the directory where the replication data are stored
     * 
     * @param replDir the replication journal data directory
     */
    public void setReplDirectory( File replDir )
    {
        this.replDir = replDir;
    }


    /**
     * @see String#toString()
     */
    public String toString()
    {
        return "Instance Layout: \n"
            + "  Instance dir                  : " + getInstanceDirectory() + "\n"
            + "  Instance conf dir             : " + getConfDirectory() + "\n"
            + "  Instance log dir              : " + getLogDirectory() + "\n"
            + "  Instance run dir              : " + getRunDirectory() + "\n"
            + "  Instance partitions dir       : " + getPartitionsDirectory() + "\n"
            + "  Instance replication data dir : " + getReplDirectory() + "\n"
            + "  Instance cache dir            : " + getCacheDirectory() + "\n";
    }
}
