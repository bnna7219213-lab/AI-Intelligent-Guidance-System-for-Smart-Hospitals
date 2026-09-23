package com.expert.mapper;

import com.expert.entity.McpTool;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * MCP工具表 Mapper
 */
@Mapper
public interface McpToolMapper {

    @Select("SELECT id, tool_code, tool_name, description, input_schema, status, create_time, update_time " +
            "FROM mcp_tool ORDER BY id ASC")
    List<McpTool> findAll();

    @Select("SELECT id, tool_code, tool_name, description, input_schema, status, create_time, update_time " +
            "FROM mcp_tool WHERE status = #{status} ORDER BY id ASC")
    List<McpTool> findByStatus(@Param("status") String status);

    @Select("SELECT id, tool_code, tool_name, description, input_schema, status, create_time, update_time " +
            "FROM mcp_tool WHERE tool_code = #{toolCode} LIMIT 1")
    McpTool findByToolCode(@Param("toolCode") String toolCode);

    @Select("SELECT id, tool_code, tool_name, description, input_schema, status, create_time, update_time " +
            "FROM mcp_tool WHERE id = #{id}")
    McpTool findById(@Param("id") Long id);

    @Insert("INSERT INTO mcp_tool (tool_code, tool_name, description, input_schema, status, create_time, update_time) " +
            "VALUES (#{toolCode}, #{toolName}, #{description}, #{inputSchema}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(McpTool tool);

    @Update("UPDATE mcp_tool SET tool_name = #{toolName}, " +
            "status = #{status}, description = #{description}, input_schema = #{inputSchema}, " +
            "update_time = NOW() WHERE id = #{id}")
    int update(McpTool tool);

    @Update("UPDATE mcp_tool SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("DELETE FROM mcp_tool WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
