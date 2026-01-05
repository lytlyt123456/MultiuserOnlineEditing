package com.example.multiuser_online_editing.controller.collaboration;

import com.example.multiuser_online_editing.controller.ApiResponse;
import com.example.multiuser_online_editing.entity.collaboration.Task;
import com.example.multiuser_online_editing.entity.collaboration.TaskPriority;
import com.example.multiuser_online_editing.entity.collaboration.TaskStatus;
import com.example.multiuser_online_editing.service.document_management.DocumentService;
import com.example.multiuser_online_editing.service.collaboration.TaskService;
import com.example.multiuser_online_editing.service.user_management.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private DocumentService documentService;

    /**
     * 创建任务
     */
    @PostMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Object>> createTask(
            @PathVariable Long documentId,
            @RequestBody CreateTaskRequest request) {
        try {

            Task task = taskService.createTask(
                    documentId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getPriority(),
                    request.getDueDate()
            );

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("taskId", task.getId());
            responseData.put("title", task.getTitle());
            responseData.put("status", task.getStatus());

            return ResponseEntity.ok(ApiResponse.success("任务创建成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 更新任务状态
     */
    @PutMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<Object>> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestBody UpdateTaskStatusRequest request) {
        try {
            Task task = taskService.updateTaskStatus(taskId, request.getStatus());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("taskId", task.getId());
            responseData.put("title", task.getTitle());
            responseData.put("status", task.getStatus());

            return ResponseEntity.ok(ApiResponse.success("任务状态更新成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取文档的所有任务
     */
    @GetMapping("/document/{documentId}")
    public ResponseEntity<ApiResponse<Object>> getDocumentTasks(@PathVariable Long documentId) {
        try {
            List<Task> tasks = taskService.getDocumentTasks(documentId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("tasks", tasksToMaps(tasks));

            return ResponseEntity.ok(ApiResponse.success("获取任务成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取用户相关的任务
     */
    @GetMapping("/my-tasks")
    public ResponseEntity<ApiResponse<Object>> getUserTasks() {
        try {
            Long currentUserId = userService.getCurrentUserId();

            List<Task> tasks = taskService.getUserTasks(currentUserId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("tasks", tasksToMaps(tasks));

            return ResponseEntity.ok(ApiResponse.success("获取用户任务成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取待处理任务
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Object>> getPendingTasks() {
        try {
            Long currentUserId = userService.getCurrentUserId();

            // 这里需要在TaskService中添加对应方法
            List<Task> tasks = taskService.getPendingTasks(currentUserId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("tasks", tasksToMaps(tasks));

            return ResponseEntity.ok(ApiResponse.success("获取待处理任务成功", responseData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // 工具方法：任务列表转换为Map
    private List<Map<String, Object>> tasksToMaps(List<Task> tasks) {
        return tasks.stream()
                .map(this::taskToMap)
                .toList();
    }

    private Map<String, Object> taskToMap(Task task) {
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getId());
        taskMap.put("title", task.getTitle());
        taskMap.put("description", task.getDescription());
        taskMap.put("status", task.getStatus());
        taskMap.put("priority", task.getPriority());
        taskMap.put("dueDate", task.getDueDate());
        taskMap.put("createdAt", task.getCreatedAt());
        taskMap.put("updatedAt", task.getUpdatedAt());
        taskMap.put("completedAt", task.getCompletedAt());

        // 文档信息
        Map<String, Object> documentMap = new HashMap<>();
        documentMap.put("id", task.getDocument().getId());
        documentMap.put("title", task.getDocument().getTitle());
        taskMap.put("document", documentMap);

        return taskMap;
    }

    // 请求DTO类
    static class CreateTaskRequest {
        private String title;
        private String description;
        private TaskPriority priority;
        private LocalDateTime dueDate;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public TaskPriority getPriority() { return priority; }
        public void setPriority(TaskPriority priority) { this.priority = priority; }
        public LocalDateTime getDueDate() { return dueDate; }
        public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    }

    static class UpdateTaskStatusRequest {
        private TaskStatus status;

        public TaskStatus getStatus() { return status; }
        public void setStatus(TaskStatus status) { this.status = status; }
    }
}