#!/bin/bash

#COAN command to show all directive variables in all C files.
#If You want to put h files. Add: --filter c,h
coan symbols -ge --ifs --locate --recurse --filter c $1 > $2
