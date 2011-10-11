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
package jdbm.helper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * This is a helper class to keep track of versions assigned to actions. As client begin
 * read only and read write actions, they call into this class and get the version they
 * can use. As the clients end their actions, minimum read version any action is using is
 * advanced and piggybacked to the client
 * 
 * This class assumes that there is one readWrite action at a time. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ActionVersioning
{
    /** Current write version */
    private Version nextVersion;
    
    /** Current read version reference */
    private AtomicReference<Version> readReference;
    
    /** List to put versions on */
    private ExplicitList<Version> versions = new ExplicitList<Version>();
    
    /** Lock to protect the list */
    private Lock listLock = new ReentrantLock();
    
    
    public ActionVersioning()
    {
        Version readVersion = new Version( 0 );
        nextVersion = new Version( 1 ); 
        readReference = new AtomicReference<Version>( readVersion );

        versions.addFirst( nextVersion.getVersionsLink() );
        versions.addFirst( readVersion.getVersionsLink() );
    }
    
    
    /**
     * Returns back the new version to be used with the read/write action.
     * Assume one read/write action at a time.
     *
     * @return new version for the action system
     */
    public Version beginWriteAction()
    {
        return nextVersion;
    }
    
    
    /**
     * Called when the read/write action completes. Advances the version of action subsystem 
     * and publishes a new version for the readers. Assume one read/write action at a time.
     *
     * @return minimum read version for the action subsystem
     */
    public Version endWriteAction()
    {
        Version minVersion;
        
        // Allocate the new nextVersion
        Version newNextVersion = new Version( nextVersion.getVersion() + 1 );
        
        // Post the commited version as the new read version
        Version oldReadVersion = readReference.getAndSet( nextVersion );
        
        // add the newnextversion to the versions list
        listLock.lock();
        versions.addLast( newNextVersion.getVersionsLink() );
        
        if ( ( oldReadVersion.getNumActions().get() == 0 ) && 
            oldReadVersion.getVersionsLink().isLinked() )
        {
            versions.remove( oldReadVersion.getVersionsLink() );
            oldReadVersion.getVersionsLink().uninit();
        }
        
        minVersion = versions.begin().getElement();
        listLock.unlock();
        
        nextVersion = newNextVersion;
        return minVersion;
    }
    
    
    /**
     * Returns a version that can be used by the read only action
     *
     * @return version to be used by the action
     */
    public Version beginReadAction()
    {
        Version readVersion = readReference.get();
        
        readVersion.getNumActions().incrementAndGet();
        
        /*
         * If the write txn just finished and published
         * a new version to read, check if we can still
         * use our version for reading
         */
        if ( readVersion != readReference.get() )
        {
            listLock.lock();
            
            if ( readVersion.getVersionsLink().isUnLinked() )
            {
                readVersion = readReference.get();
                readVersion.getNumActions().incrementAndGet();
            }
            
            listLock.unlock();
        }
        
        return readVersion;
    }
    
    
    /**
     * Called when the read action with the given action is ended.
     * Checks whether the minimum read version advanced
     *
     * @param version version of the read only action
     * @return returns the miminum read version. Might return null if read version did not 
     * advance for sure.
     */
    public Version endReadAction( Version version )
    {
        long numActions = version.getNumActions().decrementAndGet();
        
        if ( numActions < 0 )
        {
            throw new IllegalStateException( "NumActions zero when read action is ended : " + version );
        }

        
        if ( ( numActions > 0 ) || ( version == readReference.get() ) )
        {
            // minimum read version did not change for sure
            return null;
        }
        
        Version minVersion = null;
        listLock.lock();
        
        if ( ( version.getNumActions().get() == 0 ) && 
            version.getVersionsLink().isLinked() )
        {
            version.getVersionsLink().remove();
            version.getVersionsLink().uninit();
        }
        
        minVersion = versions.begin().getElement();
        listLock.unlock();
        
        return minVersion;
    }
    
    
    public static class Version
    {
        /** Represented version */
        private long version;
        
        /** Used to put on versions chain */
        private ExplicitList.Link<Version> versionsLink;
        
        /** Number of txns running with this version */
        private  AtomicInteger numActions;
        
        
        public Version ( long version )
        {
            this.version = version;
            
            versionsLink = new ExplicitList.Link<ActionVersioning.Version>( this );
            
            numActions = new AtomicInteger( 0 );
        }
        
        
        private ExplicitList.Link<Version> getVersionsLink()
        {
            return versionsLink;
        }
        
        
        private AtomicInteger getNumActions()
        {
            return numActions;
        }
        
        
        public long getVersion()
        {
            return version;
        }
        
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Version: ");
            sb.append( "(vesion: " ).append( version );
            sb.append( ", numActions: " ).append( numActions );
            sb.append( ")\n" );
            
            return sb.toString();
        }
    }
}