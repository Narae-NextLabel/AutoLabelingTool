package com.next.label;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LabelApplication {

    // @SpringBootApplication 안에 있는 기능

    // 1. @AutoConfiguration

    //			SPRING을 실행하기 위한 다양한 설정 Class 들이 자동으로 세팅됨
    //			(web.xml, servlet-context 등 ...)
    // 			application.properties를 보고, 지정한 설정들을 반영 시켜줌

    // 2. @ComponentScan

    // 			지정된 패키지를 스캔하고, 어노테이션이 붙은 클래스들을 spring 메모리 생성
    // 			기본으로, Application class가 있는 패키지를 Scan

    public static void main(String[] args) {

        SpringApplication.run(LabelApplication.class, args);
    }

}
