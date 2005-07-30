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
package org.apache.ldap.server.tools.schema;


import org.apache.ldap.server.schema.bootstrap.AbstractBootstrapSchema;
import org.apache.tools.ant.BuildException;


/**
 * Document this class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DirectorySchemaToolTask extends org.apache.tools.ant.Task
{
    private String javaSrc;
    private String pkg;
    private String name;
    private String owner;
    private String[] dependencies;
    private BuildException lastFault;

  
    public void setJavaSrc( String javaSrc )
    {
        this.javaSrc = javaSrc;
    }


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
        DirectorySchemaTool tool;

        try
        {
            tool = new DirectorySchemaTool();
        }
        catch ( Exception e )
        {
            lastFault = new BuildException( "Failed to create schema tool", e );
            throw lastFault;
        }

        AbstractBootstrapSchema schema =
                new AbstractBootstrapSchema( owner, name, pkg, dependencies ){};
        tool.setSchema( schema );

        if ( javaSrc != null )
        {
            tool.setJavaSrcDir( javaSrc ); 
        }

        try
        {
            tool.generate();
        }
        catch ( Exception e )
        {
            lastFault = new BuildException( "Failed to generate " + name +
                    " schema classes in package " + pkg, e );
            throw lastFault;
        }
    }


    public BuildException getLastFault()
    {
        return lastFault;
    }


    public boolean hasFaulted()
    {
        return lastFault != null;
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
