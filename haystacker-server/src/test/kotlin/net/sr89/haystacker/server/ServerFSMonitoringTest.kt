package net.sr89.haystacker.server

internal class ServerFSMonitoringTest {
    /**
     * Start server
     * Create index
     * Add directories to index
     * Remove some of the subdirectories
     *
     * Make one search to see that everything is OK
     *
     * Add, rename, delete.. etc. Files in watched directories.
     *
     * Check now that search results reflect those changes.
     *
     * Restart the server. Check that all still works by making more changes to the files and more searches
     *    - basically making sure that settings were loaded, correct FS watchers were set up
     */
}