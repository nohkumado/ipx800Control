#!/bin/bash
javadoc -sourcepath src/main/java/:$CLASSPATH -d ~/www/nohkumado/ipx800/ipx800-doc com.nohkumado.ipx800control src/main/java/com/nohkumado/ipx800control/*.java
