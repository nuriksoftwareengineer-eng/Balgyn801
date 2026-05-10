package com.nurba.java.service;

import com.nurba.java.dto.request.LoginRequest;
import com.nurba.java.dto.request.RegisterRequest;
import com.nurba.java.dto.responce.AuthMeResponse;
import com.nurba.java.dto.responce.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthMeResponse me(String email);
}
