CREATE TABLE "SNAPSHOTS"(
    "UUID" VARCHAR(50) NOT NULL,
    "COMPONENT_UUID" VARCHAR(50) NOT NULL,
    "STATUS" VARCHAR(4) DEFAULT 'U' NOT NULL,
    "ISLAST" BOOLEAN DEFAULT FALSE NOT NULL,
    "VERSION" VARCHAR(500),
    "PURGE_STATUS" INTEGER,
    "BUILD_STRING" VARCHAR(100),
    "REVISION" VARCHAR(100),
    "BUILD_DATE" BIGINT,
    "PERIOD1_MODE" VARCHAR(100),
    "PERIOD1_PARAM" VARCHAR(100),
    "PERIOD1_DATE" BIGINT,
    "CREATED_AT" BIGINT
);
CREATE INDEX "SNAPSHOT_COMPONENT" ON "SNAPSHOTS"("COMPONENT_UUID");
