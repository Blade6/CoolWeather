create table City (
	id integer primary key autoincrement,
	city_name text,
	city_code int
)

create table County (
	id integer primary key autoincrement,
	county_name text,
	county_code char(10),
	city_code int
)