
/**
 * Small Milo OPC-UA server for tests.
 *
 * It exposes:
 *
 *  - 3 Float nodes:   Float_Good, Float_Bad, Float_Null
 *  - 3 Int nodes:     Int_Good, Int_Bad, Int_Null  (Int32)
 *  - 3 String nodes:  String_Good, String_Bad, String_Null
 *  - 3 Boolean nodes: Boolean_Good, Boolean_Bad, Boolean_Null
 *
 * For each type:
 *   - *_Good:  non-null value, StatusCode = Good
 *   - *_Bad:   non-null value, StatusCode = Bad_InternalError
 *   - *_Null:  null value (Variant.NULL_VALUE), StatusCode = Good
 *
 * Plus:
 *   - N random Double nodes under "Test/Random":
 *       Random_0, Random_1, ..., Random_(N-1)
 *
 *     Every X milliseconds each random node is updated:
 *       - 10% of updates => Bad status (Bad_InternalError)
 *       - 5% of updates  => null value (Variant.NULL_VALUE), status implicitly Good
 *       - 85% of updates => random double, status Good
 *
 * N and X are configurable via system properties:
 *
 *   -Dopcua.test.randomNodeCount=<int>        (default 5)
 *   -Dopcua.test.randomUpdateIntervalMillis=<long>  (default 1000)
 *
 * This server is intended to be started once before the tests and stopped once
 * after all tests finish (e.g. via JUnit @BeforeAll / @AfterAll).
 */