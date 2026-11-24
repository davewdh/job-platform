package com.jobplatform.backend.repository;

import com.jobplatform.backend.model.Job;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findAllByOrderByIdDesc(Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Job j where j.id = :id")
    Optional<Job> lockById(@Param("id") Long id);

    @Query("""
            select j.id from Job j
            where (j.status in (com.jobplatform.backend.model.JobStatus.PENDING, com.jobplatform.backend.model.JobStatus.PROCESSING)
                and (j.nextAttemptAt is null or j.nextAttemptAt <= :now)
                and (j.dispatchedAt is null or j.dispatchedAt <= :stale))
              or (j.status = com.jobplatform.backend.model.JobStatus.FAILED and j.deadLetteredAt is null)
            order by j.id
            """)
    List<Long> findDispatchable(@Param("now") Instant now, @Param("stale") Instant stale, Pageable page);
}
