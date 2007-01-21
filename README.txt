Checking out this folder verses the trunk itself may be preferrable for our
developers.  It will contain all the current dependencies of apacheds so 
eclipse and other tools can see them all after one checkout.

This folder contains externals to the apacheds trunk and the other dependencies
that trunk has.  The branches pointed to for shared and daemon may not always
by the trunk and the externals may or may not be to a writable branch: meaning 
it may point to a release with read-only http instead of read-write https URL 
scheme.

Before you commit, or deploy stuff please make sure you check these externals
so you don't commit to a branch you do not want to or deploy to production
non-snapshot repositories.

