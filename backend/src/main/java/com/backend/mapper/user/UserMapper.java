package com.backend.mapper.user;

import com.backend.domain.chat.ChatRoom;
import com.backend.domain.user.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("""
                INSERT INTO user
                (email, password, nick_name, phone_number)
                VALUES (#{email}, #{password}, #{nickName}, #{phoneNumber})
            """)
    int insertUser(User user);

    @Select("""
                SELECT *
                FROM user
                WHERE email = #{email}
            """)
    User selectUserByEmail(String email);

    @Select("""
                SELECT *
                FROM user
                WHERE nick_name = #{nickName}
            """)
    User selectUserByNickName(String nickName);

    @Select("""
                SELECT name
                FROM authority
                WHERE user_id = #{userId}
            """)
    List<String> selectAuthoritiesByUserId(Integer userId);

    @Select("""
                SELECT email
                FROM user
                WHERE id = #{userId}
            """)
    String selectEmailById(Integer userId);

    // -- ChatService
    @Select("""
            SELECT nick_name
            FROM user
            WHERE id = #{sellerId}
            """)
    String selectSellerName(ChatRoom chatRoom);

    @Select("""
                SELECT *
                FROM user
                WHERE id = #{userId}
            """)
    User selectUserById(Integer id);

    @Delete(
            """
                        DELETE FROM user
                        WHERE id = #{id}
                    """
    )
    int deleteUserById(Integer id);

    @Update("""
                UPDATE user
                SET password = #{password},
                    nick_name = #{nickName}
                WHERE id = #{id}
            """)
    int updateUser(User user);

//    @Select("""
//                <script>
//                SELECT id, email, nick_name, inserted
//                FROM user
//                ORDER BY id DESC
//                LIMIT #{offset}, 10
//                </script>
//            """)
//    List<User> selectUserList(int offset);

    @Select("""
                <script>
                SELECT id, email, nick_name, inserted, black_count
                FROM user
                    <bind name="pattern" value="'%' + keyword + '%'" />
                WHERE
                    (email LIKE #{pattern} OR nick_name LIKE #{pattern})
                    <if test="type == 'black'">
                        AND black_count > 4
                    </if>
                ORDER BY id DESC
                LIMIT #{offset}, 10
                </script>
            """)
    List<User> selectUserList(int offset, String type, String keyword);

    @Delete("""
                DELETE FROM authority
                WHERE user_id = #{userId}
            """)
    int deleteAuthorityById(Integer userId);

    @Select("""
                SELECT COUNT(*) FROM user
            """)
    int selectTotalUserCount();

    @Insert("""
                INSERT INTO code
                (phone_number, code)
                VALUES (#{phoneNumber}, #{code})
            """)
    void insertCode(String phoneNumber, String code);

    @Select("""
                SELECT code AS verificationCode
                FROM code
                WHERE phone_number = #{phoneNumber}
            """)
    Integer selectCodeByPhoneNumber(String phoneNumber);

    @Delete("""
                DELETE FROM code
                WHERE phone_number = #{phone_number}
            """)
    int deleteCodeByPhoneNumber(String phoneNumber);

    @Update("""
                UPDATE user
                SET black_count = black_count + 1
                WHERE id = #{id}
            """)
    int updateBlackCountByUserId(Integer id);

    @Select("""
                SELECT email
                FROM user
                WHERE phone_number = #{phoneNumber}
            """)
    String selectEmailByPhoneNumber(String phoneNumber);

    @Update("""
                UPDATE user
                SET password = #{password}
                WHERE email = #{email}
            """)
    int updatePassword(User user);
}
