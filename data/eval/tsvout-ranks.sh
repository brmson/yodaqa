#!/bin/bash
#
# List questions with correct answers considered, ordered by the correct
# answer rank and numbered so that we can easily find the median etc.

cut -f1,3,5,7,9 "$1" | sort -t $'\t' -k3 -n | grep -v '[-]1' | nl | less -F
