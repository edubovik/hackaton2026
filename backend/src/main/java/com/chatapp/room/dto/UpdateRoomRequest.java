package com.chatapp.room.dto;

public record UpdateRoomRequest(String name, String description, Boolean isPublic) {}
