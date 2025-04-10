-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema tennis_tournament
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema tennis_tournament
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `tennis_tournament` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `tennis_tournament` ;

-- -----------------------------------------------------
-- Table `tennis_tournament`.`users`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `tennis_tournament`.`users` ;

CREATE TABLE IF NOT EXISTS `tennis_tournament`.`users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6) NULL DEFAULT NULL,
  `email` VARCHAR(255) NOT NULL,
  `first_name` VARCHAR(255) NOT NULL,
  `last_name` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `updated_at` DATETIME(6) NULL DEFAULT NULL,
  `user_type` ENUM('ADMIN', 'PLAYER', 'REFEREE') NOT NULL,
  `username` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UK6dotkott2kjsp8vw4d0m25fb7` (`email` ASC) VISIBLE,
  UNIQUE INDEX `UKr43af9ap4edm43mmtq01oddj6` (`username` ASC) VISIBLE)
ENGINE = InnoDB
AUTO_INCREMENT = 28
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `tennis_tournament`.`tournaments`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `tennis_tournament`.`tournaments` ;

CREATE TABLE IF NOT EXISTS `tennis_tournament`.`tournaments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6) NULL DEFAULT NULL,
  `description` TEXT NULL DEFAULT NULL,
  `end_date` DATE NOT NULL,
  `location` VARCHAR(255) NOT NULL,
  `max_participants` INT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `registration_deadline` DATE NOT NULL,
  `start_date` DATE NOT NULL,
  `updated_at` DATETIME(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 16
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `tennis_tournament`.`matches`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `tennis_tournament`.`matches` ;

CREATE TABLE IF NOT EXISTS `tennis_tournament`.`matches` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `court_number` INT NULL DEFAULT NULL,
  `created_at` DATETIME(6) NULL DEFAULT NULL,
  `round` ENUM('FINAL', 'QUARTER_FINAL', 'ROUND_1', 'ROUND_2', 'SEMI_FINAL') NULL DEFAULT NULL,
  `scheduled_time` DATETIME(6) NOT NULL,
  `status` ENUM('CANCELLED', 'COMPLETED', 'IN_PROGRESS', 'SCHEDULED') NOT NULL,
  `updated_at` DATETIME(6) NULL DEFAULT NULL,
  `player1_id` BIGINT NOT NULL,
  `player2_id` BIGINT NOT NULL,
  `referee_id` BIGINT NOT NULL,
  `tournament_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `FK399aa3d3u7tilrrvtuj396nuj` (`player1_id` ASC) VISIBLE,
  INDEX `FKf9p2o4y87q9pjb8dgn34np8wk` (`player2_id` ASC) VISIBLE,
  INDEX `FKjefeporn1y6kfowpanuya6b69` (`referee_id` ASC) VISIBLE,
  INDEX `FKeeniokyjgo5k6rmhjujatn27i` (`tournament_id` ASC) VISIBLE,
  CONSTRAINT `FK399aa3d3u7tilrrvtuj396nuj`
    FOREIGN KEY (`player1_id`)
    REFERENCES `tennis_tournament`.`users` (`id`),
  CONSTRAINT `FKeeniokyjgo5k6rmhjujatn27i`
    FOREIGN KEY (`tournament_id`)
    REFERENCES `tennis_tournament`.`tournaments` (`id`),
  CONSTRAINT `FKf9p2o4y87q9pjb8dgn34np8wk`
    FOREIGN KEY (`player2_id`)
    REFERENCES `tennis_tournament`.`users` (`id`),
  CONSTRAINT `FKjefeporn1y6kfowpanuya6b69`
    FOREIGN KEY (`referee_id`)
    REFERENCES `tennis_tournament`.`users` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 18
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `tennis_tournament`.`match_scores`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `tennis_tournament`.`match_scores` ;

CREATE TABLE IF NOT EXISTS `tennis_tournament`.`match_scores` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `created_at` DATETIME(6) NULL DEFAULT NULL,
  `player1_score` INT NOT NULL,
  `player2_score` INT NOT NULL,
  `set_number` INT NOT NULL,
  `updated_at` DATETIME(6) NULL DEFAULT NULL,
  `match_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UKj72r7qt1ii07tgqr3s4k6nkaj` (`match_id` ASC, `set_number` ASC) VISIBLE,
  CONSTRAINT `FKk9sxw5nwn3qejk2mkwc9c88bt`
    FOREIGN KEY (`match_id`)
    REFERENCES `tennis_tournament`.`matches` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 19
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


-- -----------------------------------------------------
-- Table `tennis_tournament`.`tournament_registrations`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `tennis_tournament`.`tournament_registrations` ;

CREATE TABLE IF NOT EXISTS `tennis_tournament`.`tournament_registrations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `registration_date` DATETIME(6) NULL DEFAULT NULL,
  `status` ENUM('APPROVED', 'PENDING', 'REJECTED') NOT NULL,
  `player_id` BIGINT NOT NULL,
  `tournament_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `UK5mhtq8mjdm32yhlwsjy0h8aoy` (`player_id` ASC, `tournament_id` ASC) VISIBLE,
  INDEX `FK2mpg0cs7jn7a10v34nhmg03lg` (`tournament_id` ASC) VISIBLE,
  CONSTRAINT `FK2mpg0cs7jn7a10v34nhmg03lg`
    FOREIGN KEY (`tournament_id`)
    REFERENCES `tennis_tournament`.`tournaments` (`id`),
  CONSTRAINT `FK2vpl5jussktcrrw96s5jgqm8n`
    FOREIGN KEY (`player_id`)
    REFERENCES `tennis_tournament`.`users` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 26
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
