package com.chatapp.auth.dto;

public record LoginRequest(String email, String password, boolean keepMeSignedIn) {}
