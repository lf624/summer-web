package com.learn.summer.web;

import com.learn.summer.exception.ServerErrorException;
import freemarker.core.HTMLOutputFormat;
import freemarker.ext.jakarta.servlet.WebappTemplateLoader;
import freemarker.template.*;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class FreeMarkerViewResolver implements ViewResolver{
    final Logger logger = LoggerFactory.getLogger(getClass());

    final String templatePath;
    final String templateEncoding;

    final ServletContext servletContext;

    Configuration config;

    public FreeMarkerViewResolver(ServletContext servletContext, String templatePath, String templateEncoding) {
        this.templatePath = templatePath;
        this.templateEncoding = templateEncoding;
        this.servletContext = servletContext;
    }

    @Override
    public void init() {
        logger.info("init {}, set template path: {}", getClass().getName(), this.templatePath);
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_33);
        cfg.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setDefaultEncoding(this.templateEncoding);
        // 从 2.3.33 开始，WebappTemplateLoader 有了 jakarta 版本，之前版本则需要复制修改该类
        cfg.setTemplateLoader(new WebappTemplateLoader(this.servletContext, this.templatePath));
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);
        cfg.setLocalizedLookup(false);

        var ow = new DefaultObjectWrapper(Configuration.VERSION_2_3_33);
        ow.setExposeFields(true);
        cfg.setObjectWrapper(ow);
        this.config = cfg;
    }

    @Override
    public void render(String viewName, Map<String, Object> model, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Template tmp = null;
        try {
            tmp = config.getTemplate(viewName);
        }catch (Exception e) {
            throw new ServerErrorException("View not found: " + viewName);
        }
        PrintWriter pw = resp.getWriter();
        try {
            tmp.process(model, pw);
        }catch (TemplateException e) {
            throw new ServerErrorException(e);
        }
        pw.flush();
    }
}
// FreeMarker 2.3.32 及之前版本需要修改 freemarker.cache.WebappTemplateLoader
// 以使用 jakarta.servlet.ServletContext
