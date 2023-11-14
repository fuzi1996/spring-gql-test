/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.experiments.springgqltest;

import graphql.GraphQL;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeVisitor;
import graphql.schema.SchemaTraverser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.GraphQlSource;


/**
 * Implementation of {@link GraphQlSource.Builder} that leaves it to subclasses
 * to initialize {@link GraphQLSchema}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 1.0.0
 */
abstract class AbstractGraphQlSourceBuilder<B extends GraphQlSource.Builder<B>> implements
    GraphQlSource.Builder<B> {

    private final List<DataFetcherExceptionResolver> exceptionResolvers = new ArrayList<>();

    private final List<GraphQLTypeVisitor> typeVisitors = new ArrayList<>();

    private final List<Instrumentation> instrumentations = new ArrayList<>();

    private Consumer<GraphQL.Builder> graphQlConfigurers = (builder) -> {
    };

    private GraphQL.Builder builder;


    @Override
    public B exceptionResolvers(List<DataFetcherExceptionResolver> resolvers) {
        this.exceptionResolvers.addAll(resolvers);
        return self();
    }

    @Override
    public B typeVisitors(List<GraphQLTypeVisitor> typeVisitors) {
        this.typeVisitors.addAll(typeVisitors);
        return self();
    }

    @Override
    public B instrumentation(List<Instrumentation> instrumentations) {
        this.instrumentations.addAll(instrumentations);
        return self();
    }

    @Override
    public B configureGraphQl(Consumer<GraphQL.Builder> configurer) {
        this.graphQlConfigurers = this.graphQlConfigurers.andThen(configurer);
        return self();
    }

    @SuppressWarnings("unchecked")
    private <T extends B> T self() {
        return (T) this;
    }

    public GraphQL.Builder getgGraphQLBuilder() {
        return this.builder;
    }

    @Override
    public FixedGraphQlSource build() {
        GraphQLSchema schema = initGraphQlSchema();

        schema = applyTypeVisitors(schema);

        this.builder = GraphQL.newGraphQL(schema);
        builder.defaultDataFetcherExceptionHandler(
            new ExceptionResolversExceptionHandler(this.exceptionResolvers));

        if (!this.instrumentations.isEmpty()) {
            builder = builder.instrumentation(new ChainedInstrumentation(this.instrumentations));
        }

        this.graphQlConfigurers.accept(builder);

        GraphQL graphQL = builder.build();

        return new FixedGraphQlSource(graphQL, schema);
    }

    /**
     * Subclasses must implement this method to provide the
     * {@link GraphQLSchema} instance.
     */
    protected abstract GraphQLSchema initGraphQlSchema();

    private GraphQLSchema applyTypeVisitors(GraphQLSchema schema) {
        List<GraphQLTypeVisitor> visitors = new ArrayList<>(this.typeVisitors);
        visitors.add(ContextDataFetcherDecorator.TYPE_VISITOR);

        GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry
            .newCodeRegistry(schema.getCodeRegistry());
        Map<Class<?>, Object> vars = Collections
            .singletonMap(GraphQLCodeRegistry.Builder.class, codeRegistry);

        SchemaTraverser traverser = new SchemaTraverser();
        traverser.depthFirstFullSchema(visitors, schema, vars);

        return schema.transformWithoutTypes(builder -> builder.codeRegistry(codeRegistry));
    }
}
