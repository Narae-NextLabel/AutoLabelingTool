create table tUpload
(
    fileOriName varchar(100) not null comment '파일 원본명'
        primary key,
    fileName    varchar(200) not null comment '파일 명',
    projectIdx  int unsigned not null comment '프로젝트 고유번호',
    labelTxt    text         null comment '라벨텍스트',
    constraint FK_tUpload_projectIdx_tProject_projectIdx
        foreign key (projectIdx) references tProject (projectIdx)
);

