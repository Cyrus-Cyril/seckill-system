package com.example.seckill.user.controller;

import com.example.seckill.common.ApiResponse;
import com.example.seckill.user.dto.LoginResponse;
import com.example.seckill.user.dto.UserLoginRequest;
import com.example.seckill.user.dto.UserProfileResponse;
import com.example.seckill.user.dto.UserRegisterRequest;
import com.example.seckill.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        Long userId = userService.register(request);
        return ApiResponse.success(userId);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody UserLoginRequest request,
                                            HttpSession session) {
        LoginResponse response = userService.login(request);
        session.setAttribute("loginUserId", response.getUserId());
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserProfileResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @GetMapping("/check")
    public ApiResponse<Boolean> checkUsername(@RequestParam String username) {
        return ApiResponse.success(userService.existsByUsername(username));
    }
}