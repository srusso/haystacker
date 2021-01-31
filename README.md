# Haystacker

A file indexer and search software.
Work in progress.

## Building the project

Simply run:

    mvn clean install
    
This will create the CLI client jar.

## Running the server

Simply run `HslServer.kt`.

## Running the client

From a terminal (won't work from IntelliJ):

    java -jar haystacker-shell\target\haystacker-shell-1.0-SNAPSHOT.jar

Then type `help` for help.

## HSL: Haystacker Language

Haystacker supports a simple but powerful language to search for indexed files.

### Searching for files based on name or location

Search for files with the given filename:

    name = "file name with spaces.txt"

Quotes can be omitted if there are no spaces:

    name = filename.txt
    
The key `name` doesn't just look at the filename, but also the full path.
For example, if a file is indexed at `C:\directory\data\archive\file.txt`, this query will match it and all other files in that directory:

    name = archive
    
### Searching for files based on their size

To return all indexed files larger than 100 megabytes:

    size > 100mb
    
The key `size` supports operators `>`, `>=`, `<`, `<=` and `=`.
Supported data size suffixes are none or `b` for bytes, `kb` for kilobytes, `mb` for megabytes, `gb` for gigabytes or `tb` for terabytes.

### Searching for files based on their created or "last modified" date

The keys that can be used are `created` and `last_modified`.

For example, to search for all files that were last modified before January 5th 2021 at 10:00:00 UTC:

    last_modified < '2020-01-05T10:00:00Z'

Note the necessary single quotes surrounding the date.
The above specifies a date-time with UTC offset (`Z`). Offsets can also be specified numerically, for example `2020-01-05T10:00:00+04:00`.
Additionally, it's possible to also just specify the date: `2020-01-05`. Note that this is interpreted as `2020-01-05T00:00:00Z` (start of day, in UTC).

Operators `>`, `>=`, `<`, `<=` and `=` are supported.

### Combining queries

The real power comes with being able to combine queries, using `AND` and `OR`. Note that `AND` has precedence over `OR`, but this can be overridden with parentheses.

Examples:

    last_modified < 2020-01-05T10:00:00Z AND name = "file.txt"
    last_modified < 2020-01-05T10:00:00Z OR name = "file.txt"
    created < '2020-01-05' AND (last_modified > '2020-01-10' OR name = "file.txt")