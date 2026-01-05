package com.example.multiuser_online_editing.repository.document_management;

import com.example.multiuser_online_editing.entity.document_management.Template;
import com.example.multiuser_online_editing.entity.document_management.TemplateCategory;
import com.example.multiuser_online_editing.entity.user_management.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {
    Page<Template> findByOwnerOrIsPublicTrueOrderByUpdatedAtDesc(User owner, Pageable pageable);
    List<Template> findByCategoryAndIsPublicTrue(TemplateCategory category);
    List<Template> findByOwnerOrderByName(User owner);
}