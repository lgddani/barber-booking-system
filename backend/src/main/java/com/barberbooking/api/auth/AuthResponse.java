package com.barberbooking.api.auth;

import com.barberbooking.api.users.Role;

public record AuthResponse(String token, String email, String fullName, Role role) {
}
