package nl.basjes.experiments.springgqltest;


import graphql.GraphQL;
import graphql.GraphQL.Builder;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeVisitor;
import graphql.schema.SchemaTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GreetingController {

    @Autowired
    private FixedGraphQlSource fixedGraphQlSource;

    @Autowired
    private DefaultSchemaResourceGraphQlSourceBuilder defaultBuilder;

    @QueryMapping
    public String greeting() {
        return "Hello World";
    }

    @GetMapping("/test")
    public String test() {

        GraphQLSchema schema = this.fixedGraphQlSource.schema();
        GraphQL graphQL = this.fixedGraphQlSource.graphQl();

        GraphQLTypeVisitor graphQLTypeVisitor = MyDynamicGraphQLApi.addVersionToSchema();

        GraphQLSchema graphQLSchema = SchemaTransformer.transformSchema(schema, graphQLTypeVisitor);
        this.fixedGraphQlSource.setGraphQlSchema(graphQLSchema);

        Builder builder = this.defaultBuilder.getgGraphQLBuilder();
        Builder newBuilder = builder.schema(graphQLSchema);
        GraphQL newGraphQL = newBuilder.build();

        this.fixedGraphQlSource.setGraphQl(newGraphQL);

        return "test add";
    }
}
