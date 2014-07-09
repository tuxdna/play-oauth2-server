# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "access_tokens" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"token" VARCHAR(254) NOT NULL,"refresh_token" VARCHAR(254) NOT NULL,"client_id" VARCHAR(254) NOT NULL,"user_id" INTEGER NOT NULL,"scope" VARCHAR(254) NOT NULL,"expires_in" BIGINT NOT NULL,"created_at" TIMESTAMP NOT NULL);
create table "auth_codes" ("authorization_code" VARCHAR(254) PRIMARY KEY NOT NULL,"user_id" INTEGER NOT NULL,"redirect_uri" VARCHAR(254),"created_at" TIMESTAMP NOT NULL,"scope" VARCHAR(254),"client_id" VARCHAR(254) NOT NULL,"expires_in" INTEGER NOT NULL);
create table "client_grant_types" ("client_id" VARCHAR(254) NOT NULL,"grant_type_id" INTEGER NOT NULL,constraint "pk_client_grant_type" primary key("client_id","grant_type_id"));
create table "clients" ("client_id" VARCHAR(254) PRIMARY KEY NOT NULL,"username" VARCHAR(254) NOT NULL,"client_secret" VARCHAR(254) NOT NULL,"description" VARCHAR(254) NOT NULL,"redirect_uri" VARCHAR(254) NOT NULL,"scope" VARCHAR(254) NOT NULL);
create table "grant_types" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"grant_type" VARCHAR(254) NOT NULL);
create table "users" ("id" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"username" VARCHAR(254) NOT NULL,"email" VARCHAR(254) NOT NULL,"password" VARCHAR(254) NOT NULL,"role" VARCHAR(254) NOT NULL);

# --- !Downs

drop table "access_tokens";
drop table "auth_codes";
drop table "client_grant_types";
drop table "clients";
drop table "grant_types";
drop table "users";

