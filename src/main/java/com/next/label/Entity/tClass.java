package com.next.label.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.*;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class tClass{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    private Long classIdx;

    @ManyToOne
    @JoinColumn(name = "modelIdx")
    private tModel modelIdx;

    @Column(nullable = false)
    private String className;

    @Column(insertable = false , columnDefinition = "VARCHAR(255) default './assets/img/model.png'")
    private String classIconPath;

    @Override
    public String toString() {
        return "tClass{" +
                "classIdx=" + classIdx +
                ", modelIdx=" + modelIdx +
                ", className='" + className + '\'' +
                ", classIconPath='" + classIconPath + '\'' +
                '}';
    }

}
