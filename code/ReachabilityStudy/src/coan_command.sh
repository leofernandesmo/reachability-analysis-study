#!/bin/bash

#COAN command to show all directive variables in all C and H files.
coan symbols -ge --ifs --locate --recurse --filter c,h $1 > $2
