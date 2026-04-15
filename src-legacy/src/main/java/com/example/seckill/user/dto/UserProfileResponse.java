package com.example.seckill.user.dto;

public class UserProfileResponse {

    private Long id;
    private String username;
    private String nickname;
    private Integer status;

    public UserProfileResponse() {
    }

    public UserProfileResponse(Long id, String username, String nickname, Integer status) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}