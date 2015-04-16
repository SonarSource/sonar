
CREATE TABLE "FILE_SOURCES" (
  "ID" INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1),
  "PROJECT_UUID" VARCHAR(50) NOT NULL,
  "FILE_UUID" VARCHAR(50) NOT NULL,
  "BINARY_DATA" BINARY(167772150),
  "TEST_DATA" BINARY(167772150),
  "TEST_HASH" VARCHAR(50),
  "DATA_HASH" VARCHAR(50) NOT NULL,
  "CREATED_AT" BIGINT NOT NULL,
  "UPDATED_AT" BIGINT NOT NULL
);
