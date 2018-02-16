#!/bin/bash

file_path=$1
out_dir=$2
train_size=$3
val_size=$4


filename=$(basename "$file_path")
extension="${filename##*.}"
filename="${filename%.*}"

if [[ ! -z "${val_size}" ]]
then
	head -${train_size} ${file_path} > ${out_dir}/$filename-train.$extension
	tail -n +$((train_size + 1)) ${file_path} | head -${val_size} > ${out_dir}/$filename-val.$extension
	tail -n +$((train_size + val_size + 1)) ${file_path} >  ${out_dir}/$filename-test.$extension
else
	head -${train_size} ${file_path} > ${out_dir}/$filename-train.$extension
	tail -n +$((train_size + 1)) ${file_path} >  ${out_dir}/$filename-test.$extension
fi
