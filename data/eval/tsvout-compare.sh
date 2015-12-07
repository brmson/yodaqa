#!/bin/bash
# Compare two output tsv files, showing summarizing questions
# by performance change.

Tred="$(tput setaf 1)"
Tgreen="$(tput setaf 2)"
Tdefault="$(setterm -default)"

# Score change tolerance: do not show answers whose score changes by this or less
stol=0.05

fgained="$(mktemp)"
fbetter="$(mktemp)"
fworse="$(mktemp)"
flost="$(mktemp)"

showline() {
	id=$1; q0=$2; g0=$3; s0=$4; s1=$5; a0=$6; a1=$7
	line=$(echo -e "$id\t$q0\t$g0\t$s0\t$s1\t$a0\t$a1")
	if [ "$(echo "define abs(i) { if (i < 0) return (-i); return (i) }; $s1 == 1 || $s0 == 1 || abs($s1 - $s0) > $stol" | bc)" != 1 ]; then
		# Ignore this answer
		return
	fi
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

evaldir=$(dirname "$0")/tsv
file1="$1"; case "$file1" in *.tsv) ;; *) file1="$(echo "$evaldir/"*"-${file1}.tsv")";; esac
file2="$2"; case "$file2" in *.tsv) ;; *) file2="$(echo "$evaldir/"*"-${file2}.tsv")";; esac
set "$file1" "$file2"

case "$1" in *ovt*) gcol0=7; acol0=13;; *) gcol0=5; acol0=11;; esac
case "$2" in *ovt*) gcol1=7; acol1=13;; *) gcol1=5; acol1=11;; esac

join -t $'\t' -o 1.1,1.3,1.$gcol0,1.$acol0,2.$acol1,1.2,2.2,1.4,2.4 "$@" |
	while IFS=$'\t' read id q g a0 a1 t0 t1 s0 s1; do
		showline "$id" "$q" "$g" "$s0" "$s1" "${a0%:*}" "${a1%:*}"
	done

join -t $'\t' -o 1.1,1.3,1.$gcol0,1.$acol0,2.$acol1,1.2,1.4 -v 1 "$@" |
	while IFS=$'\t' read id q g a0 t0 s0; do
		showline "$id" "$q" "$g" "$s0" "0.0" "${a0%:*}"
	done
join -t $'\t' -o 2.1,2.3,2.$gcol1,1.$acol0,2.$acol1,2.2,2.4 -v 2 "$@" |
	while IFS=$'\t' read id q g a1 t1 s1; do
		showline "$id" "$q" "$g" "0.0" "$s1" "${a1%:*}"
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
