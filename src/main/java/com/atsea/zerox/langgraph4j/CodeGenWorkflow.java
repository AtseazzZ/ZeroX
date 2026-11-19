package com.atsea.zerox.langgraph4j;

import com.atsea.zerox.exception.BusinessException;
import com.atsea.zerox.exception.ErrorCode;
import com.atsea.zerox.langgraph4j.model.QualityResult;
import com.atsea.zerox.langgraph4j.node.*;
import com.atsea.zerox.langgraph4j.state.WorkflowContext;
import com.atsea.zerox.model.enums.CodeGenTypeEnum;
import com.atsea.zerox.monitor.MonitorContext;
import com.atsea.zerox.monitor.MonitorContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

/**
 * 代码生成工作流（实际可用）
 */
@Slf4j
public class CodeGenWorkflow {

    /**
     * 创建完整的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加节点 - 使用完整实现的节点
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // 添加边
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
                    // 新增质检条件边：根据质检结果决定下一步
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",   // 质检通过且需要构建
                                    "skip_build", END,            // 质检通过但跳过构建
                                    "fail", "code_generator"      // 质检失败，重新生成
                            ))
                    .addEdge("project_builder", END)

                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    /**
     * 执行工作流
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        return executeWorkflow(originalPrompt, null, null);
    }

    /**
     * 执行工作流并绑定应用 ID。
     */
    public WorkflowContext executeWorkflow(String originalPrompt, Long appId) {
        return executeWorkflow(originalPrompt, appId, null);
    }

    /**
     * 执行工作流并绑定应用 ID 与应用既有代码生成类型。
     */
    public WorkflowContext executeWorkflow(String originalPrompt, Long appId, CodeGenTypeEnum preferredGenerationType) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();

        // 初始化 WorkflowContext
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .appId(appId)
                .preferredGenerationType(preferredGenerationType)
                .currentStep("初始化")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            // 显示当前状态
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("代码生成工作流执行完成！");
        return finalContext;
    }

    /**
     * 执行工作流（Flux 流式输出版本）
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt) {
        return executeWorkflowWithFlux(originalPrompt, null, null);
    }

    /**
     * 执行工作流（Flux 流式输出版本），绑定应用 ID。
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId) {
        return executeWorkflowWithFlux(originalPrompt, appId, null);
    }

    /**
     * 执行工作流（Flux 流式输出版本），绑定应用 ID 与应用既有代码生成类型。
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId,
                                                CodeGenTypeEnum preferredGenerationType) {
        return executeWorkflowWithFlux(originalPrompt, appId, preferredGenerationType, null, null);
    }

    /**
     * 执行工作流并在线程内设置监控上下文；后台任务结束时触发清理回调。
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt, Long appId,
                                                CodeGenTypeEnum preferredGenerationType,
                                                MonitorContext monitorContext, Runnable onTermination) {
        return Flux.create(sink -> {
            AtomicReference<Thread> workerRef = new AtomicReference<>();
            AtomicBoolean terminationNotified = new AtomicBoolean();
            Runnable notifyTermination = () -> {
                if (terminationNotified.compareAndSet(false, true) && onTermination != null) {
                    onTermination.run();
                }
            };
            sink.onCancel(() -> {
                Thread worker = workerRef.get();
                if (worker != null) {
                    worker.interrupt();
                }
            });
            if (sink.isCancelled()) {
                notifyTermination.run();
                return;
            }

            Thread worker = Thread.ofVirtual().unstarted(() -> {
                if (monitorContext != null) {
                    MonitorContextHolder.setContext(monitorContext);
                }
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .originalPrompt(originalPrompt)
                            .appId(appId)
                            .preferredGenerationType(preferredGenerationType)
                            .currentStep("初始化")
                            .build();
                    // AppController 会将 Flux 每个元素包装成 {d: ...}，因此这里返回普通文本。
                    sink.next("工作流已启动：正在分析需求");
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());

                    int stepCounter = 1;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        log.info("--- 第 {} 步完成 ---", stepCounter);
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            sink.next(String.format("工作流进度：第 %d 步 - %s", stepCounter,
                                    currentContext.getCurrentStep()));
                            log.info("当前步骤上下文: {}", currentContext);
                        }
                        stepCounter++;
                    }
                    sink.next("工作流已完成：正在刷新应用预览");
                    log.info("代码生成工作流执行完成！");
                    sink.complete();
                } catch (Exception e) {
                    log.error("工作流执行失败: {}", e.getMessage(), e);
                    if (!sink.isCancelled()) {
                        sink.next("工作流执行失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage()));
                        sink.error(e);
                    }
                } finally {
                    MonitorContextHolder.clearContext();
                    notifyTermination.run();
                }
            });
            workerRef.set(worker);
            if (sink.isCancelled()) {
                worker.interrupt();
                notifyTermination.run();
                return;
            }
            try {
                worker.start();
            } catch (RuntimeException e) {
                notifyTermination.run();
                sink.error(e);
            }
        });
    }

    /**
     * 执行工作流（SSE 流式输出版本）
     */
    public SseEmitter executeWorkflowWithSse(String originalPrompt) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        Thread.startVirtualThread(() -> {
            try {
                CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                WorkflowContext initialContext = WorkflowContext.builder()
                        .originalPrompt(originalPrompt)
                        .currentStep("初始化")
                        .build();
                sendSseEvent(emitter, "workflow_start", Map.of(
                        "message", "开始执行代码生成工作流",
                        "originalPrompt", originalPrompt
                ));
                GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                log.info("工作流图:\n{}", graph.content());

                int stepCounter = 1;
                for (NodeOutput<MessagesState<String>> step : workflow.stream(
                        Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                    log.info("--- 第 {} 步完成 ---", stepCounter);
                    WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                    if (currentContext != null) {
                        sendSseEvent(emitter, "step_completed", Map.of(
                                "stepNumber", stepCounter,
                                "currentStep", currentContext.getCurrentStep()
                        ));
                        log.info("当前步骤上下文: {}", currentContext);
                    }
                    stepCounter++;
                }
                sendSseEvent(emitter, "workflow_completed", Map.of(
                        "message", "代码生成工作流执行完成！"
                ));
                log.info("代码生成工作流执行完成！");
                emitter.complete();
            } catch (Exception e) {
                log.error("工作流执行失败: {}", e.getMessage(), e);
                sendSseEvent(emitter, "workflow_error", Map.of(
                        "error", e.getMessage(),
                        "message", "工作流执行失败"
                ));
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    /**
     * 发送 SSE 事件的辅助方法
     */
    private void sendSseEvent(SseEmitter emitter, String eventType, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
        } catch (IOException e) {
            log.error("发送 SSE 事件失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据质检结果决定下一步
     *
     * @param state
     * @return
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();
        // 如果质检失败，重新生成代码
        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("代码质检失败，需要重新生成代码");
            return "fail";
        }
        // 质检通过，使用原有的构建路由逻辑
        log.info("代码质检通过，继续后续流程");
        return routeBuildOrSkip(state);
    }

    /**
     * 根据代码生成类型决定是否需要构建
     *
     * @param state
     * @return
     */
    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        // HTML 和 MULTI_FILE 类型不需要构建，直接结束
        if (generationType == CodeGenTypeEnum.HTML || generationType == CodeGenTypeEnum.MULTI_FILE) {
            return "skip_build";
        }
        // VUE_PROJECT 需要构建
        return "build";
    }
}
