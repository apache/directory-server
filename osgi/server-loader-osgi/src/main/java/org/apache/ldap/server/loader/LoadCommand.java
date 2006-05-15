/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.ldap.server.loader;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.protocol.shared.store.Krb5KdcEntryFilter;
import org.apache.directory.server.protocol.shared.store.LdifFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ungoverned.osgi.service.shell.Command;

public class LoadCommand implements Command
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( LoadCommand.class );

    private DirContext ctx;
    private LdifFileLoader loader;
    private InitialContextFactory factory;

    public String getName()
    {
        return "load";
    }

    public String getUsage()
    {
        return "load <path> <context>";
    }

    public String getShortDescription()
    {
        return "Load LDIF entries into the embedded directory backing store.";
    }

    public void execute( String line, PrintStream out, PrintStream err )
    {
        String[] components = line.split( "\\s" );

        int arguments = components.length - 1;

        if ( arguments < 2 )
        {
            err.println( "Incorrect number of arguments (" + arguments + "):  load <path> <context>" );
            return;
        }

        String pathToLdif = components[ 1 ];
        String initialContext = components[ 2 ];

        Hashtable env = new Hashtable( new LoaderConfiguration().toJndiEnvironment() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, initialContext );

        try
        {
            ctx = (DirContext) factory.getInitialContext( env );
        }
        catch ( NamingException ne )
        {
            if ( log.isDebugEnabled() )
            {
                log.error( "Error obtaining initial context " + initialContext, ne );
            }
            else
            {
                log.error( "Error obtaining initial context " + initialContext );
            }

            return;
        }

        List filters = new ArrayList();
        filters.add(  new Krb5KdcEntryFilter() );

        loader = new LdifFileLoader( ctx, new File( pathToLdif ), filters );
        loader.execute();
    }

    public void setInitialContextFactory( InitialContextFactory factory )
    {
        this.factory = factory;
        log.debug( getName() + " has bound to " + factory );
    }

    public void unsetInitialContextFactory( InitialContextFactory factory )
    {
        this.factory = null;
        log.debug( getName() + " has unbound from " + factory );
    }
}
