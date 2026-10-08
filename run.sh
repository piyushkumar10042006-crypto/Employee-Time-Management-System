#!/bin/bash

# =========================================================
# Employee Time Management System - Run Script (macOS M1)
# =========================================================

# Resolve Java & Javac from Homebrew or System PATH
if [ -x "/opt/homebrew/opt/openjdk/bin/java" ]; then
    JAVA_BIN="/opt/homebrew/opt/openjdk/bin/java"
    JAVAC_BIN="/opt/homebrew/opt/openjdk/bin/javac"
else
    JAVA_BIN="java"
    JAVAC_BIN="javac"
fi

echo "========================================================="
echo "  Starting Employee Time Management System (Review 1)    "
echo "========================================================="
echo "Using Java: $($JAVA_BIN --version | head -n 1)"

# Create output directory
mkdir -p bin

# Copy resources (properties file) into classpath
cp -r src/main/resources/* bin/ 2>/dev/null || true

# Compile source files
echo "Compiling Java source files..."
$JAVAC_BIN -d bin -cp "lib/*:src/main/resources" $(find src/main/java -name "*.java")

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed."
    exit 1
fi

echo "✓ Compilation successful."
echo "Launching GUI Application..."

# Run the Main class with classpath
$JAVA_BIN -cp "bin:lib/*" com.timemanagement.Main
