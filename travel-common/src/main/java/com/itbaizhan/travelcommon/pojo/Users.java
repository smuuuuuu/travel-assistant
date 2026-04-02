package com.itbaizhan.travelcommon.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户基本信息表
 * @TableName users
 */
@TableName(value ="users")
@Data
public class Users implements Serializable {
    /**
     * 用户ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 用户名
     */
    @TableField(value = "username")
    private String username;

    /**
     * 邮箱地址
     */
    @TableField(value = "email")
    private String email;

    /**
     * 手机号码
     */
    @TableField(value = "phone")
    private String phone;

    /**
     * 密码哈希值
     */
    @TableField(value = "password_hash")
    private String password_hash;

    /**
     * 昵称
     */
    @TableField(value = "nickname")
    private String nickname;

    /**
     * 头像URL
     */
    @TableField(value = "avatar_url")
    private String avatar_url;

    /**
     * 性别
     */
    @TableField(value = "gender")
    private String gender;

    /**
     * 生日
     */
    @TableField(value = "birthday")
    private LocalDate birthday;

    /**
     * 个人简介
     */
    @TableField(value = "bio")
    private String bio;

    /**
     * 账户状态
     */
    @TableField(value = "status")
    private String status;

    /**
     * 邮箱是否已验证
     */
    @TableField(value = "email_verified")
    private Integer email_verified;

    /**
     * 手机是否已验证
     */
    @TableField(value = "phone_verified")
    private Integer phone_verified;

    /**
     * 最后登录时间
     */
    @TableField(value = "last_login_at")
    private LocalDateTime last_login_at;

    /**
     * 最后登录IP
     */
    @TableField(value = "last_login_ip")
    private String last_login_ip;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime created_at;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updated_at;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        Users other = (Users) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getUsername() == null ? other.getUsername() == null : this.getUsername().equals(other.getUsername()))
            && (this.getEmail() == null ? other.getEmail() == null : this.getEmail().equals(other.getEmail()))
            && (this.getPhone() == null ? other.getPhone() == null : this.getPhone().equals(other.getPhone()))
            && (this.getPassword_hash() == null ? other.getPassword_hash() == null : this.getPassword_hash().equals(other.getPassword_hash()))
            && (this.getNickname() == null ? other.getNickname() == null : this.getNickname().equals(other.getNickname()))
            && (this.getAvatar_url() == null ? other.getAvatar_url() == null : this.getAvatar_url().equals(other.getAvatar_url()))
            && (this.getGender() == null ? other.getGender() == null : this.getGender().equals(other.getGender()))
            && (this.getBirthday() == null ? other.getBirthday() == null : this.getBirthday().equals(other.getBirthday()))
            && (this.getBio() == null ? other.getBio() == null : this.getBio().equals(other.getBio()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getEmail_verified() == null ? other.getEmail_verified() == null : this.getEmail_verified().equals(other.getEmail_verified()))
            && (this.getPhone_verified() == null ? other.getPhone_verified() == null : this.getPhone_verified().equals(other.getPhone_verified()))
            && (this.getLast_login_at() == null ? other.getLast_login_at() == null : this.getLast_login_at().equals(other.getLast_login_at()))
            && (this.getLast_login_ip() == null ? other.getLast_login_ip() == null : this.getLast_login_ip().equals(other.getLast_login_ip()))
            && (this.getCreated_at() == null ? other.getCreated_at() == null : this.getCreated_at().equals(other.getCreated_at()))
            && (this.getUpdated_at() == null ? other.getUpdated_at() == null : this.getUpdated_at().equals(other.getUpdated_at()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getUsername() == null) ? 0 : getUsername().hashCode());
        result = prime * result + ((getEmail() == null) ? 0 : getEmail().hashCode());
        result = prime * result + ((getPhone() == null) ? 0 : getPhone().hashCode());
        result = prime * result + ((getPassword_hash() == null) ? 0 : getPassword_hash().hashCode());
        result = prime * result + ((getNickname() == null) ? 0 : getNickname().hashCode());
        result = prime * result + ((getAvatar_url() == null) ? 0 : getAvatar_url().hashCode());
        result = prime * result + ((getGender() == null) ? 0 : getGender().hashCode());
        result = prime * result + ((getBirthday() == null) ? 0 : getBirthday().hashCode());
        result = prime * result + ((getBio() == null) ? 0 : getBio().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getEmail_verified() == null) ? 0 : getEmail_verified().hashCode());
        result = prime * result + ((getPhone_verified() == null) ? 0 : getPhone_verified().hashCode());
        result = prime * result + ((getLast_login_at() == null) ? 0 : getLast_login_at().hashCode());
        result = prime * result + ((getLast_login_ip() == null) ? 0 : getLast_login_ip().hashCode());
        result = prime * result + ((getCreated_at() == null) ? 0 : getCreated_at().hashCode());
        result = prime * result + ((getUpdated_at() == null) ? 0 : getUpdated_at().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", username=").append(username);
        sb.append(", email=").append(email);
        sb.append(", phone=").append(phone);
        sb.append(", password_hash=").append(password_hash);
        sb.append(", nickname=").append(nickname);
        sb.append(", avatar_url=").append(avatar_url);
        sb.append(", gender=").append(gender);
        sb.append(", birthday=").append(birthday);
        sb.append(", bio=").append(bio);
        sb.append(", status=").append(status);
        sb.append(", email_verified=").append(email_verified);
        sb.append(", phone_verified=").append(phone_verified);
        sb.append(", last_login_at=").append(last_login_at);
        sb.append(", last_login_ip=").append(last_login_ip);
        sb.append(", created_at=").append(created_at);
        sb.append(", updated_at=").append(updated_at);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}