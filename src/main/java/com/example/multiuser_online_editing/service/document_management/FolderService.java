package com.example.multiuser_online_editing.service.document_management;

import com.example.multiuser_online_editing.entity.document_management.Folder;
import com.example.multiuser_online_editing.entity.document_management.Document;
import com.example.multiuser_online_editing.entity.document_management.DocumentStatus;
import com.example.multiuser_online_editing.entity.user_management.OperationLog;
import com.example.multiuser_online_editing.entity.user_management.Role;
import com.example.multiuser_online_editing.entity.user_management.User;
import com.example.multiuser_online_editing.repository.document_management.DocumentRepository;
import com.example.multiuser_online_editing.repository.document_management.FolderRepository;
import com.example.multiuser_online_editing.repository.user_management.OperationLogRepository;
import com.example.multiuser_online_editing.repository.user_management.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class FolderService {

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private DocumentRepository documentRepository;

    // 创建文件夹
    public Folder createFolder(String name, String description, Long parentId, User owner) {
        if (owner.getRole() == Role.VIEWER) {
            throw new RuntimeException("您当前的角色为查看者，无法创建文件夹");
        }

        // 检查同名文件夹
        Folder parent = null;
        if (parentId != null) {
            parent = folderRepository.findByIdAndOwner(parentId, owner)
                    .orElseThrow(() -> new RuntimeException("父文件夹不存在或无权访问"));
        }

        if (folderRepository.existsByNameAndOwnerAndParent(name, owner, parent)) {
            throw new RuntimeException("该文件夹名称已存在");
        }

        Folder folder = new Folder();
        folder.setName(name);
        folder.setDescription(description);
        folder.setParent(parent);
        folder.setOwner(owner);

        Folder savedFolder = folderRepository.save(folder);

        // 记录操作日志
        logOperation(owner.getId(), "CREATE_FOLDER", "FOLDER", savedFolder.getId(),
                "创建文件夹: " + name);

        return savedFolder;
    }

    // 更新文件夹
    public Folder updateFolder(Long folderId, String name, String description, User user) {
        Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new RuntimeException("文件夹不存在或无权访问"));

        // 检查同名文件夹（排除自身）
        if (!folder.getName().equals(name)) {
            if (folderRepository.existsByNameAndOwnerAndParent(name, user, folder.getParent())) {
                throw new RuntimeException("该文件夹名称已存在");
            }
        }

        if (name != null) folder.setName(name);
        if (description != null) folder.setDescription(description);

        folder.setUpdatedAt(LocalDateTime.now());

        Folder updatedFolder = folderRepository.save(folder);

        // 记录操作日志
        logOperation(user.getId(), "UPDATE_FOLDER", "FOLDER", folderId,
                "更新文件夹: " + name);

        return updatedFolder;
    }

    // 删除文件夹
    public void deleteFolder(Long folderId, User user) {
        Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new RuntimeException("文件夹不存在或无权访问"));

        // 检查文件夹是否为空
        if (!folder.getDocuments().isEmpty()) {
            throw new RuntimeException("文件夹不为空，无法删除");
        }

        if (!folder.getChildren().isEmpty()) {
            throw new RuntimeException("文件夹包含子文件夹，无法删除");
        }

        folderRepository.delete(folder);

        // 记录操作日志
        logOperation(user.getId(), "DELETE_FOLDER", "FOLDER", folderId,
                "删除文件夹: " + folder.getName());
    }

    // 获取用户的根文件夹列表
    public List<Folder> getRootFolders(User user) {
        return folderRepository.findByOwnerAndParentIsNullOrderByName(user);
    }

    // 获取子文件夹列表
    public List<Folder> getSubFolders(Long parentId, User user) {
        Folder parent = folderRepository.findByIdAndOwner(parentId, user)
                .orElseThrow(() -> new RuntimeException("父文件夹不存在或无权访问"));

        return folderRepository.findByOwnerAndParentOrderByName(user, parent);
    }

    // 获取子文件列表
    public List<Document> getSubDocuments(Long parentId, User user) {
        Folder parent = folderRepository.findByIdAndOwner(parentId, user)
                .orElseThrow(() -> new RuntimeException("父文件夹不存在或无权访问"));

        return documentRepository.findByOwnerAndFolderAndStatusNot(user, parent, DocumentStatus.DELETED);
    }

    // 获取文件夹详情
    public Folder getFolderDetail(Long folderId, User user) {
        return folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new RuntimeException("文件夹不存在或无权访问"));
    }

    // 移动文件夹
    public Folder moveFolder(Long folderId, Long newParentId, User user) {
        Folder folder = folderRepository.findByIdAndOwner(folderId, user)
                .orElseThrow(() -> new RuntimeException("文件夹不存在或无权访问"));

        Folder newParent = null;
        if (newParentId != null) {
            newParent = folderRepository.findByIdAndOwner(newParentId, user)
                    .orElseThrow(() -> new RuntimeException("目标文件夹不存在或无权访问"));

            // 检查循环引用
            if (isCircularReference(folder, newParent)) {
                throw new RuntimeException("不能将文件夹移动到其子文件夹中");
            }
        }

        // 检查同名文件夹
        if (folderRepository.existsByNameAndOwnerAndParent(folder.getName(), user, newParent)) {
            throw new RuntimeException("目标位置已存在同名文件夹");
        }

        folder.setParent(newParent);
        Folder movedFolder = folderRepository.save(folder);

        // 记录操作日志
        logOperation(user.getId(), "MOVE_FOLDER", "FOLDER", folderId,
                "移动文件夹: " + folder.getName());

        return movedFolder;
    }

    // 检查循环引用
    private boolean isCircularReference(Folder folder, Folder potentialParent) {
        // 沿着potentialParent到根的路径一步步检查每个文件夹是不是folder，如果是，则不能执行移动文件夹的操作
        Folder current = potentialParent;
        while (current != null) {
            if (current.getId().equals(folder.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void logOperation(Long userId, String operation, String resourceType, Long resourceId, String details) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            OperationLog log =
                    new OperationLog(user, operation, resourceType, resourceId);
            log.setDetails(details);
            operationLogRepository.save(log);
        }
    }
}