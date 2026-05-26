package com.example.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private Integer age;
    private String sex;
    private String address;
    private String token;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuDTO> menus;

    @Data
    public static class MenuDTO {
        private String name;
        private String code;
        private String path;
        private String icon;
        private Integer sort;
    }
}
