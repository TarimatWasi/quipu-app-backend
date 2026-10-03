-- RF-16 busca la cuenta por correo sin distinguir mayúsculas. El UNIQUE de V1 sí las distingue: dos
-- filas "A@x" y "a@x" harían fallar esa búsqueda. Este índice lo impide para que el correo sea único
-- de verdad.
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

-- El restablecimiento busca la cuenta por el hash del código, en un endpoint público: sin índice cada
-- intento recorrería toda la tabla. Es único (un código pertenece a una sola cuenta) y parcial (la
-- mayoría de las filas no tiene código).
CREATE UNIQUE INDEX uq_users_reset_token_hash ON users (reset_token_hash) WHERE reset_token_hash IS NOT NULL;
