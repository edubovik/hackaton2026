package com.chatapp.auth.dto;

public record ResetPasswordRequest(String token, String newPassword) {}
