package elotech.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import elotech.taskmanager.entity.Task;

public interface TaskRepository extends JpaRepository<Task, Long> {

}
