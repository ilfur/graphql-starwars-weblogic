/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ichag.graphql.java.test.data;

import java.util.List;

public class Droid implements FilmCharacter {
    final String id;
    final String name;
    final List<String> friends;
    final List<Integer> appearsIn;
    final String primaryFunction;

    public Droid(String id, String name, List<String> friends, List<Integer> appearsIn, String primaryFunction) {
        this.id = id;
        this.name = name;
        this.friends = friends;
        this.appearsIn = appearsIn;
        this.primaryFunction = primaryFunction;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getFriends() {
        return friends;
    }

    public List<Integer> getAppearsIn() {
        return appearsIn;
    }

    public String getPrimaryFunction() {
        return primaryFunction;
    }

    @Override
    public String toString() {
        return "Droid{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}