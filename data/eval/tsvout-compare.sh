#!/bin/bash
# Compare two output tsv files, showing summarizing questions
# by performance change.

Tred="$(tput setaf 1)"
Tgreen="$(tput setaf 2)"
Tdefault="$(setterm -default)"

fgained="$(mktemp)"
fbetter="$(mktemp)"
fworse="$(mktemp)"
flost="$(mktemp)"

# FIXME: The join here will not work on completely missing questions.
join -t $'\t' "$@" |
	while IFS=$'\t' read id t0 q0 s0 g0 a0 c00 c10 c20 c30 c40 t1 q1 s1 g1 a1 c01 c11 c21 c31 c41; do
		line=$(echo -e "$id\t$q0\t$g0\t$s0\t$s1")
		if [ "$(echo "$s1 > $s0" | bc)" = 1 ]; then
			if [ "$(echo "$s1 == 1" | bc)" = 1 ]; then
				line="$Tgreen$line$Tdefault"
			fi
			if [ "$(echo "$s0 == 0" | bc)" = 1 ]; then
				echo "$line" >>"$fgained"
			else
				echo "$line" >>"$fbetter"
			fi
		elif [ "$(echo "$s1 < $s0" | bc)" = 1 ]; then
			if [ "$(echo "$s0 == 1" | bc)" = 1 ]; then
				line="$Tred$line$Tdefault"
			fi
			if [ "$(echo "$s1 == 0" | bc)" = 1 ]; then
				echo "$line" >>"$flost"
			else
				echo "$line" >>"$fworse"
			fi
		fi
	done

echo "------------------- Gained answer to:"
cat "$fgained"
echo
echo "------------------- Improved score for:"
cat "$fbetter"
echo
echo "------------------- Worsened score for:"
cat "$fworse"
echo
echo "------------------- Lost answer to:"
cat "$flost"

rm "$fgained" "$fbetter" "$fworse" "$flost"
