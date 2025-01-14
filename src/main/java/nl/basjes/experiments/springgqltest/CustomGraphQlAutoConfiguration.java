package nl.basjes.experiments.springgqltest;
/*
 * Copyright 2012-2022 the original author or authors.
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

import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.idl.RuntimeWiring.Builder;
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.log.LogMessage;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for creating a Spring GraphQL base
 * infrastructure.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
//@AutoConfiguration
//@ConditionalOnClass({GraphQL.class, GraphQlSource.class})
//@ConditionalOnGraphQlSchema
//@AutoConfigureBefore(GraphQlAutoConfiguration.class)
@Configuration
@EnableConfigurationProperties(GraphQlProperties.class)
public class CustomGraphQlAutoConfiguration {

    private static final Log logger = LogFactory.getLog(
        org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration.class);

    @Bean
    public DefaultSchemaResourceGraphQlSourceBuilder defaultSchemaResourceGraphQlSourceBuilder() {
        return new DefaultSchemaResourceGraphQlSourceBuilder();
    }

    @Bean
    public FixedGraphQlSource graphQlSource(ResourcePatternResolver resourcePatternResolver,
        GraphQlProperties properties,
        ObjectProvider<DataFetcherExceptionResolver> exceptionResolvers,
        ObjectProvider<Instrumentation> instrumentations,
        ObjectProvider<RuntimeWiringConfigurer> wiringConfigurers,
        ObjectProvider<GraphQlSourceBuilderCustomizer> sourceCustomizers,
        DefaultSchemaResourceGraphQlSourceBuilder defaultBuilder) {
        String[] schemaLocations = properties.getSchema().getLocations();
        Resource[] schemaResources = resolveSchemaResources(resourcePatternResolver,
            schemaLocations,
            properties.getSchema().getFileExtensions());
        GraphQlSource.SchemaResourceBuilder builder = defaultBuilder
            .schemaResources(schemaResources).exceptionResolvers(toList(exceptionResolvers))
            .instrumentation(toList(instrumentations));
        if (!properties.getSchema().getIntrospection().isEnabled()) {
            builder.configureRuntimeWiring(this::enableIntrospection);
        }
        wiringConfigurers.orderedStream().forEach(builder::configureRuntimeWiring);
        sourceCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return (FixedGraphQlSource) builder.build();
    }

    private Builder enableIntrospection(Builder wiring) {
        return wiring.fieldVisibility(
            NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY);
    }

    private Resource[] resolveSchemaResources(ResourcePatternResolver resolver, String[] locations,
        String[] extensions) {
        List<Resource> resources = new ArrayList<>();
        for (String location : locations) {
            for (String extension : extensions) {
                resources.addAll(resolveSchemaResources(resolver, location + "*" + extension));
            }
        }
        return resources.toArray(new Resource[0]);
    }

    private List<Resource> resolveSchemaResources(ResourcePatternResolver resolver,
        String pattern) {
        try {
            return Arrays.asList(resolver.getResources(pattern));
        } catch (IOException ex) {
            logger.debug(LogMessage.format("Could not resolve schema location: '%s'", pattern), ex);
            return Collections.emptyList();
        }
    }


    private <T> List<T> toList(ObjectProvider<T> provider) {
        return provider.orderedStream().collect(Collectors.toList());
    }

}
