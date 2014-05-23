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

showline() {
	id=$1; q0=$2; g0=$3; s0=$4; s1=$5
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
}

case "$1" in *ovt*) gcol0=7;; *) gcol0=5;; esac
case "$2" in *ovt*) gcol1=7;; *) gcol1=5;; esac

join -t $'\t' -o 1.1,1.3,1.$gcol0,1.2,2.2,1.4,2.4 "$@" |
	while IFS=$'\t' read id q g t0 t1 s0 s1; do
		showline "$id" "$q" "$g" "$s0" "$s1"
	done

join -t $'\t' -o 1.1,1.3,1.$gcol0,1.2,1.4 -v 1 "$@" |
	while IFS=$'\t' read id q g t0 s0; do
		showline "$id" "$q" "$g" "$s0" "0.0"
	done
join -t $'\t' -o 2.1,2.3,2.$gcol1,2.2,2.4 -v 2 "$@" |
	while IFS=$'\t' read id q g t1 s1; do
		showline "$id" "$q" "$g" "0.0" "$s1"
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
