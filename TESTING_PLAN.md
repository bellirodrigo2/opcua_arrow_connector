# OPC-UA Arrow Connector - Testing Plan

## Overview

This document outlines a comprehensive testing strategy for the OPC-UA Arrow Connector, which reads data from OPC-UA servers, transforms it into Apache Arrow format, and manages data points configured in PostgreSQL.

## System Architecture Summary

The connector consists of several layers:

1. **OPC-UA Layer**: Connection management and data reading (polling/subscription modes)
2. **Data Transformation Layer**: Converting OPC-UA DataValue to TSValue
3. **Buffer Layer**: Batching data into Apache Arrow format
4. **Queue Layer**: Thread-safe queues for data flow
5. **Configuration Layer**: PostgreSQL-based configuration management
6. **Orchestration Layer**: Context and lifecycle management

## Testing Strategy

### 1. Unit Tests

Unit tests should focus on individual components in isolation, using mocks for dependencies.

#### 1.1 Data Model Tests (`com.opcua_arrow.data`)

**Test Class**: `TSValueTest`
- Test TSValue creation with valid values
- Test consistency check (timestamp >= 0)
- Test immutability of TSValue fields
- Test edge cases (null values, negative timestamps)

**Test Class**: `DataPointTest`
- Test data point parameter creation and validation
- Test equality and hashCode for proper map/set usage
- Test different read modes (polling vs subscription)
- Test filter configurations (NoFilter, StrictEqual, RangeEqual)

**Test Class**: `IntRangeTest`
- Test range validation
- Test boundary conditions
- Test invalid ranges (min > max)

#### 1.2 OPC-UA Reader Tests (`com.opcua_arrow.opcua.milo`)

**Test Class**: `MiloOPCUAReaderTest`
- Test lock-free node list management (add/remove/set operations)
- Test atomic snapshot updates
- Test concurrent modifications
- Test read operation with mock OpcUaClient
- Test error handling when client is not connected
- Test retry policy integration
- Mock: `IOPCUAConnection`, `IRetryPolicy`, `OpcUaClient`

**Test Class**: `MiloOPCUASubscriptionReaderTest`
- Test subscription creation and lifecycle
- Test monitored item management
- Test notification listener callback
- Test concurrent data point additions/removals during active subscription
- Test subscription updates when data points change
- Test error handling for failed monitored items
- Mock: `IOPCUAConnection`, `OpcUaClient`, `UaSubscription`

**Test Class**: `TSValueFactoryTest`
- Test creation of TSValue from OPC-UA DataValue
- Test handling of different data types (Boolean, Integer, Double, String)
- Test quality code handling (Good, Bad, Uncertain)
- Test timestamp extraction (server time, source time)
- Test null and invalid value handling

**Test Class**: `MiloOPCUAConnectionTest`
- Test connection establishment
- Test connection state management (connected/disconnected)
- Test thread-safe client access with ReadWriteLock
- Test concurrent connection attempts
- Test disconnect during active operations
- Test keep-alive mechanism (mock scheduled executor)
- Test reconnection on connection loss
- Test session activity listener callbacks
- Mock: `DiscoveryClient`, endpoint discovery

#### 1.3 Arrow Buffer Builder Tests (`com.opcua_arrow.batch_builder.arrow`)

**Test Class**: `AcumBatchArrowBuilderTest`
- Test batch accumulation logic
- Test time-based flush (minFlushIntervalNanos)
- Test size-based flush (minBatchSize)
- Test that flush returns null when conditions not met
- Test multiple flush cycles

**Test Class**: `BaseArrowBufferBuilderTest`
- Test Arrow schema creation
- Test data appending for each data type column
- Test batch flush and buffer reset
- Test compression (if compress=true)
- Test buffer capacity management
- Test memory allocation and deallocation

**Test Class**: `ColumnTests` (BooleanValueColumn, DoubleValueColumn, IntegerValueColumn, StringValueColumn)
- Test value writing to Arrow vectors
- Test null value handling
- Test type conversion and validation

#### 1.4 Queue Tests (`com.opcua_arrow.queues`)

**Test Class**: `QueueWrapperTest`
- Test push operation (blocking behavior)
- Test pop operation with timeout
- Test batch draining
- Test queue capacity limits
- Test interrupt handling
- Test concurrent push/pop operations
- Test edge case: pop on empty queue returns empty batch after timeout

#### 1.5 Retry Policy Tests (`com.opcua_arrow.retry`)

**Test Class**: `Resilience4jRetryPolicyTest`
- Test successful execution without retries
- Test retry on transient failures
- Test maximum retry attempts
- Test exponential backoff configuration
- Test failure after all retries exhausted
- Test different exception types (retryable vs non-retryable)

#### 1.6 Configuration Tests (`com.opcua_arrow.config`)

**Test Class**: `OPCUAClientConfigTest`
- Test configuration validation
- Test default values
- Test timeout configurations

**Test Class**: `RetryPolicyConfigTest`
- Test retry configuration parsing
- Test default retry parameters

#### 1.7 Read/Write Component Tests

**Test Class**: `ReadTaskTest`
- Test scheduled execution at fixed intervals
- Test start/stop lifecycle
- Test data point management delegation
- Test error handling in scheduled tasks
- Test executor shutdown on stop
- Mock: `IOPCUAReader`, `IQueue`

**Test Class**: `LoopReaderTest`
- Test data point addition/removal routing to correct ReadTask
- Test ReadTask creation through registry
- Test start/stop propagation to all tasks
- Test cleanup when ReadTask becomes empty
- Mock: `ReadTaskRegistry`

**Test Class**: `LoopWriterTest`
- Test data grouping by DataWriteGroup
- Test buffer lookup from registry
- Test batch flushing and sink pushing
- Test reusable collections optimization
- Test interruption handling
- Mock: `IQueue`, `BufferRegistry`, `IBufferBuilder`

#### 1.8 Registry Tests (`com.opcua_arrow.maps`)

**Test Class**: `ReadTaskRegistryTest`
- Test thread-safe task registration
- Test get-or-create semantics
- Test concurrent access

**Test Class**: `BufferRegistryTest`
- Test buffer registration by DataWriteGroup
- Test thread-safe access
- Test buffer cleanup

**Test Class**: `RunningStateTest`
- Test thread-safe state management
- Test concurrent reads/writes

#### 1.9 Context Tests

**Test Class**: `ContextTest`
- Test data point update logic (existing vs new)
- Test write group management
- Test read group management
- Test data point deletion
- Test buffer and task registry coordination
- Test start/stop lifecycle
- Mock: `IReader`, `IWriter`, registries

### 2. Integration Tests

Integration tests verify interactions between components with real dependencies (but still isolated from external systems like OPC-UA servers).

#### 2.1 OPC-UA Integration Tests

**Test Class**: `MiloOPCUAIntegrationTest`
- Set up in-memory OPC-UA test server (using Milo test utilities)
- Test complete read cycle: connection → read → data conversion
- Test subscription-based reading with real subscriptions
- Test connection loss and reconnection
- Test concurrent readers on same connection
- Test keep-alive behavior

#### 2.2 Arrow Serialization Integration Tests

**Test Class**: `ArrowSerializationIntegrationTest`
- Test end-to-end serialization: TSValue list → Arrow batch → deserialize and verify
- Test schema consistency across batches
- Test large batches (performance and memory)
- Test mixed data types in same batch
- Test compression effectiveness

#### 2.3 Queue Flow Integration Tests

**Test Class**: `QueueFlowIntegrationTest`
- Test Reader → Queue → Writer flow with real queue
- Test backpressure handling (slow consumer)
- Test multiple concurrent readers pushing to same queue
- Test queue overflow scenarios

#### 2.4 Configuration Integration Tests

**Test Class**: `PostgreSQLConfigIntegrationTest`
- Use embedded PostgreSQL (e.g., testcontainers-postgresql)
- Test loading OPC-UA client config from database
- Test loading data points from view
- Test detecting updated data points
- Test detecting deleted data points
- Test range parsing from PostgreSQL INT4RANGE
- Test JSON config parsing

#### 2.5 Dependency Injection Tests

**Test Class**: `GuiceModuleIntegrationTest`
- Test complete module configuration (ApplicationModule)
- Test that all bindings are satisfied
- Test singleton scope for shared components
- Test that injected instances work correctly together

### 3. End-to-End Tests

E2E tests verify the complete system flow with all components working together.

#### 3.1 Full Pipeline Test

**Test Class**: `EndToEndPipelineTest`
- Set up test OPC-UA server with known nodes
- Set up embedded PostgreSQL with test configuration
- Start the application context
- Verify data flows from OPC-UA → Arrow buffers
- Verify data accuracy and consistency
- Test dynamic configuration updates (add/remove/update data points)
- Test graceful shutdown

#### 3.2 Subscription Mode E2E Test

**Test Class**: `SubscriptionModeE2ETest`
- Configure connector in subscription mode
- Verify notifications are received and processed
- Verify batch accumulation
- Test subscription updates when configuration changes

#### 3.3 Polling Mode E2E Test

**Test Class**: `PollingModeE2ETest`
- Configure connector in polling mode
- Verify scheduled reads occur at correct intervals
- Test multiple read groups with different intervals

### 4. Performance Tests

Performance tests measure throughput, latency, and resource usage.

#### 4.1 Throughput Tests

**Test Class**: `ThroughputTest`
- Measure data points per second processed
- Test with 100, 1000, 10000 data points
- Measure queue throughput
- Measure Arrow serialization throughput
- Profile CPU and memory usage

#### 4.2 Latency Tests

**Test Class**: `LatencyTest`
- Measure end-to-end latency (OPC-UA read → Arrow batch)
- Measure p50, p95, p99 latencies
- Test latency under various loads

#### 4.3 Memory Tests

**Test Class**: `MemoryLeakTest`
- Run connector for extended period
- Monitor heap usage
- Verify Arrow buffers are properly released
- Verify no thread leaks
- Test with different batch sizes

#### 4.4 Concurrency Tests

**Test Class**: `ConcurrencyTest`
- Test high concurrency (many read groups)
- Test lock contention in MiloOPCUAReader
- Test thread safety under stress
- Test concurrent configuration updates

### 5. Resilience Tests

Resilience tests verify system behavior under failure conditions.

#### 5.1 Connection Failure Tests

**Test Class**: `ConnectionFailureTest`
- Test behavior when OPC-UA server is unavailable
- Test automatic reconnection
- Test retry exhaustion
- Test partial connection failures (connect succeeds, read fails)

#### 5.2 Network Failure Simulation

**Test Class**: `NetworkFailureTest`
- Simulate network interruptions
- Test keep-alive failure detection
- Test session recovery
- Test subscription re-establishment after reconnect

#### 5.3 Database Failure Tests

**Test Class**: `DatabaseFailureTest`
- Test behavior when PostgreSQL is unavailable
- Test configuration loading failures
- Test graceful degradation

#### 5.4 Resource Exhaustion Tests

**Test Class**: `ResourceExhaustionTest`
- Test behavior under memory pressure
- Test queue full scenarios
- Test thread pool exhaustion
- Verify graceful degradation

### 6. Edge Case and Error Handling Tests

#### 6.1 Data Quality Tests

**Test Class**: `DataQualityTest`
- Test handling of Bad quality OPC-UA values
- Test handling of Uncertain quality values
- Test null value handling
- Test invalid timestamps

#### 6.2 Configuration Edge Cases

**Test Class**: `ConfigurationEdgeCaseTest`
- Test empty data point list
- Test invalid node IDs
- Test conflicting configurations
- Test invalid filter configurations
- Test malformed JSON in database

#### 6.3 Boundary Tests

**Test Class**: `BoundaryTest`
- Test with minimum/maximum interval values
- Test with very large batch sizes
- Test with single data point
- Test with maximum supported data points

## Test Infrastructure

### Test Utilities

1. **OPC-UA Test Server Builder**: Utility to create in-memory OPC-UA servers with configurable nodes
2. **PostgreSQL Test Data Builder**: Utility to populate test database with realistic configurations
3. **Arrow Data Verifier**: Utility to deserialize and verify Arrow batches
4. **Mock Builder Utilities**: Builders for complex mock objects (OpcUaClient, DataValue, etc.)

### Test Fixtures

1. **Sample Data Points**: Predefined sets of data points for various test scenarios
2. **Sample OPC-UA Values**: Realistic OPC-UA DataValue samples with different types and qualities
3. **Configuration Templates**: JSON configuration templates for testing

### Test Containers

Use Testcontainers for integration tests:
- `PostgreSQLContainer`: For database integration tests
- Consider OPC-UA server container if available

## Testing Tools and Frameworks

### Core Testing
- **JUnit 5**: Main testing framework
- **Mockito**: Mocking framework for unit tests
- **AssertJ**: Fluent assertions

### Integration Testing
- **Testcontainers**: For PostgreSQL and other containerized dependencies
- **Awaitility**: For asynchronous testing

### Performance Testing
- **JMH** (Java Microbenchmark Harness): For micro-benchmarks
- **VisualVM/JProfiler**: For profiling

### Code Coverage
- **JaCoCo**: Code coverage measurement
- Target: Minimum 80% line coverage, 70% branch coverage

## Test Organization

```
src/test/java/com/opcua_arrow/
├── unit/
│   ├── data/
│   ├── opcua/
│   ├── batch_builder/
│   ├── queues/
│   ├── retry/
│   ├── read/
│   └── writer/
├── integration/
│   ├── opcua/
│   ├── postgres/
│   ├── arrow/
│   └── queue/
├── e2e/
│   ├── subscription/
│   └── polling/
├── performance/
│   ├── throughput/
│   ├── latency/
│   └── memory/
├── resilience/
│   ├── connection/
│   ├── network/
│   └── database/
└── fixtures/
    ├── builders/
    └── data/
```

## Test Execution Strategy

### Continuous Integration

1. **On Pull Request**:
   - Run all unit tests
   - Run fast integration tests
   - Generate code coverage report
   - Fail if coverage drops below threshold

2. **On Merge to Main**:
   - Run full test suite (unit + integration + E2E)
   - Run resilience tests
   - Generate test report

3. **Nightly Builds**:
   - Run full test suite
   - Run performance tests
   - Run extended resilience tests
   - Memory leak detection tests

### Local Development

- Developers should run unit tests before committing
- Use Maven profiles to select test categories:
  - `mvn test` - Unit tests only (fast)
  - `mvn verify -Pintegration` - Unit + Integration tests
  - `mvn verify -Pfull` - All tests

## Success Criteria

### Coverage Targets
- Unit test coverage: ≥ 80% line coverage
- Integration test coverage: ≥ 60% line coverage
- Overall coverage: ≥ 75% line coverage

### Quality Gates
- All critical paths must have tests
- All public APIs must have tests
- All error handling paths must have tests
- Zero high-severity bugs in production code

### Performance Targets
- Throughput: ≥ 10,000 data points/second
- Latency: p99 ≤ 100ms for read-to-buffer
- Memory: No leaks detected in 24-hour run

## Test Implementation Priority

### Phase 1: Foundation (Weeks 1-2)
1. Set up test infrastructure and utilities
2. Implement unit tests for data models
3. Implement unit tests for core components (readers, writers, queues)
4. Set up CI pipeline

### Phase 2: Integration (Weeks 3-4)
1. Implement OPC-UA integration tests
2. Implement PostgreSQL integration tests
3. Implement Arrow serialization tests
4. Implement queue flow integration tests

### Phase 3: End-to-End (Week 5)
1. Implement E2E pipeline tests
2. Implement subscription mode E2E tests
3. Implement polling mode E2E tests

### Phase 4: Resilience & Performance (Week 6)
1. Implement connection failure tests
2. Implement performance benchmarks
3. Implement memory leak tests
4. Implement concurrency stress tests

## Maintenance

- Review and update tests when features are added
- Refactor tests when code is refactored
- Keep test data and fixtures up to date
- Monitor test execution time and optimize slow tests
- Regularly review test coverage and add tests for uncovered code

## Appendix: Key Test Scenarios

### Scenario 1: Cold Start
1. Application starts with no prior state
2. Loads configuration from PostgreSQL
3. Connects to OPC-UA server
4. Begins reading and processing data
5. Verifies first batch is correct

### Scenario 2: Configuration Update
1. System running with initial configuration
2. Database configuration is updated (new data point added)
3. System detects update
4. New data point is added without restart
5. Verifies new data point appears in batches

### Scenario 3: Connection Loss and Recovery
1. System connected and reading data
2. OPC-UA server connection is lost
3. System detects disconnection
4. Automatic reconnection succeeds
5. Data reading resumes
6. No data loss or corruption

### Scenario 4: High Load
1. Configure 5000+ data points
2. Multiple read groups with different intervals
3. System processes all data points correctly
4. Verify performance metrics are within targets
5. Verify no resource exhaustion

### Scenario 5: Graceful Shutdown
1. System running under load
2. Shutdown signal received
3. In-flight operations complete
4. Resources are properly released
5. No errors during shutdown
