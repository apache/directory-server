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
package org.apache.eve.tools.schema;


import org.apache.tools.ant.BuildException;
import org.apache.eve.schema.bootstrap.AbstractBootstrapSchema;


/**
 * Document this class.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EveSchemaToolTask extends org.apache.tools.ant.Task
{
    private String pkg;
    private String name;
    private String owner;
    private String[] dependencies;


    public void setPackage( String pkg )
    {
        this.pkg = pkg;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public void setOwner( String owner )
    {
        this.owner = owner;
    }


    public void setDependencies( String deps )
    {
        dependencies = deps.split( "," );

        for ( int ii = 0; ii < dependencies.length; ii++ )
        {
            dependencies[ii] = dependencies[ii].trim();
        }
    }


    public void execute() throws BuildException
    {
        super.execute();
        EveSchemaTool tool;

        try
        {
            tool = new EveSchemaTool();
        }
        catch ( Exception e )
        {
            throw new BuildException( "Failed to create schema tool", e );
        }

        AbstractBootstrapSchema schema =
                new AbstractBootstrapSchema( owner, name, pkg, dependencies ){};
        tool.setSchema( schema );

        try
        {
            tool.generate();
        }
        catch ( Exception e )
        {
            throw new BuildException( "Failed to generate " + name +
                    " schema classes in package " + pkg, e );
        }
    }
    
    
    public String toString()
    {
    	StringBuffer buf = new StringBuffer();
    	buf.append( "\nSCHEMA:\nname = " ).append( name ).append( '\n' );
    	buf.append( "owner = " ).append( owner ).append( '\n' );
    	buf.append( "package = " ).append( pkg ).append( '\n' );
    	buf.append( "dependencies = " );
    	
    	for ( int ii = 0; ii < dependencies.length; ii++ )
    	{
    		buf.append( dependencies[ii] );
    		if ( ii < dependencies.length-1 )
    		{
    			buf.append( ',' );
    		}
    	}
    	
    	return buf.toString();
    }
}
