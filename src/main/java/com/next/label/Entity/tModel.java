package com.next.label.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class tModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private Long modelIdx;

    @OneToMany(mappedBy = "modelIdx", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<tClass> tClasses;

    @Column(nullable = false)
    private String modelPath;

    @Column(nullable = false)
    private String modelName;

    @ManyToOne
    @JoinColumn(name = "userId")
    private tUser userId;

    @Column(columnDefinition = "int default 0", insertable = false)
    private int usedCnt;

    @Override
    public String toString() {
        return "tModel{" +
                "modelIdx=" + modelIdx +
                ", modelPath='" + modelPath + '\'' +
                ", modelName='" + modelName + '\'' +
                ", userId='" + userId + '\'' +
                ", usedCnt=" + usedCnt +
                '}';
    }


}
