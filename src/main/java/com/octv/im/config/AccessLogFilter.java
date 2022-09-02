package com.octv.im.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@Component
@Slf4j
public class AccessLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String url = request.getRequestURI();
        RequestWrapper requestWrapper = null;

        StringBuilder sb = new StringBuilder();
        if (request instanceof HttpServletRequest) {
            requestWrapper = new RequestWrapper(request);
            BufferedReader bufferedReader = requestWrapper.getReader();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        }

        ResponseWrapper responseWrapper = new ResponseWrapper(response);

        if (null == requestWrapper) {
            filterChain.doFilter(request, response);
        } else {
            filterChain.doFilter(requestWrapper, responseWrapper);
        }
        log.info("========>  url {} ", url);
        log.info("========>  request param: {}", sb.toString());
        String result = new String(responseWrapper.getResponseData());
        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(result.getBytes());
        outputStream.flush();
        outputStream.close();
        // 打印response
        log.info("========>response return data: {}", result);

    }
}
