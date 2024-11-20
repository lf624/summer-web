package com.learn.summer.web;

import com.learn.summer.annotation.*;
import com.learn.summer.context.ApplicationContext;
import com.learn.summer.context.ConfigurableApplicationContext;
import com.learn.summer.exception.ErrorResponseException;
import com.learn.summer.exception.NestedRuntimeException;
import com.learn.summer.exception.ServerErrorException;
import com.learn.summer.exception.ServerWebInputException;
import com.learn.summer.io.PropertyResolver;
import com.learn.summer.utils.ClassUtils;
import com.learn.summer.web.utils.JsonUtils;
import com.learn.summer.web.utils.PathUtils;
import com.learn.summer.web.utils.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {
    final Logger logger = LoggerFactory.getLogger(getClass());

    ApplicationContext applicationContext;
    ViewResolver viewResolver;

    String resourcePath;
    String faviconPath;

    List<Dispatcher> getDispatchers = new ArrayList<>();
    List<Dispatcher> postDispatchers = new ArrayList<>();

    public DispatcherServlet(ApplicationContext applicationContext, PropertyResolver propertyResolver) {
        this.applicationContext = applicationContext;
        this.viewResolver = applicationContext.getBean(ViewResolver.class);
        this.resourcePath = propertyResolver.getProperty("${summer.web.static-path:/static/}");
        this.faviconPath = propertyResolver.getProperty("${summer.web.favicon-path:/favicon.ico}");
        if(!this.resourcePath.endsWith("/"))
            this.resourcePath += "/";
    }

    @Override
    public void init() throws ServletException {
        logger.info("init {}", getClass().getName());
        // scan @Controller and @RestController
        for(var def : ((ConfigurableApplicationContext)this.applicationContext).findBeanDefinitions(Object.class)) {
            Class<?> beanClass = def.getBeanClass();
            Object bean = def.getRequiredInstance();
            Controller controller = beanClass.getAnnotation(Controller.class);
            RestController restController = beanClass.getAnnotation(RestController.class);
            if(controller != null && restController != null) {
                throw new ServletException("@Controller and @RestController on both define in class: " + beanClass.getName());
            }
            if(controller != null)
                addController(false, def.getName(), bean);
            if(restController != null)
                addController(true, def.getName(), bean);
        }
    }

    @Override
    public void destroy() {
        this.applicationContext.close();
    }

    void addController(boolean isRest, String name, Object instance) throws ServletException{
        logger.info("add {}controller '{}':{}", isRest ? "REST" : "MVC", name, instance.getClass().getName());
        addMethods(isRest, name, instance, instance.getClass());
    }

    void addMethods(boolean isRest, String name, Object instance, Class<?> type) throws ServletException{
        for(Method m : type.getDeclaredMethods()) {
            GetMapping get = m.getAnnotation(GetMapping.class);
            if(get != null) {
                checkMethod(m);
                this.getDispatchers.add(new Dispatcher("GET", isRest, instance, m, get.value()));
            }
            PostMapping post = m.getAnnotation(PostMapping.class);
            if(post != null) {
                checkMethod(m);
                this.postDispatchers.add(new Dispatcher("POST", isRest, instance, m, post.value()));
            }
        }
        Class<?> superClass = type.getSuperclass();
        if(superClass != null)
            addMethods(isRest, name, instance, superClass);
    }

    void checkMethod(Method m) throws ServletException{
        int mod = m.getModifiers();
        if(Modifier.isStatic(mod))
            throw new ServletException("Can not do URL mapping to static method: " + m);
        m.setAccessible(true);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getRequestURI();
        if(url.equals(this.faviconPath) || url.startsWith(this.resourcePath)) {
            doResource(url, req, resp);
        } else {
            doService(req, resp, this.getDispatchers);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doService(req, resp, this.postDispatchers);
    }

    void doService(HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws ServletException, IOException{
        String url = req.getRequestURI();
        try {
            doService(url, req, resp, dispatchers);
        } catch (ErrorResponseException e) {
            logger.warn("process request failed with status " + e.statusCode + " : " + url, e);
            if(!resp.isCommitted()) {
                resp.resetBuffer();
                resp.sendError(e.statusCode);
            }
        } catch (RuntimeException | ServletException | IOException e) {
            logger.warn("process request failed: " + url, e);
            throw e;
        } catch (Exception e) {
            logger.warn("process request failed: " + url, e);
            throw new NestedRuntimeException(e);
        }
    }

    void doService(String url, HttpServletRequest req, HttpServletResponse resp, List<Dispatcher> dispatchers) throws Exception{
        // 依次匹配每个 Dispatcher 的 URL
        for(Dispatcher dispatcher : dispatchers) {
            Result result = dispatcher.process(url, req, resp);
            // 匹配成功并处理后
            if(result.processed()) {
                // 对结果进行处理
                Object r = result.returnObject();
                if(dispatcher.isRest) {
                    // 发送 rest 响应
                    if(!resp.isCommitted()) {
                        resp.setContentType("application/json");
                    }
                    if(dispatcher.isResponseBody) {
                        if(r instanceof String s) {
                            PrintWriter pw = resp.getWriter();
                            pw.write(s);
                            pw.flush();
                        } else if (r instanceof byte[] data){
                            ServletOutputStream output = resp.getOutputStream();
                            output.write(data);
                            output.flush();
                        } else {
                            throw new ServletException("Unable to process REST result when handle url: " + url);
                        }
                    } else if (!dispatcher.isVoid) {
                        PrintWriter pw = resp.getWriter();
                        logger.info("r is others: {}", r);
                        JsonUtils.writeJson(pw, r);
                        pw.flush();
                    }
                } else {
                    // 处理 MVC
                    if(!resp.isCommitted()) {
                        resp.setContentType("text/html");
                    }
                    if(r instanceof String s) {
                        if(dispatcher.isResponseBody) {
                            PrintWriter pw = resp.getWriter();
                            pw.write(s);
                            pw.flush();
                        } else if (s.startsWith("redirect:")) {
                            resp.sendRedirect(s.substring(9));
                        } else {
                            throw new ServletException("Unable to process String result when handle url: " + url);
                        }
                    } else if (r instanceof byte[] data) {
                        if(dispatcher.isResponseBody) {
                            ServletOutputStream output = resp.getOutputStream();
                            output.write(data);
                            output.flush();
                        } else {
                            throw new ServletException("Unable to process String result when handle url: " + url);
                        }
                    } else if (r instanceof ModelAndView mv) {
                        String view = mv.getViewName();
                        if(view.startsWith("redirect:")) {
                            resp.sendRedirect(view.substring(9));
                        } else {
                            this.viewResolver.render(view, mv.getModel(), req, resp);
                        }
                    } else if(!dispatcher.isVoid && r != null) {
                        throw new ServletException("Unable to process " + r.getClass().getName() + " result when handle url: " + url);
                    }
                }
                return;
            }
        }
        // 未匹配到任何 Dispatcher
        resp.sendError(404, "Not Found");
    }

    void doResource(String url, HttpServletRequest req, HttpServletResponse resp) throws IOException{
        ServletContext ctx = req.getServletContext();
        try(InputStream input = ctx.getResourceAsStream(url)) {
            if(input == null) {
                resp.sendError(404, "Not Found");
            } else {
                // 猜 content type
                String file = url;
                int n = url.lastIndexOf("/");
                if(n >= 0)
                    file = url.substring(n + 1);
                String mime = ctx.getMimeType(file);
                if(mime == null)
                    mime = "application/octet-stream";
                resp.setContentType(mime);
                ServletOutputStream output = resp.getOutputStream();
                input.transferTo(output);
                output.flush();
            }
        }
    }

    static class Dispatcher {
        final Logger logger = LoggerFactory.getLogger(getClass());

        final static Result NOT_PROCESSED = new Result(false, null);

        boolean isRest;
        boolean isResponseBody;
        boolean isVoid;
        Pattern urlPattern;
        Object controller;
        Method handlerMethod;
        Param[] methodParameters;

        public Dispatcher(String httpMethod, boolean isRest, Object controller, Method method, String path) throws ServletException{
            this.isRest = isRest;
            this.isResponseBody = method.getAnnotation(ResponseBody.class) != null;
            this.isVoid = method.getReturnType() == void.class;
            this.urlPattern = PathUtils.compile(path);
            this.controller = controller;
            this.handlerMethod = method;
            Parameter[] params = method.getParameters();
            Annotation[][] annos = method.getParameterAnnotations();
            this.methodParameters = new Param[params.length];
            for(int i = 0; i < params.length; i++) {
                this.methodParameters[i] = new Param(httpMethod, method, params[i], annos[i]);
            }

            logger.atDebug().log("mapping {} to {}.{}", urlPattern, controller.getClass().getName(), method.getName());
            if(logger.isDebugEnabled()) {
                for(var p : methodParameters)
                    logger.debug("> parameter: {}", p);
            }
        }

        // 调用url对应的处理方法
        Result process(String url, HttpServletRequest req, HttpServletResponse resp) throws Exception{
            Matcher matcher = urlPattern.matcher(url);
            if(matcher.matches()) {
                Object[] arguments = new Object[this.methodParameters.length];
                for(int i = 0; i < arguments.length; i++) {
                    Param param = methodParameters[i];
                    arguments[i] = switch (param.paramType) {
                        case PATH_VARIABLE -> {
                            try {
                                String s = matcher.group(param.name);
                                yield convertToType(param.classType, s);
                            }catch (IllegalArgumentException e) {
                                throw new ServerWebInputException("Path variable " + param.name + " not found.");
                            }
                        }
                        case REQUEST_BODY -> {
                            BufferedReader reader = req.getReader();
                            yield JsonUtils.readJson(reader, param.classType);
                        }
                        case REQUEST_PARAM -> {
                            String s = getOrDefault(req, param.name, param.defaultValue);
                            yield convertToType(param.classType, s);
                        }
                        case SERVLET_VARIABLE -> {
                            Class<?> classType = param.classType;
                            if(classType == HttpServletRequest.class) {
                                yield req;
                            } else if (classType == HttpServletResponse.class) {
                                yield resp;
                            } else if (classType == HttpSession.class) {
                                yield req.getSession();
                            } else if (classType == ServletContext.class) {
                                yield req.getServletContext();
                            } else {
                                throw new ServerErrorException("Could not determine argument type " + classType);
                            }
                        }
                    };
                }
                Object result = null;
                try {
                    result = this.handlerMethod.invoke(controller, arguments);
                }catch (InvocationTargetException e) {
                    Throwable t = e.getCause(); // 获得更具体的异常
                    if(t instanceof Exception ex) {
                        throw ex;
                    }
                    throw e;
                }catch (ReflectiveOperationException e) {
                    throw new ServerErrorException(e);
                }
                return new Result(true, result);
            }
            return NOT_PROCESSED;
        }

        Object convertToType(Class<?> clazz, String s) {
            if(clazz == String.class) {
                return s;
            } else if(clazz == boolean.class || clazz == Boolean.class) {
                return Boolean.valueOf(s);
            } else if(clazz == int.class || clazz == Integer.class) {
                return Integer.valueOf(s);
            } else if(clazz == float.class || clazz == Float.class) {
                return Float.valueOf(s);
            } else if(clazz == long.class || clazz == Long.class) {
                return Long.valueOf(s);
            } else if(clazz == byte.class || clazz == Byte.class) {
                return Byte.valueOf(s);
            } else if(clazz == short.class || clazz == Short.class) {
                return Short.valueOf(s);
            } else if (clazz == double.class || clazz == Double.class) {
                return Double.valueOf(s);
            } else {
                throw new ServerErrorException("Could not determine argument type: " + clazz);
            }
        }

        String getOrDefault(HttpServletRequest req, String name, String defaultValue) {
            String s = req.getParameter(name);
            if(s == null) {
                if(WebUtils.DEFAULT_PARAM_VALUE.equals(defaultValue))
                    throw new ServerWebInputException("Request parameter '" + name + "' not found.");
                return defaultValue;
            }
            return s;
        }
    }

    static enum ParamType {
        PATH_VARIABLE, REQUEST_PARAM, REQUEST_BODY, SERVLET_VARIABLE;
    }

    static class Param {
        String name;
        ParamType paramType;
        Class<?> classType;
        String defaultValue;

        public Param(String httpMethod, Method method, Parameter parameter, Annotation[] annotations) throws ServletException{
            PathVariable pv = ClassUtils.getAnnotation(annotations, PathVariable.class);
            RequestParam rp = ClassUtils.getAnnotation(annotations, RequestParam.class);
            RequestBody rb = ClassUtils.getAnnotation(annotations, RequestBody.class);
            int total = (pv == null ? 0 : 1) + (rp == null ? 0 : 1) + (rb == null ? 0 : 1);
            if(total > 1)
                throw new ServletException("Annotation @PathVariable, @RequestParam and @RequestBody cannot be combined at method: " + method);
            this.classType = parameter.getType();
            if(pv != null) {
                this.name = pv.value();
                this.paramType = ParamType.PATH_VARIABLE;
            } else if (rp != null) {
                this.name = rp.value();
                this.defaultValue = rp.defaultValue();
                this.paramType = ParamType.REQUEST_PARAM;
            } else if (rb != null) {
                this.paramType = ParamType.REQUEST_BODY;
            } else {
                this.paramType = ParamType.SERVLET_VARIABLE;
                if(this.classType != HttpServletRequest.class && this.classType != HttpServletResponse.class
                        && this.classType != HttpSession.class && this.classType != ServletContext.class) {
                    throw new ServerErrorException("(Missing annotation?) Unsupported argument type: "
                            + classType + " in method: " + method);
                }
            }
        }

        @Override
        public String toString() {
            return "Param [name=" + name + ", paramType=" + paramType + ", classType=" + classType
                    + ", defaultValue=" + defaultValue + "]";
        }
    }

    static record Result(boolean processed, Object returnObject) {}
}
