package com.chatapp.auth.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) {}
