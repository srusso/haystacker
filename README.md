# Haystacker

A file indexer and search software.

https://user-images.githubusercontent.com/1757629/122649447-369ea700-d12e-11eb-9d12-f06e60a4170f.mp4

## Building the project

Simply run:

    mvn clean install
    
This will create the CLI client jar.

## Running the server

Simply run `HaystackerApplication.kt`, optionally specifying the port by using the `--port` command line argument, such as `--port 9000`.

## Running the UI client

Download the javafx library from [here](https://gluonhq.com/products/javafx/) and extract it.
Let's say it's extracted to `D:\tools\javafx-sdk-11.0.2\lib`.

Run: `net.sr89.haystacker.ui.UIStart`
With VM options: `--module-path D:\tools\javafx-sdk-11.0.2\lib --add-modules=javafx.controls`

## Running the shell client

Running an interactive shell from the terminal (won't work from IntelliJ):

    java -jar haystacker-shell\target\haystacker-shell-1.0-SNAPSHOT.jar --port 9000 --host localhost
    
The shell can also be run in "bash style":

    java -jar haystacker-shell\target\haystacker-shell-1.0-SNAPSHOT.jar --port 9000 --host localhost create-index D:\my-index
    java -jar haystacker-shell\target\haystacker-shell-1.0-SNAPSHOT.jar --port 9000 --host localhost --index D:\my-index add-to-index D:\my-data
    java -jar haystacker-shell\target\haystacker-shell-1.0-SNAPSHOT.jar --port 9000 --host localhost --index D:\my-index search "name = \"file.txt\"" --max-results 10

Note that in bash style quotes need to be escaped as shown in the last example above.

## Using the shell client (interactive mode)

Type `help` for help. Example usage:

    ; create an empty index at C:\\my-index on the server
    haystacker> create-index C:\\my-index
    
    ; set the current index to C:\\my-index (will be used by future commands)
    haystacker> set-index C:\\my-index
    
    ; adds directory D:\\my-data to the index, to make it searchable
    haystacker> add-to-index D:\\my-data
    
    ; searches the index using the provided HSL (Haystacker Search Language) query
    haystacker> search "name = myfile.txt"
    Total results: 1
    Returned results: 1
    Items:
    Path: D:\my-data\myfile.txt
    Took: 23 ms

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

Note that the single quotes around the value are allowed but not necessary.
The above specifies a date-time with UTC offset (`Z`). Offsets can also be specified numerically, for example `2020-01-05T10:00:00+04:00`.
Additionally, it's possible to also just specify the date: `2020-01-05`. Note that this is interpreted as `2020-01-05T00:00:00Z` (start of day, in UTC).

Operators `>`, `>=`, `<`, `<=` and `=` are supported.

### Combining queries

The real power comes with being able to combine queries, using `AND` and `OR`. Note that `AND` has precedence over `OR`, but this can be overridden with parentheses.

Examples:

    last_modified < 2020-01-05T10:00:00Z AND name = "file.txt"
    last_modified < 2020-01-05T10:00:00Z OR name = "file.txt"
    created < '2020-01-05' AND (last_modified > '2020-01-10' OR name = "file.txt")
    
### Sorting results

One or more optional `order by` clauses can be used to sort results.

Examples:

    name = archive AND size > 100mb order by created desc, last_modified desc
    name = archive AND size > 100mb order by size desc
    
## Keeping the index updated

Haystacker uses JNA's [FileMonitor](https://github.com/java-native-access/jna/blob/master/www/PlatformLibrary.md) to receive notifications from the operating system about changes to the file system.
It is possible for notifications to be lost in case many changes are performed in a short time, due to the operating system's own buffer overflowing, or due to other reasons.

To update the index manually in such case, this shell command can be used:

    haystacker> add-to-index D:\\my-data
    
This way, the contents of `D:\\my-data` will be re-indexed.
