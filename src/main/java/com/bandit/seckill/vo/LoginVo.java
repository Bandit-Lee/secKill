package com.bandit.seckill.vo;

import com.bandit.seckill.validator.IsMobile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

/**
 * @author Bandit
 * @createTime 2022/7/1 10:46
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVo {

    //@IsMobile
    @NotNull
    private String mobile;

    @NotNull
    @Length(min = 32)
    private String password;

}
