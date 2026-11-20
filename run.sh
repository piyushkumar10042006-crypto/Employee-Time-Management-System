
#!/bin/bash

cd "$(dirname "$0")"

echo "=============================================="
echo " Employee Time Management System - Review 2"
echo "=============================================="

if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven is not installed or not available."
    exit 1
fi

echo "Building project..."
mvn clean compile dependency:build-classpath \
    -Dmdep.outputFile=target/classpath.txt

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Starting application..."

java -cp "target/classes:$(cat target/classpath.txt)" \
    com.timemanagement.Main
