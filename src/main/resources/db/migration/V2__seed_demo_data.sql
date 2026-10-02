-- Datos de ejemplo para poder ver el dashboard funcionando apenas se levanta la app.
-- Las fechas de los gastos se calculan relativas al mes actual (date_trunc) para que
-- el dashboard siempre muestre movimientos "del mes" sin importar cuando se corra la app.

insert into users (name, email, password) values
    ('Usuario Demo', 'demo@budgettracker.com', '$2a$10$KUOaOUlum90BwZIHnx8eqehFOssH/7s24z3o5nsj3OcapBHfCv0MG');
-- password en texto plano: demo1234

insert into categories (name, monthly_limit) values
    ('Comida', 500.00),
    ('Transporte', 150.00),
    ('Entretenimiento', 100.00),
    ('Servicios', 300.00),
    ('Salud', 200.00);

insert into expenses (user_id, category_id, amount, description, date)
select u.id, c.id, v.amount, v.description, date_trunc('month', current_date)::date + v.day_offset
from (values
    ('Comida', 120.00, 'Supermercado', 1),
    ('Comida', 85.50, 'Verduleria', 3),
    ('Comida', 55.00, 'Delivery', 5),
    ('Transporte', 40.00, 'Nafta', 2),
    ('Entretenimiento', 70.00, 'Cine y salidas', 4),
    ('Entretenimiento', 50.00, 'Streaming anual', 6),
    ('Servicios', 300.00, 'Alquiler internet + luz', 1)
) as v(category_name, amount, description, day_offset)
join categories c on c.name = v.category_name
join users u on u.email = 'demo@budgettracker.com';
