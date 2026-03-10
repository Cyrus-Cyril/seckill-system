package com.example.seckill.user.mapper;

import com.example.seckill.user.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("""
        SELECT id, username, password, nickname, status, create_time, update_time
        FROM tb_user
        WHERE username = #{username}
        LIMIT 1
        """)
    User findByUsername(String username);

    @Select("""
        SELECT id, username, password, nickname, status, create_time, update_time
        FROM tb_user
        WHERE id = #{id}
        LIMIT 1
        """)
    User findById(Long id);

    @Insert("""
        INSERT INTO tb_user(username, password, nickname, status, create_time, update_time)
        VALUES(#{username}, #{password}, #{nickname}, #{status}, NOW(), NOW())
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}