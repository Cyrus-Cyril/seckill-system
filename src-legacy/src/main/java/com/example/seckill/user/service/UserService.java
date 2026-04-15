package com.example.seckill.user.service;

import com.example.seckill.user.dto.LoginResponse;
import com.example.seckill.user.dto.UserLoginRequest;
import com.example.seckill.user.dto.UserProfileResponse;
import com.example.seckill.user.dto.UserRegisterRequest;
import com.example.seckill.user.entity.User;
import com.example.seckill.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Long register(UserRegisterRequest request) {
        User existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(1);

        userMapper.insert(user);
        return user.getId();
    }

    public LoginResponse login(UserLoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        boolean match = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!match) {
            throw new RuntimeException("密码错误");
        }

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname()
        );
    }

    public UserProfileResponse getById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getStatus()
        );
    }

    public boolean existsByUsername(String username) {
        return userMapper.findByUsername(username) != null;
    }
}