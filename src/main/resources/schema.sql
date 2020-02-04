create table if not exists mappings  (
    uid varchar(255) not null,
    json_guid varchar(255) not null,
    unique ( uid, json_guid)
)  ;
