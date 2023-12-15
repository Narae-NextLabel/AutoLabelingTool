package com.next.label.Repository;

import com.next.label.Entity.tClass;


import com.next.label.Entity.tModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<tClass, Long> {



    @Query("SELECT tClass.className FROM tClass tClass WHERE tClass.modelIdx.modelIdx = :modelIdx")
    List<String> findClassNameByModelIdx(@Param("modelIdx") Long modelIdx);

    List<tClass> findByModelIdx(tModel modelIdx);
    List<tClass> findAllByClassName(String className);
}
