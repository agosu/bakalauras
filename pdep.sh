#CREDIT https://gist.github.com/robertbraeutigam/6cca2e021bed38df7e5510db5eec4be0
#!/bin/sh

cat >dependencies.dot <<EOF
digraph g {
graph [
    rankdir = "LR" 
];   
node [
    fontsize = "12" 
    fontname = "Courier"
    shape = "ellipse"
];   
EOF

PACKAGE=`pwd | sed "s/.*src\/main\/java\/\(.*\)/\1/" | sed "s/\//./g"`
for i in `ls | grep -v \.java`
do
    grep -roh "import $PACKAGE.[a-z][^.]*" $i | sort| uniq | grep -v static | sed "s/import $PACKAGE.\([a-z][^.]*\).*/\1/" | grep -v $i | sed "s/\(.*\)/\"$i\" -> \"\1\"/";
    if grep -roh "import $PACKAGE.[A-Z][^.]*" $i >/dev/null
    then
        echo "\"$i\" -> \"<root>\"" >>dependencies.dot
    fi
done >>dependencies.dot
grep -oh "import $PACKAGE.[a-z][^.]*" *java 2>/dev/null| sort| uniq | grep -v static | sed "s/import $PACKAGE.\([a-z][^.]*\).*/\1/" | sed "s/\(.*\)/\"<root>\" -> \"\1\"/" >>dependencies.dot;

cat >>dependencies.dot <<EOF
}
EOF