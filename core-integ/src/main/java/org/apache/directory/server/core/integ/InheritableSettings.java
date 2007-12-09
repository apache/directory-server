/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import org.apache.directory.server.core.integ.annotations.*;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Inheritable settings of a test suite, test class, or test method.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InheritableSettings
{
    /** the default setup mode to use if inheritence leads to null value */
    public static final SetupMode DEFAULT_MODE = SetupMode.ROLLBACK;
    /** the default factory to use if inheritence leads to a null value */
    public static final DirectoryServiceFactory DEFAULT_FACTORY = DirectoryServiceFactory.DEFAULT;

    /** parent settings to inherit from */
    private final InheritableSettings parent;
    /** junit test description containing all annotations queried */
    private final Description description;
    /** default scope of a service */
    private static final ServiceCleanupLevel DEFAULT_CLEANUP_LEVEL = ServiceCleanupLevel.TESTSUITE;


    /**
     * Creates a new InheritableSettings instance for test suites description.
     *
     * @param description junit description for the suite
     */
    public InheritableSettings( Description description )
    {
        this.description = description;
        this.parent = null;

//        if ( ! description.isSuite() )
//        {
//            throw new IllegalStateException( String.format( "%s is not a suite! It requires parent settings.",
//                    description.getDisplayName() ) );
//        }
    }


    /**
     * Creates a new InheritableSettings instance based on a test object's
     * description and it's parent's settings.
     *
     * @param description junit description for the test object
     * @param parent the parent settings or null if the test entity is a suite
     */
    public InheritableSettings( Description description, InheritableSettings parent )
    {
        this.description = description;
        this.parent = parent;

        if ( description.isSuite() && ! isSuiteLevel() )
        {
            throw new IllegalStateException( String.format( "The parent must be null for %s suite",
                    description.getDisplayName() ) );
        }
    }


    public Description getDescription()
    {
        return description;
    }


    public InheritableSettings getParent()
    {
        return parent;
    }


    public boolean isSuiteLevel()
    {
        return parent == null;
    }


    public boolean isClassLevel()
    {
        return parent != null && parent.getParent() == null;
    }


    public boolean isMethodLevel()
    {
        return parent != null && parent.getParent() != null;
    }


    public SetupMode getMode()
    {
        SetupMode parentMode = DEFAULT_MODE;
        if ( parent != null )
        {
            parentMode = parent.getMode();
        }

        Mode annotation = description.getAnnotation( Mode.class );
        if ( annotation == null )
        {
            return parentMode;
        }
        else
        {
            return annotation.value();
        }
    }


    public DirectoryServiceFactory getFactory() throws IllegalAccessException, InstantiationException
    {
        DirectoryServiceFactory parentFactory = DEFAULT_FACTORY;
        if ( parent != null )
        {
            parentFactory = parent.getFactory();
        }

        Factory annotation = description.getAnnotation( Factory.class );
        if ( annotation == null )
        {
            return parentFactory;
        }
        else
        {
            return ( DirectoryServiceFactory ) annotation.value().newInstance();
        }
    }


    public List<String> getLdifs( List<String> ldifs )
    {
        if ( ldifs == null )
        {
            ldifs = new ArrayList<String>();
        }

        if ( parent != null )
        {
            parent.getLdifs( ldifs );
        }

        ApplyLdifs annotation = description.getAnnotation( ApplyLdifs.class );
        if ( annotation != null && annotation.value() != null )
        {
            ldifs.addAll( Arrays.asList( annotation.value() ) );
        }

        return ldifs;
    }


    public List<String> getLdifFiles( List<String> ldifFiles )
    {
        if ( ldifFiles == null )
        {
            ldifFiles = new ArrayList<String>();
        }

        if ( parent != null )
        {
            parent.getLdifFiles( ldifFiles );
        }

        ApplyLdifFiles annotation = description.getAnnotation( ApplyLdifFiles.class );
        if ( annotation != null && annotation.value() != null )
        {
            ldifFiles.addAll( Arrays.asList( annotation.value() ) );
        }

        return ldifFiles;
    }


    public ServiceCleanupLevel getCleanupLevel()
    {
        ServiceCleanupLevel parentCleanupLevel = DEFAULT_CLEANUP_LEVEL;
        if ( parent != null )
        {
            parentCleanupLevel = parent.getCleanupLevel();
        }

        CleanupLevel annotation = description.getAnnotation( CleanupLevel.class );
        if ( annotation == null )
        {
            return parentCleanupLevel;
        }
        else
        {
            return annotation.value();
        }
    }
}
