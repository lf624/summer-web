package com.learn.summer.web.utils;

import jakarta.servlet.ServletException;

import java.util.regex.Pattern;

public class PathUtils {

    // 为路径生成 pattern，/user/{id} --> ^/user/(?<id>[^/]*)$
    public static Pattern compile(String path) throws ServletException{
        // 使用 named-capturing group
        String regPath = path.replaceAll("\\{([a-zA-Z][a-zA-Z0-9]*)}", "(?<$1>[^/]*)");
        if(regPath.indexOf('{') >= 0 || regPath.indexOf('}') >=0) {
            throw new ServletException("Invalid path: " + path);
        }
        return Pattern.compile("^" + regPath + "$");
    }
}
