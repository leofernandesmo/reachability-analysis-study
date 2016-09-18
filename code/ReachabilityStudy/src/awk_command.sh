#!/bin/bash
awk 'NR > first && /^}$/ { print NR; exit }' first=$1 $2
#1329
#/home/ubuntu-vm/workspace/libssh-0.7.2-temp/src/sftp.c
