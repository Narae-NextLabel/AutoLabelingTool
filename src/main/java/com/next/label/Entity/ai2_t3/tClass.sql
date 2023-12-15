create table tClass
(
    classIdx      bigint auto_increment
        primary key,
    classIconPath varchar(255) null,
    className     varchar(255) not null,
    modelIdx      bigint       null
)
    engine = MyISAM;

create index FK4vypciyc3kkd0ydouumd6o8tk
    on tClass (modelIdx);

