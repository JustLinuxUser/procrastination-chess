#/usr/bin/bash
#
OLD=1464
LOC=$(cloc . | grep Java | grep -v JavaScript| awk -F" " '{print $5}')
echo $(($LOC - $OLD))
