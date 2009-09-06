package org.apache.directory.server.core.changelog;

/** A structure telling the changeLog what to do with the incoming change */
public enum LogChange
{
    TRUE,  // The change must me stored 
    FALSE  // The change must not be stred
}