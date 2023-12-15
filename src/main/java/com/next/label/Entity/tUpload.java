package com.next.label.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class tUpload {

    @Id
    @Column(nullable = false)
    private String fileOriName;

    @Column(nullable = false)
    private String fileName;

    @ManyToOne
    @JoinColumn(name = "projectIdx")
    private tProject projectIdx;


    @Lob // text 속성이고 사이즈 큰 상태일 때
    @Column(columnDefinition = "text")
    private String labelTxt;
    @Transient
    private boolean isImage;

    // 추가: 비디오 여부를 나타내는 플래그
    @Transient
    private boolean isVideo;

    // 기타 코드...

    public void setIsImage(boolean isImage) {
        this.isImage = isImage;
    }

    public void setIsVideo(boolean isVideo) {
        this.isVideo = isVideo;
    }


    @Override
    public String toString() {
        return "tUpload{" +
                ", fileName='" + fileName + '\'' +
                ", fileOriName='" + fileOriName + '\'' +
                ", projectIdx='" + projectIdx + '\'' +
                ", labelTxt='" + labelTxt + '\'' +
                '}';
    }

}
