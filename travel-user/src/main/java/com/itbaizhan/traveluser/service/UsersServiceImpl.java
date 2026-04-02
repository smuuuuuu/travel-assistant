package com.itbaizhan.traveluser.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.itbaizhan.travelcommon.info.UserInfo;
import com.itbaizhan.travelcommon.pojo.RegisterUser;
import com.itbaizhan.travelcommon.pojo.Users;
import com.itbaizhan.travelcommon.result.BusException;
import com.itbaizhan.travelcommon.result.CodeEnum;
import com.itbaizhan.travelcommon.result.Common;
import com.itbaizhan.travelcommon.service.UsersService;
import com.itbaizhan.travelcommon.util.Md5Util;
import com.itbaizhan.traveluser.mapper.UsersMapper;
import com.itbaizhan.travelcommon.util.JWTUtil;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
* @author smuuuu
* @description 针对表【users(用户基本信息表)】的数据库操作Service实现
* @createDate 2025-10-10 19:42:45
*/
@DubboService
public class UsersServiceImpl implements UsersService {

    @Autowired
    private UsersMapper usersMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Override
    public String login(String username, String password,boolean remember) {
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        Users users = usersMapper.selectOne(queryWrapper);
        if(users == null){
            throw new BusException(CodeEnum.LOGIN_USER_STATUS_ERROR);
        }
        boolean verify = Md5Util.verify(password, users.getPassword_hash());
        if(!verify){
            throw new BusException(CodeEnum.LOGIN_NAME_PASSWORD_ERROR);
        }
        String token = JWTUtil.sign(users.getId(), username);

        return token;
    }

    @Override
    public String loginWithCode(String phone, String code) {
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        Users users = usersMapper.selectOne(queryWrapper);
        if(users == null){
            throw new BusException(CodeEnum.LOGIN_USER_STATUS_ERROR);
        }
        checkLoginCode(phone,code);
        return JWTUtil.sign(users.getId(), users.getUsername());
    }

    @Override
    @Transactional
    public void register(RegisterUser registerUser) {
        checkLoginCode(registerUser.getPhone(), registerUser.getCode());

        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", registerUser.getUsername());
        List<Users> usersList = usersMapper.selectList(queryWrapper);
        if(usersList != null && usersList.size() > 0){
            throw new BusException(CodeEnum.REGISTER_REPEAT_NAME_ERROR);
        }

        Users users = new Users();
        users.setUsername(registerUser.getUsername());
        users.setPassword_hash(Md5Util.encode(registerUser.getPassword()));
        users.setPhone(registerUser.getPhone());
        users.setEmail(registerUser.getEmail());
        users.setEmail_verified(1);
        users.setPhone_verified(1);
        users.setLast_login_ip(registerUser.getIp());
        users.setLast_login_at(LocalDateTime.now());
        users.setAvatar_url(Common.DEFAULT_AVATAR.getValue());

        usersMapper.insert(users);
    }

    @Override
    public void checkPhone(String phone) {
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        List<Users> users = usersMapper.selectList(queryWrapper);
        if(users != null && users.size() > 0){
            throw new BusException(CodeEnum.REGISTER_REPEAT_PHONE_ERROR);
        }
    }

    @Override
    public void saveLoginCode(String phone, String code) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("loginCode:"+phone,code,3000, TimeUnit.SECONDS);
    }

    @Override
    public void checkLoginCode(String phone, String code) {
        Object object = redisTemplate.opsForValue().get("loginCode:" + phone);
        if(!code.equals(object)){
            throw new BusException(CodeEnum.REGISTER_CODE_ERROR);
        }
    }
    @Override
    public UserInfo getUserInfo(String token) {
        Long userId = getUserId(token);
        Users users = usersMapper.selectById(userId);
        UserInfo userInfo = new UserInfo();
        BeanUtils.copyProperties(users, userInfo);
        return userInfo;
    }
    public Long getUserId(String token) {
        Map<String, Object> verify = JWTUtil.verify(token);
        return (Long) verify.get("userId");
    }
}




