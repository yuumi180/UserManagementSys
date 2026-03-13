package com.example.demo.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.utils.ExcelUtils;
import com.example.demo.utils.PasswordEncoder;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    UserMapper userMapper;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, loginDTO.getUsername()));
        
        if (user == null) {
            return Result.error("400", "用户名不存在");
        }
        
        if (!PasswordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            return Result.error("400", "密码错误");
        }
        
        String token = UUID.randomUUID().toString().replace("-", "");
        
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        userDTO.setToken(token);
        
        return Result.success(userDTO);
    }

    @PostMapping
    public Result<?> save(@RequestBody User user){
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordEncoder.encode(user.getPassword()));
        }
        userMapper.insert(user);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody User user){
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userMapper.updateById(user);
        return Result.success();
    }

    @GetMapping
    public Result<?> findpage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search){
        Page<User> userPage = userMapper.selectPage(new Page<>(pageNum, pageSize), 
                Wrappers.<User>lambdaQuery()
                        .like(User::getUsername, search)
                        .or()
                        .like(User::getNickname, search));
        return Result.success(userPage);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id){
        userMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success();
    }

    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody java.util.List<Long> ids) {
        for (Long id : ids) {
            userMapper.deleteById(id);
        }
        return Result.success();
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        List<User> userList = userMapper.selectList(null);
        ExcelUtils.exportUsers(userList, response);
    }

    @PostMapping("/import")
    public Result<?> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("400", "文件为空");
        }

        List<User> userList = ExcelUtils.importUsers(file.getInputStream());
        
        int successCount = 0;
        for (User user : userList) {
            try {
                // 检查用户名是否存在
                User existUser = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, user.getUsername()));
                
                if (existUser == null) {
                    userMapper.insert(user);
                    successCount++;
                }
            } catch (Exception e) {
                // 跳过导入失败的用户
            }
        }

        return Result.success("成功导入 " + successCount + " 条数据");
    }
}
