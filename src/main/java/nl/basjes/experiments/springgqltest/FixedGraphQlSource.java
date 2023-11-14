package nl.basjes.experiments.springgqltest;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.springframework.graphql.execution.GraphQlSource;

/**
 * @author fuzi1996
 * @since 2.3
 */
public class FixedGraphQlSource implements GraphQlSource {

    private GraphQL graphQl;

    private GraphQLSchema schema;

    FixedGraphQlSource(GraphQL graphQl, GraphQLSchema schema) {
        this.graphQl = graphQl;
        this.schema = schema;
    }

    @Override
    public GraphQL graphQl() {
        return this.graphQl;
    }

    @Override
    public GraphQLSchema schema() {
        return this.schema;
    }

    public void setGraphQl(GraphQL graphQl) {
        this.graphQl = graphQl;
    }

    public void setGraphQlSchema(GraphQLSchema schema) {
        this.schema = schema;
    }
}
