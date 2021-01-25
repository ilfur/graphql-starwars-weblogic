/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ichag.graphql.java.test;

import com.ichag.graphql.java.test.data.Episode;
import com.ichag.graphql.java.test.data.FilmCharacter;
import com.ichag.graphql.java.test.data.Human;
import com.ichag.graphql.java.test.data.StarWarsData;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.EnumValuesProvider;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * This is our wiring used to put behaviour to a graphql type.
 */
public class StarWarsWiring {

    /**
     * The context object is passed to each level of a graphql query and in this case it contains
     * the data loader registry.  This allows us to keep our data loaders per request since
     * they cache data and cross request caches are often not what you want.
     */
    public static class Context {

        final DataLoaderRegistry dataLoaderRegistry;

        public Context() {
            this.dataLoaderRegistry = new DataLoaderRegistry();
            dataLoaderRegistry.register("characters", newCharacterDataLoader());
        }

        public DataLoaderRegistry getDataLoaderRegistry() {
            return dataLoaderRegistry;
        }

        public DataLoader<String, Object> getCharacterDataLoader() {
            return dataLoaderRegistry.getDataLoader("characters");
        }
    }

    private static List<Object> getCharacterDataViaBatchHTTPApi(List<String> keys) {
        return keys.stream().map(StarWarsData::getCharacterData).collect(Collectors.toList());
    }

    // a batch loader function that will be called with N or more keys for batch loading
    private static BatchLoader<String, Object> characterBatchLoader = keys -> {

        //
        // we are using multi threading here.  Imagine if getCharacterDataViaBatchHTTPApi was
        // actually a HTTP call - its not here - but it could be done asynchronously as
        // a batch API call say
        //
        //
        // direct return of values
        //CompletableFuture.completedFuture(getCharacterDataViaBatchHTTPApi(keys))
        //
        // or
        //
        // async supply of values
        
        //return CompletableFuture.supplyAsync(() -> getCharacterDataViaBatchHTTPApi(keys));
        return CompletableFuture.completedFuture(getCharacterDataViaBatchHTTPApi(keys));

    };

    // a data loader for characters that points to the character batch loader
    private static DataLoader<String, Object> newCharacterDataLoader() {
        return new DataLoader<>(characterBatchLoader);
    }

    // we define the normal StarWars data fetchers so we can point them at our data loader
    static DataFetcher humanDataFetcher = environment -> {
        String id = environment.getArgument("id");
        //Context ctx = environment.getContext();
        //return ctx.getCharacterDataLoader().load(id);
        return StarWarsData.getCharacterData(id);
    };
        

    static DataFetcher droidDataFetcher = environment -> {
        String id = environment.getArgument("id");
        //Context ctx = environment.getContext();
        //return ctx.getCharacterDataLoader().load(id);
        return StarWarsData.getCharacterData(id);
    };

    static DataFetcher heroDataFetcher = environment -> {
        //Context ctx = environment.getContext();
        //return ctx.getCharacterDataLoader().load("2001"); // R2D2
        return StarWarsData.getCharacterData("2001");

    };

    static DataFetcher friendsDataFetcher = environment -> {
        FilmCharacter character = environment.getSource();
        List<String> friendIds = character.getFriends();
        List<FilmCharacter> chars = new java.util.ArrayList();
        for (String id : friendIds) {
            FilmCharacter fc = (FilmCharacter)(StarWarsData.getCharacterData(id));
            chars.add(fc);
        }
        //Context ctx = environment.getContext();
        //return ctx.getCharacterDataLoader().loadMany(friendIds);
        return chars;
    };

    /**
     * Character in the graphql type system is an Interface and something needs
     * to decide that concrete graphql object type to return
     */
    static TypeResolver characterTypeResolver = environment -> {
        FilmCharacter character = (FilmCharacter) environment.getObject();
        if (character instanceof Human) {
            return (GraphQLObjectType) environment.getSchema().getType("Human");
        } else {
            return (GraphQLObjectType) environment.getSchema().getType("Droid");
        }
    };

    static EnumValuesProvider episodeResolver = Episode::valueOf;
}