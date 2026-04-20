package com.chatapp.room.dto;

public record CreateRoomRequest(String name, String description, boolean isPublic) {}
