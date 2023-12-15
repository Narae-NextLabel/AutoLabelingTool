package com.next.label.Repository;

import com.next.label.Entity.tUser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<tUser, String> {


    Optional<tUser> findByUserId(String userId);

    tUser findByUserIdAndUserPw(String userId, String userPw);
    Optional<tUser> findByUserName(String userName);

    void deleteById(String fileOriName);


}
