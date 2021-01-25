/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ichag.graphql.java.test;

import static com.ichag.graphql.java.test.StarWarsServlet.starWarsSchema;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.kickstart.servlet.GraphQLHttpServlet;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.servlet.annotation.WebServlet;

/**
 *
 * @author MPFEIFER
 */
@WebServlet(name = "StarWarsSimpleServlet", urlPatterns = {"graphqlsw2/*"}, loadOnStartup = 1)
public class StarWarsSimpleServlet extends GraphQLHttpServlet {

    @Override
    protected GraphQLConfiguration getConfiguration() {
        return GraphQLConfiguration.with(createSchema()).build();
    }

    private GraphQLSchema createSchema() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("starwars.graphqls");
        Reader streamReader = new InputStreamReader(stream);
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(streamReader);

        RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("hero", StarWarsWiring.heroDataFetcher)
                        .dataFetcher("human", StarWarsWiring.humanDataFetcher)
                        .dataFetcher("droid", StarWarsWiring.droidDataFetcher)
                )
                .type(newTypeWiring("Human")
                        .dataFetcher("friends", StarWarsWiring.friendsDataFetcher)
                )
                .type(newTypeWiring("Droid")
                        .dataFetcher("friends", StarWarsWiring.friendsDataFetcher)
                )
                .type(newTypeWiring("Character")
                        .typeResolver(StarWarsWiring.characterTypeResolver)
                )
                .type(newTypeWiring("Episode")
                        .enumValues(StarWarsWiring.episodeResolver)
                )
                .build();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, wiring);
    }

}
