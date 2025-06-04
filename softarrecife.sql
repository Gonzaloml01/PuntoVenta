DROP DATABASE IF EXISTS softarrecife;

-- Crea la base de datos si no existe
CREATE DATABASE IF NOT EXISTS softarrecife;

-- Selecciona la base de datos para usarla
USE softarrecife;

-- Tabla para almacenar usuarios del sistema
CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL, -- Para almacenar contraseñas hasheadas (ej. BCrypt)
    rol VARCHAR(50) DEFAULT 'empleado' -- Ej: 'admin', 'empleado'
);

-- Tabla para almacenar categorías de productos
CREATE TABLE IF NOT EXISTS categorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

-- Tabla para almacenar subcategorías de productos
CREATE TABLE IF NOT EXISTS subcategorias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    categoria_id INT,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- Tabla para almacenar productos
CREATE TABLE IF NOT EXISTS productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    precio DECIMAL(10,2) NOT NULL,
    categoria_id INT NULL,
    subcategoria_id INT NULL,
    stock INT DEFAULT 0, -- Se añade la columna stock
    FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    FOREIGN KEY (subcategoria_id) REFERENCES subcategorias(id)
);

-- Tabla para registrar ventas directas (sin mesa)
CREATE TABLE IF NOT EXISTS ventas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME DEFAULT NOW(),
    total DECIMAL(10,2) NOT NULL,
    metodo_pago VARCHAR(20) NOT NULL, -- Ej: 'Efectivo', 'Tarjeta', 'Transferencia'
    usuario_id INT, -- Quién realizó la venta (opcional, pero útil)
    turno_id INT NULL, -- Vínculo al turno si es aplicable (para ventas directas)
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
    -- FOREIGN KEY (turno_id) REFERENCES turnos(id) -- Se añade después de crear la tabla turnos
);

-- Tabla para el detalle de cada venta (qué productos se vendieron en cada venta)
CREATE TABLE IF NOT EXISTS detalle_venta (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Tabla para almacenar mesas del comedor
CREATE TABLE IF NOT EXISTS mesas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero INT NOT NULL UNIQUE, -- Número de mesa (ej. 1, 2, 3)
    nombre_mesa VARCHAR(100) DEFAULT 'Mesa', -- Nombre personalizable (ej. "Mesa 5", "Terraza 1")
    estado VARCHAR(20) DEFAULT 'cerrada' -- 'abierta', 'cerrada', 'ocupada', etc.
);

-- Tabla para los pedidos de las mesas (productos asociados a una mesa)
CREATE TABLE IF NOT EXISTS pedidos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mesa_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    UNIQUE (mesa_id, producto_id) -- Asegura que un producto solo aparece una vez por mesa
);

-- Tabla para registrar turnos de trabajo
CREATE TABLE IF NOT EXISTS turnos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha_apertura DATETIME DEFAULT NOW(),
    usuario_apertura_id INT, -- Quién abrió el turno
    fondo_inicial DECIMAL(10,2) DEFAULT 0.00, -- Efectivo con el que se inicia el turno

    fecha_cierre DATETIME NULL,
    usuario_cierre_id INT NULL, -- Quién cerró el turno
    ventas_efectivo DECIMAL(10,2) DEFAULT 0.00,
    ventas_tarjeta DECIMAL(10,2) DEFAULT 0.00,
    ventas_transferencia DECIMAL(10,2) DEFAULT 0.00,
    total_ventas DECIMAL(10,2) DEFAULT 0.00,

    retiros_efectivo DECIMAL(10,2) DEFAULT 0.00, -- Suma de los retiros/salidas de efectivo
    depositos_efectivo DECIMAL(10,2) DEFAULT 0.00, -- Suma de los depósitos/entradas de efectivo (si los hay)

    total_caja_esperado DECIMAL(10,2) DEFAULT 0.00, -- Cálculo del sistema
    total_caja_final DECIMAL(10,2) NULL, -- Efectivo contado al cierre
    diferencia_caja DECIMAL(10,2) DEFAULT 0.00, -- Diferencia entre esperado y final

    FOREIGN KEY (usuario_apertura_id) REFERENCES usuarios(id),
    FOREIGN KEY (usuario_cierre_id) REFERENCES usuarios(id)
);

-- Añadir FK a la tabla 'ventas' para vincular con turnos
ALTER TABLE ventas ADD FOREIGN KEY (turno_id) REFERENCES turnos(id);


-- Tabla para registrar movimientos de efectivo dentro del turno (retiros/depositos)
CREATE TABLE IF NOT EXISTS movimientos_caja (
    id INT AUTO_INCREMENT PRIMARY KEY,
    turno_id INT NOT NULL,
    tipo VARCHAR(20) NOT NULL, -- 'Retiro' o 'Deposito'
    monto DECIMAL(10,2) NOT NULL,
    fecha DATETIME DEFAULT NOW(),
    descripcion VARCHAR(255),
    usuario_id INT, -- Quién realizó el movimiento
    FOREIGN KEY (turno_id) REFERENCES turnos(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);


--
-- DATOS DE EJEMPLO
--

-- Usuarios de ejemplo (contraseña simple, ¡en producción usa bcrypt!)
-- admin/adminpass
-- empleado/emppass
INSERT INTO usuarios (username, password_hash, rol) VALUES
('admin', 'adminpass', 'admin'),
('empleado', 'emppass', 'empleado');

-- Categorías de ejemplo
INSERT INTO categorias (nombre) VALUES
('Cervezas'),
('Coctelería'),
('Refrescos'),
('Alimentos'),
('Licores');

-- Subcategorías de ejemplo
INSERT INTO subcategorias (nombre, categoria_id) VALUES
('Nacionales', (SELECT id FROM categorias WHERE nombre = 'Cervezas')),
('Importadas', (SELECT id FROM categorias WHERE nombre = 'Cervezas')),
('Con Alcohol', (SELECT id FROM categorias WHERE nombre = 'Coctelería')),
('Sin Alcohol', (SELECT id FROM categorias WHERE nombre = 'Coctelería')),
('Comida Rápida', (SELECT id FROM categorias WHERE nombre = 'Alimentos')),
('Platos Fuertes', (SELECT id FROM categorias WHERE nombre = 'Alimentos')),
('Postres', (SELECT id FROM categorias WHERE nombre = 'Alimentos')),
('Tequila', (SELECT id FROM categorias WHERE nombre = 'Licores')),
('Whisky', (SELECT id FROM categorias WHERE nombre = 'Licores')),
('Ron', (SELECT id FROM categorias WHERE nombre = 'Licores')),
('Vodka', (SELECT id FROM categorias WHERE nombre = 'Licores')),
('Ginebra', (SELECT id FROM categorias WHERE nombre = 'Licores'));


-- Productos de ejemplo con categorías y subcategorías
INSERT INTO productos (nombre, precio, categoria_id, subcategoria_id, stock) VALUES
-- Cervezas
('Corona', 45.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), (SELECT id FROM subcategorias WHERE nombre = 'Nacionales'), 100),
('Modelo Especial', 48.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), (SELECT id FROM subcategorias WHERE nombre = 'Nacionales'), 100),
('Heineken', 60.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), (SELECT id FROM subcategorias WHERE nombre = 'Importadas'), 50),
('Stella Artois', 65.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), (SELECT id FROM subcategorias WHERE nombre = 'Importadas'), 50),
('Michelada', 140.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), null, 80), -- Sin subcategoría directa
('Beermato', 140.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), null, 80),
('Chamochela', 140.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), null, 80),
('Mangochela', 140.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), null, 80),
('Fresachela', 140.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), null, 80),
('Tamarindochela', 140.00, (SELECT id FROM categorias WHERE nombre = 'Cervezas'), null, 80),

-- Coctelería
('Margarita', 120.00, (SELECT id FROM categorias WHERE nombre = 'Coctelería'), (SELECT id FROM subcategorias WHERE nombre = 'Con Alcohol'), 70),
('Mojito', 110.00, (SELECT id FROM categorias WHERE nombre = 'Coctelería'), (SELECT id FROM subcategorias WHERE nombre = 'Con Alcohol'), 70),
('Piña Colada', 130.00, (SELECT id FROM categorias WHERE nombre = 'Coctelería'), (SELECT id FROM subcategorias WHERE nombre = 'Con Alcohol'), 60),
('Limonada Mineral', 50.00, (SELECT id FROM categorias WHERE nombre = 'Coctelería'), (SELECT id FROM subcategorias WHERE nombre = 'Sin Alcohol'), 100),
('Clericot sin Alcohol', 80.00, (SELECT id FROM categorias WHERE nombre = 'Coctelería'), (SELECT id FROM subcategorias WHERE nombre = 'Sin Alcohol'), 40),

-- Refrescos
('Coca-Cola', 35.00, (SELECT id FROM categorias WHERE nombre = 'Refrescos'), null, 150),
('Agua Mineral', 30.00, (SELECT id FROM categorias WHERE nombre = 'Refrescos'), null, 150),
('Jugo de Naranja', 40.00, (SELECT id FROM categorias WHERE nombre = 'Refrescos'), null, 80),

-- Alimentos
('Hamburguesa Clásica', 150.00, (SELECT id FROM categorias WHERE nombre = 'Alimentos'), (SELECT id FROM subcategorias WHERE nombre = 'Comida Rápida'), 50),
('Papas a la Francesa', 70.00, (SELECT id FROM categorias WHERE nombre = 'Alimentos'), (SELECT id FROM subcategorias WHERE nombre = 'Comida Rápida'), 80),
('Ensalada César', 130.00, (SELECT id FROM categorias WHERE nombre = 'Alimentos'), (SELECT id FROM subcategorias WHERE nombre = 'Platos Fuertes'), 30),
('Brownie con Nieve', 90.00, (SELECT id FROM categorias WHERE nombre = 'Alimentos'), (SELECT id FROM subcategorias WHERE nombre = 'Postres'), 40),

-- Licores
('Don Julio Reposado', 800.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Tequila'), 20),
('Maestro Dobel Diamante', 950.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Tequila'), 15),
('Etiqueta Roja', 150.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Whisky'), 30),
('Red Label Lt', 150.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Whisky'), 25),
('B&W Label Lt', 150.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Whisky'), 25),
('Bacardi Blanco', 350.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Ron'), 40),
('Appleton Estate', 500.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Ron'), 20),
('Absolut Azul', 400.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Vodka'), 35),
('Stolichnaya', 380.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Vodka'), 30),
('Sangria', 150.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Vodka'), 40), -- Podría ser un cóctel, pero aquí lo ponemos como "vodka preparado"
('Baby Mango', 150.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Vodka'), 40),
('Azulito', 150.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Vodka'), 40),
('Bombay Sapphire', 600.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Ginebra'), 25),
('Hendricks', 850.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Ginebra'), 18),
('Naranjita', 150.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Ginebra'), 30),
('Tampico Madero', 150.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Ginebra'), 30),
('Tom Collins', 150.00, (SELECT id FROM categorias WHERE nombre = 'Licores'), (SELECT id FROM subcategorias WHERE nombre = 'Ginebra'), 30);


-- Mesas de ejemplo (todas 'cerradas' al inicio)
INSERT INTO mesas (numero, nombre_mesa, estado) VALUES
(1, 'Mesa 1', 'cerrada'),
(2, 'Mesa 2', 'cerrada'),
(3, 'Mesa 3', 'cerrada'),
(4, 'Mesa 4', 'cerrada'),
(5, 'Barra 1', 'cerrada');