package com.example.seabattleonline.service;

public interface MatchMakingService {

    void findMatch(String player);

    void disconnect(String player);
}
