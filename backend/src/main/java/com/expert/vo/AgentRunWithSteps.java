package com.expert.vo;

import com.expert.entity.AgentRun;
import com.expert.entity.AgentStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent运行记录详情（含步骤列表）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunWithSteps {

    private AgentRun run;

    private List<AgentStep> steps;
}
