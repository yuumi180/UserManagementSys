package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.service.AiAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private AiAnalysisService aiAnalysisService;

    @GetMapping("/dashboard-analysis")
    public Result<?> dashboardAnalysis() {
        return Result.success(aiAnalysisService.analyzeDashboard());
    }

    @GetMapping("/log-risk-analysis")
    public Result<?> logRiskAnalysis() {
        return Result.success(aiAnalysisService.analyzeLogs());
    }
}
