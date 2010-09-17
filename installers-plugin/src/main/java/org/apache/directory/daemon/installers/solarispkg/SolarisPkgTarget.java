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
package org.apache.directory.daemon.installers.solarispkg;


import java.io.File;

import org.apache.directory.daemon.installers.Target;


/**
 * A PKG installer for the Solaris platform.
 * 
 * To create a PKG installer we use the pkgmk and pkgtrans utilities that are 
 * bundled in the Solaris OS.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SolarisPkgTarget extends Target
{
    private File pkgMaker = new File( "/usr/bin/pkgmk" );
    private File pkgTranslator = new File( "/usr/bin/pkgtrans" );


    /**
     * Gets the 'pkgmk' utility.
     *
     * @return
     *      the 'pkgmk' utility.
     */
    public File getPkgMaker()
    {
        return pkgMaker;
    }


    /**
     * Sets the 'pkgmk' utility.
     *
     * @param pkgMaker
     *      the 'pkgmk' utility.
     */
    public void setPkgMaker( File pkgMaker )
    {
        this.pkgMaker = pkgMaker;
    }


    /**
     * Gets the 'pkgtrans' utility.
     *
     * @return
     *      the 'pkgtrans' utility.
     */
    public File getPkgTranslator()
    {
        return pkgTranslator;
    }


    /**
     * Sets the 'pkgtrans' utility.
     *
     * @param pkgTranslator
     *      the 'pkgtrans' utility.
     */
    public void setPkgTranslator( File pkgTranslator )
    {
        this.pkgTranslator = pkgTranslator;
    }
}