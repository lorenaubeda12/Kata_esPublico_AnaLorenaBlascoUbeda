-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 13-09-2023 a las 12:11:59
-- Versión del servidor: 10.4.24-MariaDB
-- Versión de PHP: 8.1.6

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `gestion_pedidos`
--
CREATE DATABASE IF NOT EXISTS `gestion_pedidos` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `gestion_pedidos`;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `orders`
--

CREATE TABLE `orders` (
  `order_ID` varchar(60) NOT NULL,
  `order_Priority` varchar(60) NOT NULL,
  `order_Date` date NOT NULL,
  `region` varchar(60) NOT NULL,
  `country` varchar(60) NOT NULL,
  `itemType` varchar(60) NOT NULL,
  `salesChannel` varchar(60) NOT NULL,
  `shipDate` date NOT NULL,
  `unitsSold` varchar(60) NOT NULL,
  `unitPrice` double NOT NULL,
  `unitCost` double NOT NULL,
  `totalRevenue` double NOT NULL,
  `totalCost` double NOT NULL,
  `totalProfit` double NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
