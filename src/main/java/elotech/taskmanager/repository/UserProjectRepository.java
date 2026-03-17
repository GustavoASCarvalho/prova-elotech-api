package elotech.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import elotech.taskmanager.entity.UserProject;
import elotech.taskmanager.entity.UserProjectId;

public interface UserProjectRepository extends JpaRepository<UserProject, UserProjectId> {
    boolean existsByUser_IdAndProject_Id(Long userId, Long projectId);

    long deleteByUser_IdAndProject_Id(Long userId, Long projectId);
}
