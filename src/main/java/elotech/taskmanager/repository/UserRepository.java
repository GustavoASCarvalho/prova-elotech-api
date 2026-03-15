package elotech.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import elotech.taskmanager.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
