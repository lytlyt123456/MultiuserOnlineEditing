package com.example.multiuser_online_editing.repository.document_management;

import com.example.multiuser_online_editing.entity.document_management.Folder;
import com.example.multiuser_online_editing.entity.user_management.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByOwnerAndParentIsNullOrderByName(User owner);
    List<Folder> findByOwnerAndParentOrderByName(User owner, Folder parent);
    Optional<Folder> findByIdAndOwner(Long id, User owner);
    boolean existsByNameAndOwnerAndParent(String name, User owner, Folder parent);
}