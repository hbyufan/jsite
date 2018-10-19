package com.jeesite.modules.config;

import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.Filter;

/**
 * Filter 配置
 * @author liuruijun
 * @version 2017年11月30日
 */
@Configuration
public class FilterConfig {

	/**
	 * Encoding Filter
	 */
	@Bean
	@Order(1000)
	public FilterRegistrationBean characterEncodingFilter() {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(new CharacterEncodingFilter());
		bean.addInitParameter("encoding", "UTF-8");
		bean.addInitParameter("forceEncoding", "true");
		bean.addUrlPatterns("/*");
		return bean;
	}

	/**
	 * Apache Shiro Filter
	 * @throws Exception 
	 */
	@Bean
	@Order(3000)
	public FilterRegistrationBean shiroFilterProxy(ShiroFilterFactoryBean shiroFilter) throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter((Filter) shiroFilter.getObject());
		bean.addUrlPatterns("/*");
		return bean;
	}
	
}
