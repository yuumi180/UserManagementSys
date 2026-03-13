package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.OperationLog;
import com.example.demo.mapper.OperationLogMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/log")
public class LogController {

    @Resource
    private OperationLogMapper operationLogMapper;

    @GetMapping
    public Result<?> findpage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<OperationLog> logPage = operationLogMapper.selectPage(
                new Page<>(pageNum, pageSize),
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<OperationLog>lambdaQuery()
                        .orderByDesc(OperationLog::getCreateTime)
        );
        return Result.success(logPage);
    }
}
