package com.example.demo.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Permission;
import com.example.demo.entity.Role;
import com.example.demo.entity.RolePermission;
import com.example.demo.entity.UserRole;
import com.example.demo.mapper.PermissionMapper;
import com.example.demo.mapper.RoleMapper;
import com.example.demo.mapper.RolePermissionMapper;
import com.example.demo.mapper.UserRoleMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/role")
public class RoleController {

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @GetMapping
    public Result<?> findpage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Role> rolePage = roleMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<Role>lambdaQuery().orderByDesc(Role::getCreateTime)
        );
        return Result.success(rolePage);
    }

    @GetMapping("/all")
    public Result<?> findAll() {
        List<Role> roles = roleMapper.selectList(null);
        return Result.success(roles);
    }

    @PostMapping
    public Result<?> save(@RequestBody Role role) {
        role.setCreateTime(new Date());
        roleMapper.insert(role);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody Role role) {
        roleMapper.updateById(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        roleMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/user/{userId}")
    public Result<?> getUserRoles(@PathVariable Integer userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
                Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId)
        );
        return Result.success(userRoles);
    }

    @PostMapping("/assign")
    public Result<?> assignRole(@RequestParam Integer userId, @RequestParam Integer roleId) {
        // 先删除用户原有角色
        userRoleMapper.delete(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId));

        // 分配新角色
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);

        return Result.success();
    }

    @GetMapping("/permissions")
    public Result<?> getPermissionTree() {
        List<Permission> permissions = permissionMapper.selectList(null);
        List<Map<String, Object>> tree = permissions.stream()
                .sorted(Comparator.comparing(permission -> permission.getSort() == null ? 0 : permission.getSort()))
                .map(permission -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", permission.getId());
                    node.put("label", permission.getName());
                    node.put("code", permission.getCode());
                    node.put("type", permission.getType());
                    node.put("path", permission.getPath());
                    node.put("children", new ArrayList<>());
                    return node;
                })
                .collect(Collectors.toList());
        return Result.success(tree);
    }

    @GetMapping("/{id}/permissions")
    public Result<?> getRolePermissions(@PathVariable Integer id) {
        List<Integer> permissionIds = rolePermissionMapper.selectList(
                        Wrappers.<RolePermission>lambdaQuery().eq(RolePermission::getRoleId, id)
                )
                .stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
        return Result.success(permissionIds);
    }

    @PostMapping("/{id}/permissions")
    public Result<?> saveRolePermissions(@PathVariable Integer id, @RequestBody List<Integer> permissionIds) {
        rolePermissionMapper.delete(Wrappers.<RolePermission>lambdaQuery().eq(RolePermission::getRoleId, id));
        if (permissionIds != null) {
            for (Integer permissionId : permissionIds) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(id);
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
        return Result.success();
    }
}
