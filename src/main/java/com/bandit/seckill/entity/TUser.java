package com.bandit.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户表
 *
 * @author LiChao
 * @since 2022-03-02
 */
@TableName("t_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(value = "用户表", description = "用户表")
public class TUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID,手机号码 **/
    @ApiModelProperty("用户ID,手机号码")
    private Long id;

    private String nickname;

    /** MD5(MD5(pass明文+固定salt)+salt) **/
    @ApiModelProperty("MD5(MD5(pass明文+固定salt)+salt)")
    private String password;

    private String salt;

    /** 头像 **/
    @ApiModelProperty("头像")
    private String head;

    /** 注册时间 **/
    @ApiModelProperty("注册时间")
    private Date registerDate;

    /** 最后一次登录事件 **/
    @ApiModelProperty("最后一次登录事件")
    private Date lastLoginDate;

    /** 登录次数 **/
    @ApiModelProperty("登录次数")
    private Integer loginCount;
}
