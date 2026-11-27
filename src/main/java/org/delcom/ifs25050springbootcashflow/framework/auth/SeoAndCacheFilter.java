package org.delcom.ifs25050springbootcashflow.framework.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SeoAndCacheFilter implements Filter {

    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String CACHE_HEADER_VALUE = "public, max-age=31536000";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest req) || !(response instanceof HttpServletResponse res)) {
            chain.doFilter(request, response);
            return;
        }

        String path = req.getRequestURI();
        if ("/robots.txt".equals(path)) {
            byte[] body = "User-agent: *\nAllow: /\n".getBytes(StandardCharsets.UTF_8);
            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType("text/plain; charset=UTF-8");
            res.setContentLength(body.length);
            res.getOutputStream().write(body);
            return;
        }

        if (isCacheableAsset(path)) {
            HttpServletResponse wrapped = new HttpServletResponseWrapper(res) {
                @Override
                public void setHeader(String name, String value) {
                    if (shouldOverrideCacheHeader(name)) {
                        super.setHeader(CACHE_CONTROL_HEADER, CACHE_HEADER_VALUE);
                        return;
                    }
                    if (!isLegacyCacheHeader(name)) {
                        super.setHeader(name, value);
                    }
                }

                @Override
                public void addHeader(String name, String value) {
                    if (shouldOverrideCacheHeader(name)) {
                        super.setHeader(CACHE_CONTROL_HEADER, CACHE_HEADER_VALUE);
                        return;
                    }
                    if (!isLegacyCacheHeader(name)) {
                        super.addHeader(name, value);
                    }
                }
            };
            wrapped.setHeader(CACHE_CONTROL_HEADER, CACHE_HEADER_VALUE);
            chain.doFilter(request, wrapped);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldOverrideCacheHeader(String name) {
        return CACHE_CONTROL_HEADER.equalsIgnoreCase(name);
    }

    private boolean isLegacyCacheHeader(String name) {
        return "Pragma".equalsIgnoreCase(name) || "Expires".equalsIgnoreCase(name);
    }

    private boolean isCacheableAsset(String path) {
        return path.startsWith("/VAADIN/")
                || path.startsWith("/lumo/")
                || path.startsWith("/aura/")
                || path.startsWith("/icons/")
                || "/styles.css".equals(path);
    }
}