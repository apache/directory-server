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
package org.apache.eve.db;


import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Comparator;


/**
 * A Serializable Comparator used to wrap a non-Serializable Comparator.  Uses a
 * static ThreadLocal to temporarily associate a non-Serializable Comparable 
 * with a Thread of execution.  When the object deserializes its transient 
 * wrapped non-Serializable Comparator is set using the one mapped into the
 * ThreadLocal.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SerializableComparator implements Comparator, Serializable
{
    /** Used to access underlying transient comparator during deserialization */
    private static ThreadLocal s_comparators = new ThreadLocal();

    /** The transient non-serializable comparator. */
    private transient Comparator nonSerializable;
    
    
    // ------------------------------------------------------------------------
    // S T A T I C   M E T H O D S
    // ------------------------------------------------------------------------


    /**
     * Associates a Comparator with the current executing thread.
     * 
     * @param comparator the comparator to associate
     */
    public static void set( Comparator comparator )
    {
        if ( comparator == null )
        {
            return;
        }
        
        // Check first that comparator is not serializable
        if ( comparator instanceof Serializable )
        {
            throw new IllegalArgumentException( "Expecting non-Serializable " 
                + "argument Comparator but got Serializable Comparator." );
        }
        
        s_comparators.set( comparator );
        return;
    }


    /**
     * Disassociates a Comparator with the current executing thread whether one
     * is already associated or not.
     */
    public static void unset()
    {
        s_comparators.set( null );
    }


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a Serializable comparator used to wrap a non Serializable 
     * comparator.
     * 
     * @param nonSerializable the non Serializable Comparator wrapped
     */    
    public SerializableComparator( Comparator nonSerializable )
    {
        this.nonSerializable = nonSerializable;
    }
    
    
    // ------------------------------------------------------------------------
    // P R O P E R T Y   A C C E S S O R   M U T A T O R S
    // ------------------------------------------------------------------------


    /**
     * Gets the non-Serializable Comparator wrapped by this Comparator.
     * 
     * @return the non-Serializable Comparator
     */
    public Comparator getNonSerializable()
    {
        return nonSerializable;
    }


    /**
     * Sets the non-Serializable Comparator wrapped by this Comparator.
     * 
     * @param nonSerializable the non Serializable Comparator wrapped
     */
    public void setNonSerializable( Comparator nonSerializable )
    {
        // Check first that comparator is not serializable
        if ( nonSerializable instanceof Serializable )
        {
            throw new IllegalArgumentException( "Expecting non-Serializable " 
                + "argument Comparator but got Serializable Comparator." );
        }
        
        this.nonSerializable = nonSerializable;
    }


    // ------------------------------------------------------------------------
    // C O M P A R A T O R   I M P L E M E N T A T I O N S 
    // ------------------------------------------------------------------------


    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Object obj1, Object obj2 )
    {
        return nonSerializable.compare( obj1, obj2 );
    }


    // ------------------------------------------------------------------------
    // S E R I A L I Z A T I O N   O V E R R I D E S
    // ------------------------------------------------------------------------


    /**
     * Writes out the object using default serialization.
     * 
     * @param out the stream for serialization
     * @throws IOException if there are any failures in writing serialized obj
     */
    private void writeObject( ObjectOutputStream out )
        throws IOException
    {
        out.defaultWriteObject();
    }


    /**
     * Reads the object from an input stream using default serialization.
     * 
     * @param in the stream to deserialize from
     * @throws IOException if there are any failures in reading serialized obj
     * @throws ClassNotFoundException if serialized object cannot be found
     */
    private void readObject( ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        // Initialize
        nonSerializable = ( Comparator ) s_comparators.get();
        
        // Choke if we cannot find a Comparator 
        if ( null == nonSerializable ) 
        {
            throw new IOException( "Could not set transient nonserializable " 
                + "comparator - comparator must have not been set for this " 
                + "thread." );
        }
        
        // Clear this threads Comparator
        s_comparators.set( null );
    }
}
