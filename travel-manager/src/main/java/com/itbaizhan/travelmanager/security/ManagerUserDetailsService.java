package com.itbaizhan.travelmanager.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.itbaizhan.travelcommon.pojo.ManagerUser;
import com.itbaizhan.travelmanager.mapper.ManagerUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagerUserDetailsService implements UserDetailsService {

    private final ManagerUserMapper managerUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ManagerUser u = managerUserMapper.selectOne(new QueryWrapper<ManagerUser>().eq("username", username));
        if (u == null) {
            throw new UsernameNotFoundException("user not found");
        }
        return new ManagerPrincipal(u);
    }
}
