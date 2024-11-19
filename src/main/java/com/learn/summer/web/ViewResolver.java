package com.learn.summer.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

public interface ViewResolver {
    // 初始化 ViewResolver
    void init();
    // 渲染
    void render(String viewName, Map<String, Object> model, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;
}
