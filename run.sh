#!/bin/bash

# OPC UA Arrow Connector Startup Script
set -e

echo "Starting OPC UA Arrow Connector..."

# Load environment variables if .env file exists
if [ -f .env ]; then
    echo "Loading environment variables from .env"
    export $(cat .env | grep -v '^#' | xargs)
fi

# Set default values if not provided
export DB_JDBC_URL=${DB_JDBC_URL:-"jdbc:postgresql://localhost:5432/opcua_arrow"}
export DB_USERNAME=${DB_USERNAME:-"postgres"}
export DB_PASSWORD=${DB_PASSWORD:-"password"}
export SOURCE_NAME=${SOURCE_NAME:-"default_source"}
export HOT_RELOAD_INTERVAL_SECONDS=${HOT_RELOAD_INTERVAL_SECONDS:-"30"}
export ENABLE_HOT_RELOAD=${ENABLE_HOT_RELOAD:-"true"}

# Build if needed
if [ ! -d "target/classes" ]; then
    echo "Building project..."
    mvn compile dependency:copy-dependencies
fi

# Java runtime options
JAVA_OPTS="${JAVA_OPTS:-"-Xmx512m -Xms256m"}"

# Classpath
CLASSPATH="target/classes:target/dependency/*"

# Main class
MAIN_CLASS="com.opcua_arrow.di.ApplicationBootstrap"

echo "Configuration:"
echo "  Database URL: $DB_JDBC_URL"
echo "  Database User: $DB_USERNAME"
echo "  Source Name: $SOURCE_NAME"
echo "  Hot Reload: $ENABLE_HOT_RELOAD (${HOT_RELOAD_INTERVAL_SECONDS}s interval)"
echo ""

echo "Starting application..."
exec java $JAVA_OPTS -cp "$CLASSPATH" "$MAIN_CLASS" "$@"