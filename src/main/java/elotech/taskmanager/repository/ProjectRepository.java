package elotech.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import elotech.taskmanager.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

}
