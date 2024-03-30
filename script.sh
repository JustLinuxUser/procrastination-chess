#!/usr/bin/env bash
java -XX:+PreserveFramePointer -cp /home/andriy/Programming/langs/cava/chess_from_scratch/app/build/libs/app.jar org.example.App "$1" "$2" "$3"
