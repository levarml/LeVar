export JAVA_HOME=$(/usr/libexec/java_home -v1.8)
export PATH=$JAVA_HOME/bin:$PATH
export JVM_OPTS="-XX:MaxMetaspaceSize=384m -Xms512m -Xmx1536m -Xss2m -XX:ReservedCodeCacheSize=256m -XX:+TieredCompilation -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"
