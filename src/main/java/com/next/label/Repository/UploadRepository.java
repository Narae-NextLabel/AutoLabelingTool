package com.next.label.Repository;

import com.next.label.Entity.tProject;
import com.next.label.Entity.tUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UploadRepository extends JpaRepository<tUpload, String> {

    List<tUpload> findByProjectIdx(tProject projectIdx);



}
