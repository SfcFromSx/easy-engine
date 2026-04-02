# Query Test Matrix

This matrix is the canonical inventory for non-trivial handwritten `query` production functions. Each listed function maps its meaningful logic branches to concrete automated tests using `ClassName#methodName` references for both production code and test methods.

Coverage evidence for this matrix is emitted by `mvn -q -f query/pom.xml test` under:

- `query/target/site/jacoco/index.html`
- `query/target/site/jacoco/jacoco.csv`
- `query/target/site/jacoco/jacoco.xml`

## Function Inventory

### `CachePolicy`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `CachePolicy#isQuerySql` | read-only statements accepted; null and write statements rejected | `CachePolicyTest#shouldRecognizeSupportedReadOnlyStatements`, `CachePolicyTest#shouldRejectNullAndWriteStatementsAsNonQuerySql` |
| `CachePolicy#shouldBypassCacheBeforeLookup` | safe mode disabled; safe mode enabled with volatile SQL; safe mode enabled with stable SQL; null parsed SQL | `CachePolicyTest#shouldBypassCacheOnlyForSafeModeVolatileQueries` |
| `CachePolicy#isFingerprintableParameterType` | supported types; unsupported types | `CachePolicyTest#shouldRecognizeFingerprintableParameterTypes` |
| `CachePolicy#cacheModeKeyTag` | safe mode disabled/enabled rendering | `CachePolicyTest#shouldRenderCacheModeKeyTagFromSafeModeSetting` |

### `PreparedParameterSupport`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `PreparedParameterSupport#shouldUseCache` | non-parameterized SQL; parameterized SQL with prepared caching disabled; null/empty/null-entry params; unsupported parameter class | `PreparedParameterSupportTest#shouldAlwaysUseCacheForNonParameterizedSql`, `PreparedParameterSupportTest#shouldDisableCacheForParameterizedSqlWhenPreparedCachingIsDisabledOrUnsupported` |
| `PreparedParameterSupport#fingerprint` | non-parameterized SQL and disabled prepared caching return `null`; null/empty params; null param entry; null value token; unsupported parameter class | `PreparedParameterSupportTest#shouldBuildFingerprintsForSupportedPreparedParameters`, `PreparedParameterSupportTest#shouldReturnNullFingerprintForUnsupportedPreparedParameterTypes` |

### `ManagerConfigClient`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `ManagerConfigClient#fetchDatasourceConfigs` | blank manager URL; trailing-slash trim + success body; null body; empty array; `RestClientException` wrapping | `ManagerConfigClientTest#shouldReturnEmptyListWhenManagerUrlIsBlank`, `ManagerConfigClientTest#shouldTrimTrailingSlashAndMapDatasourceConfigs`, `ManagerConfigClientTest#shouldReturnEmptyListWhenManagerRespondsWithoutBody`, `ManagerConfigClientTest#shouldReturnEmptyListWhenManagerRespondsWithEmptyArray`, `ManagerConfigClientTest#shouldWrapRestClientFailures` |

### `ManagedDataSourceRegistry`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `ManagedDataSourceRegistry#getDefinition` | named datasource hit; missing datasource falls back to default | `ManagedDataSourceRegistryTest#shouldPreferManagerDatasourceConfigsWhenAvailable`, `ManagedDataSourceRegistryTest#shouldFallbackToStaticDatasourceConfigsWhenManagerUnavailable` |
| `ManagedDataSourceRegistry#getConnection` / `ManagedDataSourceRegistry#getOrCreate` | successful pool creation; datasource reuse; missing driver failure | `ManagedDataSourceRegistryTest#shouldReuseDataSourcePoolsAcrossConnections`, `ManagedDataSourceRegistryTest#shouldFailWhenDriverClassCannotBeLoaded` |
| `ManagedDataSourceRegistry#loadDefinitions` | remote manager success; manager failure fallback; unusable remote configs fallback | `ManagedDataSourceRegistryTest#shouldPreferManagerDatasourceConfigsWhenAvailable`, `ManagedDataSourceRegistryTest#shouldFallbackToStaticDatasourceConfigsWhenManagerUnavailable`, `ManagedDataSourceRegistryTest#shouldFallbackWhenManagerReturnsOnlyBlankConfigs` |
| `ManagedDataSourceRegistry#remoteBootstrap` | null/blank configs ignored; explicit default preferred | `ManagedDataSourceRegistryTest#shouldIgnoreBlankManagerConfigsAndPreferExplicitDefaultFlag`, `ManagedDataSourceRegistryTest#shouldPreferManagerDatasourceConfigsWhenAvailable` |
| `ManagedDataSourceRegistry#fallbackBootstrap` | default fallback; named datasource uses map key when explicit name blank | `ManagedDataSourceRegistryTest#shouldFallbackToStaticDatasourceConfigsWhenManagerUnavailable`, `ManagedDataSourceRegistryTest#shouldUseMapKeyWhenFallbackNamedDatasourceNameIsBlank` |
| `ManagedDataSourceRegistry#close` | closes created Hikari pools | `ManagedDataSourceRegistryTest#shouldCloseManagedDataSources` |

### `SqlCommentParser`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `SqlCommentParser#parse` | null/empty SQL; driver hints stripped from execution SQL; metadata preserved; flag parsing; quoted-string immunity; unknown `YH_*` metadata retained | `SqlCommentParserTest#shouldReturnEmptyMetadataForNullAndEmptySql`, `SqlCommentParserTest#shouldStripDriverHintsButKeepMetadataInExecutionSql`, `SqlCommentParserTest#shouldParseNoCacheAndRefreshFlags`, `SqlCommentParserTest#shouldIgnoreCommentSyntaxInsideQuotedStrings`, `SqlCommentParserTest#shouldPreserveMetadataOnlyCommentsAndUnknownYhMetadata` |
| `SqlCommentParser#safeParse` | successful parse passthrough; invalid hint fallback to original SQL | `SqlCommentParserTest#shouldStripDriverHintsButKeepMetadataInExecutionSql`, `SqlCommentParserTest#shouldFallbackToOriginalSqlWhenSafeParseEncountersInvalidHintValues` |

### `SqlRouteService`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `SqlRouteService#routeAndRewrite` | default routing; `YH_TARGET_ENGINE` precedence over `engine`; unknown datasource fallback; case-insensitive adapter lookup; `parsed == null` | `SqlRouteServiceTest#shouldRouteToDefaultDatasourceWhenNoHintsArePresent`, `SqlRouteServiceTest#shouldPreferYhTargetEngineOverDriverEngineHint`, `SqlRouteServiceTest#shouldFallbackToDefaultDatasourceWhenTargetIsUnknown`, `SqlRouteServiceTest#shouldUseDatasourceTypeLookupCaseInsensitively`, `SqlRouteServiceTest#shouldHandleNullParsedSqlByPassingThroughOriginalSql`, `QueryWebIntegrationTest#shouldRouteByPreservedMetadataHint` |

### `QueryCacheService`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `QueryCacheService#tryGet` | cache hit round-trip; deserialization failure returns `null` | `QueryCacheServiceTest#shouldRoundTripCachedResponse`, `QueryCacheServiceTest#shouldReturnNullWhenCachedPayloadCannotBeDeserialized` |
| `QueryCacheService#put` | normal write; oversize payload skipped | `QueryCacheServiceTest#shouldRoundTripCachedResponse`, `QueryCacheServiceTest#shouldSkipCachingOversizedResponses` |
| `QueryCacheService#effectiveTtl` | metadata override; default TTL fallback | `QueryCacheServiceTest#shouldResolveEffectiveTtlFromMetadataOverrideOrDefault` |
| `QueryCacheService#buildKey` | datasource-isolated keys; shared keys; explicit cache-key override | `QueryCacheServiceTest#shouldBuildDifferentKeysForDifferentDatasourcesWhenIsolationEnabled`, `QueryCacheServiceTest#shouldBuildSharedKeysAndHonorExplicitCacheKey` |

### `QueryExecutionService`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `QueryExecutionService#execute` | statement success; non-Kylin prepared success; Kylin literalized prepared success; Kylin placeholder mismatch rejection; blank/null/unsupported-request rejection; non-query rejection; cache hit; `no-cache` / refresh bypass; datasource failure | `QueryExecutionServiceTest#shouldExecuteStatementQueriesAgainstDatasourceAndCacheTheResponse`, `QueryExecutionServiceTest#shouldExecutePreparedQueriesAndBindConvertedParameters`, `QueryExecutionServiceTest#shouldLiteralizePreparedParametersForKylinDatasources`, `QueryExecutionServiceTest#shouldRejectKylinPreparedQueriesWhenPlaceholderCountDoesNotMatch`, `QueryExecutionServiceTest#shouldRejectBlankQueryRequestsBeforeRouting`, `QueryExecutionServiceTest#shouldRejectNullQueryRequestsBeforeRouting`, `QueryExecutionServiceTest#shouldRejectUnsupportedRequestShapesBeforeRouting`, `QueryExecutionServiceTest#shouldRejectNonQuerySqlWithExceptionPayload`, `QueryExecutionServiceTest#shouldReturnCachedResponsesWithoutOpeningDatasourceConnections`, `QueryExecutionServiceTest#shouldSkipCacheLookupWhenSqlForcesNoCacheOrRefresh`, `QueryExecutionServiceTest#shouldReturnExceptionResponseWhenDatasourceExecutionFails`, `QueryWebIntegrationTest#shouldServeCacheHitOnSecondQuery`, `QueryWebIntegrationTest#shouldReturnKylinStyleExceptionPayloadForNonQuerySql`, `QueryWebIntegrationTest#shouldReturnKylinStyleExceptionPayloadForEmptyRequestBody`, `QueryWebIntegrationTest#shouldReturnKylinStyleExceptionPayloadForUnsupportedRequestShape` |
| `QueryExecutionService#executeAgainstDatasource` | statement branch; Kylin literalized statement branch; JDBC prepared branch | `QueryExecutionServiceTest#shouldExecuteStatementQueriesAgainstDatasourceAndCacheTheResponse`, `QueryExecutionServiceTest#shouldLiteralizePreparedParametersForKylinDatasources`, `QueryExecutionServiceTest#shouldExecutePreparedQueriesAndBindConvertedParameters` |
| `QueryExecutionService#bindParameters` | converted parameter binding to prepared statement for non-Kylin datasources | `QueryExecutionServiceTest#shouldExecutePreparedQueriesAndBindConvertedParameters`, `QueryWebIntegrationTest#shouldPublishPreparedExecutionModeInTracePayload` |
| `QueryExecutionService#convertValue` | supported scalar/date/time/timestamp conversions; null param/value; unsupported class fallback | `QueryExecutionServiceTest#shouldConvertSupportedParameterTypesAndFallbackToRawStrings` |
| `QueryExecutionService#resolveExecutionMode` | no params => statement; params => prepared | `QueryExecutionServiceTest#shouldResolveExecutionModeFromParameterPresence`, `QueryWebIntegrationTest#shouldPublishStatementExecutionModeInTracePayload`, `QueryWebIntegrationTest#shouldPublishPreparedExecutionModeInTracePayload` |

### `QueryResultMapper`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `QueryResultMapper#toResponse` | result rows mapped; column metadata mapped; scan count/defaults set | `QueryResultMapperTest#shouldMapResultSetRowsAndColumnMetadata` |
| `QueryResultMapper#exceptionResponse` | Kylin-style error payload defaults | `QueryResultMapperTest#shouldBuildKylinStyleExceptionResponses` |

### `TraceReportingService`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `TraceReportingService#report` | trace disabled early return; successful publish; writer failure swallowed | `TraceReportingServiceTest#shouldSkipPublishingWhenTraceReportingIsDisabled`, `TraceReportingServiceTest#shouldPublishSerializedTracePayloads`, `TraceReportingServiceTest#shouldSwallowTraceWriterFailures` |
| `TraceReportingService#resolveParameterPayload` | failed prepared payload emitted; success/statement/empty params return `null`; serialization failure returns `null` | `TraceReportingServiceTest#shouldIncludeReadableParameterPayloadOnlyForFailedPreparedExecutions`, `TraceReportingServiceTest#shouldOmitParameterPayloadWhenItIsNotNeeded`, `TraceReportingServiceTest#shouldDropParameterPayloadWhenSerializationFails`, `QueryWebIntegrationTest#shouldPublishReadableParameterPayloadForFailedPreparedExecution` |

### `JdbcTraceWriter`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `JdbcTraceWriter#publish` | blank payload skipped; invalid JSON persisted as parse error; valid payload persisted with fingerprint; missing SQL marked skipped | `JdbcTraceWriterTest#shouldIgnoreBlankTracePayloads`, `JdbcTraceWriterTest#shouldPersistParseErrorsForInvalidJsonPayloads`, `JdbcTraceWriterTest#shouldPersistFingerprintAndCreatePatternStatsForValidPayloads`, `JdbcTraceWriterTest#shouldMarkTraceRowsAsSkippedWhenSqlIsMissing`, `QueryTracePersistenceIntegrationTest#shouldPersistTraceRowsDirectlyToTraceDatabase` |
| `JdbcTraceWriter#fillRecord` | populated JSON fields mapped to row columns | `JdbcTraceWriterTest#shouldPersistFingerprintAndCreatePatternStatsForValidPayloads`, `QueryTracePersistenceIntegrationTest#shouldPersistTraceRowsDirectlyToTraceDatabase` |
| `JdbcTraceWriter#upsertPatternStats` | create new stats row; update existing count/average/sample/signature | `JdbcTraceWriterTest#shouldPersistFingerprintAndCreatePatternStatsForValidPayloads`, `JdbcTraceWriterTest#shouldUpdateExistingPatternStatsWithAverageSampleAndSignature`, `QueryTracePersistenceIntegrationTest#shouldPersistTraceRowsDirectlyToTraceDatabase` |
| `JdbcTraceWriter#truncate` | sample truncation applied to long SQL text | `JdbcTraceWriterTest#shouldTruncateLongPatternSamples` |

### `RedisQueryCacheStore`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `RedisQueryCacheStore#get` | successful read; exception returns `null` | `RedisQueryCacheStoreTest#shouldReadCachedValuesFromRedis`, `RedisQueryCacheStoreTest#shouldReturnNullWhenRedisGetFails` |
| `RedisQueryCacheStore#set` | successful write; exception swallowed | `RedisQueryCacheStoreTest#shouldWriteCachedValuesToRedisWithTtl`, `RedisQueryCacheStoreTest#shouldSwallowRedisSetFailures` |

### `FingerprintUtil`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `FingerprintUtil#sha256Hex` | null input; deterministic hash output | `FingerprintUtilTest#shouldHashStringsDeterministically` |
| `FingerprintUtil#normalizeForFingerprint` | null input; whitespace and case normalization | `FingerprintUtilTest#shouldNormalizeSqlForFingerprinting` |

### `BasicAuthInterceptor`

| Production function | Meaningful branches | Automated tests |
|---|---|---|
| `BasicAuthInterceptor#preHandle` | `OPTIONS` bypass; auth disabled bypass; missing header; malformed base64; missing colon; wrong credentials; successful auth | `BasicAuthInterceptorTest#shouldAllowOptionsRequestsWithoutAuthentication`, `BasicAuthInterceptorTest#shouldAllowRequestsWhenExpectedUsernameIsBlank`, `BasicAuthInterceptorTest#shouldRejectRequestsWithoutAuthorizationHeader`, `BasicAuthInterceptorTest#shouldRejectRequestsWithMalformedBase64Credentials`, `BasicAuthInterceptorTest#shouldRejectRequestsWithMissingColonOrWrongCredentials`, `BasicAuthInterceptorTest#shouldAllowRequestsWithMatchingCredentials` |
| `BasicAuthInterceptor#safeEquals` | matching and mismatching credential comparisons | `BasicAuthInterceptorTest#shouldRejectRequestsWithMissingColonOrWrongCredentials`, `BasicAuthInterceptorTest#shouldAllowRequestsWithMatchingCredentials` |

## Explicit Exclusions

The following classes are excluded from the required traceability inventory because they are boilerplate value holders, Spring wiring, pass-through wrappers, or repository declarations rather than branch-heavy handwritten logic:

- DTO/entity/config/value-holder boilerplate: `PreparedQueryRequestDto`, `StatementParameterDto`, `SqlResponseStubDto`, `SqlExecutionRecord`, `SqlPatternStats`, `SqlMetadata`, `TracePayload`, `RoutedSql`, `DataSourceDefinition`
- Spring bootstrapping/wiring/pass-through code: `EngineQueryApplication`, `QueryInfrastructureConfig`, `WebConfig`, `QueryController`, `PassThroughSqlAdapter`
- repository interfaces: `SqlExecutionRecordRepository`, `SqlPatternStatsRepository`, `QueryCacheStore`, `TraceWriter`, `SqlAdapter`

## Coverage Notes

- The JaCoCo report still includes excluded boilerplate/wiring classes because it records all compiled `com.smartbi.query.*` classes. Those misses are informational only and are not part of this task's required branch inventory.
- Remaining misses inside inventoried classes are limited to defensive or platform-dependent sub-branches that are not treated as meaningful logic branches for this task, such as JVM fallback handling in `FingerprintUtil#sha256Hex`, Hikari defensive shutdown/load guards in `ManagedDataSourceRegistry`, and switch-arm permutations in `SqlCommentParser` that do not change the module contract.
