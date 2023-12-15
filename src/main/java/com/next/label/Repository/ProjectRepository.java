package com.next.label.Repository;

import com.next.label.Entity.tProject;
import com.next.label.Entity.tUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<tProject, Long> {

    List<tProject> findByUserId(tUser userId);

    void deleteById(Long projectIdx);
    @Modifying
    @Query("UPDATE tProject p SET p.projectName = :projectName WHERE p.projectIdx = :projectIdx")
    void updateProjectName(Long projectIdx, String projectName);
}
