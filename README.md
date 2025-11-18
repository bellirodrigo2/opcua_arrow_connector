# OPC-UA Arrow Converter

A Java library for reading data from OPC-UA servers and converting it to Apache Arrow IPC format. This library provides a high-performance, type-safe way to collect industrial data and convert it to a columnar format suitable for analytics.

## Features

- **OPC-UA Client with Eclipse Milo**: Full-featured OPC-UA client implementation
- **Auto-reconnect & Keep-alive**: Maintains persistent connections with automatic recovery
- **Retry Policy with Resilience4j**: Configurable retry mechanisms for resilient data collection
- **Apache Arrow IPC Format**: Efficient columnar data format for analytics
- **Type-safe Generic API**: Support for different scalar types (Double, Float, Boolean, String, Integer, Long)
- **Flexible Configuration**: Extensive configuration options for both OPC-UA client and retry policies
- **ID Lookup Support**: Optional mapping of string node IDs to integers for optimized storage

## Architecture

The library follows a modular architecture with clear separation of concerns:

```
┌─────────────────────┐
│ OPCUAArrowCollector │  Main collector coordinating data flow
└──────────┬──────────┘
           │
    ┌──────┴──────┐
    │             │
┌───▼──────┐  ┌──▼──────────┐
│IOPCUAClient│  │IArrowAdapter│  Interfaces for extensibility
└───┬──────┘  └──┬──────────┘
    │            │
┌───▼──────────┐ ▼
│MiloOPCUAClient│ ArrowAdapter   Implementations
└───┬──────────┘
    │
┌───▼─────────┐
│IRetryPolicy │  Retry mechanism
└─────────────┘
```

## Dependencies

- Java 11+
- Eclipse Milo 0.6.11
- Apache Arrow 14.0.0
- Resilience4j 2.1.0
- SLF4J/Logback for logging

## Installation

Build the project using Maven:

```bash
mvn clean install
```

## Core Components

### 1. OPCUAArrowCollector
The main collector that orchestrates reading from OPC-UA and converting to Arrow format.

### 2. IOPCUAClient Interface
Defines the contract for OPC-UA client implementations:
- Asynchronous read operations
- Connection management
- Keep-alive functionality

### 3. IArrowAdapter Interface
Defines the contract for Arrow format conversion:
- Schema generation based on value types
- Efficient batch conversion to IPC format

### 4. IRetryPolicy Interface
Abstraction for retry mechanisms:
- Configurable retry attempts
- Exponential backoff with jitter
- Integration with Resilience4j

### 5. Configuration Classes

#### OPCUAClientConfig
- `serverUrl`: OPC-UA server endpoint
- `username`/`password`: Authentication credentials
- `requestTimeout`: Timeout for individual requests
- `sessionTimeout`: OPC-UA session timeout
- `keepAliveInterval`: Interval for keep-alive pings
- `additionalProperties`: Map for custom properties

#### RetryPolicyConfig
- `maxAttempts`: Maximum retry attempts
- `initialDelay`: Initial delay between retries
- `maxDelay`: Maximum delay between retries
- `backoffMultiplier`: Exponential backoff multiplier
- `useJitter`: Enable/disable jitter for retry delays

## Implementation Details

### MiloOPCUAClient
- Implements auto-reconnection logic
- Maintains session with keep-alive mechanism
- Handles connection failures gracefully
- Converts OPC-UA DataValues to generic OPCUAValue objects

### ArrowAdapter
- Creates Arrow schemas dynamically based on value types
- Efficient batch processing using Arrow vectors
- Supports nullable values and status codes
- Handles ID lookup for optimized storage

### Resilience4jRetryPolicy
- Full jitter implementation for distributed systems
- Configurable exponential backoff
- Event listeners for monitoring retry attempts

## Arrow Schema

The generated Arrow schema contains the following fields:

| Field | Type | Description |
|-------|------|-------------|
| pointid | int32 or string | Node identifier (int32 if idLookup provided) |
| timestamp | timestamp[ns, tz=UTC] | Source or server timestamp |
| value | varies | Scalar value (type based on valueType parameter) |
| statuscode | int32 | OPC-UA status code |

## Thread Safety

- OPCUAArrowCollector: Thread-safe after connection
- MiloOPCUAClient: No Thread-safe
- MiloOPCUAClientThreadSafe: Thread-safe with internal synchronization
- ArrowAdapter: Not thread-safe (create per-thread instances)

## Error Handling

- Connection failures trigger automatic reconnection
- Read failures are retried according to retry policy
- Bad status codes are properly handled with null values
- All operations return CompletableFutures for async error handling

## License

Apache License 2.0
