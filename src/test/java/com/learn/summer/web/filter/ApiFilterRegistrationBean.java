package com.learn.summer.web.filter;

import com.learn.summer.annotation.Component;
import com.learn.summer.web.FilterRegistrationBean;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class ApiFilterRegistrationBean extends FilterRegistrationBean {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public List<String> getUrlPatterns() {
        return List.of("/api");
    }

    @Override
    public Filter getFilter() {
        return (req, resp, chain) -> {
            logger.info("do filter: {}", ((HttpServletRequest) req).getRequestURI());
            chain.doFilter(req, resp);
        };
    }
}
