package com.next.label.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class tUser {
    @Id
    private String userId;

    @Column(nullable = false)
    private String userPw;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(columnDefinition = "datetime default now()", insertable = false, updatable = false)
    private Timestamp joinedAt;

    private String profilePath;

    @Override
    public String toString() {
        return "tUser{" +
                "userId='" + userId + '\'' +
                ", userPw='" + userPw + '\'' +
                ", userName='" + userName + '\'' +
                ", joinedAt=" + joinedAt +
                ", profilePath=" + profilePath +
                '}';
    }

}
