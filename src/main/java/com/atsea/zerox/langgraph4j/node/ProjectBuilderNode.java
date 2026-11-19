package com.atsea.zerox.langgraph4j.node;

import com.atsea.zerox.core.builder.VueProjectBuilder;
import com.atsea.zerox.exception.BusinessException;
import com.atsea.zerox.exception.ErrorCode;
import com.atsea.zerox.langgraph4j.state.WorkflowContext;
import com.atsea.zerox.model.enums.CodeGenTypeEnum;
import com.atsea.zerox.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 项目构建节点
 */
@Slf4j
public class ProjectBuilderNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 项目构建");

            // 获取必要的参数
            String generatedCodeDir = context.getGeneratedCodeDir();
            CodeGenTypeEnum generationType = context.getGenerationType();
            String buildResultDir;
            // 一定是 Vue 项目类型：使用 VueProjectBuilder 进行构建
            try {
                File distDir = new File(generatedCodeDir, "dist");
                boolean buildSuccess;
                if (distDir.isDirectory() && hasFiles(distDir)) {
                    // VUE_PROJECT 的门面在 TokenStream 完成时已构建，避免重复 npm install/build。
                    log.info("检测到已有 dist 目录，跳过重复构建: {}", distDir.getAbsolutePath());
                    buildSuccess = true;
                } else {
                    VueProjectBuilder vueBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                    // 执行 Vue 项目构建（npm install + npm run build）
                    buildSuccess = vueBuilder.buildProject(generatedCodeDir);
                }
                if (buildSuccess) {
                    // 构建成功，返回 dist 目录路径
                    buildResultDir = generatedCodeDir + File.separator + "dist";
                    log.info("Vue 项目构建成功，dist 目录: {}", buildResultDir);
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败");
                }
            } catch (Exception e) {
                log.error("Vue 项目构建异常: {}", e.getMessage(), e);
                buildResultDir = generatedCodeDir; // 异常时返回原路径
            }
            // 更新状态
            context.setCurrentStep("项目构建");
            context.setBuildResultDir(buildResultDir);
            log.info("项目构建节点完成，最终目录: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }

    private static boolean hasFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            if (file.isFile() || (file.isDirectory() && hasFiles(file))) {
                return true;
            }
        }
        return false;
    }
}
