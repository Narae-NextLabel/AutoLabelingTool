package com.next.label.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class startDTO {
    private Long modelIdx;
    private String modelName;
    private String className;
    private String classIconPath;
    private List<String> classNames; // className의 리스트
    private int classCount;          // className 리스트의 길이
    private String firstClassName;   // 첫 번째 className

}
