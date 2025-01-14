/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2022 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.experiments.springgqltest;


import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.util.TraversalControl.CONTINUE;

import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchemaElement;
import graphql.schema.GraphQLTypeVisitor;
import graphql.schema.GraphQLTypeVisitorStub;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//@Configuration(proxyBeanMethods = false)
@SuppressWarnings("java:S1854")
// https://community.sonarsource.com/t/unresolved-variable-in-anonymous-class-s1854-remove-this-useless-assignment-to-local-variable-unknown/46076
public class MyDynamicGraphQLApi {

    private static final Logger LOG = LogManager.getLogger(MyDynamicGraphQLApi.class);

    public static GraphQLTypeVisitor addVersionToSchema() {
        // New type
        GraphQLObjectType version = GraphQLObjectType
            .newObject()
            .name("Version")
            .description("The version info")
            .field(newFieldDefinition()
                .name("commit")
                .description("The commit hash")
                .type(Scalars.GraphQLString)
                .build())
            .field(newFieldDefinition()
                .name("url")
                .description("Where can we find it")
                .type(Scalars.GraphQLString)
                .build())
            .build();

        // New "field" to be put in Query
        GraphQLFieldDefinition getVersion = newFieldDefinition()
            .name("getVersion")
            .description("It should return the do something here")
            .argument(GraphQLArgument.newArgument().name("thing").type(Scalars.GraphQLString).build())
            .type(version)
            .build();

        return new GraphQLTypeVisitorStub() {
            @Override
            public TraversalControl visitGraphQLObjectType(GraphQLObjectType objectType, TraverserContext<GraphQLSchemaElement> context) {
                GraphQLCodeRegistry.Builder codeRegistry = context.getVarFromParents(GraphQLCodeRegistry.Builder.class);

                if (objectType.getName().equals("Query")) {
                    // Adding an extra field with a type and a data fetcher
                    GraphQLObjectType updatedQuery = objectType.transform(builder -> builder.field(getVersion));
                    FieldCoordinates coordinates = FieldCoordinates.coordinates(objectType.getName(), getVersion.getName());
                    codeRegistry.dataFetcher(coordinates, getVersionDataFetcher);
                    return changeNode(context, updatedQuery);
                }

                if (objectType.getName().equals("Version")) {
                    // Adding the data fetchers for all fields we just added.
                    FieldCoordinates commitCoordinates = FieldCoordinates.coordinates(objectType.getName(), "commit");
                    FieldCoordinates urlCoordinates = FieldCoordinates.coordinates(objectType.getName(), "url");
                    codeRegistry.dataFetcher(commitCoordinates, testDataFetcher);
                    codeRegistry.dataFetcher(urlCoordinates, testDataFetcher);
                    return CONTINUE;
                }

                return CONTINUE;
            }
        };
    }

    public static DataFetcher<?> getVersionDataFetcher = environment -> {
        String arguments = environment
            .getArguments()
            .entrySet()
            .stream()
            .map(entry -> "{ " + entry.getKey() + " = " + entry.getValue().toString() + " }")
            .collect(Collectors.joining(" | "));

        String result = "getVersion Fetch: %s(%s)".formatted(environment.getField(), arguments);

        LOG.info("{}", result);
        return result;
    };

    public static DataFetcher<?> testDataFetcher = environment -> {
        String arguments = environment
            .getArguments()
            .entrySet()
            .stream()
            .map(entry -> "{ " + entry.getKey() + " = " + entry.getValue().toString() + " }")
            .collect(Collectors.joining(" | "));

        String result = "Fetch: %s(%s)".formatted(environment.getField().getName(), arguments);

        LOG.info("{}", result);
        return result;
    };

}


