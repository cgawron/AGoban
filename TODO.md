Open
=============
- Improve metadata sync for local files
- Improve Google Sync (choose folder, create folder, add resource id to provider db)
- Improve GameTreeControl
- Improve game info activity
- Optimze loading of sgf files (memory footprint). Goal: Load Kodo's joseki library

Done
=============
- Remove stack issue while saving sgf file (i.e. remove recursion).
- SGFProvider: Delete non-existing files from DB
- Improve support for google documents
- Improve markup rendering
- Bug: setCharSet() is not thread save: A connection between node and lexer is needed, or lexer needs to handle this
