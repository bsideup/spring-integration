-- Autogenerated: do not edit this file

CREATE TABLE INT_MESSAGE  (
	MESSAGE_ID CHAR(36) NOT NULL,
	REGION VARCHAR(100) NOT NULL,
	CREATED_DATE TIMESTAMP NOT NULL,
	MESSAGE_BYTES LONGVARBINARY,
	constraint MESSAGE_PK primary key (MESSAGE_ID, REGION)
);

CREATE INDEX INT_MESSAGE_IX1 ON INT_MESSAGE (CREATED_DATE);

CREATE TABLE INT_GROUP_TO_MESSAGE  (
	GROUP_KEY CHAR(36) NOT NULL,
	MESSAGE_ID CHAR(36) NOT NULL,
	REGION VARCHAR(100) NOT NULL,
	constraint GROUP_TO_MESSAGE_PK primary key (GROUP_KEY, MESSAGE_ID, REGION)
);

CREATE TABLE INT_MESSAGE_GROUP  (
	GROUP_KEY CHAR(36) NOT NULL,
	REGION VARCHAR(100) NOT NULL,
	MARKED BIGINT,
	COMPLETE BIGINT,
	LAST_RELEASED_SEQUENCE BIGINT,
	CREATED_DATE TIMESTAMP NOT NULL,
	UPDATED_DATE TIMESTAMP DEFAULT NULL,
	constraint MESSAGE_GROUP_PK primary key (GROUP_KEY, REGION)
);

CREATE TABLE INT_LOCK  (
	LOCK_KEY CHAR(36) NOT NULL,
	REGION VARCHAR(100) NOT NULL,
	CLIENT_ID CHAR(36),
	CREATED_DATE TIMESTAMP NOT NULL,
	constraint LOCK_PK primary key (LOCK_KEY, REGION)
);
