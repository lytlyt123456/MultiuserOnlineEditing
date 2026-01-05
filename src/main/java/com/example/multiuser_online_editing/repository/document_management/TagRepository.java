package com.example.multiuser_online_editing.repository.document_management;

import com.example.multiuser_online_editing.entity.document_management.Tag;
import com.example.multiuser_online_editing.entity.user_management.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByNameAndOwner(String name, User owner);
    List<Tag> findByOwnerOrderByName(User owner);
    List<Tag> findByNameContainingIgnoreCaseAndOwner(String name, User owner);
}