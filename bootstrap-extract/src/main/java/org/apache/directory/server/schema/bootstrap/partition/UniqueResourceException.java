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


package org.apache.directory.server.schema.bootstrap.partition;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * @version $Rev$ $Date$
 */
public class UniqueResourceException extends RuntimeException
{
    public static final long serialVersionUID = 1L;

    private final String resourceName;
    private final List<URL> urls;
    private final String resourceDescription;

    public UniqueResourceException( String resourceName, String resourceDescription )
    {
        this( resourceName, null, resourceDescription );
    }

    public UniqueResourceException( String resourceName, List<URL> urls, String resourceDescription )
    {
        this.resourceName = resourceName;
        this.urls = urls;
        this.resourceDescription = resourceDescription;
    }

    public UniqueResourceException( String resourceName, URL first, Enumeration<URL> urlEnum, String resourceDescription )
    {
        this( resourceName, toList( first, urlEnum ), resourceDescription );
    }

    private static List<URL> toList( URL first, Enumeration<URL> urlEnum )
    {
        ArrayList<URL> urls = new ArrayList<URL>();
        urls.add( first );
        while( urlEnum.hasMoreElements() )
        {
            urls.add( urlEnum.nextElement() );
        }
        return urls;
    }

    public String getMessage()
    {
        StringBuffer buf = new StringBuffer( "Problem locating " ).append( resourceDescription ).append( "\n" );
        if ( urls == null )
        {
            buf.append( "No resources named '" ).append( resourceName ).append( "' located on classpath" );
        } else
        {
            buf.append( "Multiple copies of resource named '" ).append( resourceName ).append(
                    "' located on classpath at urls" );
            for ( URL url : urls )
            {
                buf.append( "\n    " ).append( url );
            }
        }
        return buf.toString();
    }


    public String getResourceName()
    {
        return resourceName;
    }

    public List<URL> getUrls()
    {
        return Collections.unmodifiableList( urls );
    }
}
