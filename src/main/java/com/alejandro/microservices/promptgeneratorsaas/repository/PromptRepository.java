package com.alejandro.microservices.promptgeneratorsaas.repository;

import com.alejandro.microservices.promptgeneratorsaas.entity.Prompt;
import com.alejandro.microservices.promptgeneratorsaas.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, Long> {
    
    List<Prompt> findByUser(User user);
    
    @Query("SELECT p FROM Prompt p WHERE p.title LIKE %:searchTerm% OR p.content LIKE %:searchTerm%")
    List<Prompt> searchPrompts(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT p FROM Prompt p WHERE p.user = :user AND (p.title LIKE %:searchTerm% OR p.content LIKE %:searchTerm%)")
    List<Prompt> searchUserPrompts(@Param("user") User user, @Param("searchTerm") String searchTerm);
    
    List<Prompt> findByCategory(String category);
    
    List<Prompt> findByUserAndCategory(User user, String category);
}
