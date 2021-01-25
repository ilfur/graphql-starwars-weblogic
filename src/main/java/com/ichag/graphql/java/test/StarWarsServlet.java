/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ichag.graphql.java.test;

import com.ichag.graphql.java.test.util.JsonKit;
import java.io.IOException;
import java.io.PrintWriter;
import com.ichag.graphql.java.test.util.QueryParameters;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataloader.DataLoaderRegistry;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;

import static graphql.ExecutionInput.newExecutionInput;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions;
import static graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions.newOptions;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import static java.util.Arrays.asList;

/**
 *
 * @author MPFEIFER
 */
public class StarWarsServlet extends HttpServlet {

    static GraphQLSchema starWarsSchema = null;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //
        // this builds out the parameters we need like the graphql query from the http request
        QueryParameters parameters = QueryParameters.from(request);
        if (parameters.getQuery() == null) {
            //
            // how to handle nonsensical requests is up to your application
            response.setStatus(400);
            return;
        }

        ExecutionInput.Builder executionInput = newExecutionInput()
                .query(parameters.getQuery())
                .operationName(parameters.getOperationName())
                .variables(parameters.getVariables());

        //
        // the context object is something that means something to down stream code.  It is instructions
        // from yourself to your other code such as DataFetchers.  The engine passes this on unchanged and
        // makes it available to inner code
        //
        // the graphql guidance says  :
        //
        //  - GraphQL should be placed after all authentication middleware, so that you
        //  - have access to the same session and user information you would in your
        //  - HTTP endpoint handlers.
        //
        StarWarsWiring.Context context = new StarWarsWiring.Context();
        executionInput.context(context);

        //
        // you need a schema in order to execute queries
        GraphQLSchema schema = buildStarWarsSchema();

        //
        // This example uses the DataLoader technique to ensure that the most efficient
        // loading of data (in this case StarWars characters) happens.  We pass that to data
        // fetchers via the graphql context object.
        //
        DataLoaderRegistry dataLoaderRegistry = context.getDataLoaderRegistry();

        DataLoaderDispatcherInstrumentation dlInstrumentation
                = new DataLoaderDispatcherInstrumentation(newOptions().includeStatistics(true));
        
        

//(dataLoaderRegistry, newOptions().includeStatistics(true));
        //Instrumentation instrumentation = new ChainedInstrumentation(
        //        asList(new TracingInstrumentation(), dlInstrumentation)
        //);

        // finally you build a runtime graphql object and execute the query
        GraphQL graphQL = GraphQL
                .newGraphQL(schema)
                // instrumentation is pluggable
                //.instrumentation(dlInstrumentation)
                .build();
        ExecutionResult executionResult = graphQL.execute(executionInput.build());

        returnAsJson(response, executionResult);
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private void returnAsJson(HttpServletResponse response, ExecutionResult executionResult) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        JsonKit.toJson(response, executionResult.toSpecification());
    }

    private GraphQLSchema buildStarWarsSchema() {
        //
        // using lazy loading here ensure we can debug the schema generation
        // and potentially get "wired" components that cant be accessed
        // statically.
        //
        // A full application would use a dependency injection framework (like Spring)
        // to manage that lifecycle.
        //
        if (starWarsSchema == null) {

            //
            // reads a file that provides the schema types
            //
            Reader streamReader = loadSchemaFile("starwars.graphqls");
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

            // finally combine the logical schema with the physical runtime
            starWarsSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, wiring);
        }
        return starWarsSchema;
    }

    @SuppressWarnings("SameParameterValue")
    private Reader loadSchemaFile(String name) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        return new InputStreamReader(stream);
    }
}
