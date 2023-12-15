package com.next.label.Repository;



import com.next.label.Entity.tModel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelRepository extends JpaRepository<tModel, Long> {
    tModel findAllByModelIdx(Long modelIdx);
    @Query("SELECT MAX(m.modelIdx) FROM tModel m")
    Long findLastModelIdx();
}
