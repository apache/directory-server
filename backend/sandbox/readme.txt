Second test area or sandbox for ldapd experimentation.

Will be using this area for trying out Steven McConnell's
layout with projects for api, impl and spi.

To build this stuff just do a the following:

1). Checkout ldapd-common somewhere and do a maven jar:install
    which copies the newly build ldapd-common-SNAPSHOT.jar into
    your local maven repository at $user.home/.maven/repository/
    ldapd-common/jars
2). Go into this directory where this readme.txt is and do a
    'maven build-all'.  This will invoke the reactor to do a
    'maven jar:install' for each subproject taking dependencies
    between projects into account.  All the snapshot artifacts
    are installed into the local repository.

Eventually I need to put everything under one directory including 
the common stuff but I'm scared of multiple layers of POM extention
in maven.
