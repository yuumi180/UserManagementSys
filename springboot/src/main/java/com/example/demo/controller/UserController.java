package com.example.demo.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.annotation.Log;
import com.example.demo.annotation.RateLimit;
import com.example.demo.common.Result;
import com.example.demo.dto.LoginDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserRole;
import com.example.demo.entity.RolePermission;
import com.example.demo.mapper.PermissionMapper;
import com.example.demo.mapper.RoleMapper;
import com.example.demo.mapper.RolePermissionMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.mapper.UserRoleMapper;
import com.example.demo.utils.ExcelUtils;
import com.example.demo.utils.JwtUtils;
import com.example.demo.utils.PasswordEncoder;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    UserMapper userMapper;

    @Resource
    RoleMapper roleMapper;

    @Resource
    UserRoleMapper userRoleMapper;

    @Resource
    PermissionMapper permissionMapper;

    @Resource
    RolePermissionMapper rolePermissionMapper;
    
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Resource
    JwtUtils jwtUtils;

    @Log("用户登录")
    @RateLimit(maxRequests = 5, timeWindow = 60)  // 1 分钟最多 5 次
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
        
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        userDTO.setToken(token);
        fillUserPermissionInfo(user.getId(), userDTO);
        
        return Result.success(userDTO);
    }

    @Log("新增用户")
    @PostMapping
    public Result<?> save(@RequestBody User user){
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(PasswordEncoder.encode(user.getPassword()));
        }
        userMapper.insert(user);
        return Result.success();
    }

    @Log("修改用户")
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

    @Log("删除用户")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id){
        userMapper.deleteById(id);
        // 清除缓存
        redisTemplate.delete("user:" + id);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success();
    }

    @Log("批量删除")
    @DeleteMapping("/batch")
    public Result<?> batchDelete(@RequestBody java.util.List<Long> ids) {
        for (Long id : ids) {
            userMapper.deleteById(id);
        }
        return Result.success();
    }

    @Log("导出 Excel")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        List<User> userList = userMapper.selectList(null);
        ExcelUtils.exportUsers(userList, response);
    }

    @Log("导入 Excel")
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

    /**
     * 根据 ID 查询用户详情（带 Redis 缓存）
     */
    @GetMapping("/{id}")
    public Result<?> getUserById(@PathVariable Long id) {
        // 1. 先查 Redis
        String key = "user:" + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return Result.success(cached);
        }
        
        // 2. Redis 没有，查数据库
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error("400", "用户不存在");
        }
        
        // 3. 写入 Redis（设置 30 分钟过期）
        redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
        
        return Result.success(user);
    }

    private void fillUserPermissionInfo(Integer userId, UserDTO userDTO) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId)
        );
        List<Integer> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            userDTO.setRoles(new ArrayList<>());
            userDTO.setPermissions(new ArrayList<>());
            userDTO.setMenus(new ArrayList<>());
            return;
        }

        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        userDTO.setRoles(roles.stream().map(Role::getCode).collect(Collectors.toList()));

        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(
                Wrappers.<RolePermission>lambdaQuery().in(RolePermission::getRoleId, roleIds)
        );
        List<Integer> permissionIds = rolePermissions.stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());
        if (permissionIds.isEmpty()) {
            userDTO.setPermissions(new ArrayList<>());
            userDTO.setMenus(new ArrayList<>());
            return;
        }

        List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
        userDTO.setPermissions(permissions.stream().map(Permission::getCode).collect(Collectors.toList()));

        List<UserDTO.MenuDTO> menus = permissions.stream()
                .filter(permission -> "MENU".equals(permission.getType()))
                .sorted(Comparator.comparing(permission -> permission.getSort() == null ? 0 : permission.getSort()))
                .map(permission -> {
                    UserDTO.MenuDTO menu = new UserDTO.MenuDTO();
                    menu.setName(permission.getName());
                    menu.setCode(permission.getCode());
                    menu.setPath(permission.getPath());
                    menu.setIcon(permission.getIcon());
                    menu.setSort(permission.getSort());
                    return menu;
                })
                .collect(Collectors.toList());
        userDTO.setMenus(menus);
    }
}
