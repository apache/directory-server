/*
 *   @(#) $Id$
 *
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
package org.apache.directory.shared.ldap.aci;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * Defines a set of zero or more users the permissions apply to.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public abstract class UserClass implements Serializable
{
    /**
     * Every directory user (with possible requirements for
     * authenticationLevel).
     */
    public static final AllUsers ALL_USERS = new AllUsers();

    /**
     * The user with the same distinguished name as the entry being accessed, or
     * if the entry is a member of a family, then additionally the user with the
     * distinguished name of the ancestor.
     */
    public static final ThisEntry THIS_ENTRY = new ThisEntry();


    /**
     * Creates a new instance.
     */
    protected UserClass()
    {
    }

    /**
     * Every directory user (with possible requirements for
     * authenticationLevel).
     */
    public static class AllUsers extends UserClass
    {
        private static final long serialVersionUID = 8967984720792510292L;


        private AllUsers()
        {
        }


        public String toString()
        {
            return "allUsers";
        }
    }

    /**
     * The user with the same distinguished name as the entry being accessed, or
     * if the entry is a member of a family, then additionally the user with the
     * distinguished name of the ancestor.
     */
    public static class ThisEntry extends UserClass
    {
        private static final long serialVersionUID = -8189325270233754470L;


        private ThisEntry()
        {
        }


        public String toString()
        {
            return "thisEntry";
        }
    }

    /**
     * A base class for all user classes which has a set of DNs.
     */
    private static abstract class NamedUserClass extends UserClass
    {
        protected final Set names;


        /**
         * Creates a new instance.
         * 
         * @param names
         *            a set of names
         */
        protected NamedUserClass(Set names)
        {
            for ( Iterator i = names.iterator(); i.hasNext(); )
            {
                Object val = i.next();
                if ( !( val instanceof javax.naming.Name ) )
                {
                    throw new IllegalArgumentException( "names contains a wrong element." );
                }
            }
            this.names = Collections.unmodifiableSet( new HashSet( names ) );
        }


        /**
         * Returns the set of all names.
         */
        public Set getNames()
        {
            return names;
        }


        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o == null )
            {
                return false;
            }

            if ( getClass().isAssignableFrom( o.getClass() ) )
            {
                Name that = ( Name ) o;
                return this.names.equals( that.names );
            }

            return false;
        }


        public String toString()
        {
            return names.toString();
        }
    }

    /**
     * The user with the specified distinguished name.
     */
    public static class Name extends NamedUserClass
    {
        private static final long serialVersionUID = -4168412030168359882L;


        /**
         * Creates a new instance.
         * 
         * @param usernames
         *            the set of user DNs.
         */
        public Name(Set usernames)
        {
            super( usernames );
        }


        public String toString()
        {
            return "name: " + super.toString();
        }
    }

    /**
     * The set of users who are members of the groupOfUniqueNames entry,
     * identified by the specified distinguished name. Members of a group of
     * unique names are treated as individual object names, and not as the names
     * of other groups of unique names.
     */
    public static class UserGroup extends NamedUserClass
    {
        private static final long serialVersionUID = 8887107815072965807L;


        /**
         * Creates a new instance.
         * 
         * @param groupNames
         *            the set of group DNs.
         */
        public UserGroup(Set groupNames)
        {
            super( groupNames );
        }


        public String toString()
        {
            return "userGroup: " + super.toString();
        }
    }

    /**
     * The set of users whose distinguished names fall within the definition of
     * the (unrefined) subtree.
     */
    public static class Subtree extends UserClass
    {
        private static final long serialVersionUID = 3949337699049701332L;

        protected final Collection subtreeSpecifications;


        /**
         * Creates a new instance.
         * 
         * @param subtreeSpecs
         *            the collection of unrefined {@link SubtreeSpecification}s.
         */
        public Subtree(Collection subtreeSpecs)
        {
            for ( Iterator i = subtreeSpecs.iterator(); i.hasNext(); )
            {
                Object val = i.next();
                if ( !( val instanceof SubtreeSpecification ) )
                {
                    throw new IllegalArgumentException( "subtreeSpecs contains a wrong element." );
                }
            }
            this.subtreeSpecifications = Collections.unmodifiableCollection( new ArrayList( subtreeSpecs ) );
        }


        /**
         * Returns the collection of unrefined {@link SubtreeSpecification}s.
         */
        public Collection getSubtreeSpecifications()
        {
            return subtreeSpecifications;
        }


        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o instanceof Subtree )
            {
                Subtree that = ( Subtree ) o;
                return this.subtreeSpecifications.equals( that.subtreeSpecifications );
            }

            return false;
        }


        public String toString()
        {
            return "subtree: " + subtreeSpecifications;
        }
    }
}
