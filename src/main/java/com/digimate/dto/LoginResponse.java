package com.digimate.dto;

public record LoginResponse(String token, String type, long expiresInMinutes){
}
