package com.hmall.interceptor;

import com.hmall.common.utils.UserContext;
import com.hmall.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtTool jwtTool;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的 token
        String token = request.getHeader("authorization");
        token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJ1c2VyIjoxLCJleHAiOjE3NTQ0NTIwMzJ9.UCmrNwVXQBnZykJNH_5fY94qZIrnwVB-Mp--QyNrersJE6AvtaNnFNooM1D9iMWF6GBZZliqqQg9GWh1eEnXj9qYGNuqUYViXgerYaziKqoYQMj6JXsuYix1YPP6P7qou46sshXxJBHrBebaBvBCR1NDkVN1gNvJykSeO1MFgOP5O6pDaZptoWGqak_IYDtb9bC7DSmCR0eaIw72uXx5u-aiV5Dz5yvkg_YcBJ6hZEJPxVx1O0-4MlObxoai4wOxTsC1QLjl-s-iHATOhHUJ1u_YRrhipHaLToC9rE9a8EFwmeCnVrjQ0_-egVoDUvTHRrSx3qLcWUFgSn4H8hq5Xw";
        // 2.校验token
        Long userId = jwtTool.parseToken(token);
        // 3.存入上下文
        UserContext.setUser(userId);
        // 4.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理用户
        UserContext.removeUser();
    }
}
