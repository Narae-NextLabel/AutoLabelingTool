create table tProject
(
    projectIdx  int unsigned auto_increment comment '프로젝트 고유번호'
        primary key,
    uploadedAt  datetime    default CURRENT_TIMESTAMP not null comment '업로드 날짜',
    userId      varchar(30)                           not null comment '사용자 이메일',
    projectName varchar(50) default '제목을 입력하세요'       not null comment '프로젝트 명',
    constraint FK_tProject_userId_tUser_userId
        foreign key (userId) references tUser (userId)
);

