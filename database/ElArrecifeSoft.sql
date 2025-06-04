CREATE DATABASE IF NOT EXISTS SoftArrecife;
USE softarrecife;

CREATE TABLE IF NOT EXISTS categorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS subcategorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    categoria_id INT,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

CREATE TABLE IF NOT EXISTS productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    precio DECIMAL(10,2),
    categoria_id INT NULL,
    subcategoria_id INT NULL,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    FOREIGN KEY (subcategoria_id) REFERENCES subcategorias(id)
);

CREATE TABLE IF NOT EXISTS ventas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME DEFAULT NOW(),
    total DECIMAL(10,2),
    metodo_pago VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS detalle_venta (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT,
    producto_id INT,
    cantidad INT,
    subtotal DECIMAL(10,2),
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE IF NOT EXISTS mesas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero INT,
    estado VARCHAR(20) DEFAULT 'abierta'
);

CREATE TABLE IF NOT EXISTS pedidos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mesa_id INT,
    producto_id INT,
    cantidad INT,
    FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL, -- Almacena la contraseña hasheada
    rol VARCHAR(50) DEFAULT 'empleado' -- 'admin', 'empleado'
);

-- Categorias
INSERT INTO categorias VALUES ('1','RON');
INSERT INTO categorias VALUES ('2', 'TEQUILA');
INSERT INTO categorias VALUES ('3', 'WHISKY');
INSERT INTO categorias VALUES ('4', 'VODKA');
INSERT INTO categorias VALUES ('5', 'CERVEZA');
INSERT INTO categorias VALUES ('6', 'GINEBRA');
INSERT INTO categorias VALUES ('7', 'OTROS');

-- Subcategorias
INSERT INTO subcategorias VALUES ('1', 'Mojitos', '1');
INSERT INTO subcategorias VALUES ('2', 'Daiquiris', '1');
INSERT INTO subcategorias VALUES ('3', 'Margaritas', '2');
INSERT INTO subcategorias VALUES ('4', 'Chamochelas', '5');

-- Productos Ron
INSERT INTO productos VALUES ('1', 'Bacardi Lt', '150.00', '1', null);
INSERT INTO productos VALUES ('2', 'Flor 4 Lt', '150.00', '1', null);
INSERT INTO productos VALUES ('3', 'Flor 7 Lt',  '220.00', '1', null);
INSERT INTO productos VALUES ('4', 'Bull', '150.00', '1', null);
INSERT INTO productos VALUES ('5', 'Piña Colada', '150.00', '1', null);
INSERT INTO productos VALUES ('6', 'Medias de Seda', '150.00', '1', null);
INSERT INTO productos VALUES ('7', 'Mojito Clasico', '150.00', '1', '1');
INSERT INTO productos VALUES ('8', 'Mojito Fresa', '150.00', '1', '1');
INSERT INTO productos VALUES ('9', 'Mojito Mango', '150.00', '1', '1');
INSERT INTO productos VALUES ('10', 'Daiquiri Limon', '150.00', '1', '2');
INSERT INTO productos VALUES ('11', 'Daiquiri Mango', '150.00', '1', '2');
INSERT INTO productos VALUES ('12', 'Daiquiri Fresa', '150.00', '1', '2');

-- Productos Tequila
INSERT INTO productos VALUES ('13', 'Dobel Lt', '280.00', '2', null);
INSERT INTO productos VALUES ('14', 'Don Julio 70 Lt', '280.00', '2', null);
INSERT INTO productos VALUES ('15', 'Tradicional Lt', '220.00', '2', null);
INSERT INTO productos VALUES ('16', 'Tequila Sunrise', '150.00', '2', null);
INSERT INTO productos VALUES ('17', 'Vampiro', '150.00', '2', null);
INSERT INTO productos VALUES ('18', 'Paloma', '150.00', '2', null);
INSERT INTO productos VALUES ('19', 'Margarita Clasica', '150.00', '2', '3');
INSERT INTO productos VALUES ('20', 'Margarita Chamoy', '150.00', '2', '3');
INSERT INTO productos VALUES ('21', 'Margarita Mango', '150.00', '2', '3');
INSERT INTO productos VALUES ('22', 'Margarita Fresa', '150.00', '2', '3');
INSERT INTO productos VALUES ('23', 'Margarita Tamarindo', '150.00', '2', '3');

-- Productos Whisky
INSERT INTO productos VALUES ('24', 'Buchanans Lt', '280.00', '3', null);
INSERT INTO productos VALUES ('25', 'Black Label Lt', '280.00', '3', null);
INSERT INTO productos VALUES ('26', 'Red Label Lt', '150.00', '3', null);
INSERT INTO productos VALUES ('27', 'B&W Label Lt', '150.00', '3', null);

-- Productos Vodka
INSERT INTO productos VALUES ('28', 'Sangria', '150.00', '4', null);
INSERT INTO productos VALUES ('29', 'Baby Mango', '150.00', '4', null);
INSERT INTO productos VALUES ('30', 'Azulito', '150.00', '4', null);

-- Productos Cerveza
INSERT INTO productos VALUES ('31', 'Michelada', '140.00', '5', null);
INSERT INTO productos VALUES ('32', 'Beermato', '140.00', '5', null);
INSERT INTO productos VALUES ('33', 'Chamochela', '140.00', '5', null);
INSERT INTO productos VALUES ('34', 'Mangochela', '140.00', '5', null);
INSERT INTO productos VALUES ('35', 'Fresachela', '140.00', '5', null);
INSERT INTO productos VALUES ('36', 'Tamarindochela', '140.00', '5', null);

-- Productos Ginebra
INSERT INTO productos VALUES ('37', 'Naranjita', '150.00', '6', null);
INSERT INTO productos VALUES ('38', 'Tampico Madero', '150.00', '6', null);
INSERT INTO productos VALUES ('39', 'Tom Collins', '150.00', '6', null);
INSERT INTO productos VALUES ('40', 'Gin Tonic', '150.00', '6', null);
INSERT INTO productos VALUES ('41', 'Bombay Lt', '220.00', '6', null);

-- Productos Otros
INSERT INTO productos VALUES ('42', 'Piña Baileys', '220', '7', null);
INSERT INTO productos VALUES ('43', 'Ruso Blanco', '150', '7', null);
INSERT INTO productos VALUES ('44', 'Carajillo', '220', '7', null);
INSERT INTO productos VALUES ('45', 'Aperol Spritz', '220', '7', null);

-- Usuarios
INSERT INTO usuarios (username, password_hash, rol) VALUES ('admin', 'admin', 'admin');
INSERT INTO usuarios (username, password_hash, rol) VALUES ('Mesero', '12345', 'empleado');

ALTER TABLE mesas ADD COLUMN nombre_mesa VARCHAR(50) NULL;
UPDATE mesas SET nombre_mesa = CONCAT('Mesa ', numero); -- Para inicializar los nombres existentes

-- Primero, asegurémonos de que ningún producto esté asignado a esta subcategoría si existe
UPDATE productos SET subcategoria_id = NULL WHERE subcategoria_id = (SELECT id FROM subcategorias WHERE nombre = 'Chamochelas');

-- Luego, eliminamos la subcategoría
DELETE FROM subcategorias WHERE nombre = 'Chamochelas' AND categoria_id = (SELECT id FROM categorias WHERE nombre = 'CERVEZA');