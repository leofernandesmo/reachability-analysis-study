#!/bin/bash

#CTags command to show all functions in all C files.
# .. if wnats to get header file use "*.[ch]" on find command
cd $1
find . -type f -iname "*.c" | xargs ctags -x -R --sort=yes --c-kinds=f --language-force=c > $2
