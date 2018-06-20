-- -----------------------------------------------------
-- Schema ocmanager
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `ocmanager` DEFAULT CHARACTER SET utf8 ;
USE `ocmanager` ;

-- -----------------------------------------------------
-- Table `ocmanager`.`tenants`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`tenants` (
  `id` VARCHAR(64) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `description` MEDIUMTEXT NULL,
  `parentId` VARCHAR(64) NULL,
  `level` INT NOT NULL,
  `createTime` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `dueTime` DATETIME NULL,
  `status` VARCHAR(64) NULL DEFAULT 'active',
  `clusters` MEDIUMTEXT NULL,
  `quota` MEDIUMTEXT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `ocmanager`.`users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`users` (
  `id` VARCHAR(64) NOT NULL,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(64) NULL,
  `email` VARCHAR(64) NULL,
  `phone` VARCHAR(64) NULL,
  `description` MEDIUMTEXT NULL,
  `createdUser` VARCHAR(64) NULL,
  `createTime` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `username_UNIQUE` (`username` ASC))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `ocmanager`.`services`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`services` (
  `id` VARCHAR(64) NOT NULL,
  `servicename` VARCHAR(64) NOT NULL,
  `description` MEDIUMTEXT NULL,
  `origin` VARCHAR(64) NULL,
  `serviceType` VARCHAR(64) NULL,
  `category` VARCHAR(64) NULL DEFAULT 'service',
  PRIMARY KEY (`servicename`),
  UNIQUE INDEX `id_UNIQUE` (`id` ASC)),
  INDEX `origin_UNIQUE` (`origin` ASC)),
  CONSTRAINT `fk_brokers_services`
    FOREIGN KEY (`origin`)
    REFERENCES `ocmanager`.`CM_BROKERS` (`BROKER_NAME`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `ocmanager`.`roles`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`roles` (
  `id` VARCHAR(64) NOT NULL,
  `rolename` VARCHAR(64) NOT NULL,
  `description` MEDIUMTEXT NULL,
  `permission` MEDIUMTEXT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `ocmanager`.`service_instances`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`service_instances` (
  `id` VARCHAR(64) NOT NULL,
  `instanceName` VARCHAR(64) NOT NULL,
  `tenantId` VARCHAR(64) NOT NULL,
  `serviceId` VARCHAR(64) NULL,
  `serviceName` VARCHAR(64) NOT NULL,
  `quota` MEDIUMTEXT NULL,
  `status` VARCHAR(64) NULL,
  `cuzBsiName` MEDIUMTEXT NULL,
  `attributes` MEDIUMTEXT NULL,
  `serviceType` VARCHAR(64) NULL,
  `category` VARCHAR(64) NULL DEFAULT 'service',
  PRIMARY KEY (`id`),
  INDEX `fk_service_instances_tenants1_idx` (`tenantId` ASC),
  INDEX `fk_service_instances_services1_idx` (`serviceName` ASC),
  CONSTRAINT `fk_service_instances_tenants1`
    FOREIGN KEY (`tenantId`)
    REFERENCES `ocmanager`.`tenants` (`id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_service_instances_services1`
    FOREIGN KEY (`serviceName`)
    REFERENCES `ocmanager`.`services` (`servicename`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `ocmanager`.`tenants_users_roles_assignment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`tenants_users_roles_assignment` (
  `tenant_id` VARCHAR(64) NOT NULL,
  `user_id` VARCHAR(64) NOT NULL,
  `role_id` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`tenant_id`, `user_id`),
  INDEX `fk_tenants_users_assignment_users1_idx` (`user_id` ASC),
  INDEX `fk_tenants_users_assignment_tenants1_idx` (`tenant_id` ASC),
  INDEX `fk_tenants_users_assignment_roles1_idx` (`role_id` ASC),
  CONSTRAINT `fk_tenants_users_assignment_tenants1`
    FOREIGN KEY (`tenant_id`)
    REFERENCES `ocmanager`.`tenants` (`id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_tenants_users_assignment_users1`
    FOREIGN KEY (`user_id`)
    REFERENCES `ocmanager`.`users` (`id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_tenants_users_assignment_roles1`
    FOREIGN KEY (`role_id`)
    REFERENCES `ocmanager`.`roles` (`id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `ocmanager`.`services_roles_permission`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`services_roles_permission` (
  `service_servicename` VARCHAR(64) NOT NULL,
  `role_id` VARCHAR(64) NOT NULL,
  `ServicePermission` MEDIUMTEXT NULL,
  PRIMARY KEY (`service_servicename`, `role_id`),
  INDEX `fk_services_roles_permission_services1_idx` (`service_servicename` ASC),
  INDEX `fk_services_roles_permission_roles1_idx` (`role_id` ASC),
  CONSTRAINT `fk_services_roles_permission_roles1`
    FOREIGN KEY (`role_id`)
    REFERENCES `ocmanager`.`roles` (`id`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT,
  CONSTRAINT `fk_services_roles_permission_services1`
    FOREIGN KEY (`service_servicename`)
    REFERENCES `ocmanager`.`services` (`servicename`)
    ON DELETE RESTRICT
    ON UPDATE RESTRICT)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `ocmanager`.`dashboard`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`CM_APPLY_BILL` (
  `ID` VARCHAR(64) NOT NULL,
  `APPLY_USER` VARCHAR(64) NULL,
  `APPLY_DATE` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `APPLY_QUOTA` MEDIUMTEXT NULL,
  `APPLY_REASON` MEDIUMTEXT NULL,
  `STATUS` INT NULL,
  PRIMARY KEY (`ID`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `ocmanager`.`dashboard`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`dashboard` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `description` MEDIUMTEXT NULL,
  `imageUrl` MEDIUMTEXT NOT NULL,
  `href` MEDIUMTEXT NOT NULL,
  `blank` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `name_UNIQUE` (`name` ASC))
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `ocmanager`.`clusters`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`CM_CLUSTERS` (
  `CLUSTER_ID` INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT,
  `CLUSTER_NAME` VARCHAR(64) NOT NULL unique,
  `CLUSTER_TYPE` VARCHAR(64) NOT NULL,
  `CLUSTER_URL` VARCHAR(64) NULL,
  `CLUSTER_ADMIN` VARCHAR(64) NULL,
  `CLUSTER_PASSWORD` VARCHAR(64) NULL,
  UNIQUE INDEX `fk_clusters_idx` (`CLUSTER_NAME` ASC))
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Table `ocmanager`.`brokers`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `ocmanager`.`CM_BROKERS` (
  `BROKER_ID` INT UNSIGNED NULL PRIMARY KEY AUTO_INCREMENT,
  `BROKER_IMAGE` VARCHAR(64) NULL,
  `BROKER_NAME` VARCHAR(64) NOT NULL,   
  `BROKER_URL` VARCHAR(64) NULL,
  `BINDED_CLUSTER` VARCHAR(64) NOT NULL,
  `DC_NAME` VARCHAR(64) NOT NULL,
  `LATEST_STATUS` VARCHAR(64) NOT NULL,
  UNIQUE INDEX `fk_brokers_brokername_idx` (`BROKER_NAME` ASC),
  INDEX `fk_brokers_clustername_idx` (`BINDED_CLUSTER` ASC),
  CONSTRAINT `fk_clusters_brokers`
    FOREIGN KEY (`BINDED_CLUSTER`)
    REFERENCES `ocmanager`.`CM_CLUSTERS` (`CLUSTER_NAME`)
    ON DELETE RESTRICT)
ENGINE = InnoDB;

-- -----------------------------------------------------
-- Init the 4 roles into the table `ocmanager`.`roles`
-- -----------------------------------------------------
INSERT INTO `ocmanager`.`roles`(id, rolename, description, permission) VALUES("a10170cb-524a-11e7-9dbb-fa163ed7d0ae", "system.admin", "system admin is super user, it can create tenant and add users, assign role to user and add services.", "{createUser: true, updateUser: true, deleteUser: true, addService: true, deleteService: true, grant: true}");
-- INSERT INTO `ocmanager`.`roles`(id, rolename, description, permission) VALUES("a1149421-524a-11e7-9dbb-fa163ed7d0ae", "subsidiary.admin", "subsidiary admin create project, add users and assign role to user.");
INSERT INTO `ocmanager`.`roles`(id, rolename, description, permission) VALUES("a12a84d0-524a-11e7-9dbb-fa163ed7d0ae", "tenant.admin", "tenant admin can add uses to the tenant and assign role to user.", "{grant: true}");
INSERT INTO `ocmanager`.`roles`(id, rolename, description, permission) VALUES("a13dd087-524a-11e7-9dbb-fa163ed7d0ae", "team.member", "the user only can read the tenant information that he is in.", "{}");


-- -----------------------------------------------------
-- Init the ocdp services in the table `ocmanager`.`services`
-- ocdp service is hard code in the catalog, so here we also hard code
-- -----------------------------------------------------
INSERT INTO `ocmanager`.`services` (id, servicename, description, origin, serviceType) VALUES("ae67d4ba-5c4e-4937-a68b-5b47cfe356d8", "hdfs", "A Hadoop hdfs service broker implementation", "ocdp", "hdfs");
INSERT INTO `ocmanager`.`services` (id, servicename, description, origin, serviceType) VALUES("d9845ade-9410-4c7f-8689-4e032c1a8450", "hbase", "A Hadoop hbase service broker implementation", "ocdp", "hbase");
INSERT INTO `ocmanager`.`services` (id, servicename, description, origin, serviceType) VALUES("2ef26018-003d-4b2b-b786-0481d4ee9fa3", "hive", "A Hadoop hive service broker implementation", "ocdp", "hive");
INSERT INTO `ocmanager`.`services` (id, servicename, description, origin, serviceType) VALUES("ae0f2324-27a8-415b-9c7f-64ab6cd88d40", "mapreduce", "A Hadoop mapreduce service broker implementation", "ocdp", "mapreduce");
INSERT INTO `ocmanager`.`services` (id, servicename, description, origin, serviceType) VALUES("d3b9a485-f038-4605-9b9b-29792f5c61d1", "spark", "A Spark service broker implementation", "ocdp", "spark");
INSERT INTO `ocmanager`.`services` (id, servicename, description, origin, serviceType) VALUES("7b738c78-d412-422b-ac3e-43a9fc72a4a7", "kafka", "A Kafka service broker implementation", "ocdp", "kafka");

-- -----------------------------------------------------
-- Init the role service permission mapping
-- -----------------------------------------------------
-- project manager permission
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("hdfs", "a12a84d0-524a-11e7-9dbb-fa163ed7d0ae", "read,write,execute");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("hbase", "a12a84d0-524a-11e7-9dbb-fa163ed7d0ae", "read,write,create,admin");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("hive", "a12a84d0-524a-11e7-9dbb-fa163ed7d0ae", "select,update,create,drop,alter,index,lock");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("mapreduce", "a12a84d0-524a-11e7-9dbb-fa163ed7d0ae", "submit-app,admin-queue");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("spark", "a12a84d0-524a-11e7-9dbb-fa163ed7d0ae", "submit-app,admin-queue");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("kafka", "a12a84d0-524a-11e7-9dbb-fa163ed7d0ae", "publish,consume,configure,describe,create,delete,kafka_admin");

-- team member permission
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("hdfs", "a13dd087-524a-11e7-9dbb-fa163ed7d0ae", "read,write,execute");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("hbase", "a13dd087-524a-11e7-9dbb-fa163ed7d0ae", "read,write,create,admin");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("hive", "a13dd087-524a-11e7-9dbb-fa163ed7d0ae", "select,update,create,drop,alter,index,lock");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("mapreduce", "a13dd087-524a-11e7-9dbb-fa163ed7d0ae", "submit-app,admin-queue");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("spark", "a13dd087-524a-11e7-9dbb-fa163ed7d0ae", "submit-app,admin-queue");
INSERT INTO `ocmanager`.`services_roles_permission` (service_servicename, role_id, ServicePermission) VALUES("kafka", "a13dd087-524a-11e7-9dbb-fa163ed7d0ae", "publish,consume,configure,describe,create,delete,kafka_admin");


-- -----------------------------------------------------
-- Init the admin user into the table `ocmanager`.`users`
-- -----------------------------------------------------
INSERT INTO `ocmanager`.`users` (id, username, password, email, phone, description) VALUES("2ef26018-003d-4b2b-b786-0481d4ee9fa8", "admin", PASSWORD("admin"), "admin@admin.com", "admin phone number", "System Admin User");

-- -----------------------------------------------------
-- Init the root tenant into `ocmanager`.`tenants`
-- -----------------------------------------------------
INSERT INTO `ocmanager`.`tenants`(id, name, level, description, quota)
  VALUES("ae783b6d-655a-11e7-aa10-fa163ed7d0ae","root.tenant", 1, "root tenant", '{"hdfs": {"nameSpaceQuota": 9223372036854775807,"storageSpaceQuota": 9223372036854775807},"hbase": {"maximumTablesQuota": 9223372036854775807,"maximumRegionsQuota": 9223372036854775807},"hive": {"storageSpaceQuota": 9223372036854775807},"mapreduce": {"yarnQueueQuota": 9223372036854775807},"spark": {"yarnQueueQuota": 9223372036854775807},"kafka": {"topicTTL": 9223372036854775807,"topicQuota": 9223372036854775807,"partitionSize": 9223372036854775807},"redis": {"memory": 9223372036854775807,"nodes":9223372036854775807,"volumeSize": 9223372036854775807}}');


-- -----------------------------------------------------
-- Init the admin user for root tenant into `ocmanager`.`tenants_users_roles_assignment`
-- -----------------------------------------------------
INSERT INTO `ocmanager`.`tenants_users_roles_assignment`(tenant_id, user_id, role_id) VALUES("ae783b6d-655a-11e7-aa10-fa163ed7d0ae","2ef26018-003d-4b2b-b786-0481d4ee9fa8", "a10170cb-524a-11e7-9dbb-fa163ed7d0ae");


