package com.next.label.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class workspaceDTO {
    private String projectName;
    private String imagePath;
    private String uploadAt;
    private String projectIdx;
}
