package com.learn.summer.web.utils;

import com.learn.summer.context.ApplicationContextUtils;
import com.learn.summer.io.PropertyResolver;
import com.learn.summer.utils.ClassPathUtils;
import com.learn.summer.utils.YamlUtils;
import com.learn.summer.web.DispatcherServlet;
import com.learn.summer.web.FilterRegistrationBean;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.*;

public class WebUtils {
    public static final String DEFAULT_PARAM_VALUE = "\0\t\0\t\0"; // 无具体含义

    static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

    static final String CONFIG_APP_YML = "/application.yml";
    static final String CONFIG_APP_PROP = "/application.properties";

    public static void registerDispatcherServlet(ServletContext servletContext, PropertyResolver propertyResolver) {
        var dispatcherServlet = new DispatcherServlet(ApplicationContextUtils.getRequiredApplicationContext(), propertyResolver);
        logger.info("register servlet {} for URL '/'", dispatcherServlet.getClass().getName());
        var dispatcherReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        dispatcherReg.addMapping("/");
        dispatcherReg.setLoadOnStartup(0);
    }

    public static void registerFilters(ServletContext servletContext) {
        var applicationContext = ApplicationContextUtils.getRequiredApplicationContext();
        for(var filterRegBean : applicationContext.getBeans(FilterRegistrationBean.class)) {
            List<String> urlPatterns = filterRegBean.getUrlPatterns();
            if(urlPatterns == null || urlPatterns.isEmpty()) {
                throw new IllegalArgumentException("No url patterns for: " + filterRegBean.getClass().getName());
            }
            var filter = Objects.requireNonNull(filterRegBean.getFilter(),
                    "FilterRegistrationBean.getFilter() must not return null");
            logger.info("register filter `{}` {} for URLs: {}", filterRegBean.getName(), filter.getClass().getName(),
                    String.join(", ", urlPatterns));
            var filterReg = servletContext.addFilter(filterRegBean.getName(), filter);
            filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true,
                    urlPatterns.toArray(String[]::new));
        }
    }

    // 从 /application.yml 或 /application.properties 文件中读取配置
    public static PropertyResolver createPropertyResolver() {
        Properties props = new Properties();
        try {
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlain(CONFIG_APP_YML);
            logger.info("load config: {}", CONFIG_APP_YML);
            for(String key : ymlMap.keySet()) {
                Object value = ymlMap.get(key);
                if(value instanceof String strValue)
                    props.put(key, strValue);
            }
        } catch (UncheckedIOException e) {
            if(e.getCause() instanceof FileNotFoundException) {
                ClassPathUtils.readInputStream(CONFIG_APP_PROP, input -> {
                    logger.info("load config: {}", CONFIG_APP_PROP);
                    props.load(input);
                    return true;
                });
            }
        }
        return new PropertyResolver(props);
    }
}
