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
package org.apache.ldap.server;


import javax.naming.directory.Attributes;


/**
 * A configuration bean for ContextPartitions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ContextPartitionConfig
{
    private String suffix;
    private String id;
    private String[] indices;
    private Attributes attributes;
    private String partitionClass;
    private String properties;


    public String getSuffix()
    {
        return suffix;
    }


    public void setSuffix( String suffix )
    {
        this.suffix = suffix;
    }


    public String getId()
    {
        return id;
    }


    public void setId( String id )
    {
        this.id = id;
    }


    public String[] getIndices()
    {
        return indices;
    }


    public void setIndices( String[] indices )
    {
        this.indices = indices;
    }


    public Attributes getAttributes()
    {
        return attributes;
    }


    public void setAttributes( Attributes attributes )
    {
        this.attributes = attributes;
    }

    public String getPartitionClass()
    {
        return partitionClass;
    }

    public void setPartitionClass( String partitionClass )
    {
        this.partitionClass = partitionClass;
    }

    public String getProperties()
    {
        return properties;
    }

    public void setProperties( String properties )
    {
        this.properties = properties;
    }
}
