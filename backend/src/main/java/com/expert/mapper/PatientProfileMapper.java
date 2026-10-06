package com.expert.mapper;

import com.expert.entity.PatientProfile;
import org.apache.ibatis.annotations.*;

/**
 * 患者档案表 Mapper
 */
@Mapper
public interface PatientProfileMapper {

    @Select("SELECT id, user_id, name, gender, age, chronic_history, allergy_history, phone, " +
            "create_time, update_time FROM patient_profile WHERE user_id = #{userId} LIMIT 1")
    PatientProfile findByUserId(@Param("userId") Long userId);

    @Select("SELECT id, user_id, name, gender, age, chronic_history, allergy_history, phone, " +
            "create_time, update_time FROM patient_profile WHERE id = #{id}")
    PatientProfile findById(@Param("id") Long id);

    @Insert("INSERT INTO patient_profile (user_id, name, gender, age, chronic_history, allergy_history, phone, " +
            "create_time, update_time) VALUES (#{userId}, #{name}, #{gender}, #{age}, " +
            "#{chronicHistory}, #{allergyHistory}, #{phone}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PatientProfile profile);

    @Update("UPDATE patient_profile SET name = #{name}, gender = #{gender}, age = #{age}, " +
            "chronic_history = #{chronicHistory}, allergy_history = #{allergyHistory}, phone = #{phone}, " +
            "update_time = NOW() WHERE id = #{id}")
    int update(PatientProfile profile);

    @Delete("DELETE FROM patient_profile WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
