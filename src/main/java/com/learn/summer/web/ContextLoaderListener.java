package com.learn.summer.web;

import com.learn.summer.context.AnnotationConfigApplicationContext;
import com.learn.summer.context.ApplicationContext;
import com.learn.summer.exception.NestedRuntimeException;
import com.learn.summer.io.PropertyResolver;
import com.learn.summer.web.utils.WebUtils;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextLoaderListener implements ServletContextListener {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("init {}.", getClass().getName());
        var servletContext = sce.getServletContext();
        // 传入 ServletContext
        WebMvcConfiguration.setServletContext(servletContext);

        var propertyResolver = WebUtils.createPropertyResolver();
        String encoding = propertyResolver.getProperty("${summer.web.character-encoding:UTF-8}");
        servletContext.setRequestCharacterEncoding(encoding);
        servletContext.setResponseCharacterEncoding(encoding);
        // 创建 IoC 容器
        var applicationContext = createApplicationContext(servletContext.getInitParameter("configuration"), propertyResolver);
        // 注册 Filter
        WebUtils.registerFilters(servletContext);
        // 注册 DispatcherServlet
        WebUtils.registerDispatcherServlet(servletContext, propertyResolver);
        servletContext.setAttribute("applicationContext", applicationContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if(sce.getServletContext().getContext("applicationContext") instanceof ApplicationContext appContext) {
            appContext.close();
        }
    }

    ApplicationContext createApplicationContext(String configName, PropertyResolver propertyResolver) {
        logger.info("init ApplicationContext by configuration: {}", configName);
        if(configName == null || configName.isEmpty())
            throw new NestedRuntimeException("Missing init param name: configuration");
        Class<?> config;
        try {
            config = Class.forName(configName);
        }catch (ClassNotFoundException e) {
            throw new NestedRuntimeException("Could not load class from init param 'configuration': " + configName);
        }
        return new AnnotationConfigApplicationContext(config, propertyResolver);
    }
}
