package com.smartbi.query.route;

import com.smartbi.analyze.route.RoutingContext;
import com.smartbi.analyze.route.RoutingDecision;
import com.smartbi.analyze.route.SqlRoutingAnalyzer;
import com.smartbi.analyze.sql.ParsedSql;
import com.smartbi.query.config.QueryRoutingContextProvider;
import com.smartbi.query.datasource.ManagedDataSourceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SqlRouteService {

    private final ManagedDataSourceRegistry registry;
    private final QueryRoutingContextProvider queryRoutingContextProvider;
    private final EffectiveEngineResolver effectiveEngineResolver;
    private final SqlRoutingAnalyzer sqlRoutingAnalyzer = new SqlRoutingAnalyzer();

    @Autowired
    public SqlRouteService(ManagedDataSourceRegistry registry,
                           QueryRoutingContextProvider queryRoutingContextProvider,
                           EffectiveEngineResolver effectiveEngineResolver) {
        this.registry = registry;
        this.queryRoutingContextProvider = queryRoutingContextProvider;
        this.effectiveEngineResolver = effectiveEngineResolver == null
                ? new EffectiveEngineResolver(null)
                : effectiveEngineResolver;
    }

    SqlRouteService(ManagedDataSourceRegistry registry) {
        this(registry, null, new EffectiveEngineResolver(null));
    }

    SqlRouteService(ManagedDataSourceRegistry registry,
                    QueryRoutingContextProvider queryRoutingContextProvider) {
        this(registry, queryRoutingContextProvider, new EffectiveEngineResolver(null));
    }

    public RoutedSql routeAndRewrite(String originalSql, ParsedSql parsed) {
        RoutingContext routingContext = queryRoutingContextProvider == null
                ? new RoutingContext(registry.getDefaultName(), registry.snapshotDescriptors(),
                java.util.Collections.emptyList())
                : queryRoutingContextProvider.getRoutingContext();
        String effectiveEngine = effectiveEngineResolver.resolve(parsed);
        RoutingDecision decision = sqlRoutingAnalyzer.analyze(originalSql, parsed, routingContext, effectiveEngine);
        String datasourceName = decision.getDatasourceName();
        String datasourceType = decision.getDatasourceType();
        if (registry.getDefinition(datasourceName) == null) {
            datasourceName = registry.getDefaultName();
            datasourceType = registry.getDefinition(datasourceName) == null
                    ? datasourceType
                    : registry.getDefinition(datasourceName).getType();
        }
        return new RoutedSql(datasourceName, datasourceType, originalSql, decision.getExecutionSql());
    }
}
