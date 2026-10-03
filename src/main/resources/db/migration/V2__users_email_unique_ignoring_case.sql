-- RF-16 busca la cuenta por correo sin distinguir mayúsculas. El UNIQUE de V1 sí las distingue: dos
-- filas "A@x" y "a@x" harían fallar esa búsqueda. Este índice lo impide para que el correo sea único
-- de verdad.
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));
