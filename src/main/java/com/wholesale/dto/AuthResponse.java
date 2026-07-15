package com.wholesale.dto;

public class AuthResponse {
    private boolean success;
    private String token;
    private String message;
    private String role;
    private String displayName;
    private Long userId;

    public static AuthResponse success(String token, String role, String displayName, Long userId) {
        AuthResponse r = new AuthResponse();
        r.success = true;
        r.token = token;
        r.role = role;
        r.displayName = displayName;
        r.userId = userId;
        return r;
    }

    public static AuthResponse error(String message) {
        AuthResponse r = new AuthResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
