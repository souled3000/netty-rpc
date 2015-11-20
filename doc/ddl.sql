create database dev default charset=utf8;
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP ON dev.* TO smarthome@'%' IDENTIFIED BY 'smarthome123';

CREATE TABLE `user` (
  `id` bigint(21) NOT NULL AUTO_INCREMENT,
  `email` varchar(255) DEFAULT NULL,
  `username` varchar(255) NOT NULL UNIQUE,
  `phone` varchar(32) NOT NULL UNIQUE,
  `nick` varchar(128) DEFAULT NULL,
  `shadow` varchar(512) DEFAULT NULL,
  `emailable` varchar(8) DEFAULT NULL COMMENT '',
  `adminid` bigint(20) DEFAULT NULL,
  `phoneable` varchar(8) DEFAULT NULL COMMENT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `mail` (`email`),
  KEY `adminid` (`adminid`),
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`adminid`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;

CREATE TABLE `device` (
  `id` bigint(21),
  `name` varchar(128) DEFAULT NULL,
  `mac` char(16) NOT NULL UNIQUE,
  `sn` char(32) DEFAULT NULL,
  `encryptkey` char(32) DEFAULT NULL,
  `regtime` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `parentid` bigint(21) DEFAULT NULL,
  `device_type_id` smallint(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;


DELIMITER // 
CREATE DEFINER=`root`@`localhost` TRIGGER `change_id_minus` BEFORE INSERT ON `device` FOR EACH ROW begin
  declare tmp int;
  set tmp = 0;
  select min(`id`) into tmp from `device`;
  if tmp is null or tmp = '' or tmp > 0 then
    set new.id = -1;
  else
    set new.id = tmp-1;
  end if;
end;
//
DELIMITER ;