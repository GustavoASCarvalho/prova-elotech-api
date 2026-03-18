package elotech.taskmanager.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import elotech.taskmanager.entity.Task;
import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    interface StatusCountProjection {
        TaskStatusEnum getStatus();

        Long getTotal();
    }

    interface PriorityCountProjection {
        PriorityEnum getPriority();

        Long getTotal();
    }

    List<Task> findByProject_Memberships_User_Id(Long userId);

    Optional<Task> findByIdAndProject_Memberships_User_Id(Long taskId, Long userId);

    long countByProject_IdAndAssignee_IdAndStatus(Long projectId, Long assigneeId, TaskStatusEnum status);

    long countByProject_IdAndAssignee_IdAndStatusAndIdNot(
            Long projectId,
            Long assigneeId,
            TaskStatusEnum status,
            Long taskId);

    @Query("""
            select t
            from Task t
            join t.project p
            join p.memberships m
            where m.user.id = :userId
            and (
                    lower(t.title) like lower(concat('%', :text, '%'))
                 or lower(coalesce(t.description, '')) like lower(concat('%', :text, '%'))
            )
            """)
    Page<Task> searchByTextForUser(@Param("userId") Long userId, @Param("text") String text, Pageable pageable);

    @Query("""
            select t.status as status, count(t) as total
            from Task t
            join t.project p
            join p.memberships m
            where p.id = :projectId
              and m.user.id = :userId
            group by t.status
            """)
    List<StatusCountProjection> countByStatusForProjectAndUser(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);

    @Query("""
            select t.priority as priority, count(t) as total
            from Task t
            join t.project p
            join p.memberships m
            where p.id = :projectId
              and m.user.id = :userId
            group by t.priority
            """)
    List<PriorityCountProjection> countByPriorityForProjectAndUser(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);
}
