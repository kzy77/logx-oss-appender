package org.logx.compatibility.config;

import java.util.*;

/**
 * 配置一致性验证报告
 */
public class ConfigConsistencyReport {

    private final List<ConsistentParameter> consistentParameters = new ArrayList<>();
    private final List<InconsistentParameter> inconsistentParameters = new ArrayList<>();

    public void addConsistentParameter(String framework, String parameter) {
        consistentParameters.add(new ConsistentParameter(framework, parameter, ""));
    }

    public void addConsistentParameterWithDescription(String parameter, String description) {
        consistentParameters.add(new ConsistentParameter("", parameter, description));
    }

    public void addInconsistentParameter(String framework, String parameter, String issue) {
        inconsistentParameters.add(new InconsistentParameter(framework, parameter, issue));
    }

    public List<ConsistentParameter> getConsistentParameters() {
        return new ArrayList<>(consistentParameters);
    }

    public List<InconsistentParameter> getInconsistentParameters() {
        return new ArrayList<>(inconsistentParameters);
    }

    public boolean isConsistent() {
        return inconsistentParameters.isEmpty();
    }

    public void printReport() {
        System.out.println("=== 配置一致性验证报告 ===");
        
        System.out.println("\n一致的配置参数:");
        for (ConsistentParameter param : consistentParameters) {
            if (param.getFramework().isEmpty()) {
                System.out.println("  - " + param.getParameter() + ": " + param.getDescription());
            } else {
                System.out.println("  - " + param.getFramework() + ": " + param.getParameter());
            }
        }
        
        System.out.println("\n不一致的配置参数:");
        for (InconsistentParameter param : inconsistentParameters) {
            System.out.println("  - " + param.getFramework() + ": " + param.getParameter() + 
                             " (" + param.getIssue() + ")");
        }
        
        System.out.println("\n总体结果: " + (isConsistent() ? "一致" : "不一致"));
    }

    /**
     * 合并另一个报告的结果
     */
    public void merge(ConfigConsistencyReport other) {
        this.consistentParameters.addAll(other.consistentParameters);
        this.inconsistentParameters.addAll(other.inconsistentParameters);
    }

    /**
     * 一致的配置参数
     */
    public static class ConsistentParameter {
        private final String framework;
        private final String parameter;
        private final String description;

        public ConsistentParameter(String framework, String parameter, String description) {
            this.framework = framework;
            this.parameter = parameter;
            this.description = description;
        }

        public String getFramework() {
            return framework;
        }

        public String getParameter() {
            return parameter;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 不一致的配置参数
     */
    public static class InconsistentParameter {
        private final String framework;
        private final String parameter;
        private final String issue;

        public InconsistentParameter(String framework, String parameter, String issue) {
            this.framework = framework;
            this.parameter = parameter;
            this.issue = issue;
        }

        public String getFramework() {
            return framework;
        }

        public String getParameter() {
            return parameter;
        }

        public String getIssue() {
            return issue;
        }
    }
}