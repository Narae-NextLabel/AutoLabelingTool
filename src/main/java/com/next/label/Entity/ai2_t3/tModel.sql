create table tModel
(
    modelIdx  int unsigned  not null
        primary key,
    modelName varchar(100)  null,
    modelPath varchar(100)  null,
    usedCnt   int default 0 null,
    userId    varchar(30)   null
)
    engine = MyISAM;

create index FKb0d3bkaq3y4rx5ovr5kfw17d2
    on tModel (userId);

