package com.macro.mall.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.macro.mall.auth.domain.SecurityUser;
import com.macro.mall.auth.constant.MessageConstant;
import com.macro.mall.auth.service.UmsAdminService;
import com.macro.mall.auth.service.UmsMemberService;
import com.macro.mall.common.constant.AuthConstant;
import com.macro.mall.common.domain.UserDto;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户管理业务类
 * Created by macro on 2020/6/19.
 */
@Service
public class UserServiceImpl implements UserDetailsService {


    protected final Log logger = LogFactory.getLog(this.getClass());
    @Autowired
    private UmsAdminService adminService;
    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private HttpServletRequest request;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String clientId = request.getParameter("client_id");
        UserDto userDto;
        if(AuthConstant.ADMIN_CLIENT_ID.equals(clientId)) {
            userDto = adminService.loadUserByUsername(username);

        }
        else if(AuthConstant.WECHAT_CLIENT_ID.equals(clientId)){
            //use username as openid
            userDto = memberService.loadUserByOpenid(username);
        }
        else{
            userDto = memberService.loadUserByUsername(username);
        }
        if (userDto==null) {
            throw new UsernameNotFoundException(MessageConstant.USERNAME_PASSWORD_ERROR);
        }
        userDto.setClientId(clientId);
        SecurityUser securityUser = new SecurityUser(userDto);
        //只需要openid去确认用户 统一密码去绕过验证
        if(AuthConstant.WECHAT_CLIENT_ID.equals(clientId)){
            securityUser.setPassword(BCrypt.hashpw(AuthConstant.WECHAT_CLIENT_SECRET));
        }
        if (!securityUser.isEnabled()) {
            throw new DisabledException(MessageConstant.ACCOUNT_DISABLED);
        } else if (!securityUser.isAccountNonLocked()) {
            throw new LockedException(MessageConstant.ACCOUNT_LOCKED);
        } else if (!securityUser.isAccountNonExpired()) {
            throw new AccountExpiredException(MessageConstant.ACCOUNT_EXPIRED);
        } else if (!securityUser.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException(MessageConstant.CREDENTIALS_EXPIRED);
        }
        return securityUser;
    }

}
