/**
 * Copyright (c) 2005-2012 springside.org.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.jeesite.common.web;

import com.google.common.net.HttpHeaders;
import com.jeesite.common.collect.MapUtils;
import com.jeesite.common.config.Global;
import com.jeesite.common.lang.StringUtils;
import com.jeesite.common.mapper.JsonMapper;
import com.jeesite.common.mapper.XmlMapper;
import com.jeesite.common.utils.Encodes;
import org.apache.commons.lang3.Validate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Http与Servlet工具类.
 * @author calvin/thinkgem
 * @version 2014-8-19
 */
public class Servlets {

	// -- 常用数值定义 --//
	public static final long ONE_YEAR_SECONDS = 60 * 60 * 24 * 365;
	
	// 静态文件后缀
	private final static String[] staticFiles = StringUtils.split(Global.getProperty("web.staticFile"), ",");
	
	// 动态映射URL后缀
	private final static String urlSuffix = Global.getUrlSuffix();

	/**
	 * 设置客户端缓存过期时间 的Header.
	 */
	public static void setExpiresHeader(HttpServletResponse response, long expiresSeconds) {
		// Http 1.0 header, set a fix expires date.
		response.setDateHeader(HttpHeaders.EXPIRES, System.currentTimeMillis() + expiresSeconds * 1000);
		// Http 1.1 header, set a time after now.
		response.setHeader(HttpHeaders.CACHE_CONTROL, "private, max-age=" + expiresSeconds);
	}

	/**
	 * 设置禁止客户端缓存的Header.
	 */
	public static void setNoCacheHeader(HttpServletResponse response) {
		// Http 1.0 header
		response.setDateHeader(HttpHeaders.EXPIRES, 1L);
		response.addHeader(HttpHeaders.PRAGMA, "no-cache");
		// Http 1.1 header
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0");
	}

	/**
	 * 设置LastModified Header.
	 */
	public static void setLastModifiedHeader(HttpServletResponse response, long lastModifiedDate) {
		response.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModifiedDate);
	}

	/**
	 * 设置Etag Header.
	 */
	public static void setEtag(HttpServletResponse response, String etag) {
		response.setHeader(HttpHeaders.ETAG, etag);
	}

	/**
	 * 根据浏览器If-Modified-Since Header, 计算文件是否已被修改.
	 * 
	 * 如果无修改, checkIfModify返回false ,设置304 not modify status.
	 * 
	 * @param lastModified 内容的最后修改时间.
	 */
	public static boolean checkIfModifiedSince(HttpServletRequest request, HttpServletResponse response,
			long lastModified) {
		long ifModifiedSince = request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
		if ((ifModifiedSince != -1) && (lastModified < ifModifiedSince + 1000)) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return false;
		}
		return true;
	}

	/**
	 * 根据浏览器 If-None-Match Header, 计算Etag是否已无效.
	 * 
	 * 如果Etag有效, checkIfNoneMatch返回false, 设置304 not modify status.
	 * 
	 * @param etag 内容的ETag.
	 */
	public static boolean checkIfNoneMatchEtag(HttpServletRequest request, HttpServletResponse response, String etag) {
		String headerValue = request.getHeader(HttpHeaders.IF_NONE_MATCH);
		if (headerValue != null) {
			boolean conditionSatisfied = false;
			if (!"*".equals(headerValue)) {
				StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ",");

				while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
					String currentToken = commaTokenizer.nextToken();
					if (currentToken.trim().equals(etag)) {
						conditionSatisfied = true;
					}
				}
			} else {
				conditionSatisfied = true;
			}

			if (conditionSatisfied) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				response.setHeader(HttpHeaders.ETAG, etag);
				return false;
			}
		}
		return true;
	}

	/**
	 * 设置让浏览器弹出下载对话框的Header.
	 * 
	 * @param fileName 下载后的文件名.
	 */
	public static void setFileDownloadHeader(HttpServletResponse response, String fileName) {
		try {
			// 中文文件名支持
			String encodedfileName = new String(fileName.getBytes(), "ISO8859-1");
			response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedfileName + "\"");
		} catch (UnsupportedEncodingException e) {
			e.getMessage();
		}
	}

	/**
	 * 取得带相同前缀的Request Parameters, copy from spring WebUtils.
	 * 
	 * 返回的结果的Parameter名已去除前缀.
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String, Object> getParametersStartingWith(ServletRequest request, String prefix) {
		Validate.notNull(request, "Request must not be null");
		Enumeration paramNames = request.getParameterNames();
		Map<String, Object> params = new TreeMap<String, Object>();
		String pre = prefix;
		if (pre == null) {
			pre = "";
		}
		while (paramNames != null && paramNames.hasMoreElements()) {
			String paramName = (String) paramNames.nextElement();
			if ("".equals(pre) || paramName.startsWith(pre)) {
				String unprefixed = paramName.substring(pre.length());
				String[] values = request.getParameterValues(paramName);
				if (values == null || values.length == 0) {
					values = new String[]{};
					// Do nothing, no values found at all.
				} else if (values.length > 1) {
					params.put(unprefixed, values);
				} else {
					params.put(unprefixed, values[0]);
				}
			}
		}
		return params;
	}

	/**
	 * 组合Parameters生成Query String的Parameter部分,并在paramter name上加上prefix.
	 * 
	 */
	public static String encodeParameterStringWithPrefix(Map<String, Object> params, String prefix) {
		StringBuilder queryStringBuilder = new StringBuilder();

		String pre = prefix;
		if (pre == null) {
			pre = "";
		}
		Iterator<Entry<String, Object>> it = params.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> entry = it.next();
			queryStringBuilder.append(pre).append(entry.getKey()).append("=").append(entry.getValue());
			if (it.hasNext()) {
				queryStringBuilder.append("&");
			}
		}
		return queryStringBuilder.toString();
	}

	/**
	 * 从请求对象中扩展参数数据，格式：JSON 或  param_ 开头的参数
	 * @param request 请求对象
	 * @return 返回Map对象
	 */
	public static Map<String, Object> getExtParams(ServletRequest request) {
		Map<String, Object> paramMap = null;
		String params =  StringUtils.trim(request.getParameter("params"));
		if (StringUtils.isNotBlank(params) && StringUtils.startsWith(params, "{")) {
			paramMap = (Map<String, Object>) JsonMapper.fromJsonString(params, Map.class);
		} else {
			paramMap = getParametersStartingWith(Servlets.getRequest(), "param_");
		}
		return paramMap;
	}

	/**
	 * 客户端对Http Basic验证的 Header进行编码.
	 */
	public static String encodeHttpBasic(String userName, String password) {
		String encode = userName + ":" + password;
		return "Basic " + Encodes.encodeBase64(encode.getBytes());
	}
	

	/**
	 * 是否是Ajax异步请求
	 * @param request
	 */
	public static boolean isAjaxRequest(HttpServletRequest request){
		
		String accept = request.getHeader("accept");
		if (accept != null && accept.indexOf("application/json") != -1){
			return true;
		}

		String xRequestedWith = request.getHeader("X-Requested-With");
		if (xRequestedWith != null && xRequestedWith.indexOf("XMLHttpRequest") != -1){
			return true;
		}
		
		String uri = request.getRequestURI();
		if (StringUtils.inStringIgnoreCase(uri, ".json", ".xml")){
			return true;
		}
		
		String ajax = request.getParameter("__ajax");
		if (StringUtils.inStringIgnoreCase(ajax, "json", "xml")){
			return true;
		}
		
		return false;
	}
	
	/**
	 * 返回结果JSON字符串（支持JsonP，请求参数加：__callback=回调函数名）
	 * @param result Global.TRUE or Globle.False
	 * @param message 执行消息
	 * @param data 消息数据
	 * @return JSON字符串：{result:'true',message:''}
	 */
	public static String renderResult(String result, String message) {
		return renderResult(result, message, null);
	}
	
	/**
	 * 返回结果JSON字符串（支持JsonP，请求参数加：__callback=回调函数名）
	 * @param result Global.TRUE or Globle.False
	 * @param message 执行消息
	 * @param data 消息数据
	 * @return JSON字符串：{result:'true',message:'', if map then key:value,key2:value2... else data:{} }
	 */
	@SuppressWarnings("unchecked")
	public static String renderResult(String result, String message, Object data) {
		Map<String, Object> resultMap = MapUtils.newHashMap();
		resultMap.put("result", result);
		resultMap.put("message", message);
		if (data != null){
			if (data instanceof Map){
				resultMap.putAll((Map<String, Object>)data);
			}else{
				resultMap.put("data", data);
			}
		}
		HttpServletRequest request = Servlets.getRequest();
		String uri = request.getRequestURI();
		if (StringUtils.endsWithIgnoreCase(uri, ".xml")){
			return XmlMapper.toXml(resultMap);
		}else{
			String functionName = request.getParameter("__callback");
			if (StringUtils.isNotBlank(functionName)){
				return JsonMapper.toJsonp(functionName, resultMap);
			}else{
				return JsonMapper.toJsonString(resultMap);
			}
		}
		
	}
	
	/**
	 * 直接将结果JSON字符串渲染到客户端（支持JsonP，请求参数加：__callback=回调函数名）
	 * @param response 渲染对象：{result:'true',message:'',data:{}}
	 * @param result Global.TRUE or Globle.False
	 * @param message 执行消息
	 * @return null
	 */
	public static String renderResult(HttpServletResponse response, String result, String message) {
		return renderString(response, renderResult(result, message), null);
	}
	
	/**
	 * 直接将结果JSON字符串渲染到客户端（支持JsonP，请求参数加：__callback=回调函数名）
	 * @param response 渲染对象：{result:'true',message:'',data:{}}
	 * @param result Global.TRUE or Globle.False
	 * @param message 执行消息
	 * @param data 消息数据
	 * @return null
	 */
	public static String renderResult(HttpServletResponse response, String result, String message, Object data) {
		return renderString(response, renderResult(result, message, data), null);
	}
	
	/**
	 * 将对象转换为JSON字符串渲染到客户端（支持JsonP，请求参数加：__callback=回调函数名）
	 * @param response 渲染对象
	 * @param object 待转换JSON并渲染的对象
	 * @return null
	 */
	public static String renderObject(HttpServletResponse response, Object object) {
		HttpServletRequest request = Servlets.getRequest();
		String uri = request.getRequestURI();
		if (StringUtils.endsWithIgnoreCase(uri, ".xml")){
			return XmlMapper.toXml(object);
		}else{
			String functionName = request.getParameter("__callback");
			if (StringUtils.isNotBlank(functionName)){
				return renderString(response, JsonMapper.toJsonp(functionName, object));
			}else{
				return renderString(response, JsonMapper.toJsonString(object));
			}
		}
	}
	
	/**
	 * 将字符串渲染到客户端
	 * @param response 渲染对象
	 * @param string 待渲染的字符串
	 * @return null
	 */
	public static String renderString(HttpServletResponse response, String string) {
		return renderString(response, string, null);
	}
	
	/**
	 * 将字符串渲染到客户端
	 * @param response 渲染对象
	 * @param string 待渲染的字符串
	 * @return null
	 */
	public static String renderString(HttpServletResponse response, String string, String type) {
		try {
//			response.reset(); // 先注释掉，否则以前设置的Header会被清理掉，如ajax登录设置记住我Cookie
	        response.setContentType(type == null ? "application/json" : type);
	        response.setCharacterEncoding("utf-8");
			response.getWriter().print(string);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	
	/**
	 * 获取当前请求对象
	 * @return
	 */
	public static HttpServletRequest getRequest() {
		try{
			return ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
		}catch(Exception e){
			return null;
		}
	}

	/**
     * 判断访问URI是否是静态文件请求
	 * @throws Exception 
     */
    public static boolean isStaticFile(String uri){
		if (staticFiles == null){
			try {
				throw new Exception("检测到“app.properties”中没有配置“web.staticFile”属性。配置示例：\n#静态文件后缀\n"
					+"web.staticFile=.css,.js,.png,.jpg,.gif,.jpeg,.bmp,.ico,.swf,.psd,.htc,.crx,.xpi,.exe,.ipa,.apk");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
//		if ((StringUtils.startsWith(uri, "/static/") || StringUtils.endsWithAny(uri, sfs)) 
//				&& !StringUtils.endsWithAny(uri, ".jsp") && !StringUtils.endsWithAny(uri, ".java")){
//			return true;
//		}
		if (StringUtils.endsWithAny(uri, staticFiles) && !StringUtils.endsWithAny(uri, urlSuffix)
				&& !StringUtils.endsWithAny(uri, ".jsp") && !StringUtils.endsWithAny(uri, ".java")){
			return true;
		}
		return false;
    }
}
