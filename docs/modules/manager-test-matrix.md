# Manager Test Matrix

This matrix inventories the non-trivial handwritten manager logic and maps each meaningful branch to an automated test or an explicit exclusion.

## Backend

### Datasource configuration

- `QueryDatasourceConfigService#list`: covered by `QueryDatasourceConfigServiceTest#listSortsByDefaultAndName`.
- `QueryDatasourceConfigService#get`: covered by `QueryDatasourceConfigServiceTest#getDelegatesToRepository`.
- `QueryDatasourceConfigService#create`: covered by `QueryDatasourceConfigServiceTest#createTrimsFieldsAndAppliesDefaults` and `QueryDatasourceConfigServiceTest#createRejectsDuplicateTrimmedName`.
  Branches: trimmed input/default values, duplicate-name failure.
- `QueryDatasourceConfigService#update`: covered by `QueryDatasourceConfigServiceTest#updateRejectsDuplicateOwnedByAnotherRecord`.
  Branches: duplicate-name rejection, existing-record lookup.
- `QueryDatasourceConfigService#delete`: covered by `QueryDatasourceConfigServiceTest#deletePromotesNextDatasourceWhenDefaultIsRemoved`.
  Branches: default removal with promotion.
- `QueryDatasourceConfigService#validate`: covered by `QueryDatasourceConfigServiceTest#createRejectsInvalidNumericBoundaries`.
  Branches: `maxPoolSize`, `minIdle`, and `connectionTimeoutMs` boundary failures.
- `DatasourceConfigController#get`: covered by `DatasourceConfigControllerTest#getReturnsDatasourceWhenPresent` and `DatasourceConfigControllerTest#getReturnsNotFoundWhenMissing`.
- `DatasourceConfigController#create`, `DatasourceConfigController#update`, `DatasourceConfigController#delete`: covered by `DatasourceConfigLifecycleTest#shouldManageDatasourceCrudLifecycle`.
- Flyway-backed datasource schema and seed behavior: covered by `DatasourceConfigFlywayIntegrationTest#shouldCreateDatasourceConfigTableAndSeedQueryDefaults`.

### Trace ingestion and browsing

- `TraceIngestionService#ingestBatch`: covered by `TraceIngestionTest#ingestBatchIgnoresEmptyEntriesAndPersistsSkippedRecord`.
- `TraceIngestionService#ingestJson`: covered by `TraceIngestionTest#shouldIngestAndSummarizeTrace`, `TraceIngestionTest#shouldAccumulateStatsForSameFingerprint`, `TraceIngestionTest#ingestBatchIgnoresEmptyEntriesAndPersistsSkippedRecord`, `TraceIngestionTest#ingestJsonStoresParseErrorWhenPayloadIsInvalid`, and `TraceFlywayExecutionModeIntegrationTest#shouldApplyFlywayTraceColumnsAndExposeStoredValues`.
  Branches: success, aggregate-update, skipped/no-SQL, malformed-payload error, Flyway-backed execution-mode/parameter-payload persistence.
- `FingerprintUtil#sha256Hex`: covered by `FingerprintUtilTest#sha256HexReturnsStableDigestAndNullPassThrough`.
- `FingerprintUtil#normalizeForFingerprint`: covered by `FingerprintUtilTest#normalizeForFingerprintCollapsesWhitespaceAndLowercases`.
- `TraceController#page`: covered by `TraceControllerTest#pageFiltersByFingerprintAndMapsToLightweightDto`, `TraceControllerTest#pageUsesDefaultListingWhenFingerprintIsBlank`, `ManagerApiTest#shouldReturnTraces`, and `ManagerApiTest#shouldReturnFilteredTraces`.
  Branches: filtered query, unfiltered query, combined-filter query, DTO mapping.
- `TraceController#resolveSourceFlag`: covered by `TraceControllerTest#pageFiltersByFingerprintAndMapsToLightweightDto` and `TraceControllerTest#pageUsesDefaultListingWhenFingerprintIsBlank`.
  Branches: `SEED`, `SELF`, `JDBC`.

### Acceleration, stats, parse, and JDBC advice

- `AccelerationService#createManual`: covered by `AccelerationServiceTest#createManualRejectsDuplicateSchemaAndName`.
- `AccelerationService#updateManual`: covered by `AccelerationServiceTest#updateManualKeepsExistingStatusAndSource`.
- `AccelerationService#delete`: covered by `AccelerationServiceTest#deleteRemovesExistingEntity`.
- `AccelerationService#createFromPattern`: covered by `AccelerationServiceTest#createFromPatternBuildsRecommendedDraftWithSanitizedName` and `AccelerationLifecycleTest#shouldManageAccelerationLifecycle`.
- `AccelerationService#list`: covered by `AccelerationServiceTest#listDelegatesToSpecificationQuery`, `ManagerControllerWebTest#listDelegatesToAccelerationService`, and `AccelerationLifecycleTest#shouldFilterAccelerationList`.
  Branches: filtered specification delegation, filtered web query.
- `AccelerationService#topPatterns`: covered by `AccelerationServiceTest#topPatternsDelegatesToMatchingRepositoryQuery`, `ManagerApiTest#shouldReturnTopPatterns`, and `ManagerApiTest#shouldReturnFilteredTopPatterns`.
- `AccelerationService#updateStatus`: covered by `AccelerationServiceTest#updateStatusCreatesSchemaAndRunsRefreshSqlWhenActivating` and `ManagerControllerWebTest#statusUpdatesAccelerationState`.
- `AccelerationController#list`: covered by `ManagerControllerWebTest#listDelegatesToAccelerationService`, `AccelerationLifecycleTest#shouldManageAccelerationLifecycle`, and `AccelerationLifecycleTest#shouldFilterAccelerationList`.
- `AccelerationController#create`: covered by `AccelerationLifecycleTest#shouldCreateManualTable`.
- `AccelerationController#fromPattern`: covered by `ManagerControllerWebTest#fromPatternRejectsMissingPatternIdAsBadRequest` and `AccelerationLifecycleTest#shouldManageAccelerationLifecycle`.
- `AccelerationController#status`: covered by `ManagerControllerWebTest#statusUpdatesAccelerationState`.
- `StatsController#summary`: covered by `StatsControllerTest#summaryIncludesAccelerationAndCacheMetrics`, `StatsControllerTest#summaryLeavesLastTraceNullWhenRepositoryHasNoRows`, and `ManagerApiTest#shouldReturnStatsSummary`.
- `SqlParseService#analyze`: covered by `SqlParseServiceTest#parsesSimpleSelect`, `SqlParseServiceTest#skippedOnEmpty`, `SqlParseServiceTest#parsesOrderByQueryAndKeepsRootKind`, and `SqlParseServiceTest#returnsErrorForInvalidSql`.
- `ParseController#preview`: covered by `ManagerControllerWebTest#parsePreviewReturnsAnalyzeOutcome`.
- `JdbcSqlAdvisorService#adviseRewrite`: covered by `JdbcSqlAdvisorServiceTest#passthroughCleanSql`, `JdbcSqlAdvisorServiceTest#emptySqlReturnsExplicitAdvisoryMessage`, and `JdbcSqlAdvisorServiceTest#activeAccelerationProducesRewriteHint`.
- `JdbcAdvisorController#sqlRewrite`: covered by `ManagerControllerWebTest#jdbcRewriteDelegatesToAdvisorService`.
- `RestExceptionHandler#handleBadRequest`: covered by `ManagerControllerWebTest#fromPatternRejectsMissingPatternIdAsBadRequest`.
- `RestExceptionHandler#handleGeneral`: covered by `RestExceptionHandlerWebTest#checkedExceptionMapsToInternalServerErrorPayload`.

### Explicit backend exclusions

- `EngineApplication`, `EngineConfig`, JPA repositories, entity getters/setters, enums, and DTO accessor classes are excluded because they are framework/bootstrap or accessor-only code with no manager-specific branching.

## Frontend

### Shared helpers

- `src/utils/formatters.js:formatDateTime`: covered by `test/formatters.spec.js` test `returns fallback text for missing or invalid dates`.
- `src/utils/formatters.js:formatDuration`: covered by `test/formatters.spec.js` test `formats duration and percentages with safe defaults`.
- `src/utils/formatters.js:formatPercent`: covered by `test/formatters.spec.js` test `formats duration and percentages with safe defaults`.
- `src/utils/formatters.js:shortFingerprint`: covered by `test/formatters.spec.js` test `shortens long fingerprints without touching short values`.

### Operator views

- `src/views/Dashboard.vue:metricValue`, `src/views/Dashboard.vue:metricPercent`, `src/views/Dashboard.vue:applySummary`, `src/views/Dashboard.vue:loadSummary`, `src/views/Dashboard.vue:loadPatterns`, `src/views/Dashboard.vue:loadTraces`, `src/views/Dashboard.vue:load`: covered by `test/dashboard.spec.js`.
  Branches: healthy summary load, summary failure fallback, derived health text and note text.
- `src/views/Traces.vue:goPattern`, `src/views/Traces.vue:buildQuery`, `src/views/Traces.vue:applyFilter`, `src/views/Traces.vue:clearFilter`, `src/views/Traces.vue:statusType`, `src/views/Traces.vue:sourceFlagType`, `src/views/Traces.vue:load`: covered by `test/traces.spec.js`.
  Branches: route-backed filter load, multi-field query updates, clear filter, API error reset.
- `src/views/Patterns.vue:goTraces`, `src/views/Patterns.vue:buildQuery`, `src/views/Patterns.vue:applyFilter`, `src/views/Patterns.vue:clearFilter`, `src/views/Patterns.vue:rowClassName`, `src/views/Patterns.vue:load`, `src/views/Patterns.vue:openDialog`, `src/views/Patterns.vue:submit`: covered by `test/patterns.spec.js`.
  Branches: selected-row highlighting, route-backed multi-filter updates, dialog defaults, submit success, empty-table validation.
- `src/views/QueryDatasources.vue:load`, `src/views/QueryDatasources.vue:matchesFilters`, `src/views/QueryDatasources.vue:clearFilters`, `src/views/QueryDatasources.vue:resetForm`, `src/views/QueryDatasources.vue:openCreate`, `src/views/QueryDatasources.vue:edit`, `src/views/QueryDatasources.vue:promote`, `src/views/QueryDatasources.vue:remove`, `src/views/QueryDatasources.vue:save`: covered by `test/query-datasources.spec.js`.
  Branches: local keyword filtering, filter reset, summary counts, empty-form validation, promote-as-default save path, confirmed removal.
- `src/views/Acceleration.vue:buildQuery`, `src/views/Acceleration.vue:applyFilter`, `src/views/Acceleration.vue:clearFilter`, `src/views/Acceleration.vue:load`, `src/views/Acceleration.vue:openCreate`, `src/views/Acceleration.vue:edit`, `src/views/Acceleration.vue:remove`, `src/views/Acceleration.vue:toggleStatus`, `src/views/Acceleration.vue:save`: covered by `test/acceleration.spec.js`.
  Branches: filtered list params, clear filter reset, disabled-count derivation, validation failure, activation patch, edit/save fallback to `public`, removal.

### Explicit frontend exclusions

- `src/App.vue`: excluded for this task because it is layout/bootstrap shell logic; locale persistence and title updates are low-risk shell behavior outside the operator data flows prioritized by `MGR-TEST-001`.
- `src/main.js`, `src/router/index.js`, `src/api/client.js`, `src/api/endpoints.js`, `src/i18n.js`, and `src/style.css`: excluded because they are bootstrap wiring, constant definitions, translation catalogs, or static styling rather than branch-heavy manager logic.
