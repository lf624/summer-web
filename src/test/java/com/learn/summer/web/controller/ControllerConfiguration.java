package com.learn.summer.web.controller;

import com.learn.summer.annotation.Configuration;
import com.learn.summer.annotation.Import;
import com.learn.summer.web.WebMvcConfiguration;
import com.learn.summer.web.filter.ApiFilterRegistrationBean;
import com.learn.summer.web.filter.MvcFilter;

@Configuration
@Import({WebMvcConfiguration.class, MvcFilter.class, ApiFilterRegistrationBean.class})
public class ControllerConfiguration {
}
