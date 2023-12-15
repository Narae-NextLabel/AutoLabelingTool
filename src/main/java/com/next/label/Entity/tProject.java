package com.next.label.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class tProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, insertable = false, updatable = false)
    private Long projectIdx;

    @OneToMany(mappedBy = "projectIdx", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<tUpload> uploads;

    @Column(columnDefinition = "datetime default now()", insertable = false, updatable = false)
    private Timestamp uploadedAt;

    @ManyToOne
    @JoinColumn(name = "userId")
    private tUser userId;


    @Column(columnDefinition = "VARCHAR(50) DEFAULT '제목을 입력하세요'",insertable = false)
    private String projectName;


    @Override
    public String toString() {
        return "tProject{" +
                ", projectIdx='" + projectIdx + '\'' +
                ", uploadedAt='" + uploadedAt + '\'' +
                ", userId='" + userId + '\'' +
                ", projectName='" + projectName + '\'' +
                '}';
    }

}

