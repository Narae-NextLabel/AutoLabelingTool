create table tUser
(
    userId      varchar(30)                        not null
        primary key,
    userPw      varchar(96)                        not null,
    userName    varchar(40)                        not null,
    joinedAt    datetime default CURRENT_TIMESTAMP not null,
    profilePath varchar(1000)                      null,
    constraint userName_UNIQUE
        unique (userName)
)
    comment '사용자';

